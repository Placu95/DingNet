package iot.mqtt;

import com.google.gson.JsonObject;
import io.moquette.broker.Server;
import iot.lora.BasicFrameHeader;
import iot.lora.EU868ParameterByDataRate;
import iot.lora.FrameHeader;
import iot.lora.RegionalParameter;
import it.unibo.acdingnet.protelis.model.FrameHeaderApp;
import it.unibo.mqttclientwrapper.MQTTClientSingleton.ClientBuilder;
import it.unibo.mqttclientwrapper.MqttClientType;
import it.unibo.mqttclientwrapper.api.MqttClientBasicApi;
import util.Constants;
import util.Converter;
import util.SettingsReader;
import util.time.DoubleTime;
import util.time.Time;

import java.io.IOException;
import java.util.*;

/**
 * Factory to retrieve an instance of {@link MqttClientBasicApi}
 */
public class MQTTClientFactory {

    private final static MqttClientType DEFAULT_INSTANCE_TYPE = SettingsReader.getInstance().getMQTTClientType();
    private final static String ADDRESS = SettingsReader.getInstance().getMQTTServerAddress();
    private static MqttClientBasicApi clientBasicApi;
    private static Server server;
    private static List<Serializer> serializers = new LinkedList<>();
    private static List<Deserializer> deserializers = new LinkedList<>();

    static {
        // region serializers
        serializers.add(new Serializer<>(FrameHeader.class, (header, type, context) -> {
            var obj = new JsonObject();
            obj.addProperty("sourceAddress", Base64.getEncoder().encodeToString(header.getSourceAddress()));
            obj.addProperty("fCtrl", header.getFCtrl());
            obj.addProperty("fCnt", header.getFCntAsShort());
            obj.addProperty("fOpts", Base64.getEncoder().encodeToString(header.getFOpts()));
            return obj;
        }));
        serializers.add(new Serializer<>(Time.class, (header, type, context) -> {
            var obj = new JsonObject();
            obj.addProperty("time", header.asMilli());
            return obj;
        }));
        // endregion
        // region deserializer
        deserializers.add(new Deserializer<>(FrameHeader.class,
            (jsonElement, type, jsonDeserializationContext) -> new BasicFrameHeader()
            .setSourceAddress(Base64.getDecoder().decode(((JsonObject) jsonElement).get("sourceAddress").getAsString()))
            .setFCnt(((JsonObject) jsonElement).get("fCnt").getAsShort())
            .setFCtrl(((JsonObject) jsonElement).get("fCtrl").getAsByte())
            .setFOpts(Base64.getDecoder().decode(((JsonObject) jsonElement).get("fOpts").getAsString()))
        ));
        deserializers.add(new Deserializer<>(FrameHeaderApp.class, (jsonElement, type, jsonDeserializationContext) ->
            new FrameHeaderApp(
                Arrays.asList(Converter.toObjectType(Base64.getDecoder().decode(((JsonObject) jsonElement).get("sourceAddress").getAsString()))),
                ((JsonObject) jsonElement).get("fCnt").getAsInt(),
                ((JsonObject) jsonElement).get("fCtrl").getAsInt(),
                Arrays.asList(Converter.toObjectType(Base64.getDecoder().decode(((JsonObject) jsonElement).get("fOpts").getAsString())))
            )
        ));
        deserializers.add(new Deserializer<>(RegionalParameter.class, (element, type, context) ->
            EU868ParameterByDataRate.valueOf(element.getAsString())
        ));
        deserializers.add(new Deserializer<>(Time.class, (element, type, context) ->
            new DoubleTime(((JsonObject) element).get("time").getAsDouble())
        ));
        // endregion
    }

    /**
     *
     * @return the singleton instance of {@link MqttClientBasicApi} of the predefined type {@link MqttClientType}
     */
    public static MqttClientBasicApi getSingletonInstance() {
        if (clientBasicApi == null) {
            var builder = new ClientBuilder();
            if (DEFAULT_INSTANCE_TYPE == MqttClientType.MOCK_SERIALIZATION) {
                addAdapters(builder);
            }
            if (DEFAULT_INSTANCE_TYPE == MqttClientType.PAHO) {
                addAdapters(builder)
                    .setAddress(ADDRESS)
                    .setClientId(Constants.PAHO_CLIENT);
                if (ADDRESS.equals(Constants.MQTT_LOCALHOST_ADDRESS)) {
                    server = new Server();
                    try {
                        var prop = new Properties();
                        prop.load(MQTTClientFactory.class.getResourceAsStream("/config/moquette.conf"));
                        server.startServer(prop);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            clientBasicApi = builder.build(DEFAULT_INSTANCE_TYPE);
        }
        return clientBasicApi;
    }

    /**
     * the types are checked by the classes {@link Serializer} and {@link Deserializer}
     */
    @SuppressWarnings("unchecked")
    private static ClientBuilder addAdapters(ClientBuilder builder) {
        serializers.forEach(s -> builder.addSerializer(s.getClazz(), s.getSerializer()));
        deserializers.forEach(d -> builder.addDeserializer(d.getClazz(), d.getDeserializer()));
        return builder;
    }

    public static List<Serializer> getSerializers() {
        return Collections.unmodifiableList(serializers);
    }

    public static List<Deserializer> getDeserializers() {
        return Collections.unmodifiableList(deserializers);
    }
}
