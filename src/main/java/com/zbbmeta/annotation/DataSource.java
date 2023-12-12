package com.zbbmeta.annotation;

import com.zbbmeta.config.DatabaseType;

import java.lang.annotation.*;

/**
 * @author springboot葵花宝典
 * @description: TODO
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataSource {

    DatabaseType type() default DatabaseType.SLAVE;

}
