package com.cn.media.dto;

import com.alibaba.fastjson2.JSONArray;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UpdateMediaDto {

    @NotNull(message = "素材ID不能为空")
    private Long mediaId;

    private String name;

    private JSONArray tags;
}
