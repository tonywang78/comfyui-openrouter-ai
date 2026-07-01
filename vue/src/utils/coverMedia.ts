import { buildApiUrl } from '@/config/runtime'

export type CoverMediaType = 'image' | 'gif' | 'video'

const VIDEO_EXTENSIONS = new Set(['mp4', 'avi', 'mov', 'webm'])
const GIF_EXTENSIONS = new Set(['gif'])
const OWN_OSS_PREFIXES = ['TEMP/', 'COMFYUI/', 'USER/']

/**
 * 根据 URL 扩展名判断封面媒体类型（忽略 query/hash）
 */
export function getCoverMediaType(url?: string | null): CoverMediaType {
  if (!url) return 'image'

  const path = url.split('?')[0].split('#')[0]
  const dotIndex = path.lastIndexOf('.')
  if (dotIndex === -1) return 'image'

  const ext = path.slice(dotIndex + 1).toLowerCase()
  if (VIDEO_EXTENSIONS.has(ext)) return 'video'
  if (GIF_EXTENSIONS.has(ext)) return 'gif'
  return 'image'
}

export function isCoverVideo(url?: string | null): boolean {
  return getCoverMediaType(url) === 'video'
}

export function extractOssObjectKey(urlOrKey?: string | null): string {
  if (!urlOrKey) return ''
  if (!urlOrKey.startsWith('http://') && !urlOrKey.startsWith('https://')) {
    return urlOrKey
  }
  try {
    const pathname = new URL(urlOrKey).pathname
    return pathname.startsWith('/') ? pathname.slice(1) : pathname
  } catch {
    return urlOrKey
  }
}

export function isOwnOssMediaUrl(url?: string | null): boolean {
  if (!url) return false
  if (url.startsWith('http://') || url.startsWith('https://')) {
    const key = extractOssObjectKey(url)
    return OWN_OSS_PREFIXES.some(prefix => key.startsWith(prefix))
  }
  return OWN_OSS_PREFIXES.some(prefix => url.startsWith(prefix))
}

/**
 * 构建同源媒体代理 URL（视频封面走此后端接口，避免 OSS CORS）
 */
export function buildOssMediaProxyUrl(urlOrKey: string): string {
  const key = extractOssObjectKey(urlOrKey)
  return buildApiUrl(`/oss/media?key=${encodeURIComponent(key)}`)
}

export async function fetchOssVideoBlobUrl(urlOrKey: string): Promise<string> {
  const token = typeof localStorage !== 'undefined' ? localStorage.getItem('token') : null
  const response = await fetch(buildOssMediaProxyUrl(urlOrKey), {
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  })
  if (!response.ok) {
    throw new Error(`媒体加载失败: ${response.status}`)
  }
  const blob = await response.blob()
  return URL.createObjectURL(blob)
}
