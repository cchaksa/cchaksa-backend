import { requestJson } from '../../../shared/api/http'
import { useMockApi } from '../../../shared/config/api'
import type { AdminSession } from '../model/types'

const mockSession: AdminSession = {
  adminAccountId: 7,
  displayName: '김척척',
  role: 'CS_AGENT',
}

export const adminSessionApi = {
  getSession: () =>
    useMockApi
      ? Promise.resolve(mockSession)
      : requestJson<AdminSession>('/api/admin/auth/me'),
  signOut: () =>
    useMockApi
      ? Promise.resolve()
      : requestJson<void>('/api/admin/auth/signout', { method: 'POST' }),
}
