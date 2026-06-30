package com.cn.common.utils;

import com.cn.common.configuration.ApiKeyProperties;
import com.cn.common.constant.ApiKeyConstant;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
@RequiredArgsConstructor
public class ApiKeyGenerator {

    private final ApiKeyProperties apiKeyProperties;

    public String generatePlainKey() {
        return ApiKeyConstant.PREFIX + RandomStringUtils.randomAlphanumeric(ApiKeyConstant.RANDOM_LENGTH);
    }

    public String hashKey(String plainKey) {
        String pepper = apiKeyProperties.getPepper();
        String source = pepper == null || pepper.isBlank() ? plainKey : plainKey + pepper;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                sb.append(String.format("%02x", value));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public String extractPrefix(String plainKey) {
        if (plainKey == null) {
            return "";
        }
        int end = Math.min(plainKey.length(), ApiKeyConstant.PREFIX_DISPLAY_LENGTH);
        return plainKey.substring(0, end);
    }

    public static boolean isApiKey(String credential) {
        return credential != null && credential.startsWith(ApiKeyConstant.PREFIX);
    }
}
