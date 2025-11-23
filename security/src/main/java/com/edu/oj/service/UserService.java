package com.edu.oj.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.edu.oj.entity.User;
import com.edu.oj.exceptions.RegisterException;
import com.edu.oj.mapper.UserMapper;

@Service
public class UserService {
    @Autowired
    UserMapper userMapper;

    User findUserByUsername(String username) {
        return userMapper.findUserByUsername(username);
    }

    int registerUser(User user) throws RegisterException{
        try {
            return userMapper.insertUser(user);
        } catch (Exception e) {
            throw new RegisterException("Registration failed: " + e.getMessage());
        }
    }
}