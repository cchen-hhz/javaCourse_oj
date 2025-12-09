package com.edu.oj.entity;

import lombok.Getter;

@Getter
public enum Status {
    PENDING("等待中"),
    JUDGING("评测中"),
    DONE("已完成");

    private final String description;

    Status(String description) {
        this.description = description;
    }
}
