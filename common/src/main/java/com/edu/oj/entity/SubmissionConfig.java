package com.edu.oj.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    
    @JsonProperty("time_used")
    private Integer timeUsed;
    
    @JsonProperty("memory_used")
    private Integer memoryUsed;
    
    @JsonProperty("compile_message")
    private String compileMessage;    

    @JsonProperty("test_result")
    private List<TestResult> testResult;
}
