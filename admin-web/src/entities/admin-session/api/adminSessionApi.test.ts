import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { configureCsrfTokenProvider } from '../../../shared/api/http'
import {
  AdminSignInCallbackError,
  adminSessionApi,
  configureKakaoAuthorizationProvider,
} from './adminSessionApi'

function createSessionStorage() {
  const values = new Map<string, string>()

  return {
    getItem: (key: string) => values.get(key) ?? null,
    setItem: (key: string, value: string) => values.set(key, value),
    removeItem: (key: string) => values.delete(key),
    clear: () => values.clear(),
    key: (index: number) => [...values.keys()][index] ?? null,
    get length() {
      return values.size
    },
  } satisfies Storage
}

beforeEach(() => {
  vi.stubGlobal('sessionStorage', createSessionStorage())
})

afterEach(() => {
  configureCsrfTokenProvider(() => null)
  vi.unstubAllGlobals()
})

describe('admin sign-in contract', () => {
  it('passes the server challenge to Kakao without storing nonce or app settings', async () => {
    const fetchMock = vi.fn().mockResolvedValueOnce(
      Response.json({
        success: true,
        data: {
          challengeId: 'a483132f-eaa7-43ab-a221-b29f2c80472d',
          nonce: 'server-issued-nonce',
          state: 'server-issued-state',
          javascriptAppKey: 'public-javascript-app-key',
          redirectUri: 'http://localhost:5173/login/callback',
        },
      }),
    )
    const authorizationProvider = vi.fn().mockResolvedValue(undefined)
    vi.stubGlobal('fetch', fetchMock)
    configureKakaoAuthorizationProvider(authorizationProvider)

    await expect(adminSessionApi.signIn()).resolves.toBeNull()

    expect(authorizationProvider).toHaveBeenCalledWith({
      javascriptAppKey: 'public-javascript-app-key',
      redirectUri: 'http://localhost:5173/login/callback',
      nonce: 'server-issued-nonce',
      state: 'server-issued-state',
    })
    expect(fetchMock.mock.calls[0][0]).toBe('/api/admin/auth/challenge')
    expect(fetchMock.mock.calls[0][1]?.cache).toBe('no-store')
    expect(fetchMock.mock.calls[0][1]?.credentials).toBe('include')
    expect([...Array(sessionStorage.length)].map((_, index) => sessionStorage.key(index))).toHaveLength(1)
    const pendingValue = sessionStorage.getItem(
      'cchaksa.admin.pending-sign-in',
    )
    expect(pendingValue).toContain('a483132f-eaa7-43ab-a221-b29f2c80472d')
    expect(pendingValue).toContain('server-issued-state')
    expect(pendingValue).not.toContain('server-issued-nonce')
    expect(pendingValue).not.toContain('public-javascript-app-key')
  })

  it('submits the callback code with the stored challenge and current CSRF token', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        Response.json({
          success: true,
          data: {
            challengeId: 'a483132f-eaa7-43ab-a221-b29f2c80472d',
            nonce: 'server-issued-nonce',
            state: 'server-issued-state',
            javascriptAppKey: 'public-javascript-app-key',
            redirectUri: 'http://localhost:5173/login/callback',
          },
        }),
      )
      .mockResolvedValueOnce(
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
    configureKakaoAuthorizationProvider(vi.fn().mockResolvedValue(undefined))
    configureCsrfTokenProvider(() => ({
      name: 'X-XSRF-TOKEN',
      value: 'server-issued-csrf-token',
    }))

    await adminSessionApi.signIn()
    await expect(
      adminSessionApi.completeSignIn({
        authorizationCode: 'kakao-authorization-code',
        state: 'server-issued-state',
      }),
    ).resolves.toMatchObject({ adminRole: 'CS_AGENT' })

    const [path, request] = fetchMock.mock.calls[1]
    const headers = new Headers(request?.headers)
    expect(path).toBe('/api/admin/auth/signin')
    expect(request?.method).toBe('POST')
    expect(request?.credentials).toBe('include')
    expect(request?.body).toBe(
      JSON.stringify({
        challengeId: 'a483132f-eaa7-43ab-a221-b29f2c80472d',
        authorizationCode: 'kakao-authorization-code',
        state: 'server-issued-state',
      }),
    )
    expect(headers.get('X-XSRF-TOKEN')).toBe('server-issued-csrf-token')
    expect(sessionStorage.length).toBe(0)
  })

  it('rejects a callback whose state does not match without sending it', async () => {
    const fetchMock = vi.fn().mockResolvedValueOnce(
      Response.json({
        success: true,
        data: {
          challengeId: 'a483132f-eaa7-43ab-a221-b29f2c80472d',
          nonce: 'server-issued-nonce',
          state: 'server-issued-state',
          javascriptAppKey: 'public-javascript-app-key',
          redirectUri: 'http://localhost:5173/login/callback',
        },
      }),
    )
    vi.stubGlobal('fetch', fetchMock)
    configureKakaoAuthorizationProvider(vi.fn().mockResolvedValue(undefined))

    await adminSessionApi.signIn()

    await expect(
      adminSessionApi.completeSignIn({
        authorizationCode: 'kakao-authorization-code',
        state: 'different-state',
      }),
    ).rejects.toBeInstanceOf(AdminSignInCallbackError)
    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(sessionStorage.length).toBe(0)
  })
})
