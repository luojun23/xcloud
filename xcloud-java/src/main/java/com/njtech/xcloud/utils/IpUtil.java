package com.njtech.xcloud.utils;

import javax.servlet.http.HttpServletRequest;

/**
 * @ClassName : IpUtil
 * @Description : IP工具类
 * @Author : 罗君
 * @Date: 2026/4/8
 */
public class IpUtil {
    /**
     * 获取客户端真实IP地址（优先IPv4）
     */
    public static String getClientIp(HttpServletRequest request) {
        // 1. 从代理头获取真实IP（Nginx/SLB必备）
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            // 2. 无代理时，直接获取
            ip = request.getRemoteAddr();
        }

        // 处理多级代理：X-Forwarded-For 格式：clientIP, proxy1, proxy2
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        // 本地IPv6 转 IPv4
        if ("0:0:0:0:0:0:0:1".equals(ip)) {
            ip = "127.0.0.1";
        }

        return ip;
    }
}
