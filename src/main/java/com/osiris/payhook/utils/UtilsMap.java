package com.osiris.payhook.utils;

import java.util.Map;

public class UtilsMap {
    public <K, V> String mapToString(Map<K, V> map) {
        StringBuilder sb = new StringBuilder();
        map.forEach((key, val) -> {
            sb.append(key).append(" = ").append(val);
        });
        return sb.toString();
    }

    public <K, V> String mapToStringWithLineBreaks(Map<K, V> map) {
        StringBuilder sb = new StringBuilder();
        map.forEach((key, val) -> {
            sb.append(key).append(" = ").append(val).append("\n");
        });
        return sb.toString();
    }
}
