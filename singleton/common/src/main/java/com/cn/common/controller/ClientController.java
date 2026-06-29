package com.cn.common.controller;

import com.cn.common.configuration.AppClientProperties;
import com.cn.common.msg.Result;
import com.cn.common.vo.ClientVersionVo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 客户端探测与健康检查接口。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/client")
public class ClientController {

    private final AppClientProperties appClientProperties;

    @GetMapping("/health")
    public Result health() {
        return Result.data(Map.of("status", "UP"));
    }

    @GetMapping("/version")
    public Result version() {
        final ClientVersionVo vo = new ClientVersionVo();
        vo.setVersion(appClientProperties.getVersion());
        vo.setMinSupportedVersion(appClientProperties.getMinSupportedVersion());
        vo.setDownloadUrl(appClientProperties.getDownloadUrl());
        return Result.data(vo);
    }
}
