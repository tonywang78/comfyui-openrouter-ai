package com.cn.common.utils;

import com.alibaba.fastjson.JSON;
import com.aliyun.auth.credentials.Credential;
import com.aliyun.auth.credentials.provider.StaticCredentialProvider;
import com.aliyun.sdk.service.dysmsapi20170525.AsyncClient;
import com.aliyun.sdk.service.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.sdk.service.dysmsapi20170525.models.SendSmsResponse;
import com.cn.common.configuration.AliConfiguration;
import com.cn.common.constant.CacheExpireConstant;
import com.cn.common.exceptions.SmsException;
import darabonba.core.client.ClientOverrideConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.cn.common.constant.VerificationCodeConstant.PHONE_CODE_KEY_PREFIX;
import static com.cn.common.constant.VerificationCodeConstant.PHONE_CODE_SEND_LOCK_PREFIX;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmsUtil {

    private static final String PHONE_PATTERN = "^1[3-9]\\d{9}$";

    private final AliConfiguration aliConfiguration;

    private final RedisUtils redisUtils;

    public void validatePhone(final String phone) {
        if (phone == null || !phone.matches(PHONE_PATTERN)) {
            throw new SmsException("请输入有效的手机号");
        }
    }

    public String getCode(final String phone) {
        final Object value = redisUtils.getValue(PHONE_CODE_KEY_PREFIX + phone);
        if (value == null) {
            return null;
        }
        return String.valueOf(value);
    }

    public void clearCode(final String phone) {
        redisUtils.delKey(PHONE_CODE_KEY_PREFIX + phone);
    }

    public void send(final String phone) {
        validatePhone(phone);
        final String lockKey = PHONE_CODE_SEND_LOCK_PREFIX + phone;
        if (redisUtils.hasKey(lockKey)) {
            throw new SmsException("验证码发送过于频繁，请稍后再试");
        }

        final AliConfiguration.Sms sms = aliConfiguration.getSms();
        if (sms == null || sms.getSignName() == null || sms.getTemplateCode() == null) {
            throw new SmsException("短信服务未配置");
        }

        final AliConfiguration.Certified certified = aliConfiguration.getCertified();
        if (certified == null || certified.getAccessKey() == null || certified.getSecretKey() == null) {
            throw new SmsException("阿里云密钥未配置");
        }

        final String code = RandomStringUtils.randomNumeric(6);
        try (AsyncClient client = AsyncClient.builder()
                .region("cn-hangzhou")
                .credentialsProvider(StaticCredentialProvider.create(
                        Credential.builder()
                                .accessKeyId(certified.getAccessKey())
                                .accessKeySecret(certified.getSecretKey())
                                .build()))
                .overrideConfiguration(ClientOverrideConfiguration.create()
                        .setEndpointOverride("dysmsapi.aliyuncs.com"))
                .build()) {

            final SendSmsRequest request = SendSmsRequest.builder()
                    .phoneNumbers(phone)
                    .signName(sms.getSignName())
                    .templateCode(sms.getTemplateCode())
                    .templateParam(JSON.toJSONString(Map.of("code", code)))
                    .build();

            final SendSmsResponse response = client.sendSms(request).get();
            if (response.getBody() == null || !"OK".equalsIgnoreCase(response.getBody().getCode())) {
                final String message = response.getBody() != null ? response.getBody().getMessage() : "发送失败";
                log.error("短信发送失败, phone={}, message={}", phone, message);
                throw new SmsException("短信验证码发送失败，请稍后再试");
            }

            redisUtils.setValueTimeout(PHONE_CODE_KEY_PREFIX + phone, code, CacheExpireConstant.EXPIRE_5_MINUTES);
            redisUtils.setValueTimeout(lockKey, "1", 60L);
        } catch (SmsException e) {
            throw e;
        } catch (Exception e) {
            log.error("短信发送异常, phone={}", phone, e);
            throw new SmsException("短信验证码发送失败，请稍后再试");
        }
    }
}
