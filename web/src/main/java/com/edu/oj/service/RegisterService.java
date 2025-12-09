package com.edu.oj.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.edu.oj.dto.RegisterDto;
import com.edu.oj.response.LoginRegisterResponse;

import lombok.extern.slf4j.Slf4j;

import com.edu.oj.entity.User;
import java.time.LocalDateTime;

@Service
@Slf4j
public class RegisterService {
    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    UserService userService;

    /**
     * 注册用户
     * @param user 用户实体，createdAt 在此处设置
     * @return LoginRegisterResponse
     * @throws ResponseStatusException 注册失败时抛出
     */
    private LoginRegisterResponse registerUser(User user) {
        System.out.println("Registering user: " + user.getUsername());
        user.setCreatedAt(LocalDateTime.now());
        user.setEnabled(true);
        int result = userService.registerUser(user);
        if(result < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration failed");
        }
        return new LoginRegisterResponse(user, "Registration successful");
    }

    private void SetUserDto(User user, RegisterDto dto) {
        user.setUsername(dto.getUsername());
        //没加密密码，fuck
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setDescription(dto.getDescription());
    }

    public LoginRegisterResponse register(RegisterDto dto) {
        User user = new User();
        SetUserDto(user, dto);
        user.setRole(dto.getRole());
        return registerUser(user);
    }

}
