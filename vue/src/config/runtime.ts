export type ClientKind = 'web' | 'desktop' | 'mobile'

const API_BASE_URL_OVERRIDE_KEY = 'apiBaseUrl'

function stripTrailingSlash(url: string): string {
  return url.replace(/\/+$/, '')
}

function detectClientKind(): ClientKind {
  const fromEnv = import.meta.env.VITE_CLIENT_KIND as ClientKind | undefined
  if (fromEnv === 'desktop' || fromEnv === 'mobile' || fromEnv === 'web') {
    return fromEnv
  }

  if (typeof window !== 'undefined') {
    const win = window as Window & {
      __TAURI__?: unknown
      __TAURI_INTERNALS__?: unknown
      Capacitor?: unknown
    }
    if (win.__TAURI__ || win.__TAURI_INTERNALS__) {
      return 'desktop'
    }
    if (win.Capacitor) {
      return 'mobile'
    }
  }

  return 'web'
}

function readApiBaseUrlFromEnv(): string {
  const fromEnv = import.meta.env.VITE_API_BASE_URL
  if (fromEnv) {
    return stripTrailingSlash(fromEnv)
  }
  return 'http://localhost:9000/api'
}

function readWsBaseUrlFromEnv(apiBaseUrl: string): string {
  const explicit = import.meta.env.VITE_API_WS_URL
  if (explicit) {
    return stripTrailingSlash(explicit)
  }
  return apiBaseUrl.replace(/^http:/i, 'ws:').replace(/^https:/i, 'wss:')
}

function joinUrl(base: string, path: string): string {
  const normalizedPath = path.startsWith('/') ? path : `/${path}`
  return `${stripTrailingSlash(base)}${normalizedPath}`
}

export function getClientKind(): ClientKind {
  return detectClientKind()
}

export function isMobileClient(): boolean {
  return getClientKind() === 'mobile'
}

export function isDesktopClient(): boolean {
  return getClientKind() === 'desktop'
}

export function getApiBaseUrl(): string {
  if (typeof localStorage !== 'undefined') {
    const override = localStorage.getItem(API_BASE_URL_OVERRIDE_KEY)?.trim()
    if (override) {
      return stripTrailingSlash(override)
    }
  }
  return readApiBaseUrlFromEnv()
}

export function setApiBaseUrlOverride(url: string | null): void {
  if (typeof localStorage === 'undefined') {
    return
  }
  if (!url?.trim()) {
    localStorage.removeItem(API_BASE_URL_OVERRIDE_KEY)
    return
  }
  localStorage.setItem(API_BASE_URL_OVERRIDE_KEY, stripTrailingSlash(url.trim()))
}

export function getWsBaseUrl(): string {
  return readWsBaseUrlFromEnv(getApiBaseUrl())
}

export function buildApiUrl(path: string): string {
  return joinUrl(getApiBaseUrl(), path)
}

export function buildWebSocketUrl(path: string): string {
  return joinUrl(getWsBaseUrl(), path)
}

export const runtime = {
  get clientKind() {
    return getClientKind()
  },
  get apiBaseUrl() {
    return getApiBaseUrl()
  },
  get wsBaseUrl() {
    return getWsBaseUrl()
  },
  buildApiUrl,
  buildWebSocketUrl,
  setApiBaseUrlOverride,
  isMobileClient,
  isDesktopClient
}
