package iot.mqtt;

import com.google.gson.JsonSerializer;

public class Serializer<T> {

    private final Class<T> clazz;
    private final JsonSerializer<T> serializer;

    public Serializer(Class<T> clazz, JsonSerializer<T> serializer) {
        this.clazz = clazz;
        this.serializer = serializer;
    }

    public Class<T> getClazz() {
        return clazz;
    }

    public JsonSerializer<T> getSerializer() {
        return serializer;
    }
}
