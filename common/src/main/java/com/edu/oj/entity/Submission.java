package com.edu.oj.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Submission {
    private Long id;
    private LocalDateTime submissionTime;
    private Status status;
    private Long userId;
    private Long problemId;
    private String language;
    private Short score;
}
