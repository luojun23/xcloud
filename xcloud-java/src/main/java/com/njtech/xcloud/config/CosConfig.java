package com.njtech.xcloud.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @ClassName : CosConfig
 * @Description :
 * @Author : 罗君
 * @Date: 2026/4/10
 */
@Component
public class CosConfig implements org.springframework.beans.factory.InitializingBean {
    @Value("${tencent.cos.file.region}")
    private String region;

    @Value("${tencent.cos.file.secretid}")
    private String secretId;

    @Value("${tencent.cos.file.secretkey}")
    private String secretKey;

    @Value("${tencent.cos.file.bucketname}")
    private String bucketName;

    @Value("${project.folder}")
    private String projectFolder;

    public static String END_POINT;
    public static String ACCESS_KEY_ID;
    public static String ACCESS_KEY_SECRET;
    public static String BUCKET_NAME;
    public static String PROJECT_FOLDER;

    @Override
    public void afterPropertiesSet() throws Exception {
        END_POINT = region;
        ACCESS_KEY_ID = secretId;
        ACCESS_KEY_SECRET = secretKey;
        BUCKET_NAME = bucketName;
        PROJECT_FOLDER = projectFolder;
    }
}
