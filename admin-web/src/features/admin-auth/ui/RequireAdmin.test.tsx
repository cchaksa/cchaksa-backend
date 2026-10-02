// @vitest-environment jsdom

import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  MemoryRouter,
  Outlet,
  Route,
  Routes,
  useLocation,
} from 'react-router'
import { RequireAdmin } from './RequireAdmin'

afterEach(() => {
  cleanup()
  vi.unstubAllGlobals()
})

function LoginProbe() {
  const location = useLocation()
  const sessionExpired = Boolean(
    (location.state as { sessionExpired?: boolean } | null)?.sessionExpired,
  )
  return <p>{sessionExpired ? '세션 만료 안내' : '일반 로그인'}</p>
}

describe('RequireAdmin', () => {
  it('redirects an initial A05 response without a session expiry notice', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        Response.json(
          { success: false, error: { code: 'A05', message: 'expired' } },
          { status: 401 },
        ),
      ),
    )
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/inquiries']}>
          <Routes>
            <Route path="/login" element={<LoginProbe />} />
            <Route element={<RequireAdmin />}>
              <Route element={<Outlet />}>
                <Route path="/inquiries" element={<p>문의</p>} />
              </Route>
            </Route>
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    )

    await waitFor(() => {
      expect(screen.getByText('일반 로그인')).toBeTruthy()
    })
  })

  it('distinguishes an unavailable server from an unauthenticated response', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(new Response(null, { status: 502 })),
    )
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter initialEntries={['/inquiries']}>
          <Routes>
            <Route path="/login" element={<LoginProbe />} />
            <Route element={<RequireAdmin />}>
              <Route path="/inquiries" element={<p>문의</p>} />
            </Route>
          </Routes>
        </MemoryRouter>
      </QueryClientProvider>,
    )

    expect(
      await screen.findByText('관리자 서버에 연결하지 못했습니다.'),
    ).toBeTruthy()
    expect(screen.queryByText('세션 만료 안내')).toBeNull()
  })
})
