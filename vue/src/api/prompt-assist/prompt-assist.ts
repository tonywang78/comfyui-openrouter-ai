import { post } from '@/utils/requestUtil'
import type { PromptStyleEnum } from '@/enums/promptStyle'

export namespace PromptEnhanceApi {
  export interface Params {
    workflowId: number
    fieldKey: string
    draftText?: string
    imageUrls?: string[]
    promptStyle: PromptStyleEnum | string
  }

  export interface Result {
    prompt: string
    explanation?: string
  }
}

export const promptAssistApi = {
  enhance: (params: PromptEnhanceApi.Params) => {
    return post<PromptEnhanceApi.Result>('/llm/prompt/enhance', params, { timeout: 30000 })
  }
}
