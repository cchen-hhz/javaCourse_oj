package com.edu.oj.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.edu.oj.entity.User;
import com.edu.oj.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.DeleteMapping;


@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    UserService userService;

    @GetMapping("/{userId}")
    public User getUserById(@PathVariable Long userId) {
        return userService.findUserById(userId);
    }

    @GetMapping("/")
    public User[] getAllUsers() {
        return userService.findAllUsers();
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public User getCurrentUser() {
        String username = org.springframework.security.core.context.SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getName();  
        return userService.findUserByUsername(username);
    }

    @PostMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or authentication.principal.id == #userId")
    public void updateUser(@PathVariable Long userId, @RequestBody  User user) {
        userService.updateUser(userId, user);
    }

    @PostMapping("/{userId}/ban")
    @PreAuthorize("hasRole('ADMIN')")
    public void banUser(@PathVariable Long userId) {
        userService.banUserById(userId);
    }

    @PostMapping("/{userId}/unban")
    @PreAuthorize("hasRole('ADMIN')")
    public void unbanUser(@PathVariable Long userId) {
        userService.unbanUserById(userId);
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or authentication.pricipal.id == #userId")
    public void deleteUser(@PathVariable Long userId) {
        userService.deleteUserById(userId);
    }
}