package com.jakdang.labs.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppConfig {
    private String frontendUrl;
    private String frontendAdminUrl;
    private String domain;
    private boolean devMode;
} 