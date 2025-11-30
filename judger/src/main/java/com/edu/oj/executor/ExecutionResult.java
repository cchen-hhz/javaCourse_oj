package com.edu.oj.executor;

import lombok.Data;

@Data
public class ExecutionResult {
    private String output;
    private String error;
    private int exitCode;

    public ExecutionResult(String output, String error, int exitCode) {
        this.output = output;
        this.error = error;
        this.exitCode = exitCode;
    }

    public boolean isSuccessful() {
        return exitCode == 0;
    }
}