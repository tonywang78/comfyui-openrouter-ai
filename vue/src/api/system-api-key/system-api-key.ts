import type { SystemApiKeyApi } from './types'
import { get, post } from '@/utils/requestUtil'

export const systemApiKeyApi = {
  fetchKeys: (params: SystemApiKeyApi.FetchKeysParams) => {
    return get<SystemApiKeyApi.PageVo<SystemApiKeyApi.SystemApiKeyVo>>('/system/api-key/page', params)
  },

  createKey: (body: SystemApiKeyApi.CreateApiKeyDto) => {
    return post<SystemApiKeyApi.CreateApiKeyResultVo>('/system/api-key/create', body)
  },

  updateKey: (body: SystemApiKeyApi.UpdateApiKeyDto) => {
    return post<void>('/system/api-key/update', body)
  },

  deleteKey: (id: number) => {
    return post<void>('/system/api-key/delete', { id } as SystemApiKeyApi.DeleteApiKeyDto)
  },

  rotateKey: (id: number) => {
    return post<SystemApiKeyApi.CreateApiKeyResultVo>('/system/api-key/rotate', { id } as SystemApiKeyApi.RotateApiKeyDto)
  }
}
