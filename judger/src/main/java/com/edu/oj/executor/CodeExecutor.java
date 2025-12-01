package com.edu.oj.executor;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

/**
 * 负责：
 * 1. 根据 submissionId 找到用户代码文件（code.py / code.java / code.c / code.cpp）
 * 2. 根据 problemId 遍历 data/problem/{problemId}/testCases 下的 *.in / *.out
 * 3. 调用 CodeRunner 在你已经准备好的镜像中执行代码
 * 4. 比对程序输出和标准输出，返回每个测试点的结果
 */
public class CodeExecutor {

    private final CodeRunner codeRunner;

    public CodeExecutor(CodeRunner codeRunner) {
        this.codeRunner = codeRunner;
    }

    /**
     * 对某一次提交进行评测
     */
    public JudgeResult judge(long submissionId, long problemId) {
        try {
            Path submissionDir = Paths.get("data", "submission", String.valueOf(submissionId));
            Path problemTestCaseDir = Paths.get("data", "problem", String.valueOf(problemId), "testCases");

            if (!Files.isDirectory(submissionDir)) {
                return JudgeResult.error("Submission dir not found: " + submissionDir);
            }
            if (!Files.isDirectory(problemTestCaseDir)) {
                return JudgeResult.error("Problem testCases dir not found: " + problemTestCaseDir);
            }

            // 题目级时空限制
            ProblemConfig problemConfig = loadProblemConfig(problemId);
            ProblemConfig.Limits limits = problemConfig.getLimitsWithDefault();

            CodeFileInfo codeFileInfo = detectCodeFile(submissionDir);
            if (codeFileInfo == null) {
                return JudgeResult.error("No code file found under: " + submissionDir);
            }
            Path executablePath = null;
            if (codeFileInfo.getLanguage() == Language.C
                    || codeFileInfo.getLanguage() == Language.CPP
                    || codeFileInfo.getLanguage() == Language.JAVA) {

                CompileResult cr = compileOnce(codeFileInfo);
                executablePath=cr.executablePath;
                if (!cr.isSuccess()) {
                    // 返回编译错误
                    return JudgeResult.compileError(cr.getMessage());
                }
            }


            System.out.println(executablePath.toString());
            List<TestCaseResult> caseResults =
                    runAllTestCases(codeFileInfo, problemTestCaseDir, limits,executablePath);

            boolean allAccepted = caseResults.stream().allMatch(r -> r.getStatus() == TestCaseStatus.AC);
            return JudgeResult.success(allAccepted, caseResults);
        } catch (Exception e) {
            e.printStackTrace();
            return JudgeResult.error("Judge failed: " + e.getMessage());
        }
    }

    /**
     * 在 submission 目录下探测 code.py / code.java / code.c / code.cpp
     */
    private CodeFileInfo detectCodeFile(Path submissionDir) throws IOException {
        Path py = submissionDir.resolve("code.py");
        Path java = submissionDir.resolve("code.java");
        Path c = submissionDir.resolve("code.c");
        Path cpp = submissionDir.resolve("code.cpp");

        if (Files.exists(py)) {
            return new CodeFileInfo(py, Language.PYTHON);
        } else if (Files.exists(java)) {
            return new CodeFileInfo(java, Language.JAVA);
        } else if (Files.exists(c)) {
            return new CodeFileInfo(c, Language.C);
        } else if (Files.exists(cpp)) {
            return new CodeFileInfo(cpp, Language.CPP);
        }
        return null;
    }
    private ProblemConfig loadProblemConfig(long problemId) throws IOException {
        Path configPath = Paths.get("data", "problem", String.valueOf(problemId),"testCases", "data.yml");
        ProblemConfig cfg;
        if (!Files.exists(configPath)) {
            System.out.println("checkout:"+problemId+" "+configPath.toString());
            // 配置文件不存在，给一个带默认限制的配置
            cfg = new ProblemConfig();
            cfg.setProblemId(problemId);
            // timeLimitMs / memoryLimitMb 保持 null，getLimitsWithDefault() 会兜底
            return cfg;
        }
        YAMLMapper mapper = new YAMLMapper();
        byte[] bytes = Files.readAllBytes(configPath);
        cfg = mapper.readValue(bytes, ProblemConfig.class);
        if (cfg.getProblemId() == null) {
            cfg.setProblemId(problemId);
        }
        return cfg;
    }
    /**
     * 遍历 testCases 目录下所有 *.in，和同名 *.out 对比
     */
    private List<TestCaseResult> runAllTestCases(CodeFileInfo codeFileInfo, Path testCaseDir, ProblemConfig.Limits limits,Path executablePath) throws IOException {
        List<Path> inFiles = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(testCaseDir, "*.in")) {
            for (Path inFile : stream) {
                inFiles.add(inFile);
            }
        }

