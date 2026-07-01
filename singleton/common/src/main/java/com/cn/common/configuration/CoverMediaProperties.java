package com.cn.common.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 工作流封面媒体转码配置
 */
@Configuration
@ConfigurationProperties(prefix = "ali.oss.cover")
@Data
public class CoverMediaProperties {

    private int maxDurationSeconds = 5;

    private int maxLongEdge = 480;

    private long transcodeThresholdBytes = 3_145_728L;

    private long maxUploadBytes = 104_857_600L;

    private int transcodeTimeoutSeconds = 120;
}
