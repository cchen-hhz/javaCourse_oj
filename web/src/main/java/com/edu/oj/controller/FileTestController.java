package com.edu.oj.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.edu.oj.judge.ProblemConfig;
import com.edu.oj.utils.ProblemFileManager;

@RestController
@RequestMapping("/api/")
public class FileTestController {
    @Autowired
    ProblemFileManager manager;

    @GetMapping("/problem/{problemId}")
    public ProblemConfig getProblem(@PathVariable("problemId") Long problemId) throws Exception {
        return manager.getProblemConfig(problemId);
    }
}
