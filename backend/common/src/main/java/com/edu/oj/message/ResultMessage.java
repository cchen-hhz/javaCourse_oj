package com.edu.oj.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResultMessage {
    private Long submissionId;
    private Long problemId;
    private Integer testcase;
    private Integer status;
    private Integer timeUsed;
    private Integer memoryUsed;
    private String input;
    private String userOutput;
    private String expectedOutput;
    private Boolean complete;
    private String message;
    private Map<String, Object> args;
}