package com.hivemq.mqtt.decode;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.time.LocalDateTime;


public class GsonProvider<T> {
    // 共用一个全局的Gson
    private static final Gson gson;

    static {
        // Gson 初始化
        GsonBuilder builder = new GsonBuilder()
                // 序列化为null的字段
                .serializeNulls()
                // 仅仅处理带有@Expose注解的变量
                .excludeFieldsWithoutExposeAnnotation()
                // 支持Map
                .enableComplexMapKeySerialization();
        // 添加对Java8LocalDateTime时间类型的支持
        builder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeConverter());
        gson = builder.create();
    }

    /**
     * 取得一个全局的Gson
     *
     * @return Gson
     */
    public static Gson getGson() {
        return gson;
    }

    public GsonProvider() {

    }
}