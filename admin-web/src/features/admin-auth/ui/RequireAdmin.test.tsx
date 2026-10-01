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
  it('marks an A05 redirect as an expired session', async () => {
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
      expect(screen.getByText('세션 만료 안내')).toBeTruthy()
    })
  })
})
