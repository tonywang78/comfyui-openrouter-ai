package com.cn.llm.controller;

import com.cn.common.annotations.RateLimit;
import com.cn.common.msg.Result;
import com.cn.llm.dto.GenerationConfirmDto;
import com.cn.llm.dto.GenerationDeleteSessionDto;
import com.cn.llm.dto.GenerationSubmitDto;
import com.cn.llm.excepitons.LlmException;
import com.cn.llm.service.GenerationAgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 生成助手 Agent 控制器
 */
@Slf4j
@RestController
@RequestMapping("/llm/generation")
@RequiredArgsConstructor
public class GenerationAgentController {

    private final GenerationAgentService generationAgentService;

    @PostMapping(value = "/submit", produces = MediaType.APPLICATION_JSON_VALUE)
    @RateLimit(permitsPerSecond = 1.0, limitType = RateLimit.LimitType.USER, message = "消息发送过于频繁，请稍后再试")
    public Result submit(@RequestBody @Validated GenerationSubmitDto dto) {
        try {
            generationAgentService.submitMessage(dto);
            return Result.ok();
        } catch (LlmException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @RateLimit(permitsPerSecond = 2.0, limitType = RateLimit.LimitType.USER, message = "连接请求过于频繁，请稍后再试")
    public SseEmitter stream(@RequestParam("sessionId") String sessionId,
                             @RequestParam(value = "enableWebSearch", required = false) String enableWebSearch,
                             @RequestParam(value = "pinnedWorkflowIds", required = false) String pinnedWorkflowIds,
                             @RequestParam(value = "pinnedWorkflowId", required = false) Long pinnedWorkflowId,
                             @RequestParam("token") String token) {
        List<Long> ids = parsePinnedWorkflowIds(pinnedWorkflowIds, pinnedWorkflowId);
        return generationAgentService.stream(sessionId, enableWebSearch, ids, token);
    }

    private static List<Long> parsePinnedWorkflowIds(String pinnedWorkflowIds, Long pinnedWorkflowId) {
        List<Long> ids = new ArrayList<>();
        if (pinnedWorkflowIds != null && !pinnedWorkflowIds.isBlank()) {
            for (String part : pinnedWorkflowIds.split(",")) {
                if (part == null || part.isBlank()) continue;
                try {
                    ids.add(Long.parseLong(part.trim()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (ids.isEmpty() && pinnedWorkflowId != null) {
            ids.add(pinnedWorkflowId);
        }
        return ids;
    }

    @PostMapping(value = "/confirm", produces = MediaType.APPLICATION_JSON_VALUE)
    @RateLimit(permitsPerSecond = 0.5, limitType = RateLimit.LimitType.USER, message = "提交过于频繁，请稍后再试")
    public Result confirm(@RequestBody @Validated GenerationConfirmDto dto) {
        try {
            String taskId = generationAgentService.confirmDraft(dto);
            return Result.data(Map.of("taskId", taskId, "draftId", dto.getDraftId()));
        } catch (LlmException e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping(value = "/session/delete", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @RateLimit(permitsPerSecond = 0.5, limitType = RateLimit.LimitType.USER, message = "删除操作过于频繁，请稍后再试")
    public Result deleteSession(@RequestBody @Validated GenerationDeleteSessionDto body) {
        try {
            generationAgentService.deleteSession(body.getSessionId());
            return Result.ok();
        } catch (LlmException e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping(value = "/workflows", produces = MediaType.APPLICATION_JSON_VALUE)
    public Result listWorkflows(@RequestParam(value = "keyword", required = false) String keyword,
                                 @RequestParam(value = "categoryId", required = false) Long categoryId,
                                 @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return Result.data(generationAgentService.listWorkflowsBrief(keyword, categoryId, limit));
    }
}
