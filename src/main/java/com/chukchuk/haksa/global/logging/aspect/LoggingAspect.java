package com.chukchuk.haksa.global.logging.aspect;

import com.chukchuk.haksa.global.exception.BaseException;
import com.chukchuk.haksa.global.logging.annotation.LogPart;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.aop.support.AopUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;

@Aspect
@Slf4j
@Component
@RequiredArgsConstructor
public class LoggingAspect {
    private final Tracer tracer; // (미사용) 추후 필요 시 활용

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

        String className = targetClass.getName();
        String methodName = method.getName();
        String msg = "[LogPart:{}] {}.{}() called";

        try {
            Object result = joinPoint.proceed();
            log.info(msg, part, className, methodName);
            return result;
        } catch (BaseException be) {
            // 정책: WARN 에서는 스택트레이스 금지
            log.warn("[LogPart:{}] {}.{}() baseException code={} msg={}",
                    part, className, methodName, be.getCode(), be.getMessage());
            throw be;
        } catch (Exception e) {
            // 정책: ERROR 에서는 스택트레이스 포함
            log.error("[LogPart:{}] {}.{}() error={}", part, className, methodName, e.getMessage(), e);
            throw e;
        }
    }
}