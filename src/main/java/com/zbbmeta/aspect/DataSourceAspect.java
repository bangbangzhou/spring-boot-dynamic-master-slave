package com.zbbmeta.aspect;

import com.zbbmeta.annotation.DataSource;
import com.zbbmeta.holder.DatabaseContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.stereotype.Component;


import java.lang.reflect.Method;

/**
 * @author springboot葵花宝典
 * @description: TODO
 */
@Aspect
@Component
@EnableAspectJAutoProxy
public class DataSourceAspect {



    @Pointcut("@annotation(com.zbbmeta.annotation.DataSource)")
    public void dataSourcePointCut() {}

    @Around("dataSourcePointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();

        DataSource dataSource = method.getAnnotation(DataSource.class);
        if (dataSource != null) {
            DatabaseContextHolder.setDatabaseType(dataSource.type());
        }

        try {
            return point.proceed();
        } finally {
            DatabaseContextHolder.clearDatabaseType();
        }
    }

}
