// @vitest-environment jsdom

import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { configureCsrfTokenProvider } from '../../shared/api/http'
import { AppRouter } from './AppRouter'

afterEach(() => {
  cleanup()
  configureCsrfTokenProvider(() => null)
  vi.unstubAllGlobals()
  window.history.replaceState({}, '', '/')
})

function renderRouter(path: string, fetchMock: ReturnType<typeof vi.fn>) {
  window.history.replaceState({}, '', path)
  vi.stubGlobal('fetch', fetchMock)
  configureCsrfTokenProvider(() => ({
    name: 'X-XSRF-TOKEN',
    value: 'csrf-token',
  }))

  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  })

  render(
    <QueryClientProvider client={queryClient}>
      <AppRouter />
    </QueryClientProvider>,
  )
}

function createFetchMock() {
  return vi.fn(async (input: RequestInfo | URL) => {
    const path = String(input)
    if (path === '/api/admin/auth/csrf') {
      return new Response(null, { status: 204 })
    }
    if (path === '/api/admin/auth/me') {
      return Response.json(
        { success: false, error: { code: 'A05', message: 'unauthenticated' } },
        { status: 401 },
      )
    }
    throw new Error(`Unexpected request: ${path}`)
  })
}

function requestedPaths(fetchMock: ReturnType<typeof vi.fn>) {
  return fetchMock.mock.calls.map(([input]) => String(input))
}

describe('AppRouter public auth routes', () => {
  it('opens the public root login without checking the admin session', async () => {
    const fetchMock = createFetchMock()
    renderRouter('/', fetchMock)

    expect(
      await screen.findByRole('heading', { name: '관리자 로그인' }),
    ).toBeTruthy()
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1))
    expect(requestedPaths(fetchMock)).toEqual(['/api/admin/auth/csrf'])
    expect(window.location.pathname).toBe('/')
  })

  it('replaces the legacy login deep link with the canonical root', async () => {
    const fetchMock = createFetchMock()
    renderRouter('/login', fetchMock)

    expect(
      await screen.findByRole('heading', { name: '관리자 로그인' }),
    ).toBeTruthy()
    await waitFor(() => expect(window.location.pathname).toBe('/'))
    expect(requestedPaths(fetchMock)).toEqual(['/api/admin/auth/csrf'])
  })

  it('sends an unknown route to login without checking the admin session', async () => {
    const fetchMock = createFetchMock()
    renderRouter('/unknown-route', fetchMock)

    expect(
      await screen.findByRole('heading', { name: '관리자 로그인' }),
    ).toBeTruthy()
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1))
    expect(requestedPaths(fetchMock)).toEqual(['/api/admin/auth/csrf'])
    expect(window.location.pathname).toBe('/')
  })

  it('checks the admin session only for a protected route', async () => {
    const fetchMock = createFetchMock()
    renderRouter('/inquiries', fetchMock)

    expect(
      await screen.findByRole('heading', { name: '관리자 로그인' }),
    ).toBeTruthy()
    await waitFor(() => {
      expect(requestedPaths(fetchMock)).toContain('/api/admin/auth/me')
      expect(requestedPaths(fetchMock)).toContain('/api/admin/auth/csrf')
      expect(window.location.pathname).toBe('/')
    })
  })
})
