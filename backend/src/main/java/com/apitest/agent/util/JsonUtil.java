package com.apitest.agent.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class JsonUtil {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static String toJsonSafe(Object obj) {
        if (obj == null) return null;
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            log.debug("JSON serialization failed: {}", e.getMessage());
            return "{}";
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> parseJsonMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return MAPPER.readValue(json, Map.class);
        } catch (Exception e) {
            log.debug("JSON parse failed: {}", e.getMessage());
            return Map.of();
        }
    }

    private JsonUtil() {}
}
