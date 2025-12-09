package com.edu.oj.controller;

import com.edu.oj.dto.SubmissionDto;
import com.edu.oj.entity.SubmissionConfig;
import com.edu.oj.entity.User;
import com.edu.oj.service.JudgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.edu.oj.entity.Role;
import com.edu.oj.entity.Submission;
import com.edu.oj.exceptions.BusinessException;
import com.edu.oj.exceptions.CommonErrorCode;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    @Autowired
    private JudgeService judgeService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public Long submitCode(@RequestBody SubmissionDto submissionDto, @AuthenticationPrincipal Object principal) throws IOException {
        User user = (User) principal;
        return judgeService.handleSubmission(user.getId(), submissionDto);
    }

    @GetMapping("/{submissionId}")
    public SubmissionConfig getSubmissionResult(@PathVariable Long submissionId) throws IOException {
        return judgeService.getSubmissionResult(submissionId);
    }

    @GetMapping("/{submissionId}/code")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> getSubmissionCode(@PathVariable Long submissionId, @AuthenticationPrincipal Object principal) throws IOException {
        User user = (User) principal;
        Submission submission = judgeService.getSubmissionById(submissionId);
        
        if (submission == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "Submission not found");
        }

        if (!user.getId().equals(submission.getUserId()) && user.getRole() == Role.USER) {
            throw new BusinessException(CommonErrorCode.FORBIDDEN, "You are not allowed to view this code");
        }

        String code = judgeService.getSubmissionCode(submissionId);
        return ResponseEntity.ok(code);
    }
}
