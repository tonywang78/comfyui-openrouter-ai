package com.cn.auth.controller;


import com.cn.auth.dto.EmailCodeLoginDto;
import com.cn.auth.dto.ForgotPasswordDto;
import com.cn.auth.dto.GetVerificationCodeDto;
import com.cn.auth.dto.PasswordLoginDto;
import com.cn.auth.dto.PhoneCodeLoginDto;
import com.cn.auth.dto.PhoneRegisterDto;
import com.cn.auth.dto.PhoneVerificationCodeDto;
import com.cn.auth.dto.RegisterDto;
import com.cn.auth.dto.WechatBindPhoneDto;
import com.cn.auth.exceptions.AuthException;
import com.cn.auth.service.AuthService;
import com.cn.common.annotations.RateLimit;
import com.cn.common.msg.Result;
import com.cn.common.service.CaptchaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    private final CaptchaService captchaService;

    @GetMapping(value = "/captcha", produces = MediaType.APPLICATION_JSON_VALUE)
    @RateLimit(permitsPerSecond = 1, limitType = RateLimit.LimitType.IP, message = "验证码请求过于频繁，请稍后再试")
    public Result getCaptcha() {
        return Result.data(captchaService.generate());
    }

    @PostMapping(value = "/password-login", produces = MediaType.APPLICATION_JSON_VALUE)
    @RateLimit(permitsPerSecond = 0.2, limitType = RateLimit.LimitType.IP, message = "登录过于频繁，请稍后再试")
    public Result passwordLogin(@RequestBody @Validated final PasswordLoginDto dto) {
        try {
            return Result.data(authService.passwordLogin(dto));
        } catch (AuthException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping(value = "/verification-code", produces = MediaType.APPLICATION_JSON_VALUE)
    @RateLimit(permitsPerSecond = 0.017, limitType = RateLimit.LimitType.IP, message = "验证码请求过于频繁，请1分钟后再试")
    public Result getVerificationCode(@RequestBody @Validated final GetVerificationCodeDto dto) {
        try {
            authService.getVerificationCode(dto);
            return Result.ok();
        } catch (AuthException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping(value = "/phone-verification-code", produces = MediaType.APPLICATION_JSON_VALUE)
    @RateLimit(permitsPerSecond = 0.017, limitType = RateLimit.LimitType.IP, message = "验证码请求过于频繁，请1分钟后再试")
    public Result getPhoneVerificationCode(@RequestBody @Validated final PhoneVerificationCodeDto dto) {
        try {
            authService.getPhoneVerificationCode(dto);
            return Result.ok();
        } catch (AuthException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping(value = "/logout", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result logout() {
        authService.logout();
        return Result.ok();
    }

    @PostMapping(value = "/register", produces = MediaType.APPLICATION_JSON_VALUE)
    @RateLimit(permitsPerSecond = 0.05, limitType = RateLimit.LimitType.IP, message = "注册过于频繁，请稍后再试")
    public Result register(@RequestBody @Validated final RegisterDto dto) {
        try {
            authService.register(dto);
            return Result.ok();
        } catch (AuthException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping(value = "/phone-register", produces = MediaType.APPLICATION_JSON_VALUE)
    @RateLimit(permitsPerSecond = 0.05, limitType = RateLimit.LimitType.IP, message = "注册过于频繁，请稍后再试")
    public Result phoneRegister(@RequestBody @Validated final PhoneRegisterDto dto) {
        try {
            authService.phoneRegister(dto);
            return Result.ok();
        } catch (AuthException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping(value = "/email-login", produces = MediaType.APPLICATION_JSON_VALUE)
    @RateLimit(permitsPerSecond = 0.2, limitType = RateLimit.LimitType.IP, message = "登录过于频繁，请稍后再试")
    public Result emailLogin(@RequestBody @Validated final EmailCodeLoginDto dto) {
        try {
            return Result.data(authService.emailCodeLogin(dto));
        } catch (AuthException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping(value = "/phone-login", produces = MediaType.APPLICATION_JSON_VALUE)
    @RateLimit(permitsPerSecond = 0.2, limitType = RateLimit.LimitType.IP, message = "登录过于频繁，请稍后再试")
    public Result phoneLogin(@RequestBody @Validated final PhoneCodeLoginDto dto) {
        try {
            return Result.data(authService.phoneCodeLogin(dto));
        } catch (AuthException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping(value = "/forgot-password", produces = MediaType.APPLICATION_JSON_VALUE)
    @RateLimit(permitsPerSecond = 0.033, limitType = RateLimit.LimitType.IP, message = "重置密码请求过于频繁，请稍后再试")
    public Result forgotPassword(@RequestBody @Validated final ForgotPasswordDto dto) {
        try {
            authService.forgotPassword(dto);
            return Result.ok();
        } catch (AuthException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping(value = "/wechat/login-state", produces = MediaType.APPLICATION_JSON_VALUE)
    @RateLimit(permitsPerSecond = 0.5, limitType = RateLimit.LimitType.IP, message = "请求过于频繁，请稍后再试")
    public Result createWechatLoginState() {
        try {
            return Result.data(authService.createWechatLoginState());
        } catch (AuthException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping(value = "/wechat/callback", produces = MediaType.TEXT_HTML_VALUE)
    public String wechatCallback(@RequestParam(required = false) final String code,
                                 @RequestParam(required = false) final String state) {
        try {
            authService.handleWechatCallback(code, state);
            return """
                    <!DOCTYPE html>
                    <html><head><meta charset="UTF-8"><title>微信登录</title></head>
                    <body><script>
                    if (window.opener) { window.opener.postMessage({ type: 'wechat-login-callback', state: '%s' }, '*'); }
                    window.close();
                    </script><p>登录成功，请关闭此窗口。</p></body></html>
                    """.formatted(state == null ? "" : state);
        } catch (Exception e) {
            log.error("微信回调处理失败", e);
            return """
                    <!DOCTYPE html>
                    <html><head><meta charset="UTF-8"><title>微信登录失败</title></head>
                    <body><p>登录失败：%s</p></body></html>
                    """.formatted(e.getMessage());
        }
    }

    @GetMapping(value = "/wechat/poll", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result pollWechatLogin(@RequestParam final String state) {
        try {
            return Result.data(authService.pollWechatLogin(state));
        } catch (AuthException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping(value = "/wechat/bind-phone", produces = MediaType.APPLICATION_JSON_VALUE)
    @RateLimit(permitsPerSecond = 0.2, limitType = RateLimit.LimitType.IP, message = "绑定请求过于频繁，请稍后再试")
    public Result bindWechatPhone(@RequestBody @Validated final WechatBindPhoneDto dto) {
        try {
            return Result.data(authService.bindWechatPhone(dto));
        } catch (AuthException e) {
            return Result.error(e.getMessage());
        }
    }
}
