package com.edu.oj.exceptions;

public enum CommonErrorCode {
    BAD_REQUEST(400),
    RESOURCE_NOT_FOUND(404),
    SYSTEM_ERROR(500);

    private final int code;

    CommonErrorCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
