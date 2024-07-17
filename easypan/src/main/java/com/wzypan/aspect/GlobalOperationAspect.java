package com.wzypan.aspect;

import com.wzypan.annotation.GlobalInterceptor;
import com.wzypan.annotation.VerifyParam;
import com.wzypan.entity.constants.Constants;
import com.wzypan.entity.dto.SessionWebUserDto;
import com.wzypan.entity.enums.ResponseCodeEnum;
import com.wzypan.exception.BusinessException;
import com.wzypan.utils.VerifyUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
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
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Objects;

@Aspect
@Component("globalOperationAspect")
public class GlobalOperationAspect {

    private static final String[] TYPE_BASE = {"java.lang.String", "java.lang.Integer", "java.lang.Long"};

    @Pointcut("@annotation(com.wzypan.annotation.GlobalInterceptor)")
    private void requestInterceptor() {

    }

    @Before("requestInterceptor()")
    public void interceptorDo(JoinPoint point) throws BusinessException {
        try {
            Object target = point.getTarget();
            Object[] args = point.getArgs();
            String methodName = point.getSignature().getName();
            Class<?>[] paramTypes = ((MethodSignature) point.getSignature()).getMethod().getParameterTypes();
            Method method = target.getClass().getMethod(methodName, paramTypes);
            GlobalInterceptor interceptor = method.getAnnotation(GlobalInterceptor.class);
            if (null == interceptor) {
                return;
            }

            //校验登录
            if (interceptor.checkLogin() || interceptor.checkAdmin()) {
                checkLogin(interceptor.checkAdmin());
            }

            //校验参数
            if (interceptor.checkParams()) {
                validateParams(method, args);
            }

//            Object pointResult = point.proceed();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ResponseCodeEnum.CODE_600.getCode(), "validate exception");
        } catch (Throwable e) {
            throw new BusinessException(ResponseCodeEnum.CODE_600.getCode(), "global aspect exception");
        }
    }

    private void validateParams(Method method, Object[] args) {
        Parameter[] params = method.getParameters();
        for (int i = 0; i < params.length; ++i) {
            Parameter param = params[i];
            Object value = args[i];
            //获取对应参数采用的验证方式注解
            VerifyParam verifyParam = param.getAnnotation(VerifyParam.class);
            if (null == verifyParam) {
                continue;
            }

            if (ArrayUtils.contains(TYPE_BASE, param.getParameterizedType().getTypeName())) {
                checkValue(value, verifyParam);
            }else {
                checkObjValue(param, value);
            }

        }
    }

    private void checkObjValue(Parameter parameter, Object value) {

    }

    private void checkLogin(Boolean checkAdmin) {
        //通过提取请求提取session获得用户信息
        HttpServletRequest request = ((ServletRequestAttributes) (Objects.requireNonNull(RequestContextHolder.getRequestAttributes()))).getRequest();
        HttpSession session = request.getSession();
        SessionWebUserDto userDto = (SessionWebUserDto) session.getAttribute(Constants.SESSION_KEY);

        if (userDto==null) {
            throw new BusinessException(ResponseCodeEnum.CODE_901);
        }

        if (checkAdmin && !userDto.getAdmin()) {
            throw new BusinessException(ResponseCodeEnum.CODE_404.getCode(), "权限不足无法访问");
        }
    }

    private void checkValue(Object value, VerifyParam verifyParam) {
        boolean isEmpty = value==null || StringUtils.isEmpty(value.toString());
        Integer length = value==null ? 0 : value.toString().length();

        /**
         * 校验空
         */
        if (isEmpty && verifyParam.required()) {
            throw new BusinessException(ResponseCodeEnum.CODE_600.getCode(), "empty Param");
        }
        /**
         * 校验长度
         */
        if (!isEmpty && (verifyParam.max() != -1 && verifyParam.max() < length || verifyParam.min() != -1 && verifyParam.min() > length)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600.getCode(), "param length illegal");
        }
        /**
         * 校验正则
         */
        if (!isEmpty && !StringUtils.isEmpty(verifyParam.regex().getRegex()) && !VerifyUtils.verify(verifyParam.regex(), String.valueOf(value))) {
            throw new BusinessException(ResponseCodeEnum.CODE_600.getCode(), "regex verified fail");
        }
    }

}
