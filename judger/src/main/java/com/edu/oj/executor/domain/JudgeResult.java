package com.edu.oj.executor.domain;

import com.edu.oj.executor.CodeExecutor;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class JudgeResult {
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