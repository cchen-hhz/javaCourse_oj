package com.edu.oj.executor.util;

import com.edu.oj.executor.domain.CodeFileInfo;
import com.edu.oj.executor.domain.CompileResult;
import com.edu.oj.executor.domain.RunRequest;
import com.edu.oj.judge.RunResult;
import com.edu.oj.executor.codeRunner.CodeRunner;

import java.nio.file.Files;
import java.nio.file.Path;

public class CompileOnce {
    public static CompileResult compileOnce(CodeRunner codeRunner, CodeFileInfo codeFileInfo) {
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

            System.out.println("???checkout:"+rr.toString());

            if (!rr.isSuccess()) {
                return new CompileResult(false, rr.getMessage(), null);
            }
            return new CompileResult(true, null, exePath);
        } catch (Exception e) {
            return new CompileResult(false, "Compile failed: " + e.getMessage(), null);
        }
    }
}
