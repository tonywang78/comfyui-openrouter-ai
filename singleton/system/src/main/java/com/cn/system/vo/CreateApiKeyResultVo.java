package com.cn.system.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class CreateApiKeyResultVo {

    private Long id;

    private String plainKey;
}
