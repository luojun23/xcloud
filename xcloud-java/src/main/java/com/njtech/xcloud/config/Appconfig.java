package com.njtech.xcloud.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @ClassName : Appconfig
 * @Description :
 * @Author : 罗君
 * @Date: 2026/4/7
 */
@Component("appconfig")
public class Appconfig {

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${admin.emails}")
    private String adminEmails;

    public String getAdminEmails() {
        return adminEmails;
    }

    public String getFromEmail() {
        return fromEmail;
    }
}
