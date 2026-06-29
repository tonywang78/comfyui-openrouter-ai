<template>
  <div class="generate-root">
    <GenerateSidebar
      :sessions="generationStore.sessions"
      :active-session-id="generationStore.activeSessionId"
      @new-session="handleNewSession"
      @select-session="handleSelectSession"
      @delete-session="handleDeleteSession"
    />

    <main class="main-content">
      <header class="top-bar">
        <div class="top-left">
          <div class="page-title">
            <span class="title-icon">✦</span>
            <span>{{ t('generate.welcome.title') }}</span>
          </div>
          <WorkflowPickerChip
            :model-value="pinnedWorkflows"
            @update:model-value="onPinnedWorkflowsChange"
          />
        </div>
        <router-link to="/comfyui" class="back-link">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M19 12H5M12 19l-7-7 7-7"/>
          </svg>
          {{ t('common.back') }}
        </router-link>
      </header>

      <div class="scroll-body">
        <div class="content-wrap">
          <GenerateWelcome
            v-if="!hasMessages"
            @send-suggestion="handleSuggestion"
          />
          <GenerateMessages
            v-else
            :messages="generationStore.activeMessages"
            :session-id="generationStore.activeSessionId || ''"
            :is-typing="isTyping"
            @draft-confirm="handleDraftConfirm"
            @draft-modify="handleDraftModify"
          />
        </div>
      </div>

      <div class="composer-dock">
        <GenerateComposer
          ref="composerRef"
          :disabled="isTyping"
          :is-typing="isTyping"
          :web-search-enabled="enableWebSearch"
          @send="handleSend"
          @stop="stopReply"
          @websearch-change="enableWebSearch = $event"
        />
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessageBox } from 'element-plus'
import { useGenerationStore } from '@/stores/modules/generation'
import { generationApi } from '@/api/generation/generation'
import GenerateSidebar from './components/GenerateSidebar.vue'
import GenerateWelcome from './components/GenerateWelcome.vue'
import GenerateMessages from './components/GenerateMessages.vue'
import GenerateComposer from './components/GenerateComposer.vue'
import WorkflowPickerChip from './components/WorkflowPickerChip.vue'
import { useGenerationStream } from './composable/useGenerationStream'
import { useSessionTasks } from './composable/useSessionTasks'
import type { TaskDraftApi } from '@/api/generation/types'
import type { PinnedWorkflow } from './types'

const { t } = useI18n()
const generationStore = useGenerationStore()
const enableWebSearch = ref(false)
const composerRef = ref<InstanceType<typeof GenerateComposer> | null>(null)

const pendingWorkflows = ref<PinnedWorkflow[]>([])

const pinnedWorkflows = computed(() =>
  generationStore.activeSession?.pinnedWorkflows ?? pendingWorkflows.value
)

const hasMessages = computed(() => generationStore.activeMessages.length > 0)

const { registerTask } = useSessionTasks()

const { isTyping, sendMessage, stopReply } = useGenerationStream({
  enableWebSearch,
  getPinnedWorkflowIds: () => pinnedWorkflows.value.map(w => w.workflowId),
  onWorkflowSwitched: (data) => {
    const workflow: PinnedWorkflow = {
      workflowId: data.workflowId,
      name: data.workflowName,
      creditsDeducted: data.creditsDeducted
    }
    pendingWorkflows.value = mergeWorkflows(pendingWorkflows.value, [workflow])
    if (generationStore.activeSessionId) {
      generationStore.addPinnedWorkflow(generationStore.activeSessionId, workflow)
    }
    generationStore.appendMessage(generationStore.activeSessionId!, {
      id: Date.now().toString(),
      role: 'system',
      content: t('generate.workflow.switched', { name: data.workflowName }),
      timestamp: Date.now()
    })
  }
})

function mergeWorkflows(existing: PinnedWorkflow[], incoming: PinnedWorkflow[]) {
  const map = new Map(existing.map(w => [w.workflowId, w]))
  for (const w of incoming) {
    map.set(w.workflowId, w)
  }
  return Array.from(map.values())
}

