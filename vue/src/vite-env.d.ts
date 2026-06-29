/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_CLIENT_KIND?: 'web' | 'desktop' | 'mobile'
  readonly VITE_API_BASE_URL: string
  readonly VITE_API_WS_URL: string
  readonly BASE_URL: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
