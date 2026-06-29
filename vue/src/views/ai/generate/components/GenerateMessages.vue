<template>
  <div class="generate-messages" ref="containerRef">
    <div v-for="msg in messages" :key="msg.id" class="message-block" :class="msg.role">
      <div v-if="msg.role === 'user'" class="user-msg">
        <div class="bubble">{{ msg.content }}</div>
        <div v-if="msg.imageUrls?.length" class="attachments">
          <img v-for="(url, i) in msg.imageUrls" :key="i" :src="url" class="thumb" alt="" />
        </div>
      </div>

      <div v-else-if="msg.role === 'assistant'" class="assistant-msg">
        <div class="avatar">AI</div>
        <div class="assistant-body">
          <div v-if="msg.toolStatus" class="tool-status">
            <span class="tool-dot" />
            {{ msg.toolStatus }}
          </div>
          <MarkdownRenderer v-if="msg.content" :content="msg.content" />
          <div v-if="msg.citations?.length" class="citations">
            <a v-for="(c, i) in msg.citations" :key="i" :href="c.url" target="_blank" rel="noopener" class="cite-link">
              {{ c.title || c.url }}
            </a>
          </div>
        </div>
      </div>

      <div v-else-if="msg.role === 'system'" class="system-msg">
        <span>{{ msg.content }}</span>
      </div>

      <div v-else-if="msg.role === 'task_draft'" class="card-msg">
        <TaskDraftCard
          :draft="msg.draft"
          :session-id="sessionId"
          @confirm="$emit('draft-confirm', $event)"
          @modify="$emit('draft-modify', $event)"
        />
      </div>

      <div v-else-if="msg.role === 'task_status'" class="card-msg">
        <TaskStatusCard
          :task-id="msg.taskId"
          :workflow-name="msg.workflowName"
          :status="msg.status"
          :progress="msg.progress"
          :location="msg.location"
          :workflow-result-model="msg.workflowResultModel"
          @remake="$emit('task-remake', $event)"
        />
      </div>
    </div>

    <div v-if="isTyping" class="typing-row">
      <div class="avatar">AI</div>
      <div class="typing-indicator"><span /><span /><span /></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue'
import MarkdownRenderer from '@/components/common/MarkdownRenderer.vue'
import TaskDraftCard from './TaskDraftCard.vue'
import TaskStatusCard from './TaskStatusCard.vue'
import type { GenerationMessage } from '../types'
import type { TaskDraftApi } from '@/api/generation/types'

const props = defineProps<{
  messages: GenerationMessage[]
  sessionId: string
  isTyping?: boolean
}>()

defineEmits<{
  'draft-confirm': [payload: { taskId: string; draftId: string; workflowName: string }]
  'draft-modify': [draft: TaskDraftApi.Draft]
  'task-remake': [taskId: string]
}>()

const containerRef = ref<HTMLElement | null>(null)

const scrollToBottom = async () => {
  await nextTick()
  const parent = containerRef.value?.parentElement
  if (parent) parent.scrollTop = parent.scrollHeight
}

watch(() => props.messages.length, scrollToBottom)
watch(() => props.isTyping, scrollToBottom)
</script>

<style scoped>
.generate-messages {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-bottom: 8px;
}

.message-block.user { display: flex; justify-content: flex-end; }

.user-msg {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  max-width: 85%;
}

.user-msg .bubble {
  background: linear-gradient(135deg, var(--el-color-primary), var(--el-color-primary-light-3));
  color: #fff;
  padding: 12px 16px;
  border-radius: 18px 18px 4px 18px;
  font-size: 14px;
  line-height: 1.55;
  word-break: break-word;
  box-shadow: 0 2px 12px rgba(64, 158, 255, 0.2);
}

.user-msg .attachments {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
  justify-content: flex-end;
}

.user-msg .thumb {
  width: 72px;
  height: 72px;
  object-fit: cover;
  border-radius: 10px;
  border: 2px solid var(--el-bg-color);
}

.assistant-msg {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  max-width: 95%;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  background: linear-gradient(135deg, var(--el-color-primary-light-7), var(--el-color-primary));
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.assistant-body {
  flex: 1;
  min-width: 0;
  padding: 12px 16px;
  border-radius: 4px 16px 16px 16px;
  background: var(--el-fill-color-blank);
  border: 1px solid var(--el-border-color-lighter);
  font-size: 14px;
  line-height: 1.6;
}

.tool-status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  margin-bottom: 8px;
  padding: 4px 10px;
  border-radius: 8px;
  background: var(--el-fill-color-light);
}

.tool-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--el-color-primary);
  animation: pulse 1.2s infinite;
}

.citations {
  margin-top: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.cite-link {
  font-size: 12px;
  color: var(--el-color-primary);
  padding: 2px 8px;
  border-radius: 6px;
  background: var(--el-color-primary-light-9);
  text-decoration: none;
}

.cite-link:hover { text-decoration: underline; }

.system-msg {
  display: flex;
  justify-content: center;
}

.system-msg span {
  font-size: 12px;
  color: var(--el-text-color-secondary);
  padding: 4px 14px;
  border-radius: 999px;
  background: var(--el-fill-color);
}

.card-msg { width: 100%; }

.typing-row {
  display: flex;
  gap: 12px;
  align-items: center;
}

.typing-indicator {
  display: flex;
  gap: 5px;
  padding: 14px 18px;
  border-radius: 4px 16px 16px 16px;
  background: var(--el-fill-color-blank);
  border: 1px solid var(--el-border-color-lighter);
}

.typing-indicator span {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--el-text-color-secondary);
  animation: blink 1.2s infinite;
}

.typing-indicator span:nth-child(2) { animation-delay: 0.2s; }
.typing-indicator span:nth-child(3) { animation-delay: 0.4s; }

@keyframes blink {
  0%, 80%, 100% { opacity: 0.25; transform: scale(0.85); }
  40% { opacity: 1; transform: scale(1); }
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.35; }
}
</style>
