package com.edu.oj.exceptions;

public class BusinessException extends RuntimeException {
    private final CommonErrorCode errorCode;

    public BusinessException(CommonErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(CommonErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public CommonErrorCode getErrorCode() {
        return errorCode;
    }
}
