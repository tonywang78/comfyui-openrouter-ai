import type { WorkflowTaskStatusEnum } from '@/enums/workflow'
import type { TaskDraftApi, WorkflowResultModel } from '@/api/generation/types'

export interface PinnedWorkflow {
  workflowId: number
  name: string
  creditsDeducted?: number
}

export type GenerationMessage =
  | UserMessage
  | AssistantMessage
  | SystemMessage
  | TaskDraftMessage
  | TaskStatusMessage

export interface UserMessage {
  id: string
  role: 'user'
  content: string
  timestamp: number
  imageUrls?: string[]
  attachments?: { url: string; filename?: string; mime?: string; kind?: string }[]
}

export interface AssistantMessage {
  id: string
  role: 'assistant'
  content: string
  timestamp: number
  toolStatus?: string
  citations?: { title?: string; url: string }[]
}

export interface SystemMessage {
  id: string
  role: 'system'
  content: string
  timestamp: number
}

export interface TaskDraftMessage {
  id: string
  role: 'task_draft'
  timestamp: number
  draft: TaskDraftApi.Draft
}

export interface TaskStatusMessage {
  id: string
  role: 'task_status'
  timestamp: number
  taskId: string
  workflowName: string
  status: WorkflowTaskStatusEnum
  progress: number
  location?: number
  workflowResultModel?: WorkflowResultModel
}

export interface TaskUpdate {
  taskId: string
  workflowName: string
  status: WorkflowTaskStatusEnum
  progress?: number
  location?: number
  workflowResultModel?: WorkflowResultModel
}
