package com.edu.oj.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResultMessage {
    private Long submissionId;
    private Long problemId;

    private Long testCaseId;
    private Long numCases;
    private Long score;
    private Long timeUsed;
    private Long memoryUsed;
    private Long status;

    private String input;
    private String expectedOutput;
    private String userOutput;
    private String message;

    private Boolean correct;
    private Boolean isOver;
}
