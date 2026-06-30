package com.cn.auth.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 密码登录DTO
 *
 * @author 时间海 @github dulaiduwang003
 * @email 2074055628@qq.com
 * @version 1.0
 */
@Data
@Accessors(chain = true)
public class PasswordLoginDto {

    @NotEmpty(message = "登录账号不能为空")
    private String account;

    @NotEmpty(message = "登录密码不能为空")
    private String password;

    @NotEmpty(message = "图形验证码标识不能为空")
    private String captchaKey;

    @NotEmpty(message = "图形验证码不能为空")
    private String captchaCode;

}
