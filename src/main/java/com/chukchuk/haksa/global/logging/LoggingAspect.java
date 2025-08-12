package com.chukchuk.haksa.global.logging;

import com.chukchuk.haksa.global.exception.BaseException;
import com.chukchuk.haksa.global.logging.annotation.LogPart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Aspect
@Slf4j
@Component
@RequiredArgsConstructor
public class LoggingAspect {

    @Around("@within(com.chukchuk.haksa.global.logging.annotation.LogPart) || @annotation(com.chukchuk.haksa.global.logging.annotation.LogPart)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        LogPart methodAnnotation = AnnotationUtils.findAnnotation(
                joinPoint.getSignature().getDeclaringType().getMethod(
                        joinPoint.getSignature().getName(), getParamTypes(joinPoint)), LogPart.class);
        LogPart classAnnotation = AnnotationUtils.findAnnotation(joinPoint.getTarget().getClass(), LogPart.class);
        String part = methodAnnotation != null ? methodAnnotation.value() : (classAnnotation != null ? classAnnotation.value() : "unknown");

        String userId = resolveUserUUID();

        String className = joinPoint.getTarget().getClass().getName();
        String methodName = joinPoint.getSignature().getName();

        String msg = className + "." + methodName + "() called";

        try {
            Object result = joinPoint.proceed();

            log.info(msg,
                    kv("part", part),
                    kv("userId", userId == null ? "Unknown" : userId),
                    kv("className", className),
                    kv("method", methodName)
            );
            return result;
        } catch (BaseException be) {
            log.warn(msg,
                    kv("part", part),
                    kv("userId", userId == null ? "Unknown" : userId),
                    kv("className", className),
                    kv("method", methodName)
            );
            throw be;
        } catch (Exception e) {
            log.error(msg,
                    kv("part", part),
                    kv("userId", userId == null ? "Unknown" : userId),
                    kv("className", className),
                    kv("method", methodName)
            );
            throw e;
        }
    }

    private String resolveUserUUID() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        if (authentication.getPrincipal() instanceof UserDetails) {
            return ((UserDetails) authentication.getPrincipal()).getUsername();
        }
        return null;
    }

    private static Class<?>[] getParamTypes(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) types[i] = args[i] == null ? Object.class : args[i].getClass();
        return types;
    }
}
