package com.edu.oj.executor.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RunResult {
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