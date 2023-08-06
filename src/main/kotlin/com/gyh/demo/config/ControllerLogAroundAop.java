package com.gyh.demo.config;


import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;
import reactor.core.publisher.SignalType;

import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Configuration
@Aspect
public class ControllerLogAroundAop {
    private final Logger log = LoggerFactory.getLogger(ControllerLogAroundAop.class);

    @Pointcut(value = " (@annotation(org.springframework.web.bind.annotation.PostMapping) " +
                    "|| @annotation(org.springframework.web.bind.annotation.GetMapping) " +
                    "|| @annotation(org.springframework.web.bind.annotation.DeleteMapping) " +
                    "|| @annotation(org.springframework.web.bind.annotation.PutMapping) " +
                    "|| @annotation(org.springframework.web.bind.annotation.RequestMapping))" +
                    " && within(com.gyh.demo.controller..*)")
    public void logAround() {
    }

    @Around("logAround()")
    public Object logAround(ProceedingJoinPoint pjp) throws Throwable {
        LocalTime start = LocalTime.now();

        // 取不到链接取方法名
        String requestUrl = String.format("%s#%s", pjp.getTarget().getClass().getName(), pjp.getSignature().getName());

        String params = getParams(pjp).toString();
        if (params.length() > 1024) {
            log.info("请求：{}，参数：{}...", requestUrl, StringUtils.left(params, 1024));
        } else {
            log.info("请求：{}，参数：{}", requestUrl, params);
        }

        Object result = pjp.proceed();
        if (result instanceof Mono<?>) {
            result = ((Mono<?>) result).doOnEach(it -> {
                if (it == null) {
                    log.info("响应：{},\n响应内容:{},\n耗时{}ms", requestUrl, "null", Duration.between(start, LocalTime.now()).toMillis());
                } else if (it.getType() != SignalType.ON_COMPLETE) {
                    String resultStr = it.toString();
                    if (resultStr.length() > 1024) {
                        log.info("响应：{},\n响应内容:{}...,\n耗时{}ms", requestUrl, StringUtils.left(resultStr, 1024), Duration.between(start, LocalTime.now()).toMillis());
                    } else {
                        log.info("响应：{},\n响应内容:{},\n耗时{}ms", requestUrl, resultStr, Duration.between(start, LocalTime.now()).toMillis());
                    }
                }
            });
        } else {
            log.info("响应为空或是文件，耗时{}ms", Duration.between(start, LocalTime.now()).toMillis());
        }

        return result;
    }

    private List<Object> getParams(ProceedingJoinPoint pjp) {
        Object[] args = pjp.getArgs();
        List<Object> params = new ArrayList<>();
        for (Object arg : args) {
            if (arg instanceof MultipartFile
                    || arg instanceof Model) {
                // 框架参数不需要打印出来
                continue;
            }
            params.add(arg);
        }
        return params;
    }
}