package com.cn.common.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "api-key")
public class ApiKeyProperties {

    /**
     * 可选 pepper，参与 SHA-256 哈希
     */
    private String pepper = "";
}
