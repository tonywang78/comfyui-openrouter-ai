export namespace SystemApiKeyApi {
  export interface SystemApiKeyVo {
    id: number
    userId: number
    userNickname?: string | null
    name: string
    keyPrefix: string
    status: 0 | 1
    expiresAt?: string | null
    lastUsedAt?: string | null
    createTime?: string | null
  }

  export interface CreateApiKeyDto {
    userId: number
    name: string
    expiresAt?: string | null
  }

  export interface UpdateApiKeyDto {
    id: number
    name?: string
    status?: 0 | 1
    expiresAt?: string | null
  }

  export interface DeleteApiKeyDto {
    id: number
  }

  export interface RotateApiKeyDto {
    id: number
  }

  export interface CreateApiKeyResultVo {
    id: number
    plainKey: string
  }

  export interface FetchKeysParams {
    page?: number
    size?: number
    keyword?: string
    status?: 0 | 1
    userId?: number
  }

  export interface PageVo<T> {
    total: number
    items: T[]
  }
}
