package com.wzypan.entity.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("appConfig")
@Getter
public class AppConfig {

    @Value("${spring.mail.username}")
    private String username;

    @Value("${admin.emails}")
    private String adminEmails;

    @Value("${spring.mail.password}")
    private String password;

    @Value("${project.folder}")
    private String projectFolder;

    @Value("${qq.app.id}")
    private String qqAppId;

    @Value("${qq.app.key}")
    private String qqAppKey;

    @Value("${qq.url.authorization}")
    private String qqUrlAuthorization;

    @Value("${qq.url.access.token}")
    private String qqUrlAccessToken;

    @Value("${qq.url.openid}")
    private String qqUrlOpenid;

    @Getter
    @Value("${qq.url.user.info}")
    private String qqUrlUserInfo;

    @Getter
    @Value("${qq.url.redirect}")
    private String qqUrlRedirect;


}
