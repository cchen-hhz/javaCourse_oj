package com.edu.oj.executor.util;

public class parseExecTime {
    public static Long parseExecTime(String stderr) {
        if (stderr == null) return null;
        Long result = null;
        String[] lines = stderr.split("\\R"); // 按行分割
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("__OJ_TIME_MS__=")) {
                String v = line.substring("__OJ_TIME_MS__=".length()).trim();
                try {
                    result = Long.parseLong(v);
                } catch (NumberFormatException ignored) {}
            }
        }
        return result;
    }
}
