// @vitest-environment jsdom

import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { useRef, useState } from 'react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { configureCsrfTokenProvider } from '../../../shared/api/http'
import { AdminPasswordChangeDialog } from './AdminPasswordChangeDialog'

function DialogHarness() {
  const openerRef = useRef<HTMLButtonElement>(null)
  const [isOpen, setIsOpen] = useState(true)
  return (
    <>
      <button ref={openerRef} type="button">
        계정 메뉴
      </button>
      <AdminPasswordChangeDialog
        isOpen={isOpen}
        onClose={() => setIsOpen(false)}
        returnFocusRef={openerRef}
      />
    </>
  )
}

beforeEach(() => {
  HTMLDialogElement.prototype.showModal = function showModal() {
    this.setAttribute('open', '')
  }
  HTMLDialogElement.prototype.close = function close() {
    this.removeAttribute('open')
  }
  vi.stubGlobal('requestAnimationFrame', (callback: FrameRequestCallback) => {
    callback(0)
    return 0
  })
})

afterEach(() => {
  cleanup()
  configureCsrfTokenProvider(() => null)
  vi.unstubAllGlobals()
})

function renderDialog(queryClient = new QueryClient()) {
  return render(
    <QueryClientProvider client={queryClient}>
      <DialogHarness />
    </QueryClientProvider>,
  )
}

describe('AdminPasswordChangeDialog', () => {
  it('validates confirmation locally and never sends it to the server', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
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
    configureCsrfTokenProvider(() => ({
      name: 'X-XSRF-TOKEN',
      value: 'csrf-token',
    }))
    const user = userEvent.setup()
    renderDialog()

    await user.type(screen.getByLabelText('기존 비밀번호'), 'old password')
    await user.type(screen.getByLabelText('변경 비밀번호'), 'new password')
    await user.type(
      screen.getByLabelText('변경 비밀번호 재확인'),
      'different password',
    )
    await user.click(screen.getByRole('button', { name: '변경' }))
    expect(screen.getByRole('alert').textContent).toContain('일치하지 않습니다')
    expect(fetchMock).not.toHaveBeenCalled()

    await user.clear(screen.getByLabelText('변경 비밀번호 재확인'))
    await user.type(
      screen.getByLabelText('변경 비밀번호 재확인'),
      'new password',
    )
    await user.click(screen.getByRole('button', { name: '변경' }))

    await screen.findByRole('status')
    expect(fetchMock).toHaveBeenCalledTimes(2)
    expect(fetchMock.mock.calls[0][1]?.body).toBe(
      JSON.stringify({
        currentPassword: 'old password',
        newPassword: 'new password',
      }),
    )
    expect(fetchMock.mock.calls[0][1]?.body).not.toContain('confirmation')
    expect(
      (screen.getByLabelText('기존 비밀번호') as HTMLInputElement).value,
    ).toBe('')
    expect(
      (screen.getByLabelText('변경 비밀번호') as HTMLInputElement).value,
    ).toBe('')
  })

  it('closes with Escape and restores focus to the profile trigger', async () => {
    const user = userEvent.setup()
    renderDialog()
    expect(document.activeElement).toBe(screen.getByLabelText('기존 비밀번호'))

    await user.keyboard('{Escape}')

    await waitFor(() =>
      expect(document.activeElement).toBe(
        screen.getByRole('button', { name: '계정 메뉴' }),
      ),
    )
    expect(screen.queryByRole('dialog')).toBeNull()
  })
})
