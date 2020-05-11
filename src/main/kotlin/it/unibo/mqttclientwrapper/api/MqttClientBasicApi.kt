package it.unibo.mqttclientwrapper.api

import com.google.gson.*
import java.lang.reflect.Type
import java.util.function.BiConsumer

/**
 * Interface with basic API for a iot.mqtt client
 */
interface MqttClientBasicApi {
    /**
     * To connect to the server
     */
    fun connect()

    /**
     * To disconnect to server
     */
    fun disconnect()

    /**
     * To close the connection with the server and release all the resources.
     * After this method is possible the you can't connect anymore to the server.
     */
    fun close()

    /**
     * Publish a message with a specified topic
     * @param topic the message topic
     * @param message the message
     */
    fun publish(topic: String, message: Any)

    /**
     * Subscribe to all the topic that start with topicFilter
     * @param subscriber instance oh the subscriber
     * @param topicFilter the topic filter with support to the wildcard '+' and '#'
     * @param classMessage the class of the message that will be receive
     * @param messageConsumer consumer for the message already converted to the required class
     * @param <T> Type of the received message on this topic
    </T> */
    fun <T> subscribe(
        subscriber: Any,
        topicFilter: String,
        classMessage: Class<T>,
        messageConsumer: (topic: String, message: T) -> Unit
    )

    /**
     * For Java interoperability to avoid to return Unit.INSTANCE
     * Subscribe to all the topic that start with topicFilter
     * @param subscriber instance oh the subscriber
     * @param topicFilter the topic filter with support to the wildcard '+' and '#'
     * @param classMessage the class of the message that will be receive
     * @param messageConsumer consumer for the message already converted to the required class
     * @param <T> Type of the received message on this topic
    </T> */
    fun <T> subscribe(
        subscriber: Any,
        topicFilter: String,
        classMessage: Class<T>,
        messageConsumer: BiConsumer<String, T>
    ) = subscribe(subscriber, topicFilter, classMessage) { topic: String, message: T ->
        messageConsumer.accept(topic, message) }

    /**
     * Unsubscribe a topic previous subscribed
     * @param subscriber the subscriber
     * @param topicFilter the topic previous subscribed
     */
    fun unsubscribe(subscriber: Any, topicFilter: String)

    /**
     * Add new serializer for the specified class.
     * If a serializer si already present, this will overwrite it
     * @param clazz class of the the object to serialize
     * @param serializer the serializer to use
     * @param T the type of the the object to serialize
     * @return this
     * @throws UnsupportedOperationException if the client do not support this operation
     */
    @Throws(UnsupportedOperationException::class)
    fun <T> addSerializer(clazz: Class<T>, serializer: JsonSerializer<T>): MqttClientBasicApi

    /**
     * Add new serializer for the specified class.
     * If a serializer si already present, this will overwrite it
     * @param clazz class of the the object to serialize
     * @param serializer the serializer to use
     * @param T the type of the the object to serialize
     * @return this
     * @throws UnsupportedOperationException if the client do not support this operation
     */
    @Throws(UnsupportedOperationException::class)
    fun <T> addSerializer(
        clazz: Class<T>,
        serializer: (T, Type, JsonSerializationContext) -> JsonElement
    ): MqttClientBasicApi

    /**
     * Add new deserializer for the specified class.
     * If a deserializer si already present, this will overwrite it
     * @param clazz class of the the object to deserialize
     * @param deserializer the deserializer to use
     * @param T the type of the the object to deserialize
     * @return this
     * @throws UnsupportedOperationException if the client do not support this operation
     */
    @Throws(UnsupportedOperationException::class)
    fun <T> addDeserializer(clazz: Class<T>, deserializer: JsonDeserializer<T>): MqttClientBasicApi

    /**
     * Add new deserializer for the specified class.
     * If a deserializer si already present, this will overwrite it
     * @param clazz class of the the object to deserialize
     * @param deserializer the deserializer to use
     * @param T the type of the the object to deserialize
     * @return this
     * @throws UnsupportedOperationException if the client do not support this operation
     */
    @Throws(UnsupportedOperationException::class)
    fun <T> addDeserializer(
        clazz: Class<T>,
        deserializer: (JsonElement, Type, JsonDeserializationContext) -> T
    ): MqttClientBasicApi

    /**
     * Add new serializer for the specified class, only if it is absent.
     * @param clazz class of the the object to serialize
     * @param serializer the serializer to use
     * @param T the type of the the object to serialize
     * @return this
     * @throws UnsupportedOperationException if the client do not support this operation
     */
    @Throws(UnsupportedOperationException::class)
    fun <T> addSerializerIfAbsent(
        clazz: Class<T>,
        serializer: JsonSerializer<T>
    ): MqttClientBasicApi = TODO("not implemented yet")

    /**
     * Add new serializer for the specified class, only if it is absent.
     * @param clazz class of the the object to serialize
     * @param serializer the serializer to use
     * @param T the type of the the object to serialize
     * @return this
     * @throws UnsupportedOperationException if the client do not support this operation
     */
    @Throws(UnsupportedOperationException::class)
    fun <T> addSerializerIfAbsent(
        clazz: Class<T>,
        serializer: (T, Type, JsonSerializationContext) -> JsonElement
    ): MqttClientBasicApi = TODO("not implemented yet")

    /**
     * Add new deserializer for the specified class, only if it is absent.
     * @param clazz class of the the object to deserialize
     * @param deserializer the deserializer to use
     * @param T the type of the the object to deserialize
     * @return this
     * @throws UnsupportedOperationException if the client do not support this operation
     */
    @Throws(UnsupportedOperationException::class)
    fun <T> addDeserializerIfAbsent(
        clazz: Class<T>,
        deserializer: JsonDeserializer<T>
    ): MqttClientBasicApi = TODO("not implemented yet")

    /**
     * Add new deserializer for the specified class, only if it is absent.
     * @param clazz class of the the object to deserialize
     * @param deserializer the deserializer to use
     * @param T the type of the the object to deserialize
     * @return this
     * @throws UnsupportedOperationException if the client do not support this operation
     */
    @Throws(UnsupportedOperationException::class)
    fun <T> addDeserializerIfAbsent(clazz: Class<T>, deserializer: (JsonElement, Type, JsonDeserializationContext) -> T): MqttClientBasicApi = TODO()
}
