package com.edu.oj.executor;

import com.edu.oj.executor.codeRunner.CodeRunner;
import com.edu.oj.executor.domain.*;
import com.edu.oj.executor.util.CompileOnce;
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

import static com.edu.oj.executor.domain.CodeFileInfo.detectCodeFile;
import static com.edu.oj.executor.domain.ProblemConfig.loadProblemConfig;
import static com.edu.oj.executor.util.CompileOnce.compileOnce;
import static com.edu.oj.executor.util.NormalizeOutput.normalizeOutput;

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

}