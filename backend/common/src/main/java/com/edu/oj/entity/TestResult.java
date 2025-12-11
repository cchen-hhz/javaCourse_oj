package com.edu.oj.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TestResult {
    private Integer caseId; 
    private Integer status;
    private Integer time;
    private Integer memory;
    private String input;
    private String userOutput;
    private String expectedOutput;
    private String message;
}
