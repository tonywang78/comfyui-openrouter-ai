package com.cn.system.service;

import com.cn.common.vo.PageVo;
import com.cn.system.dto.CreateApiKeyDto;
import com.cn.system.dto.DeleteApiKeyDto;
import com.cn.system.dto.RotateApiKeyDto;
import com.cn.system.dto.UpdateApiKeyDto;
import com.cn.system.vo.CreateApiKeyResultVo;
import com.cn.system.vo.SystemApiKeyVo;

public interface SystemApiKeyService {

    PageVo<SystemApiKeyVo> page(Integer page, Integer size, String keyword, Integer status, Long userId);

    CreateApiKeyResultVo create(CreateApiKeyDto dto);

    void update(UpdateApiKeyDto dto);

    void delete(DeleteApiKeyDto dto);

    CreateApiKeyResultVo rotate(RotateApiKeyDto dto);
}
