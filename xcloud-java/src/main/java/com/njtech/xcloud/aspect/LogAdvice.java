package com.njtech.xcloud.aspect;

import com.njtech.xcloud.utils.IpUtil;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * @ClassName : LogAdvice
 * @Description : aop切面类
 * @Author : 罗君
 * @Date: 2026/4/8
 */
@Aspect
@Component("logAdvice")
@Slf4j
public class LogAdvice {

    @Pointcut("@annotation(org.springframework.web.bind.annotation.GetMapping)")
    private void logAdvicePointCut(){}

    @Before("logAdvicePointCut()")
    public void logAdvice(){
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        // 获取请求URL
        String requestURL = request.getRequestURL().toString();
        // 获取请求IP
        String requestIP = IpUtil.getClientIp(request);
        log.info("请求的URL为：{},请求的IP为：{}", requestURL, requestIP);
        log.info("get请求的Advice被触发了");
    }
}
