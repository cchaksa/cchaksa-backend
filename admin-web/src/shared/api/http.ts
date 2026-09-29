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
}

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

export class CsrfTokenUnavailableError extends Error {
  constructor() {
    super('상태 변경 요청에 필요한 CSRF 토큰이 없습니다.')
    this.name = 'CsrfTokenUnavailableError'
  }
}

let csrfTokenProvider: CsrfTokenProvider = () => null

export function configureCsrfTokenProvider(provider: CsrfTokenProvider) {
  csrfTokenProvider = provider
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
  const { csrf, ...requestOptions } = options
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
    throw new ApiError('관리자 API 요청을 처리하지 못했습니다.', response.status)
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
