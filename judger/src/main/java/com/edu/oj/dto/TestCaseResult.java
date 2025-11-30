package com.edu.oj.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Represents the result of a single test case execution.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseResult {
    private int caseId;
    private int status;
    private long time;
    private long memory;
    private String input;
    private String userOutput;
    private String expectedOutput;
    private String message;
}
