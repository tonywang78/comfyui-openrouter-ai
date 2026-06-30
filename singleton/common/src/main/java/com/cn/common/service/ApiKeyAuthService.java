package com.cn.common.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cn.common.entity.ApiKey;
import com.cn.common.entity.User;
import com.cn.common.enums.ApiKeyStatusEnum;
import com.cn.common.mapper.ApiKeyMapper;
import com.cn.common.mapper.UserMapper;
import com.cn.common.structure.UserInfoStructure;
import com.cn.common.utils.ApiKeyGenerator;
import com.cn.common.utils.UserUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyAuthService {

    private final ApiKeyMapper apiKeyMapper;
    private final UserMapper userMapper;
    private final ApiKeyGenerator apiKeyGenerator;

    public Optional<User> authenticate(String rawKey) {
        if (!ApiKeyGenerator.isApiKey(rawKey)) {
            return Optional.empty();
        }
        String hash = apiKeyGenerator.hashKey(rawKey);
        ApiKey apiKey = apiKeyMapper.selectOne(new LambdaQueryWrapper<ApiKey>()
                .eq(ApiKey::getKeyHash, hash));
        if (apiKey == null || !ApiKeyStatusEnum.isEnabled(apiKey.getStatus())) {
            return Optional.empty();
        }
        if (apiKey.getExpiresAt() != null && apiKey.getExpiresAt().isBefore(LocalDateTime.now())) {
            return Optional.empty();
        }
        User user = userMapper.selectById(apiKey.getUserId());
        if (user == null) {
            return Optional.empty();
        }
        touchLastUsedAsync(apiKey.getId());
        return Optional.of(user);
    }

    public void loginAsUser(User user) {
        StpUtil.login(user.getId());
        UserUtils.updateUserInfo(new UserInfoStructure()
                .setAvatar(user.getAvatar())
                .setNickname(user.getNickname())
                .setRole(user.getRole())
                .setUserId(user.getId()));
    }

    public Long resolveUserIdByCredential(String credential) {
        if (StringUtils.isBlank(credential)) {
            throw new IllegalArgumentException("凭证不能为空");
        }
        if (ApiKeyGenerator.isApiKey(credential)) {
            return authenticate(credential)
                    .map(User::getId)
                    .orElseThrow(() -> new IllegalArgumentException("API Key 无效或已过期"));
        }
        return UserUtils.getLoginIdByToken(credential);
    }

    private void touchLastUsedAsync(Long apiKeyId) {
        CompletableFuture.runAsync(() -> {
            try {
                ApiKey update = new ApiKey()
                        .setId(apiKeyId)
                        .setLastUsedAt(LocalDateTime.now());
                apiKeyMapper.updateById(update);
            } catch (Exception e) {
                log.warn("更新 API Key 最后使用时间失败: id={}", apiKeyId, e);
            }
        });
    }
}
