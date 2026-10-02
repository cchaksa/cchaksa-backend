import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', '')
  const apiProxyTarget = env.ADMIN_API_PROXY_TARGET

  return {
    plugins: [react()],
    server: apiProxyTarget
      ? {
          proxy: {
            '/api/admin': {
              target: apiProxyTarget,
              changeOrigin: true,
            },
          },
        }
      : undefined,
  }
})
