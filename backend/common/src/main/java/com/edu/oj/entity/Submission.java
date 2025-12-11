package com.edu.oj.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Submission {
    private Long id;
    private LocalDateTime submissionTime;
    private Status status;
    private Long userId;
    private Long problemId;
    private String language;
    private Short score;
}
