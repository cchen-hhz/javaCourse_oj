package com.edu.oj.executor.codeRunner;

import com.edu.oj.executor.domain.Language;
import com.edu.oj.executor.domain.RunRequest;
import com.edu.oj.executor.domain.RunResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.edu.oj.executor.util.CollectIO.collectIO;
import static com.edu.oj.executor.util.KillContainer.killContainer;

import java.io.InputStream;

/**
 * 使用 Docker 在沙箱中编译和运行代码。
 *
 * 目前实现：
 *   - C / C++：编译和运行分离
 *     * compile(...) 使用镜像 cpp-compile，只做 g++ 编译
 *     * runInSandbox(...) 使用镜像 cpp-run，只运行已编译的可执行文件
 *
 * 目录约定：
 *   - 源码：data/submission/{sid}/code.cpp
 *   - 编译产物：data/submission/{sid}/build/main
 *   - 输入：data/problem/{pid}/testCases/{id}.in
 */
public class DockerCodeRunner implements CodeRunner {

    public static final String DOCKER = "docker";

    // 镜像名（你要保证这些镜像已经 docker build 或 docker load 好）
    private static final String CPP_COMPILE_IMAGE = "cpp-compile";
    private static final String CPP_RUN_IMAGE     = "cpp-run";

    // 如果以后扩展 Java / Python，可以再补充对应镜像
    // private static final String JAVA_COMPILE_IMAGE = "java-compile";
    // private static final String JAVA_RUN_IMAGE     = "java-run";
    // private static final String PY_RUN_IMAGE       = "python-run";

    /**
     * 编译：目前只实现 C / C++。
     *
     * 约定：
     *  - request.language 是 C 或 CPP
     *  - request.codePath 指向源码文件，如 data/submission/{sid}/code.cpp
     *  - request.executablePath 指向编译产物，如 data/submission/{sid}/build/main
     *  - timeLimitMs 是编译时限（墙钟时间），例如 5000ms
     */
    @Override
    public RunResult compile(RunRequest request) throws IOException, InterruptedException {
        if (request.getLanguage() != Language.C && request.getLanguage() != Language.CPP) {
            return RunResult.fail("Compile not supported for language: " + request.getLanguage(), null);
        }

        Path codePath = request.getCodePath();
        if (codePath == null) {
            return RunResult.fail("Compile failed: codePath is null", null);
        }
        Path exePath = request.getExecutablePath();
        if (exePath == null) {
            return RunResult.fail("Compile failed: executablePath is null", null);
        }

        Path srcDir   = codePath.getParent();   // data/submission/{sid}
        Path buildDir = exePath.getParent();    // data/submission/{sid}/build
        Files.createDirectories(buildDir);

        long timeLimitMs   = (request.getTimeLimitMs()   != null) ? request.getTimeLimitMs()   : 5000L;
        long memoryLimitMb = (request.getMemoryLimitMb() != null) ? request.getMemoryLimitMb() : 512L;

        String containerName = "compile-" + UUID.randomUUID();

        List<String> cmd = new ArrayList<>();
        cmd.add(DOCKER); cmd.add("run");
        cmd.add("--rm");
        cmd.add("--name"); cmd.add(containerName);

        // 强制使用 run-compile.sh 作为入口
        cmd.add("--entrypoint"); cmd.add("/app/run-compile.sh");

        // 内存限制
        cmd.add("--memory");      cmd.add(memoryLimitMb + "m");
        cmd.add("--memory-swap"); cmd.add(memoryLimitMb + "m");

        // 挂载源码目录到 /app/src（只读）
        cmd.add("-v");
        cmd.add(srcDir.toAbsolutePath().toString() + ":/app/src:ro");

        // 挂载 build 目录到 /app/build（可写入 main）
        cmd.add("-v");
        cmd.add(buildDir.toAbsolutePath().toString() + ":/app/build");

        // 镜像
        cmd.add(CPP_COMPILE_IMAGE);

        // 入口参数：源码文件名 + 编译产物容器内路径
        String srcFileName = codePath.getFileName().toString();   // code.cpp
        cmd.add(srcFileName);              // /app/src/code.cpp
        cmd.add("/app/build/main");        // 输出位置

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(srcDir.toFile());

        Process process = pb.start();
        ByteArrayOutputStream stdoutBuf = new ByteArrayOutputStream();
        ByteArrayOutputStream stderrBuf = new ByteArrayOutputStream();
        Thread ioThread = collectIO(process, stdoutBuf, stderrBuf);

        boolean finished = process.waitFor(timeLimitMs, TimeUnit.MILLISECONDS);
        if (!finished) {
            killContainer(containerName);
            process.destroyForcibly();
            try { ioThread.join(200); } catch (InterruptedException ignored) {}
            return RunResult.fail("Compile Time Limit Exceeded", stderrBuf.toString());
        }

        try { ioThread.join(200); } catch (InterruptedException ignored) {}

        int exit = process.exitValue();
        String stdout = stdoutBuf.toString();
        String stderr = stderrBuf.toString();

        if (exit != 0) {
            // 编译错误信息一般在 stderr
            return RunResult.fail("Compile Error (exit " + exit + ")", stderr);
        }

        // 到这里，data/submission/{sid}/build/main 应该已经生成
        return RunResult.success(stdout);
    }


