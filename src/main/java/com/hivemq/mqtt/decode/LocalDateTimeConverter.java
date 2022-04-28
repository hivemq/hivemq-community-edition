package com.hivemq.mqtt.decode;

import com.google.gson.*;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * LocalDateTime 是一个Java8的新时间类型，
 * 使用起来比常规的Date更加Nice；
 * 但是Gson目前并没有默认支持对LocalDateTime的转换
 * <p>
 * 该工具类主要是为了解决LocalDateTime与Json字符串相互转换的问题
 *
 * @author qiujuer Email:qiujuer.live.cn
 */
public class LocalDateTimeConverter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
    /**
     * 时间转换的格式为：yyyy-MM-dd'T'HH:mm:ss.SSS
     */
    public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.ENGLISH);

    /**
     * 把一个LocalDateTime格式的时间转换为Gson支持的JsonElement
     * <p>
     * Gson invokes this call-back method during serialization when it encounters a field of the
     * specified type. <p>
     * <p>
     * In the implementation of this call-back method, you should consider invoking
     * {@link JsonSerializationContext#serialize(Object, Type)} method to create JsonElements for any
     * non-trivial field of the {@code src} object. However, you should never invoke it on the
     * {@code src} object itself since that will cause an infinite loop (Gson will call your
     * call-back method again).
     *
     * @param src       the object that needs to be converted to Json.
     * @param typeOfSrc the actual type (fully genericized version) of the source object.
     * @return a JsonElement corresponding to the specified object.
     */
    @Override
    public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
        return new JsonPrimitive(FORMATTER.format(src));
    }

    /**
     * 把一个Gson的JsonElement转换为LocalDateTime时间格式
     * <p>
     * <p>
     * Gson invokes this call-back method during deserialization when it encounters a field of the
     * specified type. <p>
     * <p>
     * In the implementation of this call-back method, you should consider invoking
     * {@link JsonDeserializationContext#deserialize(JsonElement, Type)} method to create objects
     * for any non-trivial field of the returned object. However, you should never invoke it on the
     * the same type passing {@code json} since that will cause an infinite loop (Gson will call your
     * call-back method again).
     *
     * @param json    The Json data being deserialized
     * @param typeOfT The type of the Object to deserialize to
     * @return a deserialized object of the specified type typeOfT which is a subclass of {@code T}
     * @throws JsonParseException if json is not in the expected format of {@code typeOfT}
     */
    @Override
    public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        return FORMATTER.parse(json.getAsString(), LocalDateTime::from);
    }
}