package com.njtech.xcloud.aspect;

import com.njtech.xcloud.annotation.GlobalInterceptor;
import com.njtech.xcloud.annotation.VerifyParam;
import com.njtech.xcloud.entity.constants.Constants;
import com.njtech.xcloud.entity.enums.ResponseCodeEnum;
import com.njtech.xcloud.entity.enums.VerifyRegexEnum;
import com.njtech.xcloud.entity.vo.SessionWebUserVO;
import com.njtech.xcloud.exception.BusinessException;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * 全局操作切面
 * 实现登录校验和参数校验
 */
@Aspect
@Component
public class GlobalOperationAspect {

    private static final Logger logger = LoggerFactory.getLogger(GlobalOperationAspect.class);

    /**
     * 拦截带有 @GlobalInterceptor 注解的方法
     */
    @Before("@annotation(com.njtech.xcloud.annotation.GlobalInterceptor)")
    public void interceptorDo(JoinPoint point) throws Exception {
        try {
            Method method = ((MethodSignature) point.getSignature()).getMethod();
            // 获取方法参数类型
            //Class<?>[] parameterTypes = ((MethodSignature) point.getSignature()).getMethod().getParameterTypes();
            GlobalInterceptor interceptor = method.getAnnotation(GlobalInterceptor.class);

            if (interceptor == null) {
                return;
            }

            // 1. 登录校验
            if (interceptor.checkLogin()) {
                checkLogin();
            }

            // 2. 参数校验
            if (interceptor.checkParams()) {
                validateParams(method, point.getArgs());
            }
        } catch (BusinessException e) {
            logger.error("全局拦截校验失败", e);
            throw e;
        } catch (Exception e) {
            logger.error("全局拦截异常", e);
            throw e;
        }
    }

    /**
     * 校验登录状态
     */
    private void checkLogin() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        HttpSession session = request.getSession();
        SessionWebUserVO userInfo = (SessionWebUserVO) session.getAttribute(Constants.SESSION_WEB_USER);

        if (userInfo == null) {
            throw new BusinessException("用户未登录或登录已过期");
        }
    }

    /**
     * 校验方法参数
     */
    private void validateParams(Method method, Object[] args) throws Exception {
        Parameter[] parameters = method.getParameters();

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Object value = args[i];

            // 校验参数上的 @VerifyParam 注解
            VerifyParam verifyParam = parameter.getAnnotation(VerifyParam.class);
            if (verifyParam != null) {
                validateValue(parameter.getName(), value, verifyParam);
            }

            // 如果参数是对象，校验对象字段上的 @VerifyParam 注解
            if (value != null && !isBasicType(value.getClass())) {
                validateObject(value);
            }
        }
    }

    /**
     * 校验对象字段
     */
    private void validateObject(Object object) throws Exception {
        Class<?> clazz = object.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            VerifyParam verifyParam = field.getAnnotation(VerifyParam.class);
            if (verifyParam == null) {
                continue;
            }

            field.setAccessible(true);
            Object value = field.get(object);
            validateValue(field.getName(), value, verifyParam);
        }
    }

    /**
     * 校验单个值
     */
    private void validateValue(String fieldName, Object value, VerifyParam verifyParam) {
        // 必填校验
        if (verifyParam.required()) {
            if (value == null || (value instanceof String && ((String) value).trim().isEmpty())) {
                throw new BusinessException(ResponseCodeEnum.CODE_600);
            }
        }

        // 如果值为空且不是必填，跳过后续校验
        if (value == null || (value instanceof String && ((String) value).trim().isEmpty())) {
            return;
        }

        // 仅对字符串类型进行长度和正则校验
        if (!(value instanceof String)) {
            return;
        }

        String strValue = (String) value;

        // 最小长度校验
        if (verifyParam.min() != -1 && strValue.length() < verifyParam.min()) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        // 最大长度校验
        if (verifyParam.max() != -1 && strValue.length() > verifyParam.max()) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }

        // 正则校验
        VerifyRegexEnum regexEnum = verifyParam.regex();
        if (regexEnum != VerifyRegexEnum.NO && !regexEnum.verify(strValue)) {
            throw new BusinessException(fieldName + "格式不正确" + (regexEnum.getDesc() != null ? "，需要" + regexEnum.getDesc() : ""));
        }
    }

    /**
     * 判断是否为基本类型
     */
    private boolean isBasicType(Class<?> clazz) {
        return clazz.isPrimitive()
                || clazz == String.class
                || clazz == Integer.class
                || clazz == Long.class
                || clazz == Double.class
                || clazz == Float.class
                || clazz == Boolean.class
                || clazz == Byte.class
                || clazz == Short.class
                || clazz == Character.class;
    }
}