onMounted(() => {
  generationStore.rehydrate()
  if (!generationStore.activeSessionId && generationStore.sessions.length) {
    generationStore.setActiveSession(generationStore.sessions[0].id)
  }
  syncPendingFromSession()
})

const handleNewSession = () => {
  pendingWorkflows.value = []
  generationStore.createSession([])
}

const handleSelectSession = (id: string) => {
  generationStore.setActiveSession(id)
  syncPendingFromSession()
}

const syncPendingFromSession = () => {
  const session = generationStore.activeSession
  pendingWorkflows.value = session?.pinnedWorkflows ? [...session.pinnedWorkflows] : []
}

watch(() => generationStore.activeSessionId, syncPendingFromSession)

const handleDeleteSession = async (id: string) => {
  try {
    await ElMessageBox.confirm(t('generate.sidebar.deleteConfirm'), t('common.warning'), { type: 'warning' })
    try {
      await generationApi.reqDeleteSession({ sessionId: id })
    } catch { /* local delete anyway */ }
    generationStore.deleteSession(id)
  } catch { /* cancelled */ }
}

const onPinnedWorkflowsChange = (workflows: PinnedWorkflow[]) => {
  pendingWorkflows.value = workflows
  const sid = generationStore.activeSessionId
  if (sid) {
    generationStore.setPinnedWorkflows(sid, workflows)
  }
}

const handleSend = (payload: {
  content: string
  imageUrls?: string[]
  attachments?: { url: string; filename?: string; mime?: string; kind?: string }[]
}) => {
  if (!generationStore.activeSessionId && !hasMessages.value) {
    const session = generationStore.createSession([...pendingWorkflows.value])
    generationStore.setActiveSession(session.id)
  }
  sendMessage(payload)
}

const handleSuggestion = (message: string) => {
  handleSend({ content: message })
}

const handleDraftConfirm = (payload: { taskId: string; draftId: string; workflowName: string }) => {
  const sid = generationStore.activeSessionId
  if (!sid) return
  generationStore.updateDraftStatus(sid, payload.draftId, 'confirmed')
  registerTask(payload.taskId, payload.workflowName)
  generationStore.appendMessage(sid, {
    id: Date.now().toString(),
    role: 'system',
    content: t('generate.draft.taskSubmitted'),
    timestamp: Date.now()
  })
}

const handleDraftModify = (draft: TaskDraftApi.Draft) => {
  const summary = draft.nodeContainer
    .map(n => `${n.tips || n.inputs}: ${n.nodeValue}`)
    .join('\n')
  composerRef.value?.fillText(t('generate.draft.modifyPrefix', { summary: draft.summary }) + '\n' + summary)
}
</script>

<style scoped>
.generate-root {
  display: flex;
  height: 100%;
  min-height: 0;
  overflow: hidden;
  background: var(--el-bg-color-page, var(--el-bg-color));
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
}

.top-bar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 24px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  background: var(--el-bg-color);
  backdrop-filter: blur(8px);
  z-index: 2;
}

.top-left {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  min-width: 0;
  flex: 1;
}

.page-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  white-space: nowrap;
  padding-top: 8px;
}

.title-icon {
  color: var(--el-color-primary);
  font-size: 14px;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--el-text-color-secondary);
  text-decoration: none;
  padding: 6px 12px;
  border-radius: 8px;
  transition: all 0.15s ease;
  flex-shrink: 0;
}

.back-link:hover {
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
}

.scroll-body {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
}

.content-wrap {
  max-width: 820px;
  margin: 0 auto;
  padding: 24px 24px 16px;
  min-height: 100%;
}

.composer-dock {
  flex-shrink: 0;
  padding: 12px 24px 20px;
  background: linear-gradient(to top, var(--el-bg-color) 70%, transparent);
  border-top: 1px solid var(--el-border-color-extra-light);
  z-index: 2;
}

.composer-dock :deep(.generate-composer) {
  max-width: 820px;
  margin: 0 auto;
}
</style>
