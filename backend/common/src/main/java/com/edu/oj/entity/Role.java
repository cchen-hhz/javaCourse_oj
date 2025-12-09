package com.edu.oj.entity;

import lombok.Getter;

@Getter
public enum Role {
    ROOT("根"),
    ADMIN("管理员"),
    USER("普通用户");

    private final String description;

    private Role(String description) {
        this.description = description;
    }
}