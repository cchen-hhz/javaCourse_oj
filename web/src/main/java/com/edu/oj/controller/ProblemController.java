package com.edu.oj.controller;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.edu.oj.entity.Problem;
import com.edu.oj.service.ProblemService;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequestMapping("/api/problems")
public class ProblemController {

    @Autowired
    private ProblemService problemService;

    @GetMapping("/")
    public Problem[] getAllProblems() {
        return problemService.getAllProblems();
    }

    @GetMapping("/{problemId}")
    public Problem getProblem(@PathVariable Long problemId) {
        return problemService.getProblemById(problemId);
    } 

    @PostMapping("/{problemId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Problem createProblem(
        @PathVariable Long problemId,
        @RequestParam("title") String title,
        @RequestParam(required = true) MultipartFile file
    ) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            return problemService.createProblem(problemId, title, inputStream);
        }
    }

    @SuppressWarnings("null")
    @GetMapping("/{problemId}/file/{fileName}")
    public ResponseEntity<Resource> getProblemFile(
        @PathVariable Long problemId,
        @PathVariable String fileName) throws IOException {
        
        InputStream inputStream = problemService.getProblemFile(problemId, fileName);
        InputStreamResource resource = new InputStreamResource(inputStream);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resource);
    }

    @PutMapping("/{problemId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void uploadProblemData(
        @PathVariable Long problemId,
        @RequestParam(required = true) MultipartFile file) throws IOException {
        
        try (InputStream inputStream = file.getInputStream()) {
            problemService.uploadProblemData(problemId, inputStream);
        }
    }
}