package com.cn.auth.service.impl;

import cn.dev33.satoken.secure.SaSecureUtil;
import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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
import com.cn.auth.vo.WechatLoginStateVo;
import com.cn.auth.vo.WechatPollVo;
import com.cn.common.configuration.WechatConfiguration;
import com.cn.common.constant.CacheExpireConstant;
import com.cn.common.entity.User;
import com.cn.common.exceptions.EmailException;
import com.cn.common.exceptions.SmsException;
import com.cn.common.mapper.UserMapper;
import com.cn.common.structure.UserInfoStructure;
import com.cn.common.structure.WechatBindStructure;
import com.cn.common.structure.WechatStateStructure;
import com.cn.common.structure.WechatTokenStructure;
import com.cn.common.utils.EmailUtil;
import com.cn.common.utils.RedisUtils;
import com.cn.common.utils.SmsUtil;
import com.cn.common.utils.UserUtils;
import com.cn.common.utils.WechatOAuthUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.cn.common.constant.VerificationCodeConstant.WECHAT_BIND_KEY_PREFIX;
import static com.cn.common.constant.VerificationCodeConstant.WECHAT_STATE_KEY_PREFIX;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_NEED_BIND = "need_bind";

    private final UserMapper userMapper;

    private final EmailUtil emailUtil;

    private final SmsUtil smsUtil;

    private final WechatOAuthUtil wechatOAuthUtil;

    private final WechatConfiguration wechatConfiguration;

    private final RedisUtils redisUtils;

    @Override
    public void logout() {
        if (StpUtil.isLogin()) {
            StpUtil.logout();
        }
    }

    @Override
    public void getVerificationCode(final GetVerificationCodeDto dto) {
        final String code = RandomStringUtils.random(8, true, true).toUpperCase();
        try {
            emailUtil.send(dto.getEmail(), code);
        } catch (EmailException e) {
            throw new EmailException(e.getMessage());
        }
    }

    @Override
    public void getPhoneVerificationCode(final PhoneVerificationCodeDto dto) {
        try {
            smsUtil.send(dto.getPhone());
        } catch (SmsException e) {
            throw new AuthException(e.getMessage());
        }
    }

    @Override
    public String passwordLogin(final PasswordLoginDto dto) {
        final String account = StringUtils.trim(dto.getAccount());
        final String hashedPassword = SaSecureUtil.md5(dto.getPassword());
        final LambdaQueryWrapper<User> wrapper = new QueryWrapper<User>()
                .lambda()
                .eq(User::getPassword, hashedPassword);
        if (account.matches("^1[3-9]\\d{9}$")) {
            wrapper.eq(User::getPhone, account);
        } else {
            wrapper.eq(User::getEmail, account);
        }
        final User user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw new AuthException("登陆账号或密码错误");
        }
        return loginAndReturnToken(user);
    }

    @Override
    public void register(final RegisterDto dto) {
        final String code = emailUtil.getCode(dto.getEmail());
        if (StringUtils.isBlank(code) || !code.equals(dto.getCode())) {
            throw new AuthException("验证码错误");
        }
        final User user = userMapper.selectOne(new QueryWrapper<User>()
                .lambda()
                .eq(User::getEmail, dto.getEmail())
        );
        if (user != null) {
            throw new AuthException("该邮箱已被注册");
        }
        userMapper.insert(new User()
                .setEmail(dto.getEmail())
                .setPassword(SaSecureUtil.md5(dto.getPassword()))
                .setNickname("用户" + RandomStringUtils.random(8, true, true))
        );

        recordNewUser();
        emailUtil.clearCode(dto.getEmail());
    }

    @Override
    public void phoneRegister(final PhoneRegisterDto dto) {
        final String code = smsUtil.getCode(dto.getPhone());
        if (StringUtils.isBlank(code) || !code.equals(dto.getCode())) {
            throw new AuthException("验证码错误");
        }
        final User existing = userMapper.selectOne(new QueryWrapper<User>()
                .lambda()
                .eq(User::getPhone, dto.getPhone()));
        if (existing != null) {
            throw new AuthException("该手机号已被注册");
        }

        final String password = StringUtils.isNotBlank(dto.getPassword())
                ? SaSecureUtil.md5(dto.getPassword())
                : SaSecureUtil.md5(RandomStringUtils.randomAlphanumeric(16));
        final String nickname = StringUtils.isNotBlank(dto.getNickname())
                ? dto.getNickname()
                : "用户" + dto.getPhone().substring(dto.getPhone().length() - 4);

        userMapper.insert(new User()
                .setPhone(dto.getPhone())
                .setPassword(password)
                .setNickname(nickname));

        recordNewUser();
        smsUtil.clearCode(dto.getPhone());
    }

    @Override
    public String emailCodeLogin(final EmailCodeLoginDto dto) {
        final String code = emailUtil.getCode(dto.getEmail());
        if (StringUtils.isBlank(code) || !code.equals(dto.getCode())) {
            throw new AuthException("验证码错误");
        }
        emailUtil.clearCode(dto.getEmail());
        final User user = userMapper.selectOne(new QueryWrapper<User>()
                .lambda()
                .eq(User::getEmail, dto.getEmail())
        );
        if (user == null) {
            throw new AuthException("该邮箱未注册，请先注册");
        }
        return loginAndReturnToken(user);
    }

    @Override
    public String phoneCodeLogin(final PhoneCodeLoginDto dto) {
        final String code = smsUtil.getCode(dto.getPhone());
        if (StringUtils.isBlank(code) || !code.equals(dto.getCode())) {
            throw new AuthException("验证码错误");
        }
        smsUtil.clearCode(dto.getPhone());
        final User user = userMapper.selectOne(new QueryWrapper<User>()
                .lambda()
                .eq(User::getPhone, dto.getPhone()));
        if (user == null) {
            throw new AuthException("该手机号未注册，请先注册");
        }
        return loginAndReturnToken(user);
    }

    @Override
    public void forgotPassword(final ForgotPasswordDto dto) {
        final String code = emailUtil.getCode(dto.getEmail());
        if (StringUtils.isBlank(code) || !code.equals(dto.getCode())) {
            throw new AuthException("验证码错误");
        }
        final User user = userMapper.selectOne(new QueryWrapper<User>()
                .lambda()
                .eq(User::getEmail, dto.getEmail())
        );
        if (user == null) {
            throw new AuthException("该邮箱未注册");
        }
        user.setPassword(SaSecureUtil.md5(dto.getPassword()));
        userMapper.updateById(user);
        emailUtil.clearCode(dto.getEmail());
    }

    @Override
    public WechatLoginStateVo createWechatLoginState() {
        if (StringUtils.isBlank(wechatConfiguration.getAppId())
                || StringUtils.isBlank(wechatConfiguration.getAppSecret())
                || StringUtils.isBlank(wechatConfiguration.getRedirectUri())) {
            throw new AuthException("微信登录未配置");
        }
        final String state = UUID.randomUUID().toString().replace("-", "");
        final WechatStateStructure stateStructure = new WechatStateStructure().setStatus(STATUS_PENDING);
        redisUtils.setValueTimeout(
                WECHAT_STATE_KEY_PREFIX + state,
                JSON.toJSONString(stateStructure),
                CacheExpireConstant.EXPIRE_5_MINUTES);
        return new WechatLoginStateVo()
                .setState(state)
                .setAppId(wechatConfiguration.getAppId())
                .setRedirectUri(wechatConfiguration.getRedirectUri());
    }

    @Override
    public void handleWechatCallback(final String code, final String state) {
        if (StringUtils.isBlank(state)) {
            throw new AuthException("微信登录状态无效");
        }
        final String stateKey = WECHAT_STATE_KEY_PREFIX + state;
        final Object cached = redisUtils.getValue(stateKey);
        if (cached == null) {
            throw new AuthException("微信登录已过期，请重新扫码");
        }

        final WechatTokenStructure tokenStructure = wechatOAuthUtil.exchangeCode(code);
        final User user = findUserByWechat(tokenStructure.getUnionid(), tokenStructure.getOpenid());

        final WechatStateStructure stateStructure = new WechatStateStructure();
        if (user != null) {
            final String token = loginAndReturnToken(user);
            stateStructure.setStatus(STATUS_SUCCESS).setToken(token);
        } else {
            final String bindTicket = UUID.randomUUID().toString().replace("-", "");
            final WechatBindStructure bindStructure = new WechatBindStructure()
                    .setOpenid(tokenStructure.getOpenid())
                    .setUnionid(tokenStructure.getUnionid());
            redisUtils.setValueTimeout(
                    WECHAT_BIND_KEY_PREFIX + bindTicket,
                    JSON.toJSONString(bindStructure),
                    600L);
            stateStructure.setStatus(STATUS_NEED_BIND).setBindTicket(bindTicket);
        }
        redisUtils.setValueTimeout(stateKey, JSON.toJSONString(stateStructure), CacheExpireConstant.EXPIRE_5_MINUTES);
    }

    @Override
    public WechatPollVo pollWechatLogin(final String state) {
        if (StringUtils.isBlank(state)) {
            throw new AuthException("微信登录状态无效");
        }
        final Object cached = redisUtils.getValue(WECHAT_STATE_KEY_PREFIX + state);
        if (cached == null) {
            return new WechatPollVo().setStatus(STATUS_PENDING);
        }
        final WechatStateStructure stateStructure = JSON.parseObject(String.valueOf(cached), WechatStateStructure.class);
        return new WechatPollVo()
                .setStatus(stateStructure.getStatus())
                .setToken(stateStructure.getToken())
                .setBindTicket(stateStructure.getBindTicket());
    }

    @Override
    public String bindWechatPhone(final WechatBindPhoneDto dto) {
        final Object cached = redisUtils.getValue(WECHAT_BIND_KEY_PREFIX + dto.getBindTicket());
        if (cached == null) {
            throw new AuthException("绑定凭证已过期，请重新扫码");
        }
        final WechatBindStructure bindStructure = JSON.parseObject(String.valueOf(cached), WechatBindStructure.class);

        final String code = smsUtil.getCode(dto.getPhone());
        if (StringUtils.isBlank(code) || !code.equals(dto.getCode())) {
            throw new AuthException("验证码错误");
        }
        smsUtil.clearCode(dto.getPhone());

        final User phoneUser = userMapper.selectOne(new QueryWrapper<User>()
                .lambda()
                .eq(User::getPhone, dto.getPhone()));
        final User wechatUser = findUserByWechat(bindStructure.getUnionid(), bindStructure.getOpenid());

        if (wechatUser != null) {
            return loginAndReturnToken(wechatUser);
        }

        User user;
        if (phoneUser != null) {
            if (StringUtils.isNotBlank(phoneUser.getWechatOpenid())
                    || StringUtils.isNotBlank(phoneUser.getWechatUnionId())) {
                throw new AuthException("该手机号已绑定其他微信账号");
            }
            phoneUser.setWechatOpenid(bindStructure.getOpenid());
            if (StringUtils.isNotBlank(bindStructure.getUnionid())) {
                phoneUser.setWechatUnionId(bindStructure.getUnionid());
            }
            userMapper.updateById(phoneUser);
            user = phoneUser;
        } else {
            user = new User()
                    .setPhone(dto.getPhone())
                    .setWechatOpenid(bindStructure.getOpenid())
                    .setWechatUnionId(bindStructure.getUnionid())
                    .setPassword(SaSecureUtil.md5(RandomStringUtils.randomAlphanumeric(16)))
                    .setNickname("用户" + dto.getPhone().substring(dto.getPhone().length() - 4));
            userMapper.insert(user);
            recordNewUser();
        }

        redisUtils.delKey(WECHAT_BIND_KEY_PREFIX + dto.getBindTicket());
        return loginAndReturnToken(user);
    }

    private User findUserByWechat(final String unionid, final String openid) {
        if (StringUtils.isNotBlank(unionid)) {
            final User byUnion = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getWechatUnionId, unionid));
            if (byUnion != null) {
                return byUnion;
            }
        }
        if (StringUtils.isNotBlank(openid)) {
            return userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getWechatOpenid, openid));
        }
        return null;
    }

    private String loginAndReturnToken(final User user) {
        StpUtil.login(user.getId());
        UserUtils.updateUserInfo(new UserInfoStructure()
                .setAvatar(user.getAvatar())
                .setNickname(user.getNickname())
                .setRole(user.getRole())
                .setUserId(user.getId()));
        return StpUtil.getTokenValue();
    }

    private void recordNewUser() {
        try {
            String today = java.time.LocalDate.now().toString();
            String key = com.cn.common.constant.SystemStatsConstant.USER_NEW_PREFIX + today;
            redisUtils.increment(key, 1L);
            redisUtils.expire(key, com.cn.common.constant.CacheExpireConstant.EXPIRE_48_HOURS);
            log.debug("记录新增用户统计: date={}", today);
        } catch (Exception e) {
            log.error("记录新增用户统计失败", e);
        }
    }

}
