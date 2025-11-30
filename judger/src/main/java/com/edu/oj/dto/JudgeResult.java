package com.edu.oj.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * Represents the overall result of judging a submission.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JudgeResult {
    /**
     * Overall status code:
     * 0 - Accepted
     * 1 - Wrong Answer
     * 2 - Time Limit Exceeded
     * 3 - Memory Limit Exceeded
     * 4 - Runtime Error
     * 5 - Compilation Error
     * 6 - System Error
     */
    private int status;
    private int score;
    private long timeUsed;
    private long memoryUsed;
    private String compileMessage;
    private String judgeMessage;
    private List<TestCaseResult> testCases;
}
