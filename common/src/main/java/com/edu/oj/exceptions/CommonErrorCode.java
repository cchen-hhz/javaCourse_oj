package com.edu.oj.exceptions;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum CommonErrorCode implements ErrorCode {
    // 通用错误
    UNKNOWN_ERROR(500, "Internal Server Error"),
    PARAM_ERROR(400, "Parameter Error"),
    RESOURCE_NOT_FOUND(404, "Resource Not Found"),
    
    // 业务错误
    OPERATION_FAILED(400, "Operation Failed"),
    PERMISSION_DENIED(403, "Permission Denied"),
    UNAUTHORIZED(401, "Unauthorized"),
    DATA_CONFLICT(409, "Data Conflict"),
    FORBIDDEN(403, "Forbidden"),
    FILE_OPERATION_ERROR(500, "File Operation Error"),  
    BAD_REQUEST(400, "Bad Request");

    private final int status;
    private final String message;
}
