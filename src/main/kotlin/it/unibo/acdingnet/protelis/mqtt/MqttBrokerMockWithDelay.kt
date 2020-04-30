package it.unibo.acdingnet.protelis.mqtt

import flexjson.JSONSerializer
import iot.GlobalClock
import iot.mqtt.TransmissionWrapper
import it.unibo.acdingnet.protelis.physicalnetwork.NetworkStatistic
import it.unibo.acdingnet.protelis.physicalnetwork.PhysicalNetwork
import it.unibo.acdingnet.protelis.util.Utils
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
        val t_endNew = it.t_send + it.delay +
            DoubleTime(it.msgSize / bwReal, TimeUnit.SECONDS)
        // t_endNew.isAfter(clock.time) is only for safety, but it shouldn't happen
        if (/*t_endNew != it.t_end && */t_endNew.isAfter(clock.time)) {
            clock.removeTrigger(it.idTrigger)
            it.idTrigger = clock.addTriggerOneShot(t_endNew, it.triggerHandler)
            it.t_end = t_endNew
        }
    }

    private fun msgArrivedToTheBroker(msgId: Long, topic: String, message: Any) = { ->
        // get my incoming msg from the queue
        val myEntry = getIncomingQueue().first { it.id == msgId }
        if (!getIncomingQueue().remove(myEntry)) {
            throw IllegalStateException("I'm not in the incoming queue :(")
        }
        // add statistic for communication client to broker
        NetworkStatistic.addDelay(
            myEntry.t_end - myEntry.t_send, NetworkStatistic.Type.UPLOAD)
        // find all the receivers
        val receivers = subscription.filterKeys { checkTopicMatch(topic, it) }
        val numOfReceiver = receivers.map { it.value }.flatten().size
        val occupationChannel = DoubleTime(
            myEntry.msgSize / (hostBroker.getDataRate() / numOfReceiver),
            TimeUnit.SECONDS
        )
        val finishToSend = Utils.maxTime(clock.time, getSendingQueueFreeFrom()) +
            occupationChannel
        val receiversRTT: MutableMap<DeviceUID, Time> = mutableMapOf()
        receivers.forEach { (t, cs) ->
            cs
                .map { it as MqttMockCastWithDelay }
                .forEach {
                    val receiverRTT = physicalNetwork.computeRTTwithBrokerHost(it.deviceUID)
                    receiversRTT[it.deviceUID] = receiverRTT
                    // when the message is arrived to the receiver
                    val msgReceivedTime = finishToSend + receiverRTT
                    // dispatch the message to the receiver
                    clock.addTriggerOneShot(msgReceivedTime) {
                        it.dispatch(t, topic, message)
                    }
                    NetworkStatistic.addDelay(
                        msgReceivedTime - clock.time, NetworkStatistic.Type.DOWNLOAD)
                }
        }
        receiversRTT.values.maxWith(
            Comparator { o1, o2 -> o1.asSecond().compareTo(o2.asSecond()) }
        )?.let {
            val receivingTime = finishToSend + it
            NetworkStatistic.addDelay(
                receivingTime - clock.time, NetworkStatistic.Type.DOWNLOAD_MAX)
        }
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
