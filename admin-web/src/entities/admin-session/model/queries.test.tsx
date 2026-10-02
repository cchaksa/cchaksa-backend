// @vitest-environment jsdom

import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, cleanup, renderHook } from '@testing-library/react'
import type { ReactNode } from 'react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  configureCsrfTokenProvider,
  configureSessionExpiredHandler,
  requestVoid,
} from '../../../shared/api/http'
import {
  adminSessionQueryKey,
  useAdminPasswordChange,
  useAdminSignOut,
} from './queries'

afterEach(() => {
  cleanup()
  configureCsrfTokenProvider(() => null)
  configureSessionExpiredHandler(null)
  vi.unstubAllGlobals()
})

function createWrapper(queryClient: QueryClient) {
  return function Wrapper({ children }: { children: ReactNode }) {
    return (
      <QueryClientProvider client={queryClient}>
        {children}
      </QueryClientProvider>
    )
  }
}

describe('admin session commands', () => {
  it('verifies and stores the rotated session after a password change', async () => {
    const queryClient = new QueryClient()
    const rotatedSession = {
      adminAccountId: '2d577d85-53d9-45a2-8b4e-c06be5975710',
      displayName: '김척척',
      adminRole: 'CS_AGENT' as const,
    }
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(
        Response.json({ success: true, data: rotatedSession }),
      )
    vi.stubGlobal('fetch', fetchMock)
    configureCsrfTokenProvider(() => ({
      name: 'X-XSRF-TOKEN',
      value: 'csrf-token',
    }))
    const { result } = renderHook(() => useAdminPasswordChange(), {
      wrapper: createWrapper(queryClient),
    })

    await act(async () => {
      await result.current.execute({
        currentPassword: 'current password',
        newPassword: 'new password',
      })
    })

    expect(fetchMock.mock.calls.map(([path]) => path)).toEqual([
      '/api/admin/auth/password',
      '/api/admin/auth/me',
    ])
    expect(queryClient.getQueryData(adminSessionQueryKey)).toEqual(
      rotatedSession,
    )
  })

  it('releases the transition and reports expiry when rotated session verification fails', async () => {
    const queryClient = new QueryClient()
    const expired = vi.fn()
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockResolvedValueOnce(
        Response.json(
          { success: false, error: { code: 'A05', message: 'expired' } },
          { status: 401 },
        ),
      )
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
    vi.stubGlobal('fetch', fetchMock)
    configureCsrfTokenProvider(() => ({
      name: 'X-XSRF-TOKEN',
      value: 'csrf-token',
    }))
    configureSessionExpiredHandler(expired)
    const { result } = renderHook(() => useAdminPasswordChange(), {
      wrapper: createWrapper(queryClient),
    })

    await act(async () => {
      await expect(
        result.current.execute({
          currentPassword: 'current password',
          newPassword: 'new password',
        }),
      ).rejects.toMatchObject({ code: 'A05' })
    })

    expect(expired).toHaveBeenCalledOnce()
    await expect(requestVoid('/api/admin/reports')).resolves.toBeUndefined()
  })

  it('clears cached administrator data after signout', async () => {
    const queryClient = new QueryClient()
    queryClient.setQueryData(adminSessionQueryKey, { displayName: '관리자' })
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(new Response(null, { status: 204 })),
    )
    configureCsrfTokenProvider(() => ({
      name: 'X-XSRF-TOKEN',
      value: 'csrf-token',
    }))
    const { result } = renderHook(() => useAdminSignOut(), {
      wrapper: createWrapper(queryClient),
    })

    await act(async () => {
      await expect(result.current.execute()).resolves.toBe(true)
    })

    expect(queryClient.getQueryData(adminSessionQueryKey)).toBeUndefined()
  })
})
