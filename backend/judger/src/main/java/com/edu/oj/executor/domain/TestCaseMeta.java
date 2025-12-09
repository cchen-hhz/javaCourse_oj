package com.edu.oj.executor.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseMeta {
    private int index;
    private int caseId;
    private String inputFile;
    private String outputFile;
}
