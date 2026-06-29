package com.ticket.app.common.exception;

import lombok.Getter;

/**
 * 参数校验异常
 */
@Getter
public class ValidationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * 错误码
     */
    private final Integer code = 100600;

    /**
     * 错误消息
     */
    private final String message;

    /**
     * 字段名
     */
    private final String fieldName;

    public ValidationException(String message) {
        super(message);
        this.message = message;
        this.fieldName = null;
    }

    public ValidationException(String message, String fieldName) {
        super(message);
        this.message = message;
        this.fieldName = fieldName;
    }
}
