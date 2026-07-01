package com.cn.media.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cn.common.entity.MediaStandardizationConfig;
import com.cn.common.mapper.MediaStandardizationConfigMapper;
import com.cn.common.msg.Result;
import com.cn.media.dto.SaveStandardizationConfigDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/media/admin")
@RequiredArgsConstructor
@SaCheckRole("ADMIN")
public class MediaAdminController {

    private final MediaStandardizationConfigMapper configMapper;

    @GetMapping("/standardization-configs")
    public Result listConfigs() {
        List<MediaStandardizationConfig> configs = configMapper.selectList(
                new LambdaQueryWrapper<MediaStandardizationConfig>().orderByAsc(MediaStandardizationConfig::getId));
        return Result.data(configs);
    }

    @PostMapping("/standardization-config")
    public Result saveConfig(@RequestBody @Valid SaveStandardizationConfigDto dto) {
        MediaStandardizationConfig config;
        if (dto.getId() != null) {
            config = configMapper.selectById(dto.getId());
            if (config == null) {
                return Result.error("配置不存在");
            }
        } else {
            config = new MediaStandardizationConfig();
        }
        config.setVariantType(dto.getVariantType());
        config.setWorkflowId(dto.getWorkflowId());
        config.setDisplayName(dto.getDisplayName());
        config.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : true);
        if (dto.getId() != null) {
            configMapper.updateById(config);
        } else {
            configMapper.insert(config);
        }
        return Result.ok("保存成功");
    }
}
