package com.njtech.xcloud.entity.constants;

/**
 * @ClassName : Constants
 * @Description : 常量
 * @Author : 罗君
 * @Date: 2026/4/8
 */
public class Constants {
    public static final Integer ONE = 1;
    public static final Integer TEN = 10;

    public static final String SESSION_WEB_USER = "session_web_user";
    public static final String REDIS_KEY_USER_SPACE_USE = "redis_key_user_space_use";
    public static final String REDIS_KEY_TEMP_SIZE = "redis_key_temp_size:";

    public static final Integer USING = 2;

    public static final long MB = 1024 * 1024L;

    public static final int REDIS_KEY_EXPIRES_DAY = 24 * 60 * 60;

    public static final int REDIS_KEY_ONE_HOUR = 1 * 60 * 60;

    public static final int REDIS_KEY_ONE_DAY = 24 * 60 * 60;

    /**
     * 文件状态 0:转码中 1:转码失败 2:转码成功
     */
    public static final Integer TRANSFER_ING = 0;
    public static final Integer TRANSFER_FAIL = 1;
    public static final Integer TRANSFER_SUCCESS = 2;

    /**
     * 视频切片文件
     */
    public static final String TS_NAME = "index.ts";
    public static final String M3U8_NAME = "index.m3u8";

    /**
     * 缩略图尺寸
     */
    public static final Integer THUMB_WIDTH = 150;
    public static final Integer THUMB_HEIGHT = 150;

    /**
     * COS文件目录
     */
    public static final String FILE_FOLDER_FILE = "/file/";
    public static final String FILE_FOLDER_TEMP = "/file/temp/";
    public static final String FILE_FOLDER_AVATAR = "/file/avatar/";
}
