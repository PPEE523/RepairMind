package com.example.equipment.aspect;

import com.example.equipment.entity.SysOperLog;
import com.example.equipment.service.SysOperLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

@Slf4j
@Aspect
@Component
public class OperLogAspect {

    @Resource
    private SysOperLogService sysOperLogService;

    @Resource
    private ObjectMapper objectMapper;

    /**
     * 定义切点 - 所有Controller方法
     */
    @Pointcut("execution(* com.example.equipment.controller..*.*(..))")
    public void controllerPointcut() {}

    /**
     * 定义切点 - 带注解的方法
     */
    @Pointcut("@annotation(com.example.equipment.aspect.OperLog)")
    public void operLogPointcut() {}

    /**
     * 环绕通知
     */
    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 获取方法信息
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = signature.getDeclaringTypeName() + "." + method.getName();

        // 获取注解信息
        OperLog operLog = method.getAnnotation(OperLog.class);

        // 获取请求信息
        HttpServletRequest request = getRequest();
        String ipAddress = getClientIp(request);
        String requestUri = request.getRequestURI();
        String requestMethod = request.getMethod();

        // 执行方法
        Object result = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable e) {
            // 记录异常日志
            log.error("方法执行异常: {}", methodName, e);
            throw e;
        } finally {
            // 记录操作日志
            long executionTime = System.currentTimeMillis() - startTime;
            saveOperLog(operLog, method, joinPoint.getArgs(), result, ipAddress, requestUri, requestMethod, executionTime);
        }
    }

    /**
     * 保存操作日志
     */
    private void saveOperLog(OperLog operLog, Method method, Object[] args, Object result,
                             String ipAddress, String requestUri, String requestMethod, long executionTime) {
        try {
            SysOperLog logEntity = new SysOperLog();

            // 从请求属性中获取用户ID
            HttpServletRequest request = getRequest();
            if (request != null) {
                Object userId = request.getAttribute("userId");
                if (userId != null) {
                    logEntity.setUserId(Long.valueOf(userId.toString()));
                }
            }

            // 设置操作类型
            if (operLog != null) {
                logEntity.setOperType(operLog.operType().isEmpty() ? requestMethod : operLog.operType());
                logEntity.setContent(operLog.description().isEmpty() ? requestUri : operLog.description());
            } else {
                logEntity.setOperType(requestMethod);
                logEntity.setContent(requestUri);
            }

            logEntity.setOperTime(LocalDateTime.now());
            logEntity.setIpAddress(ipAddress);

            // 异步保存日志
            sysOperLogService.save(logEntity);

        } catch (Exception e) {
            log.error("保存操作日志失败", e);
        }
    }

    /**
     * 获取Request对象
     */
    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 多个代理时，第一个IP为客户端真实IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        // IPv6回环地址转为IPv4回环地址
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1";
        }

        return ip;
    }
}
