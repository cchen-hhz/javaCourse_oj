package com.edu.oj.executor;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SandboxRunner {

    public static RunResult run(Path exe, Path input, Path userOut, long timeLimitMs, long memoryLimitMb) throws Exception {
        Files.deleteIfExists(userOut);
        Path errFile = userOut.getParent().resolve("stderr_" + System.nanoTime() + ".log");
        Files.deleteIfExists(errFile);

        // Set stack size to match memory limit (or a reasonable large value)
        // ulimit -s unlimited is not easily set via ProcessBuilder directly without a shell wrapper
        // But we can try to set it via shell invocation if needed.
        // However, for simple C++ programs, the default stack might be small (8MB).
        // Let's try to run it via /bin/sh to set ulimit.
        
        ProcessBuilder pb;
        boolean useShellWrapper = true;
        
        if (useShellWrapper) {
            // ulimit -s unlimited; ./main
            // Note: 'unlimited' might be restricted by Docker container limits.
            // We can try setting it to the memory limit (in KB).
            long stackSizeKb = memoryLimitMb * 1024;
            // Use 'exec' to replace the shell process with the target process, 
            // ensuring the PID remains the same for memory monitoring and signal handling.
            String cmd = String.format("ulimit -s %d; exec %s", stackSizeKb, exe.toAbsolutePath().toString());
            // Run as code_runner user for isolation
            // We use 'su' to switch user. The Java process must be running as root.
            pb = new ProcessBuilder("su", "code_runner", "-s", "/bin/bash", "-c", cmd);
        } else {
            pb = new ProcessBuilder("su", "code_runner", "-s", "/bin/bash", "-c", exe.toAbsolutePath().toString());
        }

        // Clear environment variables to prevent leakage of sensitive info
        pb.environment().clear();
        pb.environment().put("PATH", "/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin");

        pb.redirectInput(input.toFile());
        pb.redirectOutput(userOut.toFile());
        pb.redirectError(errFile.toFile());

        long startNs = System.nanoTime();
        Process p = pb.start();
        long pid = p.pid();

        long limitKb = memoryLimitMb * 1024;
        long maxRssKb = 0;
        boolean mle = false;
        boolean finished = false;

        // Monitor loop
        long timeLimitNs = timeLimitMs * 1_000_000L;
        // Add a small buffer to the deadline to allow the process to finish naturally if it's close to the limit
        // But strictly, we should enforce timeLimitMs. 
        // The original code used p.waitFor(timeLimitMs).
        long deadlineNs = startNs + timeLimitNs;

        while (true) {
            // Check if process finished
            if (p.waitFor(10, TimeUnit.MILLISECONDS)) {
                finished = true;
                break;
            }

            // Check memory
            long currentRss = getVmRSS(pid);
            if (currentRss > maxRssKb) {
                maxRssKb = currentRss;
            }

            if (currentRss > limitKb) {
                mle = true;
                p.destroyForcibly();
                break;
            }

            // Check time limit manually since we are in a loop
            if (System.nanoTime() > deadlineNs) {
                break; // TLE will be handled below
            }
        }

        long endNs = System.nanoTime();

        RunResult rr = new RunResult();
        rr.timeMs = (endNs - startNs) / 1_000_000L;
        rr.rssKb = maxRssKb;
        rr.mle = mle;

        if (mle) {
            rr.ok = false;
            rr.tle = false;
            rr.exitCode = -1;
            rr.detail = "memory_limit_exceeded";
            return rr;
        }

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

    private static long getVmRSS(long pid) {
        try {
            Path statusPath = Paths.get("/proc", String.valueOf(pid), "status");
            if (!Files.exists(statusPath)) return 0;
            List<String> lines = Files.readAllLines(statusPath);
            for (String line : lines) {
                if (line.startsWith("VmRSS:")) {
                    // Example: VmRSS:    1234 kB
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 2) {
                        return Long.parseLong(parts[1]);
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return 0;
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
