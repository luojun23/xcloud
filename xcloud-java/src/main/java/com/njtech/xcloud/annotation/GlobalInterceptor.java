package com.njtech.xcloud.annotation;

import java.lang.annotation.*;

/**
 * @ClassName : GlobalInterceptor
 * @Description : 全局拦截注解
 * @Author : 罗君
 * @Date: 2026/4/8
 */
//作用域
@Target({ElementType.METHOD, ElementType.TYPE})
//运行时依然有效 → 可以被反射读取
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface GlobalInterceptor {

    /**
     * 是否需要登录校验
     */
    boolean checkLogin() default true;

    /**
     * 是否需要参数校验
     */
    boolean checkParams() default false;
}
