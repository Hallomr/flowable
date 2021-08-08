package com.zxk.flowable.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
@Slf4j
@Configuration
@Data
@ConfigurationProperties(prefix = "spring.datasource")
public class ProcessEngineConfig {
    private String url;
    private String driverClassName;
    private String username;
    private String password;
    /**
     * 初始化流程引擎
     * @return
     */
    @Primary
    @Bean
    public ProcessEngine initProcessEngine() {
        log.info("=============================ProcessEngineBegin=============================");

        // 流程引擎配置
        ProcessEngineConfiguration cfg = null;

        try {
            cfg = new StandaloneProcessEngineConfiguration()
                    .setJdbcUrl(url)
                    .setJdbcUsername(username)
                    .setJdbcPassword(password)
                    .setJdbcDriver(driverClassName)
                    // 初始化基础表，不需要的可以改为 DB_SCHEMA_UPDATE_FALSE
                    .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_FALSE)
                    // 默认邮箱配置
                    // 发邮件的主机地址，先用 QQ 邮箱
                    .setMailServerHost("smtp.qq.com")
                    // POP3/SMTP服务的授权码
                    .setMailServerPassword("hergynqfusnsbbdf")
                    // 默认发件人
                    .setMailServerDefaultFrom("836369078@qq.com")
                    // 设置发件人用户名
                    .setMailServerUsername("管理员");
        } catch (Exception e) {
            log.info(e.getMessage());
        }
        // 初始化流程引擎对象
        ProcessEngine processEngine = cfg.buildProcessEngine();
        log.info("=============================ProcessEngineEnd=============================");
        return processEngine;
    }
}
