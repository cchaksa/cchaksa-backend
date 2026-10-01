export interface CsrfHeader {
  name: string
  value: string
}

export interface SuccessResponse<T> {
  success: true
  data: T
  message?: string
}

export type CsrfTokenProvider = () => CsrfHeader | null

export interface AdminRequestOptions extends RequestInit {
  csrf?: CsrfHeader
  allowDuringSessionTransition?: boolean
  sessionTransitionRequest?: boolean
  notifySessionExpired?: boolean
}

interface ErrorResponseBody {
  success?: unknown
  error?: {
    code?: unknown
  }
}

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly code: string | null = null,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

export class AdminSessionTransitionError extends Error {
  constructor() {
    super('관리자 세션을 갱신하고 있습니다.')
    this.name = 'AdminSessionTransitionError'
  }
}

export class CsrfTokenUnavailableError extends Error {
  constructor() {
    super('상태 변경 요청에 필요한 CSRF 토큰이 없습니다.')
    this.name = 'CsrfTokenUnavailableError'
  }
}

let csrfTokenProvider: CsrfTokenProvider = () => null
let sessionExpiredHandler: (() => void) | null = null
let sessionTransitionInProgress = false
let sessionGeneration = 0

export function configureCsrfTokenProvider(provider: CsrfTokenProvider) {
  csrfTokenProvider = provider
}

export function hasCsrfToken() {
  return csrfTokenProvider() !== null
}

export function configureSessionExpiredHandler(handler: (() => void) | null) {
  sessionExpiredHandler = handler
}

export function beginAdminSessionTransition() {
  if (sessionTransitionInProgress) throw new AdminSessionTransitionError()
  sessionTransitionInProgress = true

  return {
    commit: () => {
      sessionGeneration += 1
      sessionTransitionInProgress = false
    },
    cancel: () => {
      sessionTransitionInProgress = false
    },
  }
}

export function createCookieCsrfTokenProvider(
  cookieName: string,
  headerName: string,
): CsrfTokenProvider {
  return () => {
    const prefix = `${encodeURIComponent(cookieName)}=`
    const cookie = document.cookie
      .split(';')
      .map((item) => item.trim())
      .find((item) => item.startsWith(prefix))
    if (!cookie) return null

    return {
      name: headerName,
      value: decodeURIComponent(cookie.slice(prefix.length)),
    }
  }
}

function requiresCsrf(method: string | undefined) {
  return ['POST', 'PUT', 'PATCH', 'DELETE'].includes(
    (method ?? 'GET').toUpperCase(),
  )
}

async function request(path: string, options: AdminRequestOptions = {}) {
  const {
    csrf,
    allowDuringSessionTransition = false,
    sessionTransitionRequest = false,
    notifySessionExpired = true,
    ...requestOptions
  } = options
  const requestGeneration = sessionGeneration

  if (sessionTransitionInProgress && !allowDuringSessionTransition) {
    throw new AdminSessionTransitionError()
  }
  const headers = new Headers(requestOptions.headers)
  headers.set('Accept', 'application/json')
  if (requestOptions.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  if (requiresCsrf(requestOptions.method)) {
    const csrfHeader = csrf ?? csrfTokenProvider()
    if (!csrfHeader) throw new CsrfTokenUnavailableError()
    headers.set(csrfHeader.name, csrfHeader.value)
  }

  const response = await fetch(path, {
    ...requestOptions,
    credentials: 'include',
    headers,
  })

  if (!response.ok) {
    let code: string | null = null
    try {
      const body = (await response.json()) as ErrorResponseBody
      if (
        body.success === false &&
        typeof body.error?.code === 'string' &&
        /^[A-Z0-9_-]{1,32}$/.test(body.error.code)
      ) {
        code = body.error.code
      }
    } catch {
      // Non-JSON failures still use the HTTP status and a generic message.
    }

    if (
      code === 'A05' &&
      notifySessionExpired &&
      (sessionTransitionRequest ||
        (!sessionTransitionInProgress && requestGeneration === sessionGeneration))
    ) {
      sessionExpiredHandler?.()
    }

    throw new ApiError(
      '관리자 API 요청을 처리하지 못했습니다.',
      response.status,
      code,
    )
  }

  return response
}

export async function requestJson<T>(
  path: string,
  options: AdminRequestOptions = {},
): Promise<T> {
  const response = await request(path, options)
  const body = (await response.json()) as SuccessResponse<T>
  return body.data
}

export async function requestVoid(
  path: string,
  options: AdminRequestOptions = {},
) {
  await request(path, options)
}
