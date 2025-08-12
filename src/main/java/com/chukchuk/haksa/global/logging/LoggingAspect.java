package com.chukchuk.haksa.global.logging;

import com.chukchuk.haksa.global.exception.BaseException;
import com.chukchuk.haksa.global.logging.annotation.LogPart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.MDC;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

@Aspect
@Slf4j
@Component
@RequiredArgsConstructor
public class LoggingAspect {

    @Around("@within(com.chukchuk.haksa.global.logging.annotation.LogPart) || @annotation(com.chukchuk.haksa.global.logging.annotation.LogPart)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        Class<?> targetClass = AopUtils.getTargetClass(joinPoint.getTarget());
        try {
            Method implMethod = ReflectionUtils.findMethod(targetClass, method.getName(), signature.getParameterTypes());
            if (implMethod != null) method = implMethod;
        } catch (Exception ignored) {
            log.debug("Method not found for annotation lookup: {}", signature.getName());
        }

        LogPart methodAnnotation = AnnotatedElementUtils.findMergedAnnotation(method, LogPart.class);
        LogPart classAnnotation  = AnnotatedElementUtils.findMergedAnnotation(targetClass, LogPart.class);
        String part = methodAnnotation != null ? methodAnnotation.value()
                : (classAnnotation != null ? classAnnotation.value() : "unknown");

        String userId = resolveUserUUID();
        String className = targetClass.getName();
        String methodName = method.getName();
        String msg = className + "." + methodName + "() called";

        // MDC 필드 설정
        MDC.put("part", part);
        MDC.put("userId", userId == null ? "Unknown" : userId);
        MDC.put("className", className);
        MDC.put("method", methodName);

        try {
            Object result = joinPoint.proceed();
            log.info(msg);
            return result;
        } catch (BaseException be) {
            log.warn(msg);
            throw be;
        } catch (Exception e) {
            log.error(msg);
            throw e;
        } finally {
            // MDC 정리(키 단위)
            MDC.remove("part");
            MDC.remove("userId");
            MDC.remove("className");
            MDC.remove("method");
        }
    }

    private String resolveUserUUID() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        return null;
    }

    private static Class<?>[] getParamTypes(ProceedingJoinPoint joinPoint) {
        if (joinPoint.getSignature() instanceof MethodSignature ms) {
            return ms.getParameterTypes();
        }
        Object[] args = joinPoint.getArgs();
        Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) types[i] = args[i] == null ? Object.class : args[i].getClass();
        return types;
    }
}
