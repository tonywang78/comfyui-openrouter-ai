package com.cn.auth.vo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class WechatPollVo {

    private String status;

    private String token;

    private String bindTicket;

}
