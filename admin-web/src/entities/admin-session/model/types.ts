export type AdminRole = 'ADMIN' | 'CS_AGENT'

export interface AdminSession {
  adminAccountId: string
  displayName: string
  role: AdminRole
}

export interface AdminSignInPreparation {
  nonce: string
  csrf: {
    name: string
    value: string
  }
}

export type AdminSignInPreparationProvider =
  () => Promise<AdminSignInPreparation>
