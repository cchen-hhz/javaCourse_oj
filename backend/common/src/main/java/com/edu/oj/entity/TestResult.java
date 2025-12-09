package com.edu.oj.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TestResult {
    @JsonProperty("case_id")
    private Integer caseId;
    
    private Integer status;
    
    private Integer time;
    
    private Integer memory;
    
    private String input;
    
    @JsonProperty("user_output")
    private String userOutput;
    
    @JsonProperty("expected_output")
    private String expectedOutput;
    
    private String message;
}
