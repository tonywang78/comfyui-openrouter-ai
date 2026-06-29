import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { GenerationMessage, PinnedWorkflow } from '@/views/ai/generate/types'

export interface GenerationSession {
  id: string
  title: string
  createdAt: number
  pinnedWorkflows: PinnedWorkflow[]
  taskIds: string[]
  messageCount: number
}

const SESSIONS_KEY = 'generation:sessions'
const ACTIVE_KEY = 'generation:active'
const MESSAGES_KEY_PREFIX = 'generation:messages:'

function generateUniqueTimestampId(existingIds: Set<string>): string {
  let id = Date.now().toString()
  while (existingIds.has(id)) {
    id = (Number(id) + 1).toString()
  }
  return id
}

function summarizeTitle(content: string): string {
  const clean = (content || '').replace(/\s+/g, ' ').trim()
  if (!clean) return '新会话'
  return clean.slice(0, 20)
}

function migrateSession(raw: Record<string, unknown>): GenerationSession {
  const pinnedWorkflows = Array.isArray(raw.pinnedWorkflows)
    ? (raw.pinnedWorkflows as PinnedWorkflow[])
    : raw.pinnedWorkflowId != null
      ? [{
          workflowId: raw.pinnedWorkflowId as number,
          name: (raw.pinnedWorkflowName as string) || ''
        }]
      : []

  return {
    id: raw.id as string,
    title: (raw.title as string) || '新会话',
    createdAt: (raw.createdAt as number) || Date.now(),
    pinnedWorkflows,
    taskIds: Array.isArray(raw.taskIds) ? (raw.taskIds as string[]) : [],
    messageCount: (raw.messageCount as number) || 0
  }
}

