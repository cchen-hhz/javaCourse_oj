package com.edu.oj.controller;

import com.edu.oj.dto.SubmissionDto;
import com.edu.oj.entity.SubmissionConfig;
import com.edu.oj.entity.User;
import com.edu.oj.service.JudgeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
}