        inFiles.sort(Comparator.comparing(p -> p.getFileName().toString()));

        List<TestCaseResult> results = new ArrayList<>();
        int index = 1;
        for (Path inFile : inFiles) {
            String fileName = inFile.getFileName().toString(); // "1.in"
            String idStr = fileName.substring(0, fileName.length() - 3); // "1"
            int caseId = Integer.parseInt(idStr);
            Path outFile = testCaseDir.resolve(idStr + ".out");

            if (!Files.exists(outFile)) {
                results.add(TestCaseResult.missingOutputFile(index, inFile, outFile));
                index++;
                continue;
            }

            TestCaseResult r = runSingleTestCase(index, caseId, codeFileInfo, inFile, outFile, limits, executablePath);
            results.add(r);
            index++;
        }
        return results;
    }

    /**
     * 对单个测试点进行评测
     */
    private TestCaseResult runSingleTestCase(int index, int caseId, CodeFileInfo codeFileInfo, Path inFile, Path outFile, ProblemConfig.Limits limits, Path executablePath) {
        try {
            // 期望输出
            String expectedOutput = Files.readString(outFile, StandardCharsets.UTF_8);

            // 组装 RunRequest
            RunRequest request = new RunRequest();
            request.setLanguage(codeFileInfo.getLanguage());
            request.setInputPath(inFile);
            request.setTimeLimitMs(limits.getTimeLimitMs());
            request.setMemoryLimitMb(limits.getMemoryLimitMb());
            request.setCaseId(caseId);
            Language lang = codeFileInfo.getLanguage();
            if (lang == Language.PYTHON) {
                request.setCodePath(codeFileInfo.getCodePath());
            } else {
                request.setExecutablePath(executablePath);
            }

            // 真正执行
            RunResult runResult = codeRunner.runInSandbox(request);

            String timeInfo = runResult.getMessage(); // 可能是 "Execution time: X ms" 或 null
            System.out.println(timeInfo);
            if (!runResult.isSuccess()) {
                String msg = "Runtime Error";
                if (timeInfo != null) {
                    msg += " (" + timeInfo + ")";
                } else if (runResult.getMessage() != null) {
                    msg += ": " + runResult.getMessage();
                }
                return new TestCaseResult(index, TestCaseStatus.RE, msg, inFile, outFile, runResult.getStdout(), expectedOutput);
            }

            String actualOutput = runResult.getStdout();
            boolean accepted = normalizeOutput(actualOutput)
                    .equals(normalizeOutput(expectedOutput));

            String msg;
            if (accepted) {
                msg = (timeInfo != null) ? ("Accepted (" + timeInfo + ")") : "Accepted";
            } else {
                msg = (timeInfo != null) ? ("Wrong Answer (" + timeInfo + ")") : "Wrong Answer";
            }

            return new TestCaseResult(
                    index,
                    accepted ? TestCaseStatus.AC : TestCaseStatus.WA,
                    msg,
                    inFile,
                    outFile,
                    actualOutput,
                    expectedOutput
            );
        } catch (Exception e) {
            return new TestCaseResult(index, TestCaseStatus.RE, "Exception: " + e.getMessage(), inFile, outFile, null, null);
        }
    }

    /**
     * 输出规约：去掉末尾多余空白、行尾空格
     */
    private String normalizeOutput(String s) {
        if (s == null) return "";
        return s
                .replaceAll("[ \\t]+(?=\\n)", "") // 行尾空格
                .replaceAll("\\s+$", "");        // 最后的空白
    }

    // ================== DTO & 枚举部分：全部用 Lombok 精简 ==================

    @Data
    @AllArgsConstructor
    public static class CodeFileInfo {
        private Path codePath;
        private Language language;
    }

    public enum Language {
        JAVA,
        PYTHON,
        C,
        CPP
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RunRequest {
        private Language language;
        private Path codePath;
        private Path inputPath;
        private Path executablePath; // 运行阶段用（编译产物）

        private Long timeLimitMs;
        private Long memoryLimitMb;
        private Integer caseId;
    }
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RunResult {
        private boolean success;
        private String stdout;
        private String stderr;
        private String message;

        public static RunResult success(String stdout) {
            RunResult r = new RunResult();
            r.setSuccess(true);
            r.setStdout(stdout);
            return r;
        }

        public static RunResult fail(String message, String stderr) {
            RunResult r = new RunResult();
            r.setSuccess(false);
            r.setMessage(message);
            r.setStderr(stderr);
            return r;
        }
    }

    /**
     * 统一抽象：在“已经准备好的镜像 / 沙箱”里执行一次代码
     */
    public interface CodeRunner {
        RunResult runInSandbox(RunRequest request) throws IOException, InterruptedException;

        RunResult compile(RunRequest req)throws IOException, InterruptedException;
    }

    @Data
    @AllArgsConstructor
    public static class TestCaseResult {
        private int index;
        private TestCaseStatus status;
        private String message;
        private Path inputFile;
        private Path outputFile;
        private String actualOutput;
        private String expectedOutput;

        public static TestCaseResult accepted(int index, Path in, Path out, String actual) {
            return new TestCaseResult(index, TestCaseStatus.AC,
                    "Accepted", in, out, actual, null);
        }

        public static TestCaseResult wrongAnswer(int index, Path in, Path out,
                                                 String actual, String expected) {
            return new TestCaseResult(index, TestCaseStatus.WA,
                    "Wrong Answer", in, out, actual, expected);
        }

        public static TestCaseResult runtimeError(int index, Path in, Path out, String msg) {
            return new TestCaseResult(index, TestCaseStatus.RE,
                    "Runtime Error: " + msg, in, out, null, null);
        }

        public static TestCaseResult missingOutputFile(int index, Path in, Path out) {
            return new TestCaseResult(index, TestCaseStatus.CONFIG_ERROR,
                    "Missing .out file: " + out, in, out, null, null);
        }
    }

    public enum TestCaseStatus {
        AC, WA, RE, CONFIG_ERROR
    }

    @Data
    @AllArgsConstructor
    public static class JudgeResult {
        private boolean success;
        private boolean allAccepted;
        private String message;
        private List<TestCaseResult> caseResults;

        public static JudgeResult success(boolean allAccepted, List<TestCaseResult> caseResults) {
            return new JudgeResult(true, allAccepted, null, caseResults);
        }

        public static JudgeResult error(String msg) {
            return new JudgeResult(false, false, msg, List.of());
        }

        public static JudgeResult compileError(String msg) {
            return new JudgeResult(false, false, msg, List.of());
        }
    }

    @Data
    private static class CompileResult {
        private final boolean success;
        private final String message;
        private final Path executablePath;

        // 构造函数/Getter 省略
    }

    private CompileResult compileOnce(CodeFileInfo codeFileInfo) {
        try {
            Path codePath = codeFileInfo.getCodePath(); // data/submission/{sid}/code.cpp
            Path submissionDir = codePath.getParent();
            Path buildDir = submissionDir.resolve("build");
            Files.createDirectories(buildDir);

            Path exePath = buildDir.resolve("main"); // data/submission/{sid}/build/main

            // 构造一个特殊的 RunRequest 专门用于编译
            RunRequest req = new RunRequest();
            req.setLanguage(codeFileInfo.getLanguage());
            req.setCodePath(codePath);
            req.setExecutablePath(exePath);
            req.setTimeLimitMs(5000L);   // 编译时限 5 秒
            req.setMemoryLimitMb(512L);  // 可稍微大一点

            RunResult rr = codeRunner.compile(req); // 新增一个 compile(...) 方法

            System.out.println("checkout:"+rr.toString());

            if (!rr.isSuccess()) {
                return new CompileResult(false, rr.getMessage(), null);
            }
            return new CompileResult(true, null, exePath);
        } catch (Exception e) {
            return new CompileResult(false, "Compile failed: " + e.getMessage(), null);
        }
    }
}