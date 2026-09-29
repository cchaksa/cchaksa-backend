import { afterEach, describe, expect, it, vi } from 'vitest'
import { configureCsrfTokenProvider } from '../../../shared/api/http'
import {
  adminSessionApi,
  configureKakaoIdTokenProvider,
} from './adminSessionApi'

afterEach(() => {
  configureCsrfTokenProvider(() => null)
  vi.unstubAllGlobals()
})

describe('admin sign-in contract', () => {
  it('uses the challenge nonce for Kakao and submits challengeId with the ID token', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(
        Response.json({
          success: true,
          data: {
            challengeId: 'a483132f-eaa7-43ab-a221-b29f2c80472d',
            nonce: 'server-issued-nonce',
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
    const idTokenProvider = vi.fn().mockResolvedValue('kakao-id-token')
    vi.stubGlobal('fetch', fetchMock)
    configureCsrfTokenProvider(() => ({
      name: 'X-XSRF-TOKEN',
      value: 'server-issued-csrf-token',
    }))
    configureKakaoIdTokenProvider(idTokenProvider)

    await expect(adminSessionApi.signIn()).resolves.toMatchObject({
      adminRole: 'CS_AGENT',
    })

    expect(idTokenProvider).toHaveBeenCalledWith({ nonce: 'server-issued-nonce' })
    expect(fetchMock.mock.calls[0][0]).toBe('/api/admin/auth/challenge')
    expect(fetchMock.mock.calls[0][1]?.cache).toBe('no-store')
    const [path, request] = fetchMock.mock.calls[1]
    const headers = new Headers(request?.headers)
    expect(path).toBe('/api/admin/auth/signin')
    expect(request?.method).toBe('POST')
    expect(request?.credentials).toBe('include')
    expect(request?.body).toBe(
      JSON.stringify({
        challengeId: 'a483132f-eaa7-43ab-a221-b29f2c80472d',
        idToken: 'kakao-id-token',
      }),
    )
    expect(headers.get('X-XSRF-TOKEN')).toBe('server-issued-csrf-token')
  })
})
