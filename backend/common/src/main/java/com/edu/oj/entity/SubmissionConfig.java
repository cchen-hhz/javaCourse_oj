package com.edu.oj.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubmissionConfig {
    private Integer status;
    private Integer score;
    private Integer timeUsed;
    private Integer memoryUsed;
    private String compileMessage;    
    private List<TestResult> testResult;
}
