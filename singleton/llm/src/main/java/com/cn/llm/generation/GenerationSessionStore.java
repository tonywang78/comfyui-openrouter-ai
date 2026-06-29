package com.cn.llm.generation;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.cn.common.utils.RedisUtils;
import com.cn.llm.config.GenerationAgentConfig;
import com.cn.llm.dto.GenerationSubmitDto;
import com.cn.llm.dto.TaskDraftDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 生成助手 Redis 会话存储
 */
@Component
@RequiredArgsConstructor
public class GenerationSessionStore {

    private static final String SESSION_PREFIX = "generation:session:";
    private static final String OWNER_PREFIX = "generation:session:owner:";
    private static final String META_PREFIX = "generation:session:meta:";
    private static final String DRAFT_PREFIX = "generation:draft:";
    private static final String ATTACHMENTS_PREFIX = "generation:session:attachments:";

    private final RedisUtils redisUtils;
    private final GenerationAgentConfig config;

    public String sessionKey(String sessionId) {
        return SESSION_PREFIX + sessionId;
    }

    public void addUserMessage(String sessionId, Long userId, String contentJson, List<GenerationSubmitDto.AttachmentDto> attachments) {
        String key = sessionKey(sessionId);
        Map<String, Object> msg = new HashMap<>();
        msg.put("role", "user");
        msg.put("content", JSON.parseArray(contentJson));
        msg.put("timestamp", System.currentTimeMillis());
        if (attachments != null && !attachments.isEmpty()) {
            msg.put("attachments", attachments);
        }

        List<Object> last = redisUtils.listGet(key, 0, 0);
        String json = JSON.toJSONString(msg);
        if (!last.isEmpty()) {
            JSONObject lastObj = JSON.parseObject((String) last.getFirst());
            if ("user".equals(lastObj.getString("role"))) {
                redisUtils.getRedisTemplate().opsForList().set(key, 0, json);
            } else {
                redisUtils.listPush(key, json);
            }
        } else {
            redisUtils.listPush(key, json);
        }

        int ttl = config.getSessionTtlSeconds();
        redisUtils.expire(key, ttl);

        String ownerKey = OWNER_PREFIX + sessionId;
        if (!redisUtils.hasKey(ownerKey)) {
            redisUtils.setValueTimeout(ownerKey, String.valueOf(userId), ttl);
        }

        if (attachments != null && !attachments.isEmpty()) {
            mergeSessionAttachments(sessionId, attachments);
        }
    }

    private void mergeSessionAttachments(String sessionId, List<GenerationSubmitDto.AttachmentDto> attachments) {
        String key = ATTACHMENTS_PREFIX + sessionId;
        Set<String> urls = new LinkedHashSet<>();
        Object existing = redisUtils.getValue(key);
        if (existing != null) {
            JSONArray arr = JSON.parseArray(existing.toString());
            for (int i = 0; i < arr.size(); i++) {
                JSONObject o = arr.getJSONObject(i);
                if (o != null && o.getString("url") != null) {
                    urls.add(o.getString("url"));
                }
            }
        }
        List<GenerationSubmitDto.AttachmentDto> merged = new ArrayList<>();
        if (existing != null) {
            merged.addAll(JSON.parseArray(existing.toString(), GenerationSubmitDto.AttachmentDto.class));
        }
        for (GenerationSubmitDto.AttachmentDto a : attachments) {
            if (a != null && a.getUrl() != null && !urls.contains(a.getUrl())) {
                merged.add(a);
                urls.add(a.getUrl());
            }
        }
        redisUtils.setValueTimeout(key, JSON.toJSONString(merged), config.getSessionTtlSeconds());
    }

    public List<GenerationSubmitDto.AttachmentDto> getSessionAttachments(String sessionId) {
        Object val = redisUtils.getValue(ATTACHMENTS_PREFIX + sessionId);
        if (val == null) return Collections.emptyList();
        return JSON.parseArray(val.toString(), GenerationSubmitDto.AttachmentDto.class);
    }

    public void addAssistantMessage(String sessionId, String text, List<Map<String, Object>> citations) {
        List<Map<String, Object>> parts = new ArrayList<>();
        if (text != null && !text.isBlank()) {
            Map<String, Object> t = new HashMap<>();
            t.put("type", "text");
            t.put("text", text);
            parts.add(t);
        }
        Map<String, Object> msg = new HashMap<>();
        msg.put("role", "assistant");
        msg.put("content", parts);
        msg.put("timestamp", System.currentTimeMillis());
        if (citations != null && !citations.isEmpty()) {
            msg.put("citations", citations);
        }
        redisUtils.listPush(sessionKey(sessionId), JSON.toJSONString(msg));
        redisUtils.expire(sessionKey(sessionId), config.getSessionTtlSeconds());
    }

