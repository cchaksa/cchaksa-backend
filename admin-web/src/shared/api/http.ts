export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

export async function requestJson<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const headers = new Headers(options.headers)
  headers.set('Accept', 'application/json')
  if (options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json')
  }

  const response = await fetch(path, {
    ...options,
    credentials: 'include',
    headers,
  })

  if (!response.ok) {
    throw new ApiError('관리자 API 요청을 처리하지 못했습니다.', response.status)
  }

  if (response.status === 204) return undefined as T
  return response.json() as Promise<T>
}
