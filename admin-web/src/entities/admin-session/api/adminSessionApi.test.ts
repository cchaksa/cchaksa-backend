import { afterEach, describe, expect, it, vi } from 'vitest'
import { configureCsrfTokenProvider } from '../../../shared/api/http'
import { adminSessionApi } from './adminSessionApi'

afterEach(() => {
  configureCsrfTokenProvider(() => null)
  vi.unstubAllGlobals()
})

describe('admin credential authentication contract', () => {
  it('bootstraps CSRF once for concurrent callers and requires a readable cookie', async () => {
    let resolveResponse: ((response: Response) => void) | undefined
    const fetchMock = vi.fn().mockImplementation(
      () =>
        new Promise<Response>((resolve) => {
          resolveResponse = resolve
        }),
    )
    vi.stubGlobal('fetch', fetchMock)
    configureCsrfTokenProvider(() => ({
      name: 'X-XSRF-TOKEN',
      value: 'csrf-token',
    }))

    const first = adminSessionApi.bootstrapCsrf()
    const second = adminSessionApi.bootstrapCsrf()
    expect(fetchMock).toHaveBeenCalledOnce()
    resolveResponse?.(new Response(null, { status: 204 }))

    await expect(Promise.all([first, second])).resolves.toEqual([
      undefined,
      undefined,
    ])
    expect(fetchMock.mock.calls[0][0]).toBe('/api/admin/auth/csrf')
    expect(fetchMock.mock.calls[0][1]?.cache).toBe('no-store')
    expect(fetchMock.mock.calls[0][1]?.credentials).toBe('include')
  })

  it('submits loginId and password unchanged with the current CSRF token', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      Response.json({
        success: true,
        data: {
          adminAccountId: '2d577d85-53d9-45a2-8b4e-c06be5975710',
          displayName: '김척척',
          adminRole: 'CS_AGENT',
        },
      }),
    )
    vi.stubGlobal('fetch', fetchMock)
    configureCsrfTokenProvider(() => ({
      name: 'X-XSRF-TOKEN',
      value: 'csrf-token',
    }))

    await adminSessionApi.signIn({
      loginId: ' Case.Sensitive ',
      password: ' password with spaces ',
    })

    const [path, request] = fetchMock.mock.calls[0]
    expect(path).toBe('/api/admin/auth/signin')
    expect(request?.body).toBe(
      JSON.stringify({
        loginId: ' Case.Sensitive ',
        password: ' password with spaces ',
      }),
    )
    expect(new Headers(request?.headers).get('X-XSRF-TOKEN')).toBe(
      'csrf-token',
    )
  })

  it('sends only currentPassword and newPassword for password changes', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }))
    vi.stubGlobal('fetch', fetchMock)
    configureCsrfTokenProvider(() => ({
      name: 'X-XSRF-TOKEN',
      value: 'csrf-token',
    }))

    await adminSessionApi.changePassword({
      currentPassword: 'old password',
      newPassword: 'new password',
    })

    const [path, request] = fetchMock.mock.calls[0]
    expect(path).toBe('/api/admin/auth/password')
    expect(request?.body).toBe(
      JSON.stringify({
        currentPassword: 'old password',
        newPassword: 'new password',
      }),
    )
    expect(request?.body).not.toContain('confirmation')
  })
})
