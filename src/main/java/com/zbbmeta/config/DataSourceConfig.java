package com.zbbmeta.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * @author springboot葵花宝典
 * @description: TODO
 */
@Configuration
@Data
public class DataSourceConfig {
    @Value("${spring.datasource.master.url}")
    private String dbUrl;
    @Value("${spring.datasource.master.username}")
    private String username;
    @Value("${spring.datasource.master.password}")
    private String password;
    @Value("${spring.datasource.master.driver-class-name}")
    private String driverClassName;


    @Value("${spring.datasource.slave.url}")
    private String slaveDbUrl;
    @Value("${spring.datasource.slave.username}")
    private String slaveUsername;
    @Value("${spring.datasource.slave.password}")
    private String slavePassword;
    @Value("${spring.datasource.slave.driver-class-name}")
    private String slaveDriverClassName;


    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.master")
    public DataSource masterDataSource() {
        return DataSourceBuilder.create()
                .driverClassName(driverClassName)
                .url(dbUrl)
                .username(username)
                .password(password)
                .build();
    }

    @Bean
    @ConfigurationProperties(prefix = "spring.datasource.slave")
    public DataSource slaveDataSource() {

        return DataSourceBuilder.create()
                .driverClassName(slaveDriverClassName)
                .url(slaveDbUrl)
                .username(slaveUsername)
                .password(slavePassword)
                .build();

    }
}