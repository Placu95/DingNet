package it.unibo.mqttclientwrapper

import com.google.gson.*
import it.unibo.mqttclientwrapper.api.MqttClientBasicApi
import it.unibo.mqttclientwrapper.mock.cast.MqttMockCast
import it.unibo.mqttclientwrapper.mock.serialization.MqttMockSer
import it.unibo.mqttclientwrapper.paho.PahoMqttClient
import java.lang.reflect.Type

/**
 * Singleton to retrieve the instance of [MqttClientBasicApi]
 */
object MQTTClientSingleton {

    private var clientBasicApi: MqttClientBasicApi? = null

    /**
     * @return the singleton instance of [MqttClientBasicApi] of the predefined type [MqttClientType]
     */
    val instance: MqttClientBasicApi
       @JvmStatic get() {
            if (clientBasicApi == null) {
                throw IllegalStateException("Before get the singleton instance of the MQTT client, " +
                        "you have to create it with MQTTClientSingleton.ClientBuilder")
            }
            return clientBasicApi!!
        }

    @JvmStatic fun destruct() {
        clientBasicApi?.let {
            it.disconnect()
            it.close()
        }
        clientBasicApi = null
    }

    class ClientBuilder {

        private val gsonBuilder = GsonBuilder()
        private var address : String? = null
        private var clientId: String? = null

        /**
         * Set the address of the MQTT server
         * @param value server address
         * @return this
         */
        fun setAddress(value: String): ClientBuilder {
            address = value
            return this
        }

        /**
         * Set the client ID for the MQTT server
         * @param value the client ID for
         * @return this
         */
        fun setClientId(value: String): ClientBuilder {
            clientId = value
            return this
        }

        /**
         * Add new serializer for the specified class.
         * If a serializer si already present, this will overwrite it
         * @param clazz class of the the object to serialize
         * @param serializer the serializer to use
         * @param T the type of the the object to serialize
         * @return this
         */
        fun <T> addSerializer(clazz: Class<T>, serializer: JsonSerializer<T>): ClientBuilder {
            gsonBuilder.registerTypeAdapter(clazz, serializer)
            return this
        }

        /**
         * Add new serializer for the specified class.
         * If a serializer si already present, this will overwrite it
         * @param clazz class of the the object to serialize
         * @param serializer the serializer to use
         * @param T the type of the the object to serialize
         * @return this
         */
        fun <T> addSerializer(clazz: Class<T>, serializer: (T, Type, JsonSerializationContext) -> JsonElement): ClientBuilder {
            gsonBuilder.registerTypeAdapter(clazz, serializer)
            return this
        }

        /**
         * Add new deserializer for the specified class.
         * If a deserializer si already present, this will overwrite it
         * @param clazz class of the the object to deserialize
         * @param deserializer the deserializer to use
         * @param T the type of the the object to deserialize
         * @return this
         */
        fun <T> addDeserializer(clazz: Class<T>, deserializer: JsonDeserializer<T>): ClientBuilder {
            gsonBuilder.registerTypeAdapter(clazz, deserializer)
            return this
        }

        /**
         * Add new deserializer for the specified class.
         * If a deserializer si already present, this will overwrite it
         * @param clazz class of the the object to deserialize
         * @param deserializer the deserializer to use
         * @param T the type of the the object to deserialize
         * @return this
         */
        fun <T> addDeserializer(clazz: Class<T>, deserializer: (JsonElement, Type, JsonDeserializationContext) -> T): ClientBuilder {
            gsonBuilder.registerTypeAdapter(clazz, deserializer)
            return this
        }

        /**
         * Build the MQTT client singleton instance of the required type
         * @param type the type of the client
         * @return the MQTT client
         */
        fun build(type: MqttClientType): MqttClientBasicApi {
            if (clientBasicApi != null) {
                throw IllegalStateException("singleton instance already created")
            }
            clientBasicApi = when(type) {
                MqttClientType.MOCK_CAST -> MqttMockCast()
                MqttClientType.MOCK_SERIALIZATION -> MqttMockSer(gsonBuilder.create())
                MqttClientType.PAHO -> {
                    when {
                        address == null -> {
                            PahoMqttClient(gson = gsonBuilder.create())
                        }
                        clientId != null -> {
                            PahoMqttClient(address!!, clientId!!, gsonBuilder.create())
                        }
                        else -> {
                            throw java.lang.IllegalStateException("Missing client id")
                        }
                    }
                }
            }
            return clientBasicApi!!
        }

    }
}