package com.nowcoder.community.aspect;


import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;


@Slf4j
@Component
@Aspect
public class ServiceLogAspect {


    @Pointcut(value = "execution(* com.nowcoder.community.service.*.*(..))")
    public void pointcut(){

    }


    @Before(value = "pointcut()")
    public void before(JoinPoint joinPoint){
        //记录如下格式的ip
        //用户【111.11.1.1】,在【2020.1.2】访问了【com.nowcoder.community.service.xxx()】
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if(attributes == null){
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        String ip = request.getRemoteHost();
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String target = joinPoint.getSignature().getDeclaringTypeName()+"."+
                joinPoint.getSignature().getName();
        log.info(String.format("用户[%s],在[%s],访问了[%s].",ip,now,target));

    }

}
