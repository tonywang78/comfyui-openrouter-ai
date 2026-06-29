package com.cn.common.structure;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class WechatStateStructure {

    private String status;

    private String token;

    private String bindTicket;

}
