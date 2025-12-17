package com.edu.oj.executor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class OutputComparator {

    public static boolean sameOutput(Path expected, Path actual) {
        try {
            List<String> a = normalize(readAllLinesSafe(expected));
            List<String> b = normalize(readAllLinesSafe(actual));
            return a.equals(b);
        } catch (Exception e) {
            return false;
        }
    }

    private static List<String> readAllLinesSafe(Path p) throws IOException {
        if (p == null || !Files.exists(p)) return Collections.emptyList();
        return Files.readAllLines(p, StandardCharsets.UTF_8);
    }

    private static List<String> normalize(List<String> in) {
        List<String> out = new ArrayList<>();
        int last = in.size() - 1;
        while (last >= 0 && in.get(last).trim().isEmpty()) last--;
        for (int i = 0; i <= last; i++) {
            out.add(rtrim(in.get(i)));
        }
        return out;
    }

    private static String rtrim(String s) {
        int i = s.length() - 1;
        while (i >= 0 && Character.isWhitespace(s.charAt(i))) i--;
        return s.substring(0, i + 1);
    }
}
