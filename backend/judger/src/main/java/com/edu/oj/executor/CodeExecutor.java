package com.edu.oj.executor;

import com.edu.oj.executor.codeRunner.CodeRunner;
import com.edu.oj.executor.domain.*;
import com.edu.oj.judge.JudgeResult;
import com.edu.oj.judge.RunResult;
import com.edu.oj.judge.TestCaseResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.edu.oj.judge.TestCaseStatus;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import static com.edu.oj.executor.domain.CodeFileInfo.detectCodeFile;
import static com.edu.oj.executor.domain.ProblemConfig.loadProblemConfig;
import static com.edu.oj.executor.util.CompileOnce.compileOnce;
import static com.edu.oj.executor.util.NormalizeOutput.normalizeOutput;
import static com.edu.oj.executor.util.StringByteUtil.truncateUtf8;

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
            Path submissionDir = Paths.get("backend","data", "submission", String.valueOf(submissionId));
            Path problemTestCaseDir = Paths.get("backend","data", "problems", String.valueOf(problemId));

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

                CompileResult cr = compileOnce(codeRunner,codeFileInfo);
                executablePath= cr.getExecutablePath();
                if (!cr.isSuccess()) {
                    // 返回编译错误
                    return JudgeResult.compileError(cr.getMessage());
                }
            }
//            System.out.println(executablePath.toString());
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
     * 遍历 testCases 目录下所有 *.in，和同名 *.out 对比
     */
    private List<TestCaseResult> runAllTestCases(CodeFileInfo codeFileInfo, Path testCaseDir, ProblemConfig.Limits limits, Path executablePath) throws IOException {

        // 1. 收集所有测试点 in/out 文件
        List<Path> inFiles = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(testCaseDir, "*.in")) {
            for (Path inFile : stream) {
                inFiles.add(inFile);
            }
        }
        inFiles.sort(Comparator.comparing(p -> p.getFileName().toString()));

        // 这里假设 submissionDir 是 codeFileInfo.getCodePath().getParent()
        // 如果你的结构不同，请替换为真实的 submission 目录
        Path submissionDir = codeFileInfo.getCodePath().getParent();
        System.out.println("submissionDir:"+submissionDir.toString());
        Path metaFile    = submissionDir.resolve("cases.yaml");
        Path resultsFile = submissionDir.resolve("results.yaml");

        // 2. 生成 cases.yaml（你也可以用 JSON，看你喜欢）
        List<TestCaseMeta> metas = new ArrayList<>();
        int index = 1;
        for (Path inFile : inFiles) {
            String fileName = inFile.getFileName().toString(); // "1.in"
            String idStr = fileName.substring(0, fileName.length() - 3); // "1"
            int caseId = Integer.parseInt(idStr);
            Path outFile = testCaseDir.resolve(idStr + ".out");

            TestCaseMeta meta = new TestCaseMeta();
            meta.setIndex(index);
            meta.setCaseId(caseId);
            meta.setInputFile(inFile.getFileName().toString());
            meta.setOutputFile(outFile.getFileName().toString());
            metas.add(meta);
            index++;
        }

        SubmissionRunMeta submissionRunMeta = new SubmissionRunMeta();
        submissionRunMeta.setLanguage(codeFileInfo.getLanguage());
        submissionRunMeta.setTimeLimitMs(limits.getTimeLimitMs());
        submissionRunMeta.setMemoryLimitMb(limits.getMemoryLimitMb());
        submissionRunMeta.setExecutableName(executablePath != null ? executablePath.getFileName().toString() : null);
        submissionRunMeta.setTestCases(metas);

        YAMLMapper mapper = new YAMLMapper();
        Files.writeString(metaFile, mapper.writeValueAsString(submissionRunMeta), StandardCharsets.UTF_8);

        // 3. 构造一次性的 RunRequest（submission 模式）
        RunRequest request = new RunRequest();
        request.setLanguage(codeFileInfo.getLanguage());
        request.setTimeLimitMs(limits.getTimeLimitMs());
        request.setMemoryLimitMb(limits.getMemoryLimitMb());
        request.setExecutablePath(executablePath);
        request.setTestCasesDir(testCaseDir);
        request.setMetaFilePath(metaFile);
        request.setResultsFilePath(resultsFile);

        RunResult runResult;
        try {
            runResult = codeRunner.runInSandbox(request);
        } catch (Exception e) {
            // 整体运行失败，所有测试点标 RE
            List<TestCaseResult> errorResults = new ArrayList<>();
            int idx = 1;
            for (Path inFile : inFiles) {
                String fileName = inFile.getFileName().toString();
                String idStr = fileName.substring(0, fileName.length() - 3);
                Path outFile = testCaseDir.resolve(idStr + ".out");
                errorResults.add(new TestCaseResult(idx, TestCaseStatus.RE,
                        "Exception in submission run: " + e.getMessage(),
                        inFile, outFile, null, null,"","",""));
                idx++;
            }
            return errorResults;
        }

        // 如果容器整体失败，按原有逻辑处理
//        if (!runResult.isSuccess()) {
//            List<TestCaseResult> errorResults = new ArrayList<>();
//            int idx = 1;
//            for (Path inFile : inFiles) {
//                String fileName = inFile.getFileName().toString();
//                String idStr = fileName.substring(0, fileName.length() - 3);
//                Path outFile = testCaseDir.resolve(idStr + ".out");
//                errorResults.add(new TestCaseResult(idx, TestCaseStatus.RE,
//                        "Submission run failed: " + runResult.getMessage(),
//                        inFile, outFile, runResult.getStdout(), null));
//                idx++;
//            }
//            return errorResults;
//        }

        // 4. 从 results.yaml 解析每个测试点结果
