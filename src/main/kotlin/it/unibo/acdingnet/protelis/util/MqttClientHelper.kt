package it.unibo.acdingnet.protelis.util

import iot.mqtt.MQTTClientFactory
import it.unibo.mqttclientwrapper.api.MqttClientBasicApi

object MqttClientHelper {

    fun addLoRaWANAdapters(mqttClient: MqttClientBasicApi): MqttClientBasicApi {
        MQTTClientFactory.getDeserializers().
            forEach { mqttClient.addDeserializer(it.clazz, it.deserializer) }
        MQTTClientFactory.getSerializers().
            forEach { mqttClient.addSerializer(it.clazz, it.serializer) }
        return mqttClient
    }
}
