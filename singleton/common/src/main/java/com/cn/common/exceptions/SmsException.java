package com.cn.common.exceptions;

import lombok.Data;

@Data
public class SmsException extends RuntimeException {

    private String message;

    private Integer code;

    public SmsException(final String message, final Integer code) {
        super(message);
        this.message = message;
        this.code = code;
    }

    public SmsException(final String message) {
        super(message);
        this.message = message;
        this.code = 500;
    }
}
