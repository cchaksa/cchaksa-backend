// @vitest-environment jsdom

import { cleanup, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it } from 'vitest'
import { InquiryAnswerPanel } from './InquiryAnswerPanel'

afterEach(cleanup)

describe('InquiryAnswerPanel', () => {
  it('shows the answering admin name without exposing the account UUID', () => {
    render(
      <InquiryAnswerPanel
        answer={{
          answer: '확인 후 처리했습니다.',
          adminAccountId: '2d577d85-53d9-45a2-8b4e-c06be5975710',
          adminDisplayName: '김척척',
          answeredAt: '2026-10-02T10:00:00Z',
        }}
      />,
    )

    expect(screen.getByText('김척척')).toBeTruthy()
    expect(
      screen.queryByText('2d577d85-53d9-45a2-8b4e-c06be5975710'),
    ).toBeNull()
  })
})
