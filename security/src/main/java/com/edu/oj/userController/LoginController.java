package com.edu.oj.userController;

import javax.naming.AuthenticationException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import jakarta.servlet.http.HttpServletRequest;

import com.edu.oj.response.ApiResponse;
import com.edu.oj.service.LoginService;
import com.edu.oj.dto.LoginRegisterDto;
import com.edu.oj.response.LoginRegisterResponse;

@RestController
@RequestMapping("/api/user/login")
public class LoginController {
    @Autowired
    LoginService loginService;

    @PostMapping("/")
    public ApiResponse<LoginRegisterResponse> login(@RequestBody LoginRegisterDto dto, HttpServletRequest request) 
        throws AuthenticationException{

        var response = loginService.login(dto, request);
        return ApiResponse.success(response);
    }
}