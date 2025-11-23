package com.edu.oj.service;

import com.edu.oj.mapper.UserMapper;
import com.edu.oj.response.LoginRegisterResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.edu.oj.dto.LoginRegisterDto;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
public class LoginService {
    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired 
    UserMapper userMapper;

    /**
     * 完成一个用户认证操作
     * @param dto
     * @param request
     * @return LoginRegisterResponse
     * @throws AuthenticationException
     */
    public LoginRegisterResponse login(LoginRegisterDto dto, HttpServletRequest request) throws AuthenticationException{
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                dto.getUsername(), 
                dto.getPassword()
            )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        //设置 Session
        HttpSession session = request.getSession(true);
        session.setAttribute("SPRING_SECURITY_CONTEXT", SecurityContextHolder.getContext());
        
        var user = userMapper.findUserByUsername(dto.getUsername());
        
        return new LoginRegisterResponse(user, "Login successful");
    }
}