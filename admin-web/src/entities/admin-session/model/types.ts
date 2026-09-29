export type AdminRole = 'ADMIN' | 'CS_AGENT'

export interface AdminSession {
  adminAccountId: string
  displayName: string
  adminRole: AdminRole
}

export interface AdminChallenge {
  challengeId: string
  nonce: string
  state: string
  javascriptAppKey: string
  redirectUri: string
}

export interface KakaoAuthorizationRequest {
  javascriptAppKey: string
  redirectUri: string
  nonce: string
  state: string
}

export interface AdminSignInCallback {
  authorizationCode: string
  state: string
}

export type KakaoAuthorizationProvider = (
  request: KakaoAuthorizationRequest,
) => Promise<void>
