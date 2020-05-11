package it.unibo.acdingnet.protelis.mqtt

import it.unibo.mqttclientwrapper.mock.serialization.MqttMockSer
import org.protelis.lang.datatype.DeviceUID

class MqttMockSerWithDelay(
    val deviceUID: DeviceUID,
    broker: MqttBrokerMockSerWithDelay
) : MqttMockSer() {

    init {
        disconnect()
        super.broker = broker
        connect()
    }

    override fun publish(topic: String, message: Any) {
        (broker as MqttBrokerMockSerWithDelay).publish(deviceUID, topic, gson.toJson(message))
    }
}
