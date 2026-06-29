package com.cn.auth.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class WechatLoginStateVo {

    private String state;

    private String appId;

    private String redirectUri;

}
