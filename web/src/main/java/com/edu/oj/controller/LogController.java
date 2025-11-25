package com.edu.oj.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import jakarta.servlet.http.HttpServletRequest;

import com.edu.oj.response.ApiResponse;
import com.edu.oj.service.LoginService;
import com.edu.oj.service.RegisterService;
import com.edu.oj.dto.LoginDto;
import com.edu.oj.dto.RegisterDto;
import com.edu.oj.response.LoginRegisterResponse;
import org.springframework.security.core.AuthenticationException;
import com.edu.oj.exceptions.RegisterException;

@RestController
@RequestMapping("/api/user/")
public class LogController {
    @Autowired
    LoginService loginService;

    @Autowired
    RegisterService registerService;

    @PostMapping("/login")
    public ApiResponse<LoginRegisterResponse> login(@RequestBody LoginDto dto, HttpServletRequest request) 
        throws AuthenticationException{


        var response = loginService.login(dto, request);
        return ApiResponse.success(response);
    }

    @PostMapping("/register")
    public ApiResponse<LoginRegisterResponse> register(@RequestBody RegisterDto dto) throws RegisterException {
        var response = registerService.register(dto);
        return ApiResponse.success(response);
    }
    
}