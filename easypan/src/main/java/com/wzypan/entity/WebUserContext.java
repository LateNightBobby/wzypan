package com.wzypan.entity;

import com.wzypan.entity.dto.SessionWebUserDto;

public class WebUserContext {
    private static final ThreadLocal<SessionWebUserDto> webUserThreadLocal = new ThreadLocal<>();

    public static void setWebUser(SessionWebUserDto webUser) {
        webUserThreadLocal.set(webUser);
    }

    public static SessionWebUserDto getWebUser() {
        return webUserThreadLocal.get();
    }

    public static void clear() {
        webUserThreadLocal.remove();
    }
}
