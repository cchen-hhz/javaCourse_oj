package com.edu.oj.executor.domain;

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
/*
docker run --rm
  -v "$PWD/data/submission/1":/app/code:ro \
  -v "$PWD/data/problem/1/testCases":/app/input:ro \
  python-judge \
  code.py /app/input/1.in
 */