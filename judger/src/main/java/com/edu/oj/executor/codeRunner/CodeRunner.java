package com.edu.oj.executor.codeRunner;

import com.edu.oj.executor.domain.RunRequest;
import com.edu.oj.executor.domain.RunResult;

import java.io.IOException;

public interface CodeRunner {
    RunResult runInSandbox(RunRequest request) throws IOException, InterruptedException;

    RunResult compile(RunRequest req)throws IOException, InterruptedException;
}