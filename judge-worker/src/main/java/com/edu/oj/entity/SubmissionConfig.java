package com.edu.oj.entity;

import lombok.Data;

@Data
public class SubmissionConfig {
    private Long submissionId;
    private Long problemId;
    private Long score;
    private Long timeUsed;
    private Long memoryUsed;
    private Long status;
    private String message;
}
