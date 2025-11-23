package com.edu.oj.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.edu.oj.dto.LoginRegisterDto;
import com.edu.oj.exceptions.RegisterException;
import com.edu.oj.response.LoginRegisterResponse;
import com.edu.oj.entity.User;
import com.edu.oj.entity.Role;
import java.time.LocalDateTime;

@Service
public class RegisterService {

    @Autowired
    UserService userService;

    /**
     * 注册用户
     * @param user 用户实体，createdAt 在此处设置
     * @return LoginRegisterResponse
     * @throws RegisterException
     */
    private LoginRegisterResponse registerUser(User user) throws RegisterException {
        user.setCreatedAt(LocalDateTime.now());
        user.setEnabled(true);
        int result = userService.registerUser(user);
        if(result < 1) {
            throw new RegisterException("Registration failed");
        }
        return new LoginRegisterResponse(user, "Registration successful");
    }

    private void SetUserDto(User user, LoginRegisterDto dto) {
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword());
        user.setDescription(dto.getDescription());
    }

    public LoginRegisterResponse registerROOT(LoginRegisterDto dto) throws RegisterException {
        User user = new User();
        SetUserDto(user, dto);
        user.setRole(Role.ROOT);
        return registerUser(user);
    }

    public LoginRegisterResponse registerADMIN(LoginRegisterDto dto) throws RegisterException {
        User user = new User();
        SetUserDto(user, dto);
        user.setRole(Role.ADMIN);
        return registerUser(user);
    }

    public LoginRegisterResponse registerUSER(LoginRegisterDto dto) throws RegisterException {
        User user = new User();
        SetUserDto(user, dto);
        user.setRole(Role.USER);
        return registerUser(user);
    }
}
