package com.cn.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class RotateApiKeyDto {

    @NotNull
    private Long id;
}
