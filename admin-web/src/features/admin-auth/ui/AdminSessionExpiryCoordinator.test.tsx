// @vitest-environment jsdom

import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { act, cleanup, render, screen, waitFor } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter, useLocation } from 'react-router'
import {
  configureSessionExpiredHandler,
  requestVoid,
} from '../../../shared/api/http'
import { routes } from '../../../shared/config/routes'
import { AdminSessionExpiryCoordinator } from './AdminSessionExpiryCoordinator'

afterEach(() => {
  cleanup()
  configureSessionExpiredHandler(null)
  vi.unstubAllGlobals()
})

function LocationProbe() {
  const location = useLocation()
  const sessionExpired = Boolean(
    (location.state as { sessionExpired?: boolean } | null)?.sessionExpired,
  )
  return <p>{`${location.pathname}:${sessionExpired}`}</p>
}

describe('AdminSessionExpiryCoordinator', () => {
  it('returns an expired protected session to the canonical root login', async () => {
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
        <MemoryRouter initialEntries={[routes.inquiries]}>
          <AdminSessionExpiryCoordinator />
          <LocationProbe />
        </MemoryRouter>
      </QueryClientProvider>,
    )

    await act(async () => {
      await expect(requestVoid('/api/admin/reports')).rejects.toMatchObject({
        code: 'A05',
      })
    })

    await waitFor(() => {
      expect(screen.getByText(`${routes.login}:true`)).toBeTruthy()
    })
  })
})
