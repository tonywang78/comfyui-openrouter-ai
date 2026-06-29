package com.cn.common.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 跨域配置，支持 Web / Tauri / Capacitor 客户端访问云端 API。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.cors")
public class AppCorsProperties {

    /**
     * 是否启用 CORS。
     */
    private boolean enabled = true;

    /**
     * 允许的 Origin 列表，支持通配符模式。
     */
    private List<String> allowedOrigins = new ArrayList<>(List.of(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "https://localhost:*",
            "https://127.0.0.1:*",
            "https://tauri.localhost",
            "https://*.yourdomain.com",
            "capacitor://localhost",
            "ionic://localhost",
            "http://localhost",
            "https://localhost"
    ));
}
