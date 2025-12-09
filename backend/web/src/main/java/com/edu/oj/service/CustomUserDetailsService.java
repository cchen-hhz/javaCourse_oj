package com.edu.oj.service;

import com.edu.oj.mapper.UserMapper;

import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.UserDetailsService;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    @Autowired 
    private UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException{
        var user = userMapper.findUserByUsername(username);
        if(user == null) {
            throw new UsernameNotFoundException("user " + username + "not found");
        }

        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getUsername())
            .password(user.getPassword())
            .roles(user.getRole().name())
            .disabled(!user.getEnabled())
            .build();
    }
}