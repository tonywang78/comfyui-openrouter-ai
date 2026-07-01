export interface BasePage<T> {
  total: number
  items: T[]
}

export namespace MediaApi {
  export interface UserMediaVo {
    mediaId: number
    name: string
    mediaType: 'IMAGE' | 'VIDEO' | 'AUDIO'
    url: string
    mimeType: string
    fileSize: number
    width?: number
    height?: number
    durationMs?: number
    source: string
    tags?: string[]
    variants?: MediaVariantVo[]
    createTime: string
  }

  export interface MediaVariantVo {
    variantId: number
    mediaId: number
    variantType: string
    url?: string
    status: 'PENDING' | 'PROCESSING' | 'SUCCEEDED' | 'FAILED'
    processor: 'BUILTIN' | 'COMFYUI'
    workflowId?: number
    taskId?: string
    meta?: Record<string, unknown>
    createTime: string
  }

  export interface MediaPickerItemVo {
    mediaId: number
    name: string
    mediaType: string
    url: string
    variantId?: number
    variantType?: string
    variantLabel?: string
  }

  export type MediaPage = BasePage<UserMediaVo>
  export type PickerPage = BasePage<MediaPickerItemVo>
}
