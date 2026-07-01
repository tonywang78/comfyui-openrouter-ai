import type { MediaApi } from './types'
import { get, post, put } from '@/utils/requestUtil'

export const mediaApi = {
  upload: (file: File, name?: string) => {
    const formData = new FormData()
    formData.append('file', file)
    if (name) formData.append('name', name)
    return post<MediaApi.UserMediaVo>('/media/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  getPage: (params: { page?: number; mediaType?: string; keyword?: string } = {}) =>
    get<MediaApi.MediaPage>('/media/page', params),

  getDetail: (mediaId: number) =>
    get<MediaApi.UserMediaVo>('/media/detail', { mediaId }),

  update: (data: { mediaId: number; name?: string; tags?: string[] }) =>
    put<void>('/media/update', data),

  delete: (mediaId: number) =>
    post<void>('/media/delete', { mediaId }),

  importFromWork: (workflowResultId: number, name?: string) =>
    post<MediaApi.UserMediaVo>('/media/import-from-work', { workflowResultId, name }),

  createBuiltinVariant: (mediaId: number) =>
    post<MediaApi.MediaVariantVo>('/media/variant/builtin', { mediaId }),

  createComfyuiVariant: (data: { mediaId: number; variantId?: number; workflowId?: number }) =>
    post<MediaApi.MediaVariantVo>('/media/variant/comfyui', data),

  getVariantStatus: (variantId: number) =>
    get<MediaApi.MediaVariantVo>('/media/variant/status', { variantId }),

  getPicker: (params: { page?: number; mediaType?: string } = {}) =>
    get<MediaApi.PickerPage>('/media/picker', params),

  createTask: (data: {
    mediaId: number
    variantId?: number
    workflowId: number
    extraNodes?: unknown[]
  }) => post<string>('/media/create-task', data)
}