    public List<Map<String, Object>> getMessages(String sessionId, int limit) {
        List<Object> raw = redisUtils.listGet(sessionKey(sessionId), 0, limit - 1);
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = raw.size() - 1; i >= 0; i--) {
            JSONObject obj = JSON.parseObject((String) raw.get(i));
            Map<String, Object> m = new HashMap<>();
            m.put("role", obj.getString("role"));
            m.put("content", obj.get("content"));
            result.add(m);
        }
        return result;
    }

    public boolean needsReply(String sessionId) {
        List<Object> last = redisUtils.listGet(sessionKey(sessionId), 0, 0);
        if (last.isEmpty()) return false;
        JSONObject obj = JSON.parseObject((String) last.getFirst());
        return "user".equals(obj.getString("role"));
    }

    public boolean isOwner(String sessionId, Long userId) {
        String owner = (String) redisUtils.getValue(OWNER_PREFIX + sessionId);
        return owner == null || owner.equals(String.valueOf(userId));
    }

    public SessionMeta getMeta(String sessionId) {
        Object val = redisUtils.getValue(META_PREFIX + sessionId);
        SessionMeta meta;
        if (val == null) {
            meta = new SessionMeta();
            if (config.getDefaultPinnedWorkflowId() != null) {
                meta.getPinnedWorkflowIds().add(config.getDefaultPinnedWorkflowId());
            }
            return meta;
        }
        meta = JSON.parseObject(val.toString(), SessionMeta.class);
        normalizePinned(meta);
        return meta;
    }

    public void saveMeta(String sessionId, SessionMeta meta) {
        redisUtils.setValueTimeout(META_PREFIX + sessionId, JSON.toJSONString(meta), config.getSessionTtlSeconds());
    }

    public void setPinnedWorkflow(String sessionId, Long workflowId) {
        addPinnedWorkflow(sessionId, workflowId);
    }

    public void addPinnedWorkflow(String sessionId, Long workflowId) {
        if (workflowId == null) return;
        SessionMeta meta = getMeta(sessionId);
        normalizePinned(meta);
        if (!meta.getPinnedWorkflowIds().contains(workflowId)) {
            meta.getPinnedWorkflowIds().add(workflowId);
        }
        saveMeta(sessionId, meta);
    }

    public void syncPinnedWorkflows(String sessionId, List<Long> workflowIds) {
        SessionMeta meta = getMeta(sessionId);
        meta.setPinnedWorkflowIds(workflowIds != null ? new ArrayList<>(workflowIds) : new ArrayList<>());
        saveMeta(sessionId, meta);
    }

    private void normalizePinned(SessionMeta meta) {
        if (meta.getPinnedWorkflowIds() == null) {
            meta.setPinnedWorkflowIds(new ArrayList<>());
        }
        if (meta.getPinnedWorkflowIds().isEmpty() && meta.getPinnedWorkflowId() != null) {
            meta.getPinnedWorkflowIds().add(meta.getPinnedWorkflowId());
        }
    }

    public void addTaskId(String sessionId, String taskId) {
        SessionMeta meta = getMeta(sessionId);
        if (meta.getTaskIds() == null) {
            meta.setTaskIds(new ArrayList<>());
        }
        if (!meta.getTaskIds().contains(taskId)) {
            meta.getTaskIds().add(taskId);
        }
        saveMeta(sessionId, meta);
    }

    public void saveDraft(TaskDraftDto draft) {
        int ttlMinutes = config.getDraftTtlMinutes() != null ? config.getDraftTtlMinutes() : 30;
        redisUtils.setValueTimeout(DRAFT_PREFIX + draft.getDraftId(), JSON.toJSONString(draft),
                (int) TimeUnit.MINUTES.toSeconds(ttlMinutes));
    }

    public TaskDraftDto getDraft(String draftId) {
        Object val = redisUtils.getValue(DRAFT_PREFIX + draftId);
        if (val == null) return null;
        return JSON.parseObject(val.toString(), TaskDraftDto.class);
    }

    public void updateDraftStatus(String draftId, String status) {
        TaskDraftDto draft = getDraft(draftId);
        if (draft != null) {
            draft.setStatus(status);
            saveDraft(draft);
        }
    }

    public void deleteSession(String sessionId) {
        redisUtils.delKey(sessionKey(sessionId));
        redisUtils.delKey(OWNER_PREFIX + sessionId);
        redisUtils.delKey(META_PREFIX + sessionId);
        redisUtils.delKey(ATTACHMENTS_PREFIX + sessionId);
    }

    @lombok.Data
    public static class SessionMeta {
        /** @deprecated 兼容旧数据，请使用 pinnedWorkflowIds */
        private Long pinnedWorkflowId;
        private List<Long> pinnedWorkflowIds = new ArrayList<>();
        private List<String> taskIds = new ArrayList<>();
    }
}
