package com.wzypan.exception;

import com.wzypan.entity.enums.ResponseCodeEnum;
import lombok.Getter;

//public class BusinessException extends RuntimeException{
//
//    @Getter
//    private final Integer code;
//    private final String msg;
//
//    public BusinessException(ResponseCodeEnum responseCode) {
//        super(responseCode.getMsg());
//        this.code = responseCode.getCode();
//        this.msg = responseCode.getMsg();
//    }
//
//    public BusinessException(String message, Throwable cause, Integer code, String msg) {
//        super(message, cause);
//        this.code = code;
//        this.msg = msg;
//    }
//
//    public BusinessException(Integer code, String message) {
//        super(message);
//        this.code = code;
//        this.msg = message;
//    }
//
//    @Override
//    public String getMessage() {
//        return msg;
//    }
//}
//
//
//package com.easypan.exception;
//import com.easypan.entity.enums.ResponseCodeEnum;
//

public class BusinessException extends RuntimeException {

    private ResponseCodeEnum codeEnum;

    private Integer code;

    private String message;

    public BusinessException(String message, Throwable e) {
        super(message, e);
        this.message = message;
    }

    public BusinessException(String message) {
        super(message);
        this.message = message;
    }

    public BusinessException(Throwable e) {
        super(e);
    }

    public BusinessException(ResponseCodeEnum codeEnum) {
        super(codeEnum.getMsg());
        this.codeEnum = codeEnum;
        this.code = codeEnum.getCode();
        this.message = codeEnum.getMsg();
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public ResponseCodeEnum getCodeEnum() {
        return codeEnum;
    }

    public Integer getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    /**
     * 重写fillInStackTrace 业务异常不需要堆栈信息，提高效率.
     */
    @Override
    public Throwable fillInStackTrace() {
        return this;
    }

}
