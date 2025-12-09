package com.edu.oj.executor.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.file.Path;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RunRequest {
    private Language language;
    private Path codePath;
    private Path inputPath;
    private Path executablePath; // 运行阶段用（编译产物）
    private Path  testCasesDir;      // data/problem/{pid}/testCases
    private Path  metaFilePath;      // data/submission/{sid}/cases.yaml
    private Path  resultsFilePath;   // data/submission/{sid}/results.yaml

    private Long timeLimitMs;
    private Long memoryLimitMb;
    private Integer caseId;
}
