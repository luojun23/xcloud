package com.njtech.xcloud.entity.enums;

import java.util.regex.Pattern;

/**
 * @ClassName : VerifyRegexEnum
 * @Description : 参数校验正则枚举
 * @Author : 罗君
 * @Date: 2026/4/8
 */
public enum VerifyRegexEnum {

    NO("", "不校验"),
    IP("([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}", "IP地址"),
    POSITIVE_INTEGER("^[0-9]*[1-9][0-9]*$", "正整数"),
    NUMBER_LETTER_UNDER_LINE("^\\w+$", "由数字、26个英文字母或者下划线组成的字符串"),
    EMAIL("^[\\w-]+(\\.[\\w-]+)*@[\\w-]+(\\.[\\w-]+)+$", "邮箱"),
    PHONE("^(1[0-9])\\d{9}$", "手机号"),
    COMMON("^[a-zA-Z0-9_\\u4e00-\\u9fa5]+$", "数字、字母、中文、下划线"),
    PASSWORD("^(?=.*\\d)(?=.*[a-zA-Z])[\\da-zA-Z~!@#$%^&*_]{8,}$", "密码（至少8位，包含数字和字母）"),
    ACCOUNT("^[0-9a-zA-Z_]{1,}$", "账号（数字、字母、下划线）");

    private String regex;
    private String desc;

    VerifyRegexEnum(String regex, String desc) {
        this.regex = regex;
        this.desc = desc;
    }

    public String getRegex() {
        return regex;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 校验是否匹配正则
     */
    public boolean verify(String value) {
        if (this == NO) {
            return true;
        }
        if (value == null || value.isEmpty()) {
            return false;
        }
        return Pattern.matches(this.regex, value);
    }

}
