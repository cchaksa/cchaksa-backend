export type AdminRole = 'ADMIN' | 'CS_AGENT'

export interface AdminSession {
  adminAccountId: string
  displayName: string
  adminRole: AdminRole
}

export interface AdminChallenge {
  challengeId: string
  nonce: string
}

export interface KakaoIdTokenRequest {
  nonce: string
}

export type KakaoIdTokenProvider = (
  request: KakaoIdTokenRequest,
) => Promise<string>
