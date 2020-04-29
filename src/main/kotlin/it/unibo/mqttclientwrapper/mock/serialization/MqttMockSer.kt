package it.unibo.mqttclientwrapper.mock.serialization

import com.google.gson.*
import it.unibo.mqttclientwrapper.api.MqttClientBasicApi
import it.unibo.mqttclientwrapper.api.MqttMessageType
import java.lang.reflect.Type

/**
 * Mock implementation of a [MqttClientBasicApi] based on cast of [MqttMessageType]
 */
open class MqttMockSer @JvmOverloads constructor (
    protected var gson: Gson = GsonBuilder().create()
) : MqttClientBasicApi {
    private val subscribed: MutableMap<String, MutableList<MqttMessageConsumer<*>>> = HashMap()
    protected var broker: MqttBrokerMockSer = MqttBrokerMockSer.instance

    init {
        connect()
    }

    override fun connect() {
        broker.connect(this)
    }

    override fun disconnect() {
        broker.disconnect(this)
        subscribed.clear()
    }

    override fun close() { }

    override fun publish(topic: String, message: Any) {
        broker.publish(topic, gson.toJson(message))
    }

    override fun <T> subscribe(subscriber: Any, topicFilter: String, classMessage: Class<T>,
                                                 messageConsumer: (topic: String, message: T) -> Unit) {
        if (!subscribed.containsKey(topicFilter)) {
            broker.subscribe(this, topicFilter)
            subscribed[topicFilter] = mutableListOf()
        }
        subscribed[topicFilter]!!.add(MqttMessageConsumer(subscriber, messageConsumer, classMessage, gson))
    }

    override fun unsubscribe(subscriber: Any, topicFilter: String) {
        subscribed[topicFilter]?.removeIf { c: MqttMessageConsumer<*> -> c.subscriber == subscriber }
        if (subscribed[topicFilter]!!.isEmpty()) {
            subscribed.remove(topicFilter)
            broker.unsubscribe(this, topicFilter)
        }
    }

    override fun <T> addSerializer(clazz: Class<T>, serializer: JsonSerializer<T>): MqttClientBasicApi {
        gson = gson.newBuilder().registerTypeAdapter(clazz, serializer).create()
        return this
    }

    override fun <T> addSerializer(
        clazz: Class<T>,
        serializer: (T, Type, JsonSerializationContext) -> JsonElement
    ): MqttClientBasicApi {
        gson = gson.newBuilder().registerTypeAdapter(clazz, serializer).create()
        return this
    }

    override fun <T> addDeserializer(clazz: Class<T>, deserializer: JsonDeserializer<T>): MqttClientBasicApi {
        gson = gson.newBuilder().registerTypeAdapter(clazz, deserializer).create()
        return this
    }

    override fun <T> addDeserializer(
        clazz: Class<T>,
        deserializer: (JsonElement, Type, JsonDeserializationContext) -> T
    ): MqttClientBasicApi {
        gson = gson.newBuilder().registerTypeAdapter(clazz, deserializer).create()
        return this
    }

    /**
     * method to deliver a message to the iot.mqtt client (used from [MqttBrokerMock])
     * @param filter the topic filter that had matched the message topic
     * @param topic the message topic
     * @param message the message
     */
    fun dispatch(filter: String, topic: String, message: String) {
        if (subscribed.containsKey(filter)) {
            subscribed[filter]?.forEach { c: MqttMessageConsumer<*> -> c.accept(topic, message) }
        }
    }

    private class MqttMessageConsumer<T>(
        val subscriber: Any,
        private val consumer: (topic: String, message: T) -> Unit,
        private val clazz: Class<T>,
        private val gson: Gson) {

        fun accept(t: String, message: String) {
            consumer.invoke(t, gson.fromJson(message, clazz))
        }
    }
}
