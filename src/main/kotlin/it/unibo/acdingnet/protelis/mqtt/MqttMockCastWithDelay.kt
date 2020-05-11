package it.unibo.acdingnet.protelis.mqtt

import com.google.gson.JsonDeserializer
import com.google.gson.JsonSerializer
import it.unibo.mqttclientwrapper.api.MqttClientBasicApi
import it.unibo.mqttclientwrapper.mock.cast.MqttMockCast
import org.protelis.lang.datatype.DeviceUID

class MqttMockCastWithDelay(
    val deviceUID: DeviceUID,
    broker: MqttBrokerMockWithDelay
) : MqttMockCast() {

    init {
        disconnect()
        super.broker = broker
        connect()
    }

    override fun publish(topic: String, message: Any) {
        (broker as MqttBrokerMockWithDelay).publish(deviceUID, topic, message)
    }

    // override both the following method to avoid print "this method don't do nothing
    override fun <T> addSerializer(
        clazz: Class<T>,
        serializer: JsonSerializer<T>
    ): MqttClientBasicApi {
        return this
    }

    override fun <T> addDeserializer(
        clazz: Class<T>,
        deserializer: JsonDeserializer<T>
    ): MqttClientBasicApi {
        return this
    }
}
