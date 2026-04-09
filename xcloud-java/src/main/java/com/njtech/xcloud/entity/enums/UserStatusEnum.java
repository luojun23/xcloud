package com.njtech.xcloud.entity.enums;

public enum UserStatusEnum {
    NORMAL(0, "禁用"),
    FORBIDDEN(1, "正常");
    private Integer num;
    private String msg;

    UserStatusEnum(Integer num, String msg) {
        this.num = num;
        this.msg = msg;
    }

    public Integer getNum() {
        return num;
    }

    public String getMsg() {
        return msg;
    }
}
