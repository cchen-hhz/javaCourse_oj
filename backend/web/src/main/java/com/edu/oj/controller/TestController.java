package com.edu.oj.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api")
public class TestController {
    @GetMapping("/test")
    public String test() {
        return "Test Ok";
    }
}
