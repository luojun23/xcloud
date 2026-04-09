package com.njtech.xcloud.aspect;

import com.njtech.xcloud.annotation.GlobalInterceptor;
import com.njtech.xcloud.annotation.VerifyParam;
import com.njtech.xcloud.entity.constants.Constants;
import com.njtech.xcloud.entity.enums.ResponseCodeEnum;
import com.njtech.xcloud.entity.enums.VerifyRegexEnum;
import com.njtech.xcloud.entity.vo.SessionWebUserVO;
import com.njtech.xcloud.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * @ClassName : GlobalOperationAspect
 * @Description : 全局操作切面（登录校验、参数校验）
 * @Author : 罗君
 * @Date: 2026/4/8
 */
@Aspect
@Component("globalOperationAspect")
@Slf4j
public class GlobalOperationAspect {

    /**
     * 切点：拦截带有@GlobalInterceptor注解的方法
     */
    @Pointcut("@annotation(com.njtech.xcloud.annotation.GlobalInterceptor)")
    private void requestInterceptor() {
    }

    /**
     * 前置通知：执行登录校验和参数校验
     */
    @Before("requestInterceptor()")
    public void interceptorDo(JoinPoint point) {
        try {
            // 获取目标方法
            Method method = ((MethodSignature) point.getSignature()).getMethod();
            // 获取注解
            GlobalInterceptor interceptor = method.getAnnotation(GlobalInterceptor.class);
            if (interceptor == null) {
                return;
            }

            // 1. 登录校验
            if (interceptor.checkLogin()) {
                checkLogin();
            }

            // 2. 管理员权限校验
            if (interceptor.checkAdmin()) {
                checkAdmin();
            }

            // 3. 参数校验
            Object[] arguments = point.getArgs();
            Parameter[] parameters = method.getParameters();
            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                Object value = arguments[i];
                VerifyParam verifyParam = parameter.getAnnotation(VerifyParam.class);
                if (verifyParam != null) {
                    // 校验基本类型参数
                    verifyValue(parameter.getName(), value, verifyParam);
                } else if (value != null) {
                    // 校验对象类型参数（校验对象中的字段）
                    verifyObjectField(value);
                }
            }

        } catch (BusinessException e) {
            log.error("全局拦截校验失败", e);
            throw e;
        } catch (Exception e) {
            log.error("全局拦截校验异常", e);
            throw new BusinessException(ResponseCodeEnum.CODE_500);
        }
    }

    /**
     * 校验登录状态
     */
    private void checkLogin() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        HttpSession session = request.getSession();
        SessionWebUserVO userVO = (SessionWebUserVO) session.getAttribute(Constants.SESSION_WEB_USER);
        if (userVO == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
    }

    /**
     * 校验管理员权限
     */
    private void checkAdmin() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        HttpSession session = request.getSession();
        SessionWebUserVO userVO = (SessionWebUserVO) session.getAttribute(Constants.SESSION_WEB_USER);
        if (userVO == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }
        if (!Boolean.TRUE.equals(userVO.getAdmin())) {
            throw new BusinessException(ResponseCodeEnum.CODE_902);
        }
    }

    /**
     * 校验对象中的字段（带有@VerifyParam注解的字段）
     */
    private void verifyObjectField(Object object) throws Exception {
        if (object == null) {
            return;
        }
        // 只校验自定义对象，排除基本类型和包装类型
        if (object.getClass().isPrimitive() || object instanceof String || object instanceof Number || object instanceof Boolean) {
            return;
        }
        // 排除集合类型和数组
        if (object instanceof java.util.Collection || object.getClass().isArray()) {
            return;
        }

        Field[] fields = object.getClass().getDeclaredFields();
        for (Field field : fields) {
            VerifyParam verifyParam = field.getAnnotation(VerifyParam.class);
            if (verifyParam == null) {
                continue;
            }
            field.setAccessible(true);
            Object value = field.get(object);
            verifyValue(field.getName(), value, verifyParam);
        }
    }

    /**
     * 校验单个值
     */
    private void verifyValue(String fieldName, Object value, VerifyParam verifyParam) {
        // 是否必填校验
        if (verifyParam.required()) {
            if (value == null || (value instanceof String && ((String) value).trim().isEmpty())) {
                throw new BusinessException(ResponseCodeEnum.CODE_903.getCode(), fieldName + "不能为空");
            }
        }

        // 值为空且非必填，直接返回
        if (value == null) {
            return;
        }

        // 转换为字符串进行后续校验
        String strValue = value.toString();

        // 最小长度校验
        if (verifyParam.min() != -1 && strValue.length() < verifyParam.min()) {
            throw new BusinessException(ResponseCodeEnum.CODE_903.getCode(), fieldName + "长度不能小于" + verifyParam.min());
        }

        // 最大长度校验
        if (verifyParam.max() != -1 && strValue.length() > verifyParam.max()) {
            throw new BusinessException(ResponseCodeEnum.CODE_903.getCode(), fieldName + "长度不能大于" + verifyParam.max());
        }

        // 正则校验
        VerifyRegexEnum regex = verifyParam.regex();
        if (regex != VerifyRegexEnum.NO && !regex.verify(strValue)) {
            throw new BusinessException(ResponseCodeEnum.CODE_903.getCode(), fieldName + "格式不正确，需要符合" + regex.getDesc());
        }
    }

}
