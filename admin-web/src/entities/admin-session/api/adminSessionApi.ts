import { requestJson, requestVoid } from '../../../shared/api/http'
import { useMockApi } from '../../../shared/config/api'
import type {
  AdminSession,
  AdminSignInPreparationProvider,
} from '../model/types'

const mockSession: AdminSession = {
  adminAccountId: '2d577d85-53d9-45a2-8b4e-c06be5975710',
  displayName: '김척척',
  role: 'CS_AGENT',
}

let signInPreparationProvider: AdminSignInPreparationProvider | null = null

export function configureAdminSignInPreparation(
  provider: AdminSignInPreparationProvider,
) {
  signInPreparationProvider = provider
}

async function signIn() {
  if (useMockApi) return
  if (!signInPreparationProvider) {
    throw new Error('관리자 로그인 준비 계약이 연결되지 않았습니다.')
  }

  const preparation = await signInPreparationProvider()
  await requestVoid('/api/admin/auth/signin', {
    method: 'POST',
    body: JSON.stringify({ nonce: preparation.nonce }),
    csrf: preparation.csrf,
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
