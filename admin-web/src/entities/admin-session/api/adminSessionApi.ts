import {
  ApiError,
  hasCsrfToken,
  requestJson,
  requestVoid,
} from '../../../shared/api/http'
import { useMockApi } from '../../../shared/config/api'
import type {
  AdminPasswordChangeInput,
  AdminSession,
  AdminSignInInput,
} from '../model/types'

const mockSession: AdminSession = {
  adminAccountId: '2d577d85-53d9-45a2-8b4e-c06be5975710',
  displayName: '김척척',
  adminRole: 'CS_AGENT',
}

let mockSignedIn = false
let csrfBootstrapPromise: Promise<void> | null = null

async function bootstrapCsrf() {
  if (useMockApi) return
  if (!csrfBootstrapPromise) {
    csrfBootstrapPromise = requestVoid('/api/admin/auth/csrf', {
      cache: 'no-store',
      notifySessionExpired: false,
      allowDuringSessionTransition: true,
    })
      .then(() => {
        if (!hasCsrfToken()) {
          throw new Error('CSRF 쿠키를 확인할 수 없습니다.')
        }
      })
      .finally(() => {
        csrfBootstrapPromise = null
      })
  }
  return csrfBootstrapPromise
}

async function signIn(input: AdminSignInInput) {
  if (useMockApi) {
    mockSignedIn = true
    return mockSession
  }
  return requestJson<AdminSession>('/api/admin/auth/signin', {
    method: 'POST',
    body: JSON.stringify(input),
    notifySessionExpired: false,
    allowDuringSessionTransition: true,
  })
}

async function getSession() {
  if (useMockApi) {
    if (!mockSignedIn) {
      throw new ApiError('관리자 로그인이 필요합니다.', 401, 'A05')
    }
    return mockSession
  }
  return requestJson<AdminSession>('/api/admin/auth/me', {
    notifySessionExpired: false,
  })
}

async function verifySession() {
  if (useMockApi) return getSession()
  return requestJson<AdminSession>('/api/admin/auth/me', {
    allowDuringSessionTransition: true,
    sessionTransitionRequest: true,
  })
}

async function signOut() {
  if (useMockApi) {
    mockSignedIn = false
    return
  }
  await requestVoid('/api/admin/auth/signout', {
    method: 'POST',
    allowDuringSessionTransition: true,
    sessionTransitionRequest: true,
  })
}

async function changePassword(input: AdminPasswordChangeInput) {
  if (useMockApi) {
    if (input.currentPassword === input.newPassword) {
      throw new ApiError('기존 비밀번호와 다른 값을 입력해 주세요.', 400, 'A15')
    }
    return
  }
  await requestVoid('/api/admin/auth/password', {
    method: 'POST',
    body: JSON.stringify(input),
    allowDuringSessionTransition: true,
    sessionTransitionRequest: true,
  })
}

export const adminSessionApi = {
  bootstrapCsrf,
  signIn,
  getSession,
  verifySession,
  signOut,
  changePassword,
}
