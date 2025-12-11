package com.edu.oj.executor.util;

public class NormalizeOutput {
    public static String normalizeOutput(String s) {
        if (s == null) return "";
        return s
                .replaceAll("[ \\t]+(?=\\n)", "") // 行尾空格
                .replaceAll("\\s+$", "");        // 最后的空白
    }
}
