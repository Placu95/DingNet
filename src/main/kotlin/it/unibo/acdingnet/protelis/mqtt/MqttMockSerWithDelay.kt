package it.unibo.acdingnet.protelis.mqtt

import iot.GlobalClock
import it.unibo.acdingnet.protelis.physicalnetwork.PhysicalNetwork
import it.unibo.mqttclientwrapper.mock.serialization.MqttMockSer
import org.protelis.lang.datatype.DeviceUID

class MqttMockSerWithDelay(
    private val physicalNetwork: PhysicalNetwork,
    private val clock: GlobalClock,
    private val deviceUID: DeviceUID
) : MqttMockSer() {

    override fun publish(topic: String, message: Any) {
        val delay = physicalNetwork.delayToPublish(deviceUID)
        clock.addTriggerOneShot(
            clock.time.plusMillis(delay.asMilli())) {
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
            val delay = physicalNetwork.delayToReceive(deviceUID)
            clock.addTriggerOneShot(
                clock.time.plusMillis(delay.asMilli())) {
                messageConsumer.invoke(topic, message)
            }
        }
    }
}
