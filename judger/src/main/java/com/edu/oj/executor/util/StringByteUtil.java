package com.edu.oj.executor.util;

import java.nio.charset.StandardCharsets;

public class StringByteUtil {
    public static String truncateUtf8(String s, int maxBytes) {
        if (s == null) return null;
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        if (bytes.length <= maxBytes) {
            return s;
        }
        int byteCount = 0;
        int endIndex = 0;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            int need;
            if (ch <= 0x7F) {
                need = 1;
            } else if (ch <= 0x7FF) {
                need = 2;
            } else if (Character.isHighSurrogate(ch)) {
                need = 4;
                i++;
            } else {
                need = 3;
            }
            if (byteCount + need > maxBytes) {
                break;
            }
            byteCount += need;
            endIndex = i + 1;
        }
        return s.substring(0, endIndex);
    }
}
