package com.cn.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class CreateApiKeyDto {

    @NotNull
    private Long userId;

    @NotBlank
    private String name;

    private LocalDateTime expiresAt;
}
