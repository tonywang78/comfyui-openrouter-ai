package com.cn.common.configuration;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "wechat.open-platform")
@Data
@Accessors(chain = true)
public class WechatConfiguration {

    private String appId;

    private String appSecret;

    private String redirectUri;

}
