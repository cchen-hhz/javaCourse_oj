package com.edu.oj.executor.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionRunResult {
    private List<SingleCaseResultDto> cases;

    public SingleCaseResultDto findByCaseId(int caseId) {
        if (cases == null) return null;
        for (SingleCaseResultDto c : cases) {
            if (c.getCaseId() == caseId) return c;
        }
        return null;
    }
}