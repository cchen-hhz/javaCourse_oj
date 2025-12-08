package com.edu.oj.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;

import com.edu.oj.exceptions.BusinessException;
import com.edu.oj.exceptions.CommonErrorCode;

import com.edu.oj.entity.User;
import com.edu.oj.mapper.UserMapper;

import java.util.List;


@Service
public class UserService {
    @Autowired
    UserMapper userMapper;

    @Autowired
    SessionRegistry sessionRegistry;

    public User[] findAllUsers() {
        return userMapper.findAllUsers();
    }

    public User findUserById(Long userId) {
        User result = userMapper.findUserById(userId);
        if (result == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "User not found with ID: " + userId);
        }
        return result;
    }

    public User findUserByUsername(String username) {
        User result = userMapper.findUserByUsername(username);
        if (result == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "User not found with username: " + username);
        }
        return result;
    }

    public int registerUser(User user) {
        return userMapper.insertUser(user);
    }

    @Transactional
    public void deleteUserById(Long userId) {
        // Ensure user exists
        findUserById(userId);
        userMapper.deleteUserById(userId);
    }

    @Transactional
    public void updateUser(Long userId, User user) {
        // Ensure user exists
        findUserById(userId);
        userMapper.updateUser(user);
    }

    @Transactional
    public void banUserById(Long userId) {
        User existingUser = findUserById(userId);
        userMapper.banUserById(userId);
        
        List<Object> principals = sessionRegistry.getAllPrincipals();
        for (Object principal : principals) {
            if (principal instanceof UserDetails) {
                UserDetails userDetails = (UserDetails) principal;
                if (userDetails.getUsername().equals(existingUser.getUsername())) {
                    List<org.springframework.security.core.session.SessionInformation> sessions = sessionRegistry.getAllSessions(principal, false);
                    for (org.springframework.security.core.session.SessionInformation session : sessions) {
                        session.expireNow();
                    }
                }
            }
        }
    }

    @Transactional
    public void unbanUserById(Long userId) {
        findUserById(userId);
        userMapper.unbanUserById(userId);
    }
}