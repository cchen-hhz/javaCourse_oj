package com.edu.oj.executor.domain;

import com.edu.oj.judge.TestCaseStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SingleCaseResultDto {
    private int caseId;
    private int index;
    private TestCaseStatus status;
    private String message;
    private String actualOutput;
    private Long execTimeMs;
    private Integer memoryKb;   // 新增字段，对应 results.yaml 里的 memoryKb

}