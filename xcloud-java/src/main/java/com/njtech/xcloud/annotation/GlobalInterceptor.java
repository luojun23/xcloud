package com.njtech.xcloud.annotation;

import java.lang.annotation.*;

/**
 * @ClassName : GlobalInterceptor
 * @Description : 全局拦截注解
 * @Author : 罗君
 * @Date: 2026/4/8
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GlobalInterceptor {

    /**
     * 是否需要登录校验
     */
    boolean checkLogin() default true;

    /**
     * 是否需要校验管理员权限
     */
    boolean checkAdmin() default false;

}
