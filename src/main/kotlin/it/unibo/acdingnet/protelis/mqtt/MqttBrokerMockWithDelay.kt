package it.unibo.acdingnet.protelis.mqtt

import flexjson.JSONSerializer
import iot.GlobalClock
import iot.mqtt.TransmissionWrapper
import it.unibo.acdingnet.protelis.physicalnetwork.NetworkStatistic
import it.unibo.acdingnet.protelis.physicalnetwork.PhysicalNetwork
import it.unibo.acdingnet.protelis.util.Utils
import it.unibo.acdingnet.protelis.util.Utils.max
import it.unibo.mqttclientwrapper.mock.cast.MqttBrokerMock
import org.protelis.lang.datatype.DeviceUID
import util.time.DoubleTime
import util.time.Time
import util.time.TimeUnit

class MqttBrokerMockWithDelay(
    val clock: GlobalClock,
    private val physicalNetwork: PhysicalNetwork
) : MqttBrokerMock() {

    private val jsonSerializer = JSONSerializer()
    private val hostBroker by lazy { physicalNetwork.hostBroker }

    private fun getIncomingQueue(): MutableList<IncomingMessage> = physicalNetwork.incomingQueue
    private fun getSendingQueueFreeFrom(): Time = physicalNetwork.sendingQueueFreeFrom
    private fun setSendingQueueFreeFrom(t: Time) {
        physicalNetwork.sendingQueueFreeFrom = t
    }

    fun publish(sender: DeviceUID, topic: String, message: Any) {
        val rtt = physicalNetwork.computeRTTwithBrokerHost(sender)
        val msgSize = computeLength(message)
        val bwReal = hostBroker.getDataRate() / (getIncomingQueue().size + 1)

        refreshTEnd(bwReal)

        val tEnd = clock.time + rtt + DoubleTime(msgSize / bwReal, TimeUnit.SECONDS)
        val msgId = IncomingMessage.getNextId()
        val triggerHandler = msgArrivedToTheBroker(msgId, topic, message)
        val idTrigger = clock.addTriggerOneShot(tEnd, triggerHandler)
        getIncomingQueue().add(IncomingMessage(msgId, clock.time, rtt, msgSize, tEnd, idTrigger, triggerHandler))
    }

    // for each incoming msg recompute new t_end and if it is changed reschedule the trigger
    private fun refreshTEnd(bwReal: Double) = getIncomingQueue().forEach {
        val tEndNew = it.tSend + it.delay + DoubleTime(it.msgSize / bwReal, TimeUnit.SECONDS)
        // t_endNew.isAfter(clock.time) is only for safety, but it shouldn't happen
        if (tEndNew.isAfter(clock.time)) {
            clock.removeTrigger(it.tEnd, it.idTrigger)
            it.idTrigger = clock.addTriggerOneShot(tEndNew, it.triggerHandler)
            it.tEnd = tEndNew
        }
    }

    private fun msgArrivedToTheBroker(msgId: Long, topic: String, message: Any) = { ->
        // get my incoming msg from the queue
        val myEntry = getIncomingQueue().first { it.id == msgId }
        if (!getIncomingQueue().remove(myEntry)) {
            throw IllegalStateException("I'm not in the incoming queue :(")
        }
        // add statistic for communication client to broker
        NetworkStatistic.addDelay(myEntry.tEnd - myEntry.tSend, NetworkStatistic.Type.UPLOAD)
        // find all the receivers
        val receivers: List<Pair<String, MqttMockCastWithDelay>> = subscription.asSequence()
            .filter { (key, _) -> checkTopicMatch(topic, key) }
            .filterIsInstance<Map.Entry<String, MutableSet<MqttMockCastWithDelay>>>()
            .flatMap { (actualTopic, subscribers) ->
                subscribers.asSequence().map { client -> actualTopic to client }
            }
            .toList()
        val occupationChannel = DoubleTime(
            myEntry.msgSize / (hostBroker.getDataRate() / receivers.size), TimeUnit.SECONDS
        )
        val finishToSend = Utils.maxTime(clock.time, getSendingQueueFreeFrom()) + occupationChannel
        var maxDelay = DoubleTime.zero()
        receivers.forEach { (actualTopic, subscriber) ->
            val receiverRTT = physicalNetwork.computeRTTwithBrokerHost(subscriber.deviceUID)
            maxDelay = maxDelay.max(receiverRTT)
            // when the message is arrived to the receiver
            val msgReceivedTime = finishToSend + receiverRTT
            // dispatch the message to the receiver
            clock.addTriggerOneShot(msgReceivedTime) {
                subscriber.dispatch(actualTopic, topic, message)
            }
            NetworkStatistic.addDelay(msgReceivedTime - clock.time, NetworkStatistic.Type.DOWNLOAD)
        }
        NetworkStatistic.addDelay(finishToSend + maxDelay - clock.time, NetworkStatistic.Type.DOWNLOAD_MAX)
        setSendingQueueFreeFrom(finishToSend)
    }

    private fun <E> computeLength(message: E) = jsonSerializer.deepSerialize(
        when (message) {
            is LoRaTransmissionWrapper -> message.transmission.content
            is TransmissionWrapper -> message.transmission.content
            else -> message
        }
    ).length * Char.SIZE_BITS
}
