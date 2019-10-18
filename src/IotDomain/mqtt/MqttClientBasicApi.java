package IotDomain.mqtt;

import java.util.function.BiConsumer;

public interface MqttClientBasicApi {

    void connect();

    void disconnect();

    void publish(String topic, MqttMessage message);

    void subscribe(String topic, BiConsumer<String, MqttMessage> messageListener);

    void unsubscribe(String topic);
}