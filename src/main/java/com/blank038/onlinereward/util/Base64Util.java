package com.blank038.onlinereward.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.util.Base64;

/**
 * @author Blank038
 */
public class Base64Util {
    private static final Gson GSON = new GsonBuilder().create();

    /**
     * 加密数据, 返回一个字符串
     */
    public static String encode(JsonObject jsonObject) {
        Base64.Encoder encoder = Base64.getEncoder();
        return encoder.encodeToString(jsonObject.toString().getBytes());
    }

    /**
     * 解密数据, 返回一个 JsonObject
     */
    public static JsonObject decode(String text) {
        Base64.Decoder decoder = Base64.getDecoder();
        return GSON.fromJson(new String(decoder.decode(text)), JsonObject.class);
    }
}
