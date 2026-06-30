package com.cn.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 修改密码 DTO
 */
@Data
@Accessors(chain = true)
public class ChangePasswordDto {

    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, message = "新密码长度至少为6位")
    private String newPassword;
}
