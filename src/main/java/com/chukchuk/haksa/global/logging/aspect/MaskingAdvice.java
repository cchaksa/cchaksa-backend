package com.chukchuk.haksa.global.logging.aspect;

import com.chukchuk.haksa.global.logging.sanitize.LogSanitizer;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** (옵션) 특정 로거 사용 시 메시지에 마스킹 적용 예시 */
@Aspect
@Component
public class MaskingAdvice {
    private static final Logger LOG = LoggerFactory.getLogger(MaskingAdvice.class);

    @AfterReturning(pointcut = "execution(* org.slf4j.Logger.info(..)) && args(msg,..)", returning = "ret")
    public void afterInfo(JoinPoint jp, Object msg, Object ret) {
        // NOTE: 실제 운영에선 로거 프록시/레이퍼로 적용하는 편이 더 안전합니다.
        if (msg instanceof String s) {
            LOG.debug("sanitized={}", LogSanitizer.clean(s));
        }
    }
}