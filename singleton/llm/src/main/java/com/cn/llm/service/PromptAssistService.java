package com.cn.llm.service;

import com.cn.llm.dto.PromptEnhanceDto;
import com.cn.llm.excepitons.LlmException;
import com.cn.llm.vo.PromptEnhanceVo;

/**
 * AI 提示词辅助服务
 */
public interface PromptAssistService {

    PromptEnhanceVo enhance(PromptEnhanceDto dto) throws LlmException;
}
