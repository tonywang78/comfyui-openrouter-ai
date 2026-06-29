package com.cn.common.utils;

import com.alibaba.fastjson.JSON;
import com.cn.common.configuration.WechatConfiguration;
import com.cn.common.exceptions.UniversalException;
import com.cn.common.structure.WechatTokenStructure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class WechatOAuthUtil {

    private static final String ACCESS_TOKEN_URL = "https://api.weixin.qq.com/sns/oauth2/access_token";

    private final WechatConfiguration wechatConfiguration;

    private final WebClient webClient;

    public WechatTokenStructure exchangeCode(final String code) {
        if (StringUtils.isBlank(code)) {
            throw new UniversalException("微信授权码无效");
        }

        final String response = webClient.get()
                .uri(ACCESS_TOKEN_URL + "?appid={appid}&secret={secret}&code={code}&grant_type=authorization_code",
                        wechatConfiguration.getAppId(),
                        wechatConfiguration.getAppSecret(),
                        code)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        if (StringUtils.isBlank(response)) {
            throw new UniversalException("微信授权失败");
        }

        final WechatTokenStructure tokenStructure = JSON.parseObject(response, WechatTokenStructure.class);
        if (tokenStructure.getErrcode() != null && tokenStructure.getErrcode() != 0) {
            log.error("微信授权失败: {}", response);
            throw new UniversalException("微信授权失败: " + tokenStructure.getErrmsg());
        }
        if (StringUtils.isBlank(tokenStructure.getOpenid())) {
            throw new UniversalException("微信授权失败，未获取到用户标识");
        }
        return tokenStructure;
    }
}
