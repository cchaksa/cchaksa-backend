// @vitest-environment jsdom

import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router'
import { configureCsrfTokenProvider } from '../../../shared/api/http'
import { AdminSignInForm } from './AdminSignInForm'

afterEach(() => {
  cleanup()
  configureCsrfTokenProvider(() => null)
  vi.unstubAllGlobals()
})

function renderSignIn(queryClient: QueryClient) {
  return render(
    <QueryClientProvider client={queryClient}>
      <MemoryRouter initialEntries={['/login']}>
        <Routes>
          <Route path="/login" element={<AdminSignInForm />} />
          <Route path="/inquiries" element={<p>로그인 완료</p>} />
        </Routes>
      </MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('AdminSignInForm', () => {
  it('keeps credentials out of mutation and browser storage while preventing duplicate submit', async () => {
    let resolveSignIn: ((response: Response) => void) | undefined
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
      .mockImplementationOnce(
        () =>
          new Promise<Response>((resolve) => {
            resolveSignIn = resolve
          }),
      )
    vi.stubGlobal('fetch', fetchMock)
    configureCsrfTokenProvider(() => ({
      name: 'X-XSRF-TOKEN',
      value: 'csrf-token',
    }))
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })
    const user = userEvent.setup()
    renderSignIn(queryClient)

    const submit = await screen.findByRole('button', { name: '로그인' })
    await user.type(screen.getByLabelText('아이디'), ' Case.Sensitive ')
    await user.type(screen.getByLabelText('비밀번호'), ' password with spaces ')
    await user.click(submit)
    await user.click(submit)

    expect(fetchMock).toHaveBeenCalledTimes(2)
    expect(fetchMock.mock.calls[1][1]?.body).toBe(
      JSON.stringify({
        loginId: ' Case.Sensitive ',
        password: ' password with spaces ',
      }),
    )
    expect(queryClient.getMutationCache().getAll()).toHaveLength(0)

    resolveSignIn?.(
      Response.json({
        success: true,
        data: {
          adminAccountId: '2d577d85-53d9-45a2-8b4e-c06be5975710',
          displayName: '김척척',
          adminRole: 'CS_AGENT',
        },
      }),
    )
    expect(await screen.findByText('로그인 완료')).toBeTruthy()
  })

  it('blocks whitespace-only credentials before sending signin', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
    vi.stubGlobal('fetch', fetchMock)
    configureCsrfTokenProvider(() => ({
      name: 'X-XSRF-TOKEN',
      value: 'csrf-token',
    }))
    const user = userEvent.setup()
    renderSignIn(new QueryClient())

    await user.type(await screen.findByLabelText('아이디'), '   ')
    await user.type(screen.getByLabelText('비밀번호'), '   ')
    await user.click(screen.getByRole('button', { name: '로그인' }))

    expect(screen.getByRole('alert').textContent).toContain(
      '아이디와 비밀번호를 모두 입력해 주세요.',
    )
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1))
  })
})
