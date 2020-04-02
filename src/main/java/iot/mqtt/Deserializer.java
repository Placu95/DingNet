package iot.mqtt;

import com.google.gson.JsonDeserializer;

public class Deserializer<T> {

    private final Class<T> clazz;
    private final JsonDeserializer deserializer;

    public Deserializer(Class<T> clazz, JsonDeserializer<T> deserializer) {
        this.clazz = clazz;
        this.deserializer = deserializer;
    }

    public Class<T> getClazz() {
        return clazz;
    }

    public JsonDeserializer<T> getDeserializer() {
        return deserializer;
    }
}
