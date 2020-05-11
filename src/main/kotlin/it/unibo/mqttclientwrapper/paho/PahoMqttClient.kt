package it.unibo.mqttclientwrapper.paho

import com.google.gson.*
import it.unibo.mqttclientwrapper.api.MqttClientBasicApi
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.lang.reflect.Type
import java.nio.charset.StandardCharsets
import java.util.concurrent.CopyOnWriteArrayList

/**
 * [MqttClientBasicApi] for a real mqtt server. This implementation is based on the library Paho.
 */
open class PahoMqttClient @JvmOverloads constructor(
    address: String = "tcp://localhost:1883",
    clientId: String = "MqttClientWrapperLibrary",
    protected var gson: Gson = GsonBuilder().create()
) :
    MqttClientBasicApi {
    private val mqttClient: MqttClient
    private val subscribed: MutableMap<String, MutableList<MqttMessageConsumer<*>>>

    init {
        try {
            mqttClient = MqttClient(address, clientId, MemoryPersistence())
            connect()
            subscribed = mutableMapOf()
        } catch (e: MqttException) {
            throw e
        }
    }

    override fun connect() {
        if (!mqttClient.isConnected) {
            try {
                mqttClient.connect(MqttConnectOptions().also { it.isCleanSession = true })
            } catch (e: MqttException) {
                e.printStackTrace()
            }
        }
    }

    override fun disconnect() {
        try {
            if (mqttClient.isConnected) {
                mqttClient.disconnect()
            }
            subscribed.clear()
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    override fun close() {
        if (mqttClient.isConnected) {
            mqttClient.close()
        }
    }

    override fun publish(topic: String, message: Any) {
        try {
            if (!mqttClient.isConnected) {
                connect()
            }
            mqttClient.publish(topic, MqttMessage(gson.toJson(message).toByteArray(StandardCharsets.US_ASCII)))
        } catch (e: MqttException) {
            e.printStackTrace()
        }
    }

    override fun <T> subscribe(
        subscriber: Any,
        topicFilter: String,
        classMessage: Class<T>,
        messageConsumer: (topic: String, message: T) -> Unit
    ) {
        if (!subscribed.containsKey(topicFilter)) {
            subscribed[topicFilter] = CopyOnWriteArrayList()
            try {
                mqttClient.subscribe(topicFilter) { topic, msg ->
                    subscribed[topicFilter]?.forEach { it.accept(topic, msg.toString()) }
                }
            } catch (e: MqttException) {
                e.printStackTrace()
            }
        }
        subscribed[topicFilter]?.add(MqttMessageConsumer(subscriber, messageConsumer, classMessage))
    }

    override fun unsubscribe(subscriber: Any, topicFilter: String) {
        subscribed[topicFilter]?.removeIf { it.subscriber == subscriber }
        if (subscribed[topicFilter]!!.isEmpty()) {
            try {
                mqttClient.unsubscribe(topicFilter)
                subscribed.remove(topicFilter)
            } catch (e: MqttException) {
                e.printStackTrace()
            }
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

    private inner class MqttMessageConsumer<T>(
        val subscriber: Any,
        private val consumer: (topic: String, message: T) -> Unit,
        private val clazz: Class<T>
    ) {
        fun accept(t: String, message: String) {
            consumer.invoke(t, gson.fromJson(message, clazz))
        }
    }
}