//        if (!Files.exists(resultsFile)) {
//            // 万一容器没写结果文件，全部 RE
//            List<TestCaseResult> errorResults = new ArrayList<>();
//            int idx = 1;
//            for (Path inFile : inFiles) {
//                String fileName = inFile.getFileName().toString();
//                String idStr = fileName.substring(0, fileName.length() - 3);
//                Path outFile = testCaseDir.resolve(idStr + ".out");
//                errorResults.add(new TestCaseResult(idx, TestCaseStatus.RE,
//                        "Missing results.yaml from container",
//                        inFile, outFile, null, null));
//                idx++;
//            }
//            return errorResults;
//        }

        String yamlContent = Files.readString(resultsFile, StandardCharsets.UTF_8);
        SubmissionRunResult submissionRunResult = mapper.readValue(yamlContent, SubmissionRunResult.class);

        // 映射回 TestCaseResult 列表（保证 index / inFile / outFile 与原来一致）
        List<TestCaseResult> finalResults = new ArrayList<>();
        for (TestCaseMeta meta : metas) {
            System.out.println(metas.toString());
            SingleCaseResultDto dto = submissionRunResult.findByCaseId(meta.getCaseId());
            Path inFile = testCaseDir.resolve(meta.getInputFile());
            Path outFile = testCaseDir.resolve(meta.getOutputFile());

            if (dto == null) {
                finalResults.add(new TestCaseResult(
                        meta.getIndex(),
                        TestCaseStatus.RE,
                        "No result from container for caseId=" + meta.getCaseId(),
                        inFile, outFile, null, null,"","",""
                ));
            } else {
                TestCaseStatus status = dto.getStatus();   // AC/WA/RE/TLE
                String msg = dto.getMessage();
                String actual = dto.getActualOutput();
                String expected = Files.exists(outFile)
                        ? Files.readString(outFile, StandardCharsets.UTF_8)
                        : null;
                Long execMs = dto.getExecTimeMs();
                Integer memKb  = dto.getMemoryKb();

                StringBuilder extra = new StringBuilder();
                if (execMs != null) {
                    extra.append("time: ").append(execMs).append(" ms");
                }
                if (memKb != null) {
                    if (extra.length() > 0) extra.append(", ");
                    extra.append("memory: ").append(memKb).append(" KB");
                }

                if (extra.length() > 0) {
                    if (msg == null || msg.isEmpty()) {
                        msg = extra.toString();
                    } else {
                        msg = msg + " (" + extra + ")";
                    }
                }
                String inputContent = Files.readString(inFile, StandardCharsets.UTF_8);
                String inputPreview    = truncateUtf8(inputContent, 50);
                String userOutputPrev  = truncateUtf8(actual, 50);
                String expectedPreview = truncateUtf8(expected, 50);
                finalResults.add(new TestCaseResult(
                        meta.getIndex(), status, msg, inFile, outFile, actual, expected,inputPreview,expectedPreview,userOutputPrev
                ));

            }
        }

        return finalResults;
    }
//    @Deprecated
//    private TestCaseResult runSingleTestCase(int index, int caseId, CodeFileInfo codeFileInfo, Path inFile, Path outFile, ProblemConfig.Limits limits, Path executablePath) {
//        try {
//            // 期望输出
//            String expectedOutput = Files.readString(outFile, StandardCharsets.UTF_8);
//
//            // 组装 RunRequest
//            RunRequest request = new RunRequest();
//            request.setLanguage(codeFileInfo.getLanguage());
//            request.setInputPath(inFile);
//            request.setTimeLimitMs(limits.getTimeLimitMs());
//            request.setMemoryLimitMb(limits.getMemoryLimitMb());
//            request.setCaseId(caseId);
//            Language lang = codeFileInfo.getLanguage();
//            if (lang == Language.PYTHON) {
//                request.setCodePath(codeFileInfo.getCodePath());
//            } else {
//                request.setExecutablePath(executablePath);
//            }
//
//            // 真正执行
//            RunResult runResult = codeRunner.runInSandbox(request);
//
//            String timeInfo = runResult.getMessage(); // 可能是 "Execution time: X ms" 或 null
//            System.out.println(timeInfo);
//            if (!runResult.isSuccess()) {
//                String msg = "Runtime Error";
//                if (timeInfo != null) {
//                    msg += " (" + timeInfo + ")";
//                } else if (runResult.getMessage() != null) {
//                    msg += ": " + runResult.getMessage();
//                }
//                return new TestCaseResult(index, TestCaseStatus.RE, msg, inFile, outFile, runResult.getStdout(), expectedOutput);
//            }
//
//            String actualOutput = runResult.getStdout();
//            boolean accepted = normalizeOutput(actualOutput).equals(normalizeOutput(expectedOutput));
//
//            String msg;
//            if (accepted) {
//                msg = (timeInfo != null) ? ("Accepted (" + timeInfo + ")") : "Accepted";
//            } else {
//                msg = (timeInfo != null) ? ("Wrong Answer (" + timeInfo + ")") : "Wrong Answer";
//            }
//
//            return new TestCaseResult(index, accepted ? TestCaseStatus.AC : TestCaseStatus.WA, msg, inFile, outFile, actualOutput, expectedOutput);
//        } catch (Exception e) {
//            return new TestCaseResult(index, TestCaseStatus.RE, "Exception: " + e.getMessage(), inFile, outFile, null, null);
//        }
//    }

}