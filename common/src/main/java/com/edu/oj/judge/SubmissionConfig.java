package com.edu.oj.judge;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubmissionConfig {
    public int status;
    public int score;
    @JsonProperty("time_used")
    public int timeUsed;
    @JsonProperty("memory_used")
    public int memoryUsed;
    @JsonProperty("compile_message")
    public String compileMessage;
    @JsonProperty("judge_message")
    public String judgeMessage;

    @JsonProperty("test_result")
    public List<TestResult> testResult;
}
