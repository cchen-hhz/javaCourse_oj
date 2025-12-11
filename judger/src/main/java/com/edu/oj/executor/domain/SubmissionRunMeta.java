package com.edu.oj.executor.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionRunMeta {
    private Language language;
    private Long timeLimitMs;
    private Long memoryLimitMb;
    private String executableName; // "main"
    private List<TestCaseMeta> testCases;
}
