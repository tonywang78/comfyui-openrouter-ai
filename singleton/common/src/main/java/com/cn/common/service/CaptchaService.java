package com.cn.common.service;

import com.cn.common.vo.CaptchaVo;

public interface CaptchaService {

    CaptchaVo generate();

    boolean validateAndConsume(final String captchaKey, final String captchaCode);

}