export const useGenerationStore = defineStore('generation', () => {
  const sessions = ref<GenerationSession[]>([])
  const activeSessionId = ref<string | null>(null)
  const messagesBySession = ref<Record<string, GenerationMessage[]>>({})

  const activeSession = computed(() =>
    sessions.value.find(s => s.id === activeSessionId.value) || null
  )
  const activeMessages = computed(() =>
    activeSessionId.value ? (messagesBySession.value[activeSessionId.value] || []) : []
  )

  const persistSessions = () => {
    localStorage.setItem(SESSIONS_KEY, JSON.stringify(sessions.value))
    if (activeSessionId.value) {
      localStorage.setItem(ACTIVE_KEY, activeSessionId.value)
    } else {
      localStorage.removeItem(ACTIVE_KEY)
    }
  }

  const persistMessages = (sessionId: string) => {
    const key = MESSAGES_KEY_PREFIX + sessionId
    localStorage.setItem(key, JSON.stringify(messagesBySession.value[sessionId] || []))
  }

  const loadMessages = (sessionId: string) => {
    if (messagesBySession.value[sessionId]) return
    const key = MESSAGES_KEY_PREFIX + sessionId
    try {
      const raw = localStorage.getItem(key)
      messagesBySession.value[sessionId] = raw ? JSON.parse(raw) : []
    } catch {
      messagesBySession.value[sessionId] = []
    }
  }

  const rehydrate = () => {
    try {
      const raw = JSON.parse(localStorage.getItem(SESSIONS_KEY) || '[]') as Record<string, unknown>[]
      sessions.value = raw.map(migrateSession)
    } catch {
      sessions.value = []
    }
    try {
      activeSessionId.value = localStorage.getItem(ACTIVE_KEY)
    } catch {
      activeSessionId.value = null
    }
    if (activeSessionId.value) {
      loadMessages(activeSessionId.value)
    }
  }

  const createSession = (pinnedWorkflows?: PinnedWorkflow[]) => {
    const existingIds = new Set(sessions.value.map(s => s.id))
    const id = generateUniqueTimestampId(existingIds)
    const session: GenerationSession = {
      id,
      title: '新会话',
      createdAt: Date.now(),
      pinnedWorkflows: pinnedWorkflows ? [...pinnedWorkflows] : [],
      taskIds: [],
      messageCount: 0
    }
    sessions.value.unshift(session)
    messagesBySession.value[id] = []
    activeSessionId.value = id
    persistSessions()
    persistMessages(id)
    return session
  }

  const setActiveSession = (sessionId: string) => {
    activeSessionId.value = sessionId
    loadMessages(sessionId)
    persistSessions()
  }

  const appendMessage = (sessionId: string, message: GenerationMessage) => {
    loadMessages(sessionId)
    if (!messagesBySession.value[sessionId]) {
      messagesBySession.value[sessionId] = []
    }
    messagesBySession.value[sessionId].push(message)
    const session = sessions.value.find(s => s.id === sessionId)
    if (session) {
      session.messageCount = messagesBySession.value[sessionId].length
      if (message.role === 'user' && session.title === '新会话') {
        session.title = summarizeTitle(message.content)
      }
    }
    persistMessages(sessionId)
    persistSessions()
  }

  const patchLastAssistant = (sessionId: string, patch: Partial<GenerationMessage>) => {
    const list = messagesBySession.value[sessionId]
    if (!list?.length) return
    for (let i = list.length - 1; i >= 0; i--) {
      if (list[i].role === 'assistant') {
        list[i] = { ...list[i], ...patch } as GenerationMessage
        persistMessages(sessionId)
        return
      }
    }
  }

  const upsertTaskDraft = (sessionId: string, draft: import('@/api/generation/types').TaskDraftApi.Draft) => {
    const list = messagesBySession.value[sessionId] || []
    const idx = list.findIndex(m => m.role === 'task_draft' && m.draft.draftId === draft.draftId)
    const msg: GenerationMessage = {
      id: `draft-${draft.draftId}`,
      role: 'task_draft',
      timestamp: Date.now(),
      draft
    }
    if (idx >= 0) {
      list[idx] = msg
    } else {
      list.push(msg)
    }
    messagesBySession.value[sessionId] = list
    persistMessages(sessionId)
  }

  const updateDraftStatus = (sessionId: string, draftId: string, status: 'pending' | 'confirmed' | 'expired') => {
    const list = messagesBySession.value[sessionId]
    if (!list) return
    const msg = list.find(m => m.role === 'task_draft' && m.draft.draftId === draftId)
    if (msg && msg.role === 'task_draft') {
      msg.draft.status = status
      persistMessages(sessionId)
    }
  }

  const upsertTaskStatus = (sessionId: string, task: import('@/views/ai/generate/types').TaskUpdate) => {
    const list = messagesBySession.value[sessionId] || []
    const idx = list.findIndex(m => m.role === 'task_status' && m.taskId === task.taskId)
    const msg: GenerationMessage = {
      id: `task-${task.taskId}`,
      role: 'task_status',
      timestamp: Date.now(),
      taskId: task.taskId,
      workflowName: task.workflowName,
      status: task.status,
      progress: task.progress ?? 0,
      location: task.location,
      workflowResultModel: task.workflowResultModel
    }
    if (idx >= 0) {
      list[idx] = { ...list[idx], ...msg }
    } else {
      list.push(msg)
    }
    messagesBySession.value[sessionId] = list
    persistMessages(sessionId)
  }

  const addTaskId = (sessionId: string, taskId: string) => {
    const session = sessions.value.find(s => s.id === sessionId)
    if (session && !session.taskIds.includes(taskId)) {
      session.taskIds.push(taskId)
      persistSessions()
    }
  }

  const setPinnedWorkflows = (sessionId: string, workflows: PinnedWorkflow[]) => {
    const session = sessions.value.find(s => s.id === sessionId)
    if (session) {
      session.pinnedWorkflows = [...workflows]
      persistSessions()
    }
  }

  const addPinnedWorkflow = (sessionId: string, workflow: PinnedWorkflow) => {
    const session = sessions.value.find(s => s.id === sessionId)
    if (!session) return
    if (!session.pinnedWorkflows.some(w => w.workflowId === workflow.workflowId)) {
      session.pinnedWorkflows.push(workflow)
      persistSessions()
    }
  }

  const removePinnedWorkflow = (sessionId: string, workflowId: number) => {
    const session = sessions.value.find(s => s.id === sessionId)
    if (!session) return
    session.pinnedWorkflows = session.pinnedWorkflows.filter(w => w.workflowId !== workflowId)
    persistSessions()
  }

  const clearMessages = (sessionId: string) => {
    messagesBySession.value[sessionId] = []
    const session = sessions.value.find(s => s.id === sessionId)
    if (session) {
      session.messageCount = 0
      session.taskIds = []
    }
    persistMessages(sessionId)
    persistSessions()
  }

  const deleteSession = (sessionId: string) => {
    sessions.value = sessions.value.filter(s => s.id !== sessionId)
    delete messagesBySession.value[sessionId]
    localStorage.removeItem(MESSAGES_KEY_PREFIX + sessionId)
    if (activeSessionId.value === sessionId) {
      activeSessionId.value = sessions.value[0]?.id ?? null
    }
    persistSessions()
  }

  return {
    sessions,
    activeSessionId,
    activeSession,
    activeMessages,
    rehydrate,
    createSession,
    setActiveSession,
    appendMessage,
    patchLastAssistant,
    upsertTaskDraft,
    updateDraftStatus,
    upsertTaskStatus,
    addTaskId,
    setPinnedWorkflows,
    addPinnedWorkflow,
    removePinnedWorkflow,
    clearMessages,
    deleteSession,
    loadMessages
  }
})
