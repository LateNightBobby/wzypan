package com.wzypan.exception;

import com.wzypan.entity.enums.ResponseCodeEnum;
import lombok.Getter;

public class BusinessException extends RuntimeException{

    @Getter
    private final Integer code;
    private final String msg;

    public BusinessException(ResponseCodeEnum responseCode) {
        super(responseCode.getMsg());
        this.code = responseCode.getCode();
        this.msg = responseCode.getMsg();
    }

    public BusinessException(String message, Throwable cause, Integer code, String msg) {
        super(message, cause);
        this.code = code;
        this.msg = msg;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.msg = message;
    }

    @Override
    public String getMessage() {
        return msg;
    }
}
