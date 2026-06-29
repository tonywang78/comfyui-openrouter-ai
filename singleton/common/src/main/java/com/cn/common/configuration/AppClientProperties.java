package com.cn.common.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 客户端版本信息配置。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.client")
public class AppClientProperties {

    private String version = "0.1.0";

    private String minSupportedVersion = "0.1.0";

    private String downloadUrl = "https://yourdomain.com/download";
}
