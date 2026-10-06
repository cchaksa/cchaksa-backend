export type AdminRole = 'ADMIN' | 'CS_AGENT'

export interface AdminSession {
  adminAccountId: string
  displayName: string
  adminRole: AdminRole
}

export interface AdminSignInInput {
  loginId: string
  password: string
}

export interface AdminPasswordChangeInput {
  currentPassword: string
  newPassword: string
}
