package com.edu.oj.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import jakarta.servlet.http.HttpServletRequest;

import com.edu.oj.service.LoginService;
import com.edu.oj.service.RegisterService;
import com.edu.oj.dto.LoginDto;
import com.edu.oj.dto.RegisterDto;
import com.edu.oj.response.LoginRegisterResponse;

@RestController
@RequestMapping("/api/log/")
public class LogController {
    @Autowired
    LoginService loginService;

    @Autowired
    RegisterService registerService;

    @PostMapping("/login")
    public LoginRegisterResponse login(@RequestBody LoginDto dto, HttpServletRequest request) {
        return loginService.login(dto, request);
    }

    @PostMapping("/register")
    public LoginRegisterResponse register(@RequestBody RegisterDto dto) {
        return registerService.register(dto);
    }
}