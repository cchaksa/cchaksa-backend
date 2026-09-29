import { requestJson, requestVoid } from '../../../shared/api/http'
import { useMockApi } from '../../../shared/config/api'
import type {
  AdminChallenge,
  AdminSession,
  AdminSignInCallback,
  KakaoAuthorizationProvider,
} from '../model/types'

const PENDING_SIGN_IN_KEY = 'cchaksa.admin.pending-sign-in'

const mockSession: AdminSession = {
  adminAccountId: '2d577d85-53d9-45a2-8b4e-c06be5975710',
  displayName: '김척척',
  adminRole: 'CS_AGENT',
}

interface PendingSignIn {
  challengeId: string
  state: string
}

let kakaoAuthorizationProvider: KakaoAuthorizationProvider | null = null

export class AdminSignInCallbackError extends Error {
  constructor() {
    super('카카오 로그인 요청 정보를 확인할 수 없습니다.')
    this.name = 'AdminSignInCallbackError'
  }
}

export function configureKakaoAuthorizationProvider(
  provider: KakaoAuthorizationProvider,
) {
  kakaoAuthorizationProvider = provider
}

export function cancelAdminSignIn() {
  takePendingSignIn()
}

function savePendingSignIn(challenge: AdminChallenge) {
  const pendingSignIn: PendingSignIn = {
    challengeId: challenge.challengeId,
    state: challenge.state,
  }
  sessionStorage.setItem(PENDING_SIGN_IN_KEY, JSON.stringify(pendingSignIn))
}

function takePendingSignIn(): PendingSignIn | null {
  const value = sessionStorage.getItem(PENDING_SIGN_IN_KEY)
  sessionStorage.removeItem(PENDING_SIGN_IN_KEY)
  if (!value) return null

  try {
    const pendingSignIn = JSON.parse(value) as Partial<PendingSignIn>
    if (
      typeof pendingSignIn.challengeId !== 'string' ||
      typeof pendingSignIn.state !== 'string'
    ) {
      return null
    }
    return pendingSignIn as PendingSignIn
  } catch {
    return null
  }
}

async function signIn() {
  if (useMockApi) return mockSession
  if (!kakaoAuthorizationProvider) {
    throw new Error('카카오 인증 공급자가 연결되지 않았습니다.')
  }

  const challenge = await requestJson<AdminChallenge>(
    '/api/admin/auth/challenge',
    { cache: 'no-store' },
  )
  savePendingSignIn(challenge)

  try {
    await kakaoAuthorizationProvider({
      javascriptAppKey: challenge.javascriptAppKey,
      redirectUri: challenge.redirectUri,
      nonce: challenge.nonce,
      state: challenge.state,
    })
  } catch (error) {
    takePendingSignIn()
    throw error
  }

  return null
}

async function completeSignIn(callback: AdminSignInCallback) {
  const pendingSignIn = takePendingSignIn()
  if (!pendingSignIn || pendingSignIn.state !== callback.state) {
    throw new AdminSignInCallbackError()
  }

  return requestJson<AdminSession>('/api/admin/auth/signin', {
    method: 'POST',
    body: JSON.stringify({
      challengeId: pendingSignIn.challengeId,
      authorizationCode: callback.authorizationCode,
      state: callback.state,
    }),
  })
}

export const adminSessionApi = {
  signIn,
  completeSignIn,
  getSession: () =>
    useMockApi
      ? Promise.resolve(mockSession)
      : requestJson<AdminSession>('/api/admin/auth/me'),
  signOut: () =>
    useMockApi
      ? Promise.resolve()
      : requestVoid('/api/admin/auth/signout', { method: 'POST' }),
}
