package com.edu.oj.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import com.edu.oj.judge.ProblemConfig;
import com.edu.oj.utils.ProblemFileManager;

import java.io.IOException;

@RestController
@RequestMapping("/api/")
public class FileTestController {
    @Autowired
    ProblemFileManager manager;

    @GetMapping("/problem/{problemId}")
    public ProblemConfig getProblem(@PathVariable("problemId") Long problemId) {
        try {
            return manager.getProblemConfig(problemId);
        } catch (IOException e) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to load problem config: " + e.getMessage()
            );
        }
    }

    
}
