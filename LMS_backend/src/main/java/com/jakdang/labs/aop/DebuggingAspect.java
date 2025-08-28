package com.jakdang.labs.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.stereotype.Component;

@Aspect
@Slf4j
@Component
public class DebuggingAspect {

    @Pointcut("execution(* com.jakdang.labs.*.*.*(..)) && !execution(* com.jakdang.labs.config.SecurityConfig.*(..))")
    private void applicationPointcut() {
    }


    @Before("applicationPointcut()")
    public void logMethodEntry(JoinPoint joinPoint) {
        logMethodInfo(joinPoint, "입력값", joinPoint.getArgs());
    }

    @AfterReturning(value = "applicationPointcut()", returning = "returnValue")
    public void logMethodExit(JoinPoint joinPoint, Object returnValue) {
        logMethodInfo(joinPoint, "반환값", new Object[]{returnValue});
    }

    private void logMethodInfo(JoinPoint joinPoint, String type, Object[] values) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        if (values == null || values.length == 0) {
            log.info("{} # {}의 {}: 없음", className, methodName, type);
            return;
        }

        for (Object value : values) {
            logValue(className, methodName, type, value);
        }
    }

    private void logValue(String className, String methodName, String type, Object value) {
        if (value == null) {
            log.info("{} # {}의 {}: null", className, methodName, type);
        } else if (value instanceof AuthenticationManager) {
            log.info("{} # {}의 {}: [AuthenticationManager 인스턴스]", className, methodName, type);
        } else {
            try {
                log.info("{} # {}의 {}: {}", className, methodName, type, value);
            } catch (Exception e) {
                log.info("{} # {}의 {}: [로깅 중 예외 발생 - {}]",
                        className, methodName, type, e.getClass().getName());
            }
        }
    }
}