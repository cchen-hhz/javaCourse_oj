package com.edu.oj.executor.codeRunner;

import com.edu.oj.executor.domain.Language;
import com.edu.oj.executor.domain.RunRequest;
import com.edu.oj.judge.RunResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.edu.oj.executor.util.CollectIO.collectIO;

public class LocalShellCodeRunner implements CodeRunner {

    private static final String DEFAULT_RUN_COMPILE_SH = "/opt/oj/run-compile.sh";
    private static final String DEFAULT_RUN_SUBMISSION_SH = "/opt/oj/run-submission.sh";

    private final boolean windows;

    public LocalShellCodeRunner() {
        String os = System.getProperty("os.name");
        if (os == null) os = "";
        os = os.toLowerCase();
        this.windows = os.contains("win");
    }

    @Override
    public RunResult compile(RunRequest request) throws IOException, InterruptedException {
        if (request.getLanguage() != Language.C && request.getLanguage() != Language.CPP) {
            return RunResult.fail("Compile not supported for language: " + request.getLanguage(), null);
        }
        if (windows) {
            return compileOnWindows(request);
        }
        return compileOnLinux(request);
    }

    @Override
    public RunResult runInSandbox(RunRequest request) throws IOException, InterruptedException {
        if (request.getLanguage() != Language.C && request.getLanguage() != Language.CPP) {
            return RunResult.fail("Submission run not implemented for language: " + request.getLanguage(), null);
        }
        if (windows) {
            return RunResult.fail("Submission run is only supported on Linux judge server", null);
        }
        return runOnLinuxSandbox(request);
    }

    private RunResult compileOnWindows(RunRequest request) throws IOException, InterruptedException {
        Path codePath = request.getCodePath();
        Path exePath = request.getExecutablePath();
        if (codePath == null || exePath == null) {
            return RunResult.fail("Compile failed: codePath or executablePath is null", null);
        }
        Path srcDir = codePath.getParent();
        Path buildDir = exePath.getParent();
        if (srcDir == null || buildDir == null) {
            return RunResult.fail("Compile failed: source or build directory is null", null);
        }
        Files.createDirectories(buildDir);
        long timeLimitMs = request.getTimeLimitMs() != null ? request.getTimeLimitMs() : 5000L;

        List<String> cmd = new ArrayList<>();
        if (request.getLanguage() == Language.C) {
            cmd.add("gcc");
            cmd.add("-O2");
            cmd.add("-std=c17");
        } else {
            cmd.add("g++");
            cmd.add("-O2");
            cmd.add("-std=c++17");
        }
        cmd.add("-pipe");
        cmd.add(codePath.getFileName().toString());
        cmd.add("-o");
        cmd.add(buildDir.resolve("main.exe").toString());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(srcDir.toFile());

        Process process = pb.start();
        ByteArrayOutputStream stdoutBuf = new ByteArrayOutputStream();
        ByteArrayOutputStream stderrBuf = new ByteArrayOutputStream();
        Thread ioThread = collectIO(process, stdoutBuf, stderrBuf);

        boolean finished = process.waitFor(timeLimitMs, TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            try { ioThread.join(200); } catch (InterruptedException ignored) {}
            return RunResult.fail("Compile Time Limit Exceeded (windows)", stderrBuf.toString());
        }
        try { ioThread.join(200); } catch (InterruptedException ignored) {}

        int exit = process.exitValue();
        String stdout = stdoutBuf.toString();
        String stderr = stderrBuf.toString();

        if (exit != 0) {
            return RunResult.fail("Compile Error on Windows (exit " + exit + ")", stderr);
        }
        RunResult ok = RunResult.success(stdout);
        ok.setMessage("Compile success on Windows");
        return ok;
    }

