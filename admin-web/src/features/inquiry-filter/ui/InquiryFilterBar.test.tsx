// @vitest-environment jsdom

import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { InquiryFilterBar } from './InquiryFilterBar'

afterEach(cleanup)

describe('InquiryFilterBar', () => {
  it('labels the user identifier search without exposing the UUID format', () => {
    render(
      <InquiryFilterBar
        status="ALL"
        searchField="USER_ID"
        query=""
        onStatusChange={vi.fn()}
        onSearch={vi.fn()}
        onReset={vi.fn()}
      />,
    )

    expect(screen.getByRole('option', { name: '사용자 ID' })).toBeTruthy()
    expect(
      screen.getByPlaceholderText('사용자 ID 정확히 입력'),
    ).toBeTruthy()
    expect(screen.queryByText('사용자 UUID')).toBeNull()
  })
})
