// @vitest-environment jsdom

import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it } from 'vitest'
import { MemoryRouter } from 'react-router'
import { adminSessionQueryKey } from '../../../entities/admin-session/model/queries'
import { AdminShell } from './AdminShell'

afterEach(cleanup)

describe('AdminShell profile menu', () => {
  it('opens from the profile trigger and returns focus on Escape', async () => {
    const queryClient = new QueryClient()
    queryClient.setQueryData(adminSessionQueryKey, {
      adminAccountId: '2d577d85-53d9-45a2-8b4e-c06be5975710',
      displayName: '김척척',
      adminRole: 'CS_AGENT',
    })
    const user = userEvent.setup()
    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminShell />
        </MemoryRouter>
      </QueryClientProvider>,
    )

    const profileButton = screen.getByRole('button', { name: /김척척/ })
    await user.click(profileButton)
    expect(profileButton.getAttribute('aria-expanded')).toBe('true')
    expect(screen.getByLabelText('관리자 계정 메뉴')).toBeTruthy()
    expect(document.activeElement).toBe(
      screen.getByRole('button', { name: '비밀번호 변경' }),
    )

    await user.keyboard('{Escape}')

    expect(screen.queryByLabelText('관리자 계정 메뉴')).toBeNull()
    expect(document.activeElement).toBe(profileButton)
  })
})
