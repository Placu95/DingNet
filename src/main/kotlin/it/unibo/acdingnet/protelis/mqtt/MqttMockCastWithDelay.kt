package it.unibo.acdingnet.protelis.mqtt

import com.google.gson.JsonDeserializer
import com.google.gson.JsonSerializer
import flexjson.JSONSerializer
import iot.GlobalClock
import iot.mqtt.TransmissionWrapper
import it.unibo.acdingnet.protelis.physicalnetwork.PhysicalNetwork
import it.unibo.mqttclientwrapper.api.MqttClientBasicApi
import it.unibo.mqttclientwrapper.mock.cast.MqttMockCast
import org.protelis.lang.datatype.DeviceUID

class MqttMockCastWithDelay(
    private val physicalNetwork: PhysicalNetwork,
    private val clock: GlobalClock,
    private val deviceUID: DeviceUID
) : MqttMockCast() {

    private val jsonSerializer = JSONSerializer()

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

    private fun <E> computeLength(message: E) = jsonSerializer.deepSerialize(
        when (message) {
            is LoRaTransmissionWrapper -> message.transmission.content
            is TransmissionWrapper -> message.transmission.content
            else -> message
        }
    ).length * Char.SIZE_BITS

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
