package com.cn.auth.service;


import com.cn.auth.dto.EmailCodeLoginDto;
import com.cn.auth.dto.ForgotPasswordDto;
import com.cn.auth.dto.GetVerificationCodeDto;
import com.cn.auth.dto.PasswordLoginDto;
import com.cn.auth.dto.PhoneCodeLoginDto;
import com.cn.auth.dto.PhoneRegisterDto;
import com.cn.auth.dto.PhoneVerificationCodeDto;
import com.cn.auth.dto.RegisterDto;
import com.cn.auth.dto.WechatBindPhoneDto;
import com.cn.auth.vo.WechatLoginStateVo;
import com.cn.auth.vo.WechatPollVo;

public interface AuthService {

    String passwordLogin(final PasswordLoginDto dto);

    String emailCodeLogin(final EmailCodeLoginDto dto);

    void getVerificationCode(final GetVerificationCodeDto dto);

    void register(final RegisterDto dto);

    void logout();

    void forgotPassword(final ForgotPasswordDto dto);

    void getPhoneVerificationCode(final PhoneVerificationCodeDto dto);

    String phoneCodeLogin(final PhoneCodeLoginDto dto);

    void phoneRegister(final PhoneRegisterDto dto);

    WechatLoginStateVo createWechatLoginState();

    void handleWechatCallback(final String code, final String state);

    WechatPollVo pollWechatLogin(final String state);

    String bindWechatPhone(final WechatBindPhoneDto dto);

}
