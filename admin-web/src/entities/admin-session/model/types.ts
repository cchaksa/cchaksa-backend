export type AdminRole = 'ADMIN' | 'CS_AGENT'

export interface AdminSession {
  adminAccountId: number
  displayName: string
  role: AdminRole
}
