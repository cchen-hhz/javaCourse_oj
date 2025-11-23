package com.edu.oj.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Role {
    ROOT("根"),
    ADMIN("管理员"),
    USER("普通用户");

    private final String description;
}
