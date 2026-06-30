package com.cn.common.service.impl;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import com.cn.common.constant.CacheExpireConstant;
import com.cn.common.service.CaptchaService;
import com.cn.common.utils.RedisUtils;
import com.cn.common.vo.CaptchaVo;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.cn.common.constant.VerificationCodeConstant.CAPTCHA_KEY_PREFIX;

@Service
@RequiredArgsConstructor
public class CaptchaServiceImpl implements CaptchaService {

    private static final int CAPTCHA_WIDTH = 120;
    private static final int CAPTCHA_HEIGHT = 40;
    private static final int CAPTCHA_CODE_COUNT = 4;
    private static final int CAPTCHA_LINE_COUNT = 30;

    private final RedisUtils redisUtils;

    @Override
    public CaptchaVo generate() {
        final LineCaptcha captcha = CaptchaUtil.createLineCaptcha(
                CAPTCHA_WIDTH,
                CAPTCHA_HEIGHT,
                CAPTCHA_CODE_COUNT,
                CAPTCHA_LINE_COUNT
        );
        final String captchaKey = UUID.randomUUID().toString();
        redisUtils.set(
                CAPTCHA_KEY_PREFIX + captchaKey,
                captcha.getCode().toLowerCase(),
                CacheExpireConstant.EXPIRE_5_MINUTES
        );
        return new CaptchaVo()
                .setCaptchaKey(captchaKey)
                .setImageBase64(captcha.getImageBase64Data());
    }

    @Override
    public boolean validateAndConsume(final String captchaKey, final String captchaCode) {
        if (StringUtils.isAnyBlank(captchaKey, captchaCode)) {
            return false;
        }
        final String redisKey = CAPTCHA_KEY_PREFIX + captchaKey.trim();
        final Object stored = redisUtils.get(redisKey);
        redisUtils.delKey(redisKey);
        if (stored == null) {
            return false;
        }
        return String.valueOf(stored).equalsIgnoreCase(captchaCode.trim());
    }

}
