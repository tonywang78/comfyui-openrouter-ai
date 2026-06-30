package com.cn.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class UpdateApiKeyDto {

    @NotNull
    private Long id;

    private String name;

    private Integer status;

    private LocalDateTime expiresAt;
}
