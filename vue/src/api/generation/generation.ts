import { get, post } from '@/utils/requestUtil'
import type {
  GenerationSubmitApi,
  GenerationConfirmApi,
  GenerationStreamApi,
  GenerationWorkflowApi
} from './types'

export const generationApi = {
  reqSubmitMessage: (params: GenerationSubmitApi.Params) => {
    return post<void>('/llm/generation/submit', params)
  },

  reqConfirmDraft: (params: GenerationConfirmApi.Params) => {
    return post<GenerationConfirmApi.Result>('/llm/generation/confirm', params)
  },

  reqDeleteSession: (params: { sessionId: string }) => {
    return post<void>('/llm/generation/session/delete', params)
  },

  reqListWorkflows: (params?: { keyword?: string; categoryId?: number; limit?: number }) => {
    const q = new URLSearchParams()
    if (params?.keyword) q.set('keyword', params.keyword)
    if (params?.categoryId != null) q.set('categoryId', String(params.categoryId))
    if (params?.limit != null) q.set('limit', String(params.limit))
    const qs = q.toString()
    return get<GenerationWorkflowApi.Item[]>(`/llm/generation/workflows${qs ? `?${qs}` : ''}`)
  },

  reqStream: (params: GenerationStreamApi.Params) => {
    const q = new URLSearchParams()
    q.set('sessionId', params.sessionId)
    if (params.enableWebSearch !== undefined) {
      q.set('enableWebSearch', String(params.enableWebSearch))
    }
    if (params.pinnedWorkflowIds?.length) {
      q.set('pinnedWorkflowIds', params.pinnedWorkflowIds.join(','))
    } else if (params.pinnedWorkflowId != null) {
      q.set('pinnedWorkflowId', String(params.pinnedWorkflowId))
    }
    const token = localStorage.getItem('token')
    if (token) q.set('token', token)
    const url = `${import.meta.env.VITE_API_BASE_URL}/llm/generation/stream?${q.toString()}`
    return new EventSource(url)
  }
}
