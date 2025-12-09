package com.edu.oj.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubmissionDto {
    private Long userId;
    private Long problemId;
    private String language;
    private String code;
}

