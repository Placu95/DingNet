package it.unibo.acdingnet.protelis.mqtt

import com.google.gson.GsonBuilder
import com.google.gson.JsonSerializer
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

    private var gson = GsonBuilder().create()

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

    override fun <T> addSerializer(
        clazz: Class<T>,
        serializer: JsonSerializer<T>
    ): MqttClientBasicApi {
        gson = gson.newBuilder().registerTypeAdapter(clazz, serializer).create()
        return this
    }
}
