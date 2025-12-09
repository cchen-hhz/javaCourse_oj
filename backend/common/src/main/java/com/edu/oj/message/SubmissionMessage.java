package com.edu.oj.message;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SubmissionMessage {
    public Long submissionId;
    public Long problemId;
}
