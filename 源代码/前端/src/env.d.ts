/// <reference types="vite/client" />

declare module 'element-plus/dist/locale/zh-cn.mjs' {
  import type { Language } from 'element-plus/es/locale'
  const zhCn: Language
  export default zhCn
}

interface ImportMetaEnv {
  readonly VITE_DIFY_BASE_URL: string
  readonly VITE_CRYPTO_ENABLED: string
  readonly VITE_CRYPTO_SECRET_KEY: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
