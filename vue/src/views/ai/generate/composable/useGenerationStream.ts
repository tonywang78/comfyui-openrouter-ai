import { ref, computed, type Ref } from 'vue'
import { generationApi } from '@/api/generation/generation'
import type { TaskDraftApi } from '@/api/generation/types'
import { useGenerationStore } from '@/stores/modules/generation'
import type { AssistantMessage, GenerationMessage } from '../types'

export function useGenerationStream(options: {
  enableWebSearch: Ref<boolean>
  getPinnedWorkflowIds: () => number[]
  onWorkflowSwitched?: (data: { workflowId: number; workflowName: string; creditsDeducted?: number }) => void
  onTaskSubmitted?: (data: { taskId: string; draftId: string }) => void
}) {
  const store = useGenerationStore()
  const isTyping = ref(false)
  const sseConnection = ref<EventSource | null>(null)
  const currentStreamingMessage = ref<AssistantMessage | null>(null)

  const sessionId = computed(() => store.activeSessionId || '')

  const closeSse = () => {
    if (sseConnection.value) {
      sseConnection.value.close()
      sseConnection.value = null
    }
  }

  const ensureAssistantPlaceholder = () => {
    const sid = store.activeSessionId
    if (!sid) return
    const msg: AssistantMessage = {
      id: Date.now().toString(),
      role: 'assistant',
      content: '',
      timestamp: Date.now()
    }
    store.appendMessage(sid, msg)
    currentStreamingMessage.value = msg
  }

  const patchAssistant = (patch: Partial<AssistantMessage>) => {
    const sid = store.activeSessionId
    if (!sid) return
    store.patchLastAssistant(sid, patch)
  }

  const setupSse = () => {
    const sid = store.activeSessionId
    if (!sid) return

    closeSse()
    const pinnedWorkflowIds = options.getPinnedWorkflowIds()
    sseConnection.value = generationApi.reqStream({
      sessionId: sid,
      enableWebSearch: options.enableWebSearch.value,
      pinnedWorkflowIds: pinnedWorkflowIds.length ? pinnedWorkflowIds : undefined
    })

    sseConnection.value.addEventListener('message', (event) => {
      try {
        const data = JSON.parse(event.data)
        if (!currentStreamingMessage.value) ensureAssistantPlaceholder()
        if (data.content) {
          patchAssistant({ content: data.content, citations: data.citations })
        }
      } catch { /* ignore */ }
    })

    sseConnection.value.addEventListener('task_draft', (event) => {
      try {
        const draft: TaskDraftApi.Draft = JSON.parse(event.data)
        store.upsertTaskDraft(sid, draft)
      } catch { /* ignore */ }
    })

    sseConnection.value.addEventListener('workflow_switched', (event) => {
      try {
        const data = JSON.parse(event.data)
        store.addPinnedWorkflow(sid, {
          workflowId: data.workflowId,
          name: data.workflowName
        })
        options.onWorkflowSwitched?.(data)
      } catch { /* ignore */ }
    })

    sseConnection.value.addEventListener('tool_status', (event) => {
      try {
        const data = JSON.parse(event.data)
        if (data.status === 'running') {
          if (!currentStreamingMessage.value) ensureAssistantPlaceholder()
          const toolLabels: Record<string, string> = {
            list_workflows: '正在搜索工作流…',
            get_workflow_schema: '正在读取工作流参数…',
            set_active_workflow: '正在添加工作流锚定…',
            create_task_draft: '正在生成任务草稿…',
            search_web: '正在联网搜索…'
          }
          patchAssistant({ toolStatus: toolLabels[data.tool] || `正在执行 ${data.tool}…` })
        }
      } catch { /* ignore */ }
    })

    sseConnection.value.addEventListener('done', (event) => {
      try {
        const data = JSON.parse(event.data)
        if (data.content) {
          if (!currentStreamingMessage.value) ensureAssistantPlaceholder()
          patchAssistant({ content: data.content, citations: data.citations, toolStatus: undefined })
        }
      } catch { /* ignore */ }
      isTyping.value = false
      currentStreamingMessage.value = null
      closeSse()
    })

    sseConnection.value.addEventListener('error', (event: MessageEvent) => {
      if (!event.data) return
      try {
        const data = JSON.parse(event.data)
        const errMsg = typeof data === 'string' ? data : (data.error || '未知错误')
        if (!currentStreamingMessage.value) ensureAssistantPlaceholder()
        patchAssistant({ content: `⚠️ ${errMsg}`, toolStatus: undefined })
      } catch {
        patchAssistant({ content: `⚠️ ${String(event.data)}`, toolStatus: undefined })
      }
      isTyping.value = false
      currentStreamingMessage.value = null
      closeSse()
    })

    sseConnection.value.onerror = () => {
      if (sseConnection.value?.readyState === EventSource.CLOSED) {
        isTyping.value = false
        currentStreamingMessage.value = null
      }
    }
  }

  const sendMessage = async (payload: {
    content: string
    imageUrls?: string[]
    attachments?: { url: string; filename?: string; mime?: string; kind?: string }[]
  }) => {
    let sid = store.activeSessionId
    if (!sid) {
      const session = store.createSession()
      sid = session.id
    }

    const pinnedWorkflowIds = options.getPinnedWorkflowIds()

    const userMsg: GenerationMessage = {
      id: Date.now().toString(),
      role: 'user',
      content: payload.content,
      timestamp: Date.now(),
      imageUrls: payload.imageUrls,
      attachments: payload.attachments
    }
    store.appendMessage(sid, userMsg)

    await generationApi.reqSubmitMessage({
      sessionId: sid,
      text: payload.content,
      imageUrls: payload.imageUrls,
      attachments: payload.attachments?.map(a => ({
        url: a.url,
        filename: a.filename,
        mime: a.mime,
        kind: a.kind as 'image' | 'video' | 'audio' | 'pdf' | undefined
      })),
      pinnedWorkflowIds: pinnedWorkflowIds.length ? pinnedWorkflowIds : undefined
    })

    isTyping.value = true
    currentStreamingMessage.value = null
    ensureAssistantPlaceholder()
    setupSse()
  }

  const stopReply = () => {
    closeSse()
    isTyping.value = false
    currentStreamingMessage.value = null
  }

  return {
    isTyping,
    sessionId,
    sendMessage,
    stopReply,
    closeSse
  }
}
