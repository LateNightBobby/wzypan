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


}
