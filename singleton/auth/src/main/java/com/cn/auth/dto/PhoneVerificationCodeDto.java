package com.cn.auth.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class PhoneVerificationCodeDto {

    @NotEmpty(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "请输入有效的手机号")
    private String phone;

    @NotEmpty(message = "图形验证码标识不能为空")
    private String captchaKey;

    @NotEmpty(message = "图形验证码不能为空")
    private String captchaCode;

}
