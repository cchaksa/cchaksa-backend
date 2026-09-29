import { requestJson, requestVoid } from '../../../shared/api/http'
import { useMockApi } from '../../../shared/config/api'
import type {
  AdminChallenge,
  AdminSession,
  KakaoIdTokenProvider,
} from '../model/types'

const mockSession: AdminSession = {
  adminAccountId: '2d577d85-53d9-45a2-8b4e-c06be5975710',
  displayName: '김척척',
  adminRole: 'CS_AGENT',
}

let kakaoIdTokenProvider: KakaoIdTokenProvider | null = null

export function configureKakaoIdTokenProvider(provider: KakaoIdTokenProvider) {
  kakaoIdTokenProvider = provider
}

async function signIn() {
  if (useMockApi) return mockSession
  if (!kakaoIdTokenProvider) {
    throw new Error('카카오 ID token 공급자가 연결되지 않았습니다.')
  }

  const challenge = await requestJson<AdminChallenge>(
    '/api/admin/auth/challenge',
    { cache: 'no-store' },
  )
  const idToken = await kakaoIdTokenProvider({ nonce: challenge.nonce })
  return requestJson<AdminSession>('/api/admin/auth/signin', {
    method: 'POST',
    body: JSON.stringify({
      challengeId: challenge.challengeId,
      idToken,
    }),
  })
}

export const adminSessionApi = {
  signIn,
  getSession: () =>
    useMockApi
      ? Promise.resolve(mockSession)
      : requestJson<AdminSession>('/api/admin/auth/me'),
  signOut: () =>
    useMockApi
      ? Promise.resolve()
      : requestVoid('/api/admin/auth/signout', { method: 'POST' }),
}
