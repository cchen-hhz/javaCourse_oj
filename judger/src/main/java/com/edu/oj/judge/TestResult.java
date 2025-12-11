package com.edu.oj.judge;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TestResult {
    public Integer caseId;
    public Integer status;
    public Integer time;
    public Integer memory;
    public String input;
    @JsonProperty("user_output")
    public String userOutput;
    @JsonProperty("expected_output")
    public String expectedOutput;
    public String message;
}