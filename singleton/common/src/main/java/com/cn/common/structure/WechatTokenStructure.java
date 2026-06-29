package com.cn.common.structure;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class WechatTokenStructure {

    @JSONField(name = "access_token")
    private String accessToken;

    private String openid;

    private String unionid;

    private Integer errcode;

    private String errmsg;

}
