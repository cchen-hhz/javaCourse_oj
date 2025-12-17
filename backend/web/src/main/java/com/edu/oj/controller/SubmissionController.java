package com.edu.oj.controller;

import com.edu.oj.dto.SubmissionDto;
import com.edu.oj.entity.SubmissionConfig;
import com.edu.oj.entity.User;
import com.edu.oj.service.JudgeService;
import com.edu.oj.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.edu.oj.entity.Role;
import com.edu.oj.entity.Submission;
import com.edu.oj.exceptions.BusinessException;
import com.edu.oj.exceptions.CommonErrorCode;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {
    @Autowired
    private JudgeService judgeService;

    @Autowired
    private UserService userService;

    @GetMapping("/")
    public Submission[] getSubmissions(@RequestParam(required = false) Long userId,
                                       @RequestParam(required = false) Long problemId,
                                       @RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        return judgeService.getSubmissions(userId, problemId, page, size);
    }
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public Long submitCode(@RequestBody SubmissionDto submissionDto) throws IOException {
        String username = org.springframework.security.core.context.SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getName();
        User user = userService.findUserByUsername(username);
        return judgeService.handleSubmission(user.getId(), submissionDto);
    }

    @GetMapping("/{submissionId}/result")
    public SubmissionConfig getSubmissionResult(@PathVariable Long submissionId) throws IOException {
        return judgeService.getSubmissionResult(submissionId);
    }

    @GetMapping("/{submissionId}/code")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> getSubmissionCode(@PathVariable Long submissionId) throws IOException {
        String username = org.springframework.security.core.context.SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getName();
        User user = userService.findUserByUsername(username);
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
