package com.njtech.xcloud.annotation;

import com.njtech.xcloud.entity.enums.VerifyRegexEnum;

import java.lang.annotation.*;

/**
 * @ClassName : VerifyParam
 * @Description : 参数校验注解
 * @Author : 罗君
 * @Date: 2026/4/8
 */
@Target({ElementType.PARAMETER, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface VerifyParam {

    /**
     * 最小长度
     */
    int min() default -1;

    /**
     * 最大长度
     */
    int max() default -1;

    /**
     * 是否必填
     */
    boolean required() default false;

    /**
     * 正则校验
     */
    VerifyRegexEnum regex() default VerifyRegexEnum.NO;

}
