package com.cn.system.controller;

import com.cn.common.msg.Result;
import com.cn.common.vo.PageVo;
import com.cn.system.dto.CreateApiKeyDto;
import com.cn.system.dto.DeleteApiKeyDto;
import com.cn.system.dto.RotateApiKeyDto;
import com.cn.system.dto.UpdateApiKeyDto;
import com.cn.system.service.SystemApiKeyService;
import com.cn.system.vo.CreateApiKeyResultVo;
import com.cn.system.vo.SystemApiKeyVo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/system/api-key")
@Validated
@RequiredArgsConstructor
public class SystemApiKeyController {

    private final SystemApiKeyService systemApiKeyService;

    @GetMapping(value = "/page", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result page(@RequestParam(defaultValue = "1") @Min(1) Integer page,
                       @RequestParam(defaultValue = "10") @Min(1) Integer size,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Integer status,
                       @RequestParam(required = false) Long userId) {
        PageVo<SystemApiKeyVo> data = systemApiKeyService.page(page, size, keyword, status, userId);
        return Result.data(data);
    }

    @PostMapping(value = "/create", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result create(@RequestBody @Valid CreateApiKeyDto dto) {
        CreateApiKeyResultVo data = systemApiKeyService.create(dto);
        return Result.data(data);
    }

    @PostMapping(value = "/update", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result update(@RequestBody @Valid UpdateApiKeyDto dto) {
        systemApiKeyService.update(dto);
        return Result.ok();
    }

    @PostMapping(value = "/delete", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result delete(@RequestBody @Valid DeleteApiKeyDto dto) {
        systemApiKeyService.delete(dto);
        return Result.ok();
    }

    @PostMapping(value = "/rotate", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Result rotate(@RequestBody @Valid RotateApiKeyDto dto) {
        CreateApiKeyResultVo data = systemApiKeyService.rotate(dto);
        return Result.data(data);
    }
}