    private static void checkDockerVersion() {
        try {
            Process p = new ProcessBuilder("docker", "--version")
                    .redirectErrorStream(true)
                    .start();
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            collectIO(p, buf, new ByteArrayOutputStream());
            int code = p.waitFor();
            System.out.println("docker --version exit=" + code + ", output=" + buf);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    /**
     * 运行：只算运行时间，不再包含编译时间。
     *
     * 约定：
     *  - request.language 是 C 或 CPP（目前只实现这两种）
     *  - request.executablePath 指向已经编译好的 main
     *  - request.inputPath 指向某个测试点的 .in 文件
     *  - timeLimitMs 是运行时限
     */
    @Override
    public RunResult runInSandbox(RunRequest request) throws IOException, InterruptedException {
        if (request.getLanguage() != Language.C && request.getLanguage() != Language.CPP) {
            return RunResult.fail("Submission run not implemented for language: " + request.getLanguage(), null);
        }

        // 调试：确认 docker 在 Java 里可用
        try {
            Process p = new ProcessBuilder("docker", "--version")
                    .redirectErrorStream(true)
                    .start();
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            try (InputStream is = p.getInputStream()) {
                is.transferTo(buf);
            }
            int code = p.waitFor();
            System.out.println("docker --version exit=" + code + ", output=" + buf);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Path exePath      = request.getExecutablePath();
        Path testCasesDir = request.getTestCasesDir();
        Path metaFile     = request.getMetaFilePath();
        Path resultsFile  = request.getResultsFilePath();

        if (exePath == null || testCasesDir == null || metaFile == null || resultsFile == null) {
            return RunResult.fail("Submission run failed: missing paths in RunRequest", null);
        }

        Path buildDir      = exePath.getParent();      // data/submission/{sid}/build
        Path submissionDir = buildDir.getParent();     // data/submission/{sid}
        Files.createDirectories(buildDir);
        Files.createDirectories(submissionDir);

        long timeLimitMs   = (request.getTimeLimitMs()   != null) ? request.getTimeLimitMs()   : 1000L;
        long memoryLimitMb = (request.getMemoryLimitMb() != null) ? request.getMemoryLimitMb() : 256L;

        String containerName = "run-sub-" + UUID.randomUUID();

        List<String> cmd = new ArrayList<>();
        cmd.add(DOCKER); cmd.add("run");
        cmd.add("--rm");
        cmd.add("--name"); cmd.add(containerName);

        cmd.add("--entrypoint"); cmd.add("/app/run-submission.sh");

        // 为避免 Docker Desktop/Windows 对 --memory-swap 限制的问题，先只保留 --memory
        cmd.add("--memory"); cmd.add(memoryLimitMb + "m");
        // cmd.add("--memory-swap"); cmd.add(memoryLimitMb + "m");

        cmd.add("-v");
        cmd.add(buildDir.toAbsolutePath().toString() + ":/app/build:ro");

        cmd.add("-v");
        cmd.add(testCasesDir.toAbsolutePath().toString() + ":/app/input:ro");

        cmd.add("-v");
        cmd.add(submissionDir.toAbsolutePath().toString() + ":/app/submission");

        cmd.add(CPP_RUN_IMAGE);

        cmd.add("/app/build/" + exePath.getFileName().toString());
        cmd.add("/app/submission/" + metaFile.getFileName().toString());
        cmd.add("/app/submission/" + resultsFile.getFileName().toString());

        System.out.println("buildDir=" + buildDir.toAbsolutePath());
        System.out.println("testCasesDir=" + testCasesDir.toAbsolutePath());
        System.out.println("submissionDir=" + submissionDir.toAbsolutePath());

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(buildDir.toFile());

        Process process = pb.start();

        ByteArrayOutputStream stdoutBuf = new ByteArrayOutputStream();
        ByteArrayOutputStream stderrBuf = new ByteArrayOutputStream();

        Thread tOut = new Thread(() -> {
            try (InputStream is = process.getInputStream()) {
                is.transferTo(stdoutBuf);
            } catch (IOException ignored) {}
        });
        Thread tErr = new Thread(() -> {
            try (InputStream is = process.getErrorStream()) {
                is.transferTo(stderrBuf);
            } catch (IOException ignored) {}
        });

        tOut.start();
        tErr.start();

        long start = System.currentTimeMillis();
        boolean finished = process.waitFor(timeLimitMs + 4000, TimeUnit.MILLISECONDS);
        long used = System.currentTimeMillis() - start;
        System.out.println("Docker submission run used " + used + " ms, finished=" + finished);

        tOut.join();
        tErr.join();

        if (!finished) {
            killContainer(containerName);
            process.destroyForcibly();
            return RunResult.fail("Time Limit Exceeded (docker submission)", stdoutBuf.toString());
        }

        int exit = process.exitValue();
        Path ResultsFile  = request.getResultsFilePath();
        System.out.println("local results path = " + ResultsFile.toAbsolutePath());
        System.out.println("exists = " + Files.exists(ResultsFile));
        if (Files.exists(ResultsFile)) {
            System.out.println("results.yaml content:");
            System.out.println(Files.readString(ResultsFile));
        }
        String stdout = stdoutBuf.toString();
        String stderr = stderrBuf.toString();

        System.out.println("==== submission docker cmd ====");
        System.out.println(String.join(" ", cmd));
        System.out.println("==== submission docker stdout ====");
        System.out.println(stdout);
        System.out.println("==== submission docker stderr ====");
        System.out.println(stderr);

        if (exit != 0) {
            System.out.println("containerName = " + containerName);
            return RunResult.fail("Submission Runtime Error (exit " + exit + ")", stderr);
        }

        RunResult ok = RunResult.success(stdout);
        ok.setMessage("Submission run finished");
        System.out.println("containerName = " + containerName);
        return ok;
    }

}