import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  configureCsrfTokenProvider,
  CsrfTokenUnavailableError,
  requestVoid,
} from './http'

afterEach(() => {
  configureCsrfTokenProvider(() => null)
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
})