    private RunResult compileOnLinux(RunRequest request) throws IOException, InterruptedException {
        Path codePath = request.getCodePath();
        Path exePath = request.getExecutablePath();
        if (codePath == null || exePath == null) {
            return RunResult.fail("Compile failed: codePath or executablePath is null", null);
        }
        Path srcDir = codePath.getParent();
        Path buildDir = exePath.getParent();
        if (srcDir == null || buildDir == null) {
            return RunResult.fail("Compile failed: source or build directory is null", null);
        }
        Files.createDirectories(buildDir);

        long timeLimitMs = request.getTimeLimitMs() != null ? request.getTimeLimitMs() : 5000L;

        String compileSh = getenvOrDefault("OJ_RUN_COMPILE_SH", DEFAULT_RUN_COMPILE_SH);

        List<String> cmd = new ArrayList<>();
        cmd.add("/bin/bash");
        cmd.add(compileSh);
        cmd.add(codePath.toAbsolutePath().toString());
        cmd.add(exePath.toAbsolutePath().toString());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(srcDir.toFile());

        Process process = pb.start();
        ByteArrayOutputStream stdoutBuf = new ByteArrayOutputStream();
        ByteArrayOutputStream stderrBuf = new ByteArrayOutputStream();
        Thread ioThread = collectIO(process, stdoutBuf, stderrBuf);

        boolean finished = process.waitFor(timeLimitMs, TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            try { ioThread.join(200); } catch (InterruptedException ignored) {}
            return RunResult.fail("Compile Time Limit Exceeded (linux)", stderrBuf.toString());
        }
        try { ioThread.join(200); } catch (InterruptedException ignored) {}

        int exit = process.exitValue();
        String stdout = stdoutBuf.toString();
        String stderr = stderrBuf.toString();

        if (exit != 0) {
            return RunResult.fail("Compile Error on Linux (exit " + exit + ")", stderr);
        }

        RunResult ok = RunResult.success(stdout);
        ok.setMessage("Compile success on Linux");
        return ok;
    }

    private RunResult runOnLinuxSandbox(RunRequest request) throws IOException, InterruptedException {
        Path exePath = request.getExecutablePath();
        Path testCasesDir = request.getTestCasesDir();
        Path metaFile = request.getMetaFilePath();
        Path resultsFile = request.getResultsFilePath();

        if (exePath == null || testCasesDir == null || metaFile == null || resultsFile == null) {
            return RunResult.fail("Submission run failed: missing paths in RunRequest", null);
        }

        Path buildDir = exePath.getParent();
        Path submissionDir = metaFile.getParent();
        if (buildDir == null || submissionDir == null) {
            return RunResult.fail("Submission run failed: buildDir or submissionDir is null", null);
        }

        Files.createDirectories(buildDir);
        Files.createDirectories(submissionDir);

        Files.deleteIfExists(resultsFile);

        long timeLimitMs = request.getTimeLimitMs() != null ? request.getTimeLimitMs() : 1000L;
        long memoryLimitMb = request.getMemoryLimitMb() != null ? request.getMemoryLimitMb() : 256L;

        String runSh = getenvOrDefault("OJ_RUN_SUBMISSION_SH", DEFAULT_RUN_SUBMISSION_SH);

        List<String> cmd = new ArrayList<>();
        cmd.add("/bin/bash");
        cmd.add(runSh);
        cmd.add(exePath.toAbsolutePath().toString());
        cmd.add(metaFile.toAbsolutePath().toString());
        cmd.add(resultsFile.toAbsolutePath().toString());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(buildDir.toFile());
        pb.environment().put("TIME_LIMIT_MS", String.valueOf(timeLimitMs));
        pb.environment().put("MEMORY_LIMIT_MB", String.valueOf(memoryLimitMb));
        pb.environment().put("TESTCASES_DIR", testCasesDir.toAbsolutePath().toString());
        pb.environment().put("SUBMISSION_DIR", submissionDir.toAbsolutePath().toString());

        Process process = pb.start();
        ByteArrayOutputStream stdoutBuf = new ByteArrayOutputStream();
        ByteArrayOutputStream stderrBuf = new ByteArrayOutputStream();
        Thread ioThread = collectIO(process, stdoutBuf, stderrBuf);

        long start = System.currentTimeMillis();
        boolean finished = process.waitFor(timeLimitMs + 4000, TimeUnit.MILLISECONDS);
        long used = System.currentTimeMillis() - start;

        if (!finished) {
            process.destroyForcibly();
            try { ioThread.join(200); } catch (InterruptedException ignored) {}
            return RunResult.fail("Time Limit Exceeded (linux submission)", stdoutBuf.toString());
        }

        try { ioThread.join(200); } catch (InterruptedException ignored) {}

        int exit = process.exitValue();
        String stdout = stdoutBuf.toString();
        String stderr = stderrBuf.toString();

        if (exit != 0) {
            return RunResult.fail("Submission Runtime Error on Linux (exit " + exit + ")", stderr);
        }

        if (!Files.exists(resultsFile)) {
            String msg = "Sandbox finished but results.yaml not found. runSh=" + runSh + ", usedMs=" + used;
            String merged = (stderr == null ? "" : stderr) + "\n" + (stdout == null ? "" : stdout);
            return RunResult.fail(msg, merged);
        }

        RunResult ok = RunResult.success(stdout);
        ok.setMessage("Submission run finished on Linux (usedMs=" + used + ")");
        return ok;
    }

    private static String getenvOrDefault(String key, String def) {
        String v = System.getenv(key);
        if (v == null) return def;
        v = v.trim();
        return v.isEmpty() ? def : v;
    }
}
