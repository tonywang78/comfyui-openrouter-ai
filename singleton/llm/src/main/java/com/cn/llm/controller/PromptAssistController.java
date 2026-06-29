package com.cn.llm.controller;

import com.cn.common.annotations.RateLimit;
import com.cn.common.msg.Result;
import com.cn.llm.dto.PromptEnhanceDto;
import com.cn.llm.excepitons.LlmException;
import com.cn.llm.service.PromptAssistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 提示词辅助控制器
 */
@Slf4j
@RestController
@RequestMapping("/llm/prompt")
@RequiredArgsConstructor
public class PromptAssistController {

    private final PromptAssistService promptAssistService;

    @PostMapping(value = "/enhance", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @RateLimit(permitsPerSecond = 0.5, limitType = RateLimit.LimitType.USER, message = "提示词优化过于频繁，请稍后再试")
    public Result enhance(@RequestBody @Validated PromptEnhanceDto dto) {
        try {
            return Result.data(promptAssistService.enhance(dto));
        } catch (LlmException e) {
            return Result.error(e.getMessage());
        }
    }
}
