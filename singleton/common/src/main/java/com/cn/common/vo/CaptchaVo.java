package com.cn.common.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class CaptchaVo {

    private String captchaKey;

    private String imageBase64;

}
