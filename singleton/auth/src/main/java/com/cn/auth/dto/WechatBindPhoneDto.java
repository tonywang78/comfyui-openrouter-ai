package com.cn.auth.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class WechatBindPhoneDto {

    @NotEmpty(message = "绑定凭证不能为空")
    private String bindTicket;

    @NotEmpty(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入有效的手机号")
    private String phone;

    @NotEmpty(message = "验证码不能为空")
    private String code;

}
