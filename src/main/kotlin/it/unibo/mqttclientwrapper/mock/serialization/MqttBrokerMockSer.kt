package it.unibo.mqttclientwrapper.mock.serialization

import it.unibo.mqttclientwrapper.api.MqttTopicConst

/**
 * Mock that represent a iot.mqtt broker
 */
open class MqttBrokerMockSer protected constructor() {
    private val clientSubscribed: MutableSet<MqttMockSer> = mutableSetOf()
    protected val subscription: MutableMap<String, MutableSet<MqttMockSer>> = mutableMapOf()

    companion object {
        /**
         * @return the singleton instance
         */
        val instance = MqttBrokerMockSer()
    }

    /**
     * Method to connect a [MqttMockSer] to this broker
     * @param instance the client
     */
    fun connect(instance: MqttMockSer) {
        check(!clientSubscribed.contains(instance))
        clientSubscribed.add(instance)
    }

    /**
     * Method to disconnect a [MqttMockSer] to this broker
     * @param instance the client
     */
    fun disconnect(instance: MqttMockSer) {
        check(clientSubscribed.contains(instance))
        clientSubscribed.remove(instance)
    }

    /**
     * method to publish a message
     * @param topic the topic of the message
     * @param message the message to publish
     */
    fun publish(topic: String, message: String) {
        subscription.filterKeys { checkTopicMatch(topic, it) }
            .forEach { it.value.forEach { c -> c.dispatch(it.key, topic, message) } }
    }

    /**
     * method to subscribe a [MqttMockSer] to a topic
     * @param instance the client
     * @param topicFilter the topic with possible wildcard
     */
    fun subscribe(instance: MqttMockSer, topicFilter: String) {
        check(clientSubscribed.contains(instance))
        subscription.getOrPut(topicFilter) { mutableSetOf() }.add(instance)
    }

    /**
     * method to unsubscribe a [MqttMockSer] to a previous subscribed topic
     * @param instance the client
     * @param topicFilter the topic previous subscribed
     */
    fun unsubscribe(instance: MqttMockSer, topicFilter: String) {
        check(clientSubscribed.contains(instance))
        check(subscription.containsKey(topicFilter))
        subscription[topicFilter]?.remove(instance)
    }

    protected fun checkTopicMatch(topic: String, filter: String): Boolean {
        if (topic == filter) {
            return true
        }
        var indexT = 0
        var indexF = 0
        while (indexF < filter.length && indexT < topic.length) {
            when (filter[indexF]) {
                topic[indexT] -> indexT++
                MqttTopicConst.WILDCARD_SINGLE_LEVEL[0] -> indexT = topic.indexOf(MqttTopicConst.LEVEL_SEPARATOR, indexT)
                MqttTopicConst.WILDCARD_MULTI_LEVEL[0] -> return indexF == filter.length - 1
                else -> return false
            }
            indexF++
        }
        return indexT == topic.length && indexF == filter.length
    }
}
