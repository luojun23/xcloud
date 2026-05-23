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

    @Value("${qq.url.user.info}")
    private String qqUrlUserInfo;

    @Value("${qq.url.redirect}")
    private String qqUrlRedirect;

    @Value("${ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    public String getAdminEmails() {
        return adminEmails;
    }

    public String getFromEmail() {
        return fromEmail;
    }

    public String getQqAppId() {
        return qqAppId;
    }

    public String getQqAppKey() {
        return qqAppKey;
    }

    public String getQqUrlAuthorization() {
        return qqUrlAuthorization;
    }

    public String getQqUrlAccessToken() {
        return qqUrlAccessToken;
    }

    public String getQqUrlOpenid() {
        return qqUrlOpenid;
    }

    public String getQqUrlUserInfo() {
        return qqUrlUserInfo;
    }

    public String getQqUrlRedirect() {
        return qqUrlRedirect;
    }

    public String getFfmpegPath() {
        return ffmpegPath;
    }
}
