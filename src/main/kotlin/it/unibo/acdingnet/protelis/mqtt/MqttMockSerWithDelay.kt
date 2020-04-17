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
        clock.addTriggerOneShot(
            physicalNetwork.arrivalTimeToBroker(deviceUID, computeLength(message))
        ) { super.publish(topic, message) }
    }

    override fun <T> subscribe(
        subscriber: Any,
        topicFilter: String,
        classMessage: Class<T>,
        messageConsumer: (topic: String, message: T) -> Unit
    ) {
        super.subscribe(subscriber, topicFilter, classMessage) { topic, message ->
            clock.addTriggerOneShot(
                physicalNetwork.arrivalTimeToSubscriber(deviceUID, computeLength(message))
            ) { messageConsumer.invoke(topic, message) }
        }
    }

    private fun <E> computeLength(message: E) = gson.toJson(
        when (message) {
            is LoRaTransmissionWrapper -> message.transmission.content
            is TransmissionWrapper -> message.transmission.content
            else -> message
        }
    ).length * Char.SIZE_BITS
}
