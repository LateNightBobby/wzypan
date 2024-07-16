package com.wzypan.annotation;

import org.springframework.web.bind.annotation.Mapping;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE}) //执行的类型 方法
@Retention(RetentionPolicy.RUNTIME) //运行时（源码、class字节码）
@Documented
@Mapping
public @interface GlobalInterceptor {

    /**
     * 校验参数
     * @return
     */
    boolean checkParams() default false;

    boolean checkLogin() default false;

    boolean checkAdmin() default false;
}
