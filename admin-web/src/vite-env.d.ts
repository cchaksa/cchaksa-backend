/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_ADMIN_SIGN_IN_PATH?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
