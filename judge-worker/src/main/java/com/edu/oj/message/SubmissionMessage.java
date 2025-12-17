package com.edu.oj.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubmissionMessage {
    public Long submissionId;
    public Long problemId;
    public String language;
}
