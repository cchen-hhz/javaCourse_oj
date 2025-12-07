package com.edu.oj.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.edu.oj.entity.User;
import com.edu.oj.mapper.UserMapper;
import com.edu.oj.exceptions.BusinessException;
import com.edu.oj.exceptions.CommonErrorCode;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;


@RestController
public class UserController {
    @Autowired
    UserMapper userMapper;

    @Autowired
    SessionRegistry sessionRegistry;

    @GetMapping("/user/{userId}")
    public User getUserById(@PathVariable Long userId) {
        User existingUser = userMapper.findUserById(userId);
        if (existingUser == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "User not found with ID: " + userId);
        }
        return existingUser;
    }

    @GetMapping("/user")
    public User[] getAllUsers() {
        return userMapper.findAllUsers();
    }

    @PostMapping("user/{userId}/update")
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.id == #userId")
    public void updateUser(@PathVariable Long userId, @RequestBody  User user) {
        User existingUser = userMapper.findUserById(userId);
        if (existingUser == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "User not found with ID: " + userId);
        }
        userMapper.updateUser(user);
    }

    @PostMapping("user/{userId}/ban")
    @PreAuthorize("hasRole('ADMIN')")
    public void banUser(@PathVariable Long userId) {
        User existingUser = userMapper.findUserById(userId);
        if (existingUser == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "User not found with ID: " + userId);
        }
        userMapper.banUserById(userId);

        // Force logout
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

    @PostMapping("user/{userId}/unban")
    @PreAuthorize("hasRole('ADMIN')")
    public void unbanUser(@PathVariable Long userId) {
        User existingUser = userMapper.findUserById(userId);
        if (existingUser == null) {
            throw new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND, "User not found with ID: " + userId);
        }
        userMapper.unbanUserById(userId);
    }
}