package it.unibo.acdingnet.protelis.mqtt

import iot.GlobalClock
import iot.mqtt.TransmissionWrapper
import it.unibo.acdingnet.protelis.physicalnetwork.PhysicalNetwork
import it.unibo.mqttclientwrapper.mock.serialization.MqttMockSer
import org.protelis.lang.datatype.DeviceUID

class MqttMockSerWithDelay(
    private val physicalNetwork: PhysicalNetwork,
    private val clock: GlobalClock,
    private val deviceUID: DeviceUID
) : MqttMockSer() {

    override fun publish(topic: String, message: Any) {
        val delay = physicalNetwork.delayToPublish(deviceUID, computeLength(message))
        clock.addTriggerOneShot(clock.time + delay) {
            super.publish(topic, message)
        }
    }

    override fun <T> subscribe(
        subscriber: Any,
        topicFilter: String,
        classMessage: Class<T>,
        messageConsumer: (topic: String, message: T) -> Unit
    ) {
        super.subscribe(subscriber, topicFilter, classMessage) { topic, message ->
            val delay = physicalNetwork.delayToReceive(deviceUID, computeLength(message))
            clock.addTriggerOneShot(clock.time + delay) {
                messageConsumer.invoke(topic, message)
            }
        }
    }

    private fun <E> computeLength(message: E) = gson.toJson(
        when (message) {
            is LoRaTransmissionWrapper -> message.transmission.content
            is TransmissionWrapper -> message.transmission.content
            else -> message
        }
    ).length
}
