package com.cn.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cn.common.entity.ApiKey;
import com.cn.common.entity.User;
import com.cn.common.enums.ApiKeyStatusEnum;
import com.cn.common.mapper.ApiKeyMapper;
import com.cn.common.mapper.UserMapper;
import com.cn.common.utils.ApiKeyGenerator;
import com.cn.common.vo.PageVo;
import com.cn.system.dto.CreateApiKeyDto;
import com.cn.system.dto.DeleteApiKeyDto;
import com.cn.system.dto.RotateApiKeyDto;
import com.cn.system.dto.UpdateApiKeyDto;
import com.cn.system.service.SystemApiKeyService;
import com.cn.system.vo.CreateApiKeyResultVo;
import com.cn.system.vo.SystemApiKeyVo;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SystemApiKeyServiceImpl implements SystemApiKeyService {

    private final ApiKeyMapper apiKeyMapper;
    private final UserMapper userMapper;
    private final ApiKeyGenerator apiKeyGenerator;

    @Override
    public PageVo<SystemApiKeyVo> page(Integer page, Integer size, String keyword, Integer status, Long userId) {
        Page<ApiKey> mpPage = new Page<>(page == null ? 1 : page, size == null ? 10 : size);
        LambdaQueryWrapper<ApiKey> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(ApiKey::getName, keyword)
                    .or()
                    .like(ApiKey::getKeyPrefix, keyword));
        }
        if (status != null) {
            wrapper.eq(ApiKey::getStatus, status);
        }
        if (userId != null) {
            wrapper.eq(ApiKey::getUserId, userId);
        }
        wrapper.orderByDesc(ApiKey::getId);
        IPage<ApiKey> result = apiKeyMapper.selectPage(mpPage, wrapper);
        Map<Long, User> users = loadUsers(result.getRecords());
        List<SystemApiKeyVo> items = result.getRecords().stream()
                .map(item -> toVo(item, users.get(item.getUserId())))
                .collect(Collectors.toList());
        return new PageVo<SystemApiKeyVo>().setTotal(result.getTotal()).setItems(items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateApiKeyResultVo create(CreateApiKeyDto dto) {
        User user = userMapper.selectById(dto.getUserId());
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        String plainKey = apiKeyGenerator.generatePlainKey();
        ApiKey entity = new ApiKey()
                .setUserId(dto.getUserId())
                .setName(dto.getName().trim())
                .setKeyPrefix(apiKeyGenerator.extractPrefix(plainKey))
                .setKeyHash(apiKeyGenerator.hashKey(plainKey))
                .setStatus(ApiKeyStatusEnum.ENABLED.getCode())
                .setExpiresAt(dto.getExpiresAt());
        apiKeyMapper.insert(entity);
        return new CreateApiKeyResultVo()
                .setId(entity.getId())
                .setPlainKey(plainKey);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(UpdateApiKeyDto dto) {
        ApiKey apiKey = requireApiKey(dto.getId());
        if (StringUtils.isNotBlank(dto.getName())) {
            apiKey.setName(dto.getName().trim());
        }
        if (dto.getStatus() != null) {
            apiKey.setStatus(dto.getStatus());
        }
        apiKey.setExpiresAt(dto.getExpiresAt());
        apiKeyMapper.updateById(apiKey);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(DeleteApiKeyDto dto) {
        requireApiKey(dto.getId());
        apiKeyMapper.deleteById(dto.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateApiKeyResultVo rotate(RotateApiKeyDto dto) {
        ApiKey apiKey = requireApiKey(dto.getId());
        apiKey.setStatus(ApiKeyStatusEnum.DISABLED.getCode());
        apiKeyMapper.updateById(apiKey);

        String plainKey = apiKeyGenerator.generatePlainKey();
        ApiKey newKey = new ApiKey()
                .setUserId(apiKey.getUserId())
                .setName(apiKey.getName())
                .setKeyPrefix(apiKeyGenerator.extractPrefix(plainKey))
                .setKeyHash(apiKeyGenerator.hashKey(plainKey))
                .setStatus(ApiKeyStatusEnum.ENABLED.getCode())
                .setExpiresAt(apiKey.getExpiresAt());
        apiKeyMapper.insert(newKey);
        return new CreateApiKeyResultVo()
                .setId(newKey.getId())
                .setPlainKey(plainKey);
    }

    private ApiKey requireApiKey(Long id) {
        ApiKey apiKey = apiKeyMapper.selectById(id);
        if (apiKey == null) {
            throw new IllegalArgumentException("API Key 不存在");
        }
        return apiKey;
    }

    private Map<Long, User> loadUsers(List<ApiKey> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> userIds = records.stream()
                .map(ApiKey::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user, (a, b) -> a));
    }

    private SystemApiKeyVo toVo(ApiKey apiKey, User user) {
        return new SystemApiKeyVo()
                .setId(apiKey.getId())
                .setUserId(apiKey.getUserId())
                .setUserNickname(user == null ? null : user.getNickname())
                .setName(apiKey.getName())
                .setKeyPrefix(apiKey.getKeyPrefix())
                .setStatus(apiKey.getStatus())
                .setExpiresAt(apiKey.getExpiresAt())
                .setLastUsedAt(apiKey.getLastUsedAt())
                .setCreateTime(apiKey.getCreateTime());
    }
}
