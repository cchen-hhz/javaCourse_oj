package com.edu.oj.exceptions;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BusinessException(String message) {
        super(message);
        this.errorCode = CommonErrorCode.OPERATION_FAILED;
    }

    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = CommonErrorCode.UNKNOWN_ERROR;
    }
}
