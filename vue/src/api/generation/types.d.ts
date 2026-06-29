import { WorkflowTaskStatusEnum, WorkflowResultModelTypeEnum } from '@/enums/workflow'

export namespace GenerationSubmitApi {
  export interface AttachmentDto {
    url: string
    filename?: string
    mime?: string
    kind?: 'image' | 'video' | 'audio' | 'pdf'
  }

  export interface Params {
    sessionId: string
    text?: string
    imageUrls?: string[]
    attachments?: AttachmentDto[]
    pinnedWorkflowId?: number
    pinnedWorkflowIds?: number[]
  }
}

export namespace GenerationConfirmApi {
  export interface Params {
    sessionId: string
    draftId: string
  }

  export interface Result {
    taskId: string
    draftId: string
  }
}

export namespace GenerationStreamApi {
  export interface Params {
    sessionId: string
    enableWebSearch?: boolean
    pinnedWorkflowId?: number
    pinnedWorkflowIds?: number[]
  }
}

export namespace TaskDraftApi {
  export interface NodeContainer {
    nodeKey: string
    inputs: string
    nodeValue: string
    isUpload?: boolean
    tips?: string
    type?: string
  }

  export interface Draft {
    draftId: string
    sessionId: string
    workflowId: number
    workflowName: string
    summary: string
    creditsDeducted: number
    status: 'pending' | 'confirmed' | 'expired'
    nodeContainer: NodeContainer[]
  }
}

export namespace GenerationWorkflowApi {
  export interface Item {
    workflowId: number
    name: string
    description?: string
    categoryName?: string
    creditsDeducted?: number
  }
}

export interface WorkflowResultModel {
  url: string
  type: WorkflowResultModelTypeEnum
  workflowResultId: number
}

export interface TaskStatusPayload {
  taskId: string
  workflowName: string
  status: WorkflowTaskStatusEnum
  progress: number
  location?: number
  workflowResultModel?: WorkflowResultModel
}
