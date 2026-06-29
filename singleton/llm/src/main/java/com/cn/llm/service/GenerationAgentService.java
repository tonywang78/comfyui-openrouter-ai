package com.cn.llm.service;

import com.cn.llm.dto.GenerationConfirmDto;
import com.cn.llm.dto.GenerationSubmitDto;
import com.cn.llm.dto.TaskDraftDto;
import com.cn.llm.excepitons.LlmException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

/**
 * 生成助手 Agent 服务
 */
public interface GenerationAgentService {

    void submitMessage(GenerationSubmitDto dto) throws LlmException;

    SseEmitter stream(String sessionId, String enableWebSearch, List<Long> pinnedWorkflowIds, String token);

    String confirmDraft(GenerationConfirmDto dto) throws LlmException;

    void deleteSession(String sessionId) throws LlmException;

    List<Map<String, Object>> listWorkflowsBrief(String keyword, Long categoryId, int limit);
}
