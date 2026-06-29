package com.cn.common.structure;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class WechatBindStructure {

    private String openid;

    private String unionid;

}
