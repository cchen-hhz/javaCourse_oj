package com.edu.oj.dto;

import lombok.Data;
import com.edu.oj.entity.Role;

@Data
public class RegisterDto {
    private String username;
    private String password;
    private String description; //个人简介
    private Role role = Role.USER; // 默认注册为普通用户
}
