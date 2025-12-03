package com.edu.oj.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.edu.oj.entity.User;
import com.edu.oj.mapper.UserMapper;

@Service
public class UserService {
    @Autowired
    UserMapper userMapper;

    User findUserByUsername(String username) {
        return userMapper.findUserByUsername(username);
    }

    int registerUser(User user) {
        try {
            return userMapper.insertUser(user);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Registration failed: " + e.getMessage());
        }
    }
}