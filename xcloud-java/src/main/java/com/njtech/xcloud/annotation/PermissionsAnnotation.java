package com.njtech.xcloud.annotation;

import java.lang.annotation.*;

/**
 * @ClassName : PermissionsAnnotation
 * @Description : 自定义注解
 * @Author : 罗君
 * @Date: 2026/4/8
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface PermissionsAnnotation {

}
