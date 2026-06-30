package com.cn.common.utils;

import cn.dev33.satoken.context.SaHolder;
import com.cn.common.constant.ApiKeyConstant;
import com.cn.common.service.ApiKeyAuthService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CredentialUtils {

    private final ApiKeyAuthService apiKeyAuthService;

    public String extractApiKeyFromRequest() {
        String headerKey = SaHolder.getRequest().getHeader(ApiKeyConstant.HEADER_NAME);
        if (StringUtils.isNotBlank(headerKey)) {
            return headerKey.trim();
        }
        String authorization = SaHolder.getRequest().getHeader("Authorization");
        if (StringUtils.isBlank(authorization)) {
            return null;
        }
        String bearer = authorization.trim();
        if (bearer.length() > 7 && bearer.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = bearer.substring(7).trim();
            if (ApiKeyGenerator.isApiKey(token)) {
                return token;
            }
        }
        return null;
    }

    public Long resolveUserId(String credential) {
        return apiKeyAuthService.resolveUserIdByCredential(credential);
    }
}
