import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  ApiError,
  beginAdminSessionTransition,
  configureCsrfTokenProvider,
  configureSessionExpiredHandler,
  createCookieCsrfTokenProvider,
  CsrfTokenUnavailableError,
  requestJson,
  requestVoid,
} from './http'

afterEach(() => {
  configureCsrfTokenProvider(() => null)
  configureSessionExpiredHandler(null)
  vi.unstubAllGlobals()
})

describe('admin HTTP CSRF handling', () => {
  it('reads the current CSRF token for every state-changing request', async () => {
    let token = 'first-token'
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }))
    vi.stubGlobal('fetch', fetchMock)
    configureCsrfTokenProvider(() => ({
      name: 'X-CSRF-TOKEN',
      value: token,
    }))

    await requestVoid('/api/admin/reports/id/answer', { method: 'POST' })
    token = 'rotated-token'
    await requestVoid('/api/admin/auth/signout', { method: 'POST' })

    const firstHeaders = new Headers(fetchMock.mock.calls[0][1]?.headers)
    const secondHeaders = new Headers(fetchMock.mock.calls[1][1]?.headers)
    expect(firstHeaders.get('X-CSRF-TOKEN')).toBe('first-token')
    expect(secondHeaders.get('X-CSRF-TOKEN')).toBe('rotated-token')
  })

  it('reads XSRF-TOKEN with the server header name', () => {
    vi.stubGlobal('document', {
      cookie: 'other=value; XSRF-TOKEN=encoded%20token',
    })

    expect(
      createCookieCsrfTokenProvider('XSRF-TOKEN', 'X-XSRF-TOKEN')(),
    ).toEqual({
      name: 'X-XSRF-TOKEN',
      value: 'encoded token',
    })
  })

  it('unwraps the common success response data', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        Response.json({
          success: true,
          data: { value: 'contract-data' },
          message: '요청 성공',
        }),
      ),
    )

    await expect(
      requestJson<{ value: string }>('/api/admin/auth/me'),
    ).resolves.toEqual({ value: 'contract-data' })
  })

  it('blocks a state-changing request when no CSRF token is available', async () => {
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)

    await expect(
      requestVoid('/api/admin/auth/signout', { method: 'POST' }),
    ).rejects.toBeInstanceOf(CsrfTokenUnavailableError)
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('allows read requests without a CSRF token', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }))
    vi.stubGlobal('fetch', fetchMock)

    await requestVoid('/api/admin/auth/me')

    expect(fetchMock).toHaveBeenCalledOnce()
  })

  it('retains only the validated error code and status', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        Response.json(
          {
            success: false,
            error: {
              code: 'A13',
              message: 'server message',
              details: { private: 'must not be retained' },
            },
          },
          { status: 401 },
        ),
      ),
    )

    const error = await requestVoid('/api/admin/auth/signin').catch(
      (caught: unknown) => caught,
    )

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({ status: 401, code: 'A13' })
    expect(JSON.stringify(error)).not.toContain('server message')
    expect(JSON.stringify(error)).not.toContain('private')
  })

  it('notifies session expiry only for current A05 responses', async () => {
    const expired = vi.fn()
    configureSessionExpiredHandler(expired)
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        Response.json(
          { success: false, error: { code: 'A05', message: 'expired' } },
          { status: 401 },
        ),
      ),
    )

    await expect(requestVoid('/api/admin/reports')).rejects.toMatchObject({
      code: 'A05',
    })
    expect(expired).toHaveBeenCalledOnce()
  })

  it('suppresses stale A05 responses while the session is transitioning', async () => {
    let resolveResponse: ((response: Response) => void) | undefined
    const expired = vi.fn()
    configureSessionExpiredHandler(expired)
    vi.stubGlobal(
      'fetch',
      vi.fn().mockImplementation(
        () =>
          new Promise<Response>((resolve) => {
            resolveResponse = resolve
          }),
      ),
    )

    const staleRequest = requestVoid('/api/admin/reports')
    const transition = beginAdminSessionTransition()
    resolveResponse?.(
      Response.json(
        { success: false, error: { code: 'A05', message: 'expired' } },
        { status: 401 },
      ),
    )

    await expect(staleRequest).rejects.toMatchObject({ code: 'A05' })
    expect(expired).not.toHaveBeenCalled()
    transition.cancel()
  })
})
