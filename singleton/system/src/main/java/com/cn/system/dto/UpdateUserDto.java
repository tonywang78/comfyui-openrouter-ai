package com.cn.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 更新用户DTO
 *
 * @author 时间海 @github dulaiduwang003
 * @email 2074055628@qq.com
 * @version 1.0
 */
@Data
@Accessors(chain = true)
public class UpdateUserDto {

    @NotNull(message = "用户ID不能为空")
    private Long id;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "请输入有效的手机号")
    private String phone;

    private String nickname;

    private String avatar;

    private String role;
} 