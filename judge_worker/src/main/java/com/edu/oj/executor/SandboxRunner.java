package com.edu.oj.executor;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;

public class SandboxRunner {

    public static RunResult run(Path exe, Path input, Path userOut, long timeLimitMs, long memoryLimitMb) throws Exception {
        Files.deleteIfExists(userOut);
        Path errFile = userOut.getParent().resolve("stderr_" + System.nanoTime() + ".log");
        Files.deleteIfExists(errFile);

        ProcessBuilder pb = new ProcessBuilder(exe.toAbsolutePath().toString());
        pb.redirectInput(input.toFile());
        pb.redirectOutput(userOut.toFile());
        pb.redirectError(errFile.toFile());

        long startNs = System.nanoTime();
        Process p = pb.start();

        boolean finished = p.waitFor(Math.max(1, timeLimitMs), TimeUnit.MILLISECONDS);
        long endNs = System.nanoTime();

        RunResult rr = new RunResult();
        rr.timeMs = (endNs - startNs) / 1_000_000L;
        rr.rssKb = 0;
        rr.mle = false;

        if (!finished) {
            p.destroyForcibly();
            p.waitFor(1000, TimeUnit.MILLISECONDS);
            rr.ok = false;
            rr.tle = true;
            rr.exitCode = -1;
            rr.detail = "killed_by_timeout";
            return rr;
        }

        rr.exitCode = p.exitValue();
        rr.tle = rr.timeMs > timeLimitMs;
        rr.ok = (rr.exitCode == 0) && !rr.tle;
        rr.detail = Files.exists(errFile) ? Files.readString(errFile, StandardCharsets.UTF_8) : "";
        return rr;
    }

    public static class RunResult {
        public boolean ok;
        public boolean tle;
        public boolean mle;
        public int exitCode;
        public long timeMs;
        public long rssKb;
        public String detail;
    }
}
