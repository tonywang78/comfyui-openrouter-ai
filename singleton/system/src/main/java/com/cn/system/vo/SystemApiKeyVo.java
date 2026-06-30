package com.cn.system.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class SystemApiKeyVo {

    private Long id;

    private Long userId;

    private String userNickname;

    private String name;

    private String keyPrefix;

    private Integer status;

    private LocalDateTime expiresAt;

    private LocalDateTime lastUsedAt;

    private LocalDateTime createTime;
}
