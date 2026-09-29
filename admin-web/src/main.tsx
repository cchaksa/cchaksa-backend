import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { App } from './app/App'
import { AppProviders } from './app/providers/AppProviders'
import { configureKakaoAuthorizationProvider } from './entities/admin-session'
import {
  configureCsrfTokenProvider,
  createCookieCsrfTokenProvider,
} from './shared/api/http'
import { authorizeWithKakao } from './shared/lib/kakao/kakaoSdk'
import './shared/styles/global.css'

configureCsrfTokenProvider(
  createCookieCsrfTokenProvider('XSRF-TOKEN', 'X-XSRF-TOKEN'),
)
configureKakaoAuthorizationProvider(authorizeWithKakao)

const rootElement = document.getElementById('root')
if (!rootElement) throw new Error('관리자 앱 root 요소를 찾을 수 없습니다.')

createRoot(rootElement).render(
  <StrictMode>
    <AppProviders>
      <App />
    </AppProviders>
  </StrictMode>,
)
