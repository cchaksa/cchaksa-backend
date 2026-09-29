import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  adminSessionApi,
  configureAdminSignInPreparation,
} from './adminSessionApi'

afterEach(() => {
  vi.unstubAllGlobals()
})

describe('admin sign-in contract', () => {
  it('submits the server nonce with POST and the prepared CSRF header', async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 204 }))
    vi.stubGlobal('fetch', fetchMock)
    configureAdminSignInPreparation(async () => ({
      nonce: 'server-issued-nonce',
      csrf: {
        name: 'X-CSRF-TOKEN',
        value: 'server-issued-csrf-token',
      },
    }))

    await adminSessionApi.signIn()

    const [path, request] = fetchMock.mock.calls[0]
    const headers = new Headers(request?.headers)
    expect(path).toBe('/api/admin/auth/signin')
    expect(request?.method).toBe('POST')
    expect(request?.body).toBe(JSON.stringify({ nonce: 'server-issued-nonce' }))
    expect(headers.get('X-CSRF-TOKEN')).toBe('server-issued-csrf-token')
  })
})
