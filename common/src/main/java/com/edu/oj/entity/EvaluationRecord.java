package com.edu.oj.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EvaluationRecord {
    private Long id;
    private LocalDateTime time;
    private String status;
    private Long userId;
    private String Language;
    private Short score;
}
