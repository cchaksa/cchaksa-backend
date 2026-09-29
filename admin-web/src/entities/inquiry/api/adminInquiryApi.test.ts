import { describe, expect, it } from 'vitest'
import { mockInquirySummaries } from '../model/mockInquiries'
import { buildInquiryListUrl, matchesExactQuery } from './adminInquiryApi'

describe('adminInquiryApi contract', () => {
  it('uses zero-based paging and exact search query parameters', () => {
    expect(
      buildInquiryListUrl({
        status: 'PENDING',
        searchField: 'USER_ID',
        query: '7f6d4516-6e8c-43c0-99f4-f6fe4daf47b8',
        page: 0,
        size: 20,
      }),
    ).toBe(
      '/api/admin/reports?page=0&size=20&status=PENDING&searchField=USER_ID&query=7f6d4516-6e8c-43c0-99f4-f6fe4daf47b8',
    )
  })

  it('does not send a search field without a query', () => {
    expect(
      buildInquiryListUrl({
        searchField: 'STUDENT_CODE',
        page: 0,
        size: 20,
      }),
    ).toBe('/api/admin/reports?page=0&size=20')
  })

  it('matches user UUID and student code only by exact value', () => {
    const inquiry = mockInquirySummaries[0]

    expect(
      matchesExactQuery(
        inquiry,
        'USER_ID',
        '7f6d4516-6e8c-43c0-99f4-f6fe4daf47b8',
      ),
    ).toBe(true)
    expect(matchesExactQuery(inquiry, 'USER_ID', '7f6d4516')).toBe(false)
    expect(matchesExactQuery(inquiry, 'STUDENT_CODE', '202012345')).toBe(true)
    expect(matchesExactQuery(inquiry, 'STUDENT_CODE', '2020')).toBe(false)
  })

  it('keeps mock inquiries in createdAt descending order', () => {
    const timestamps = mockInquirySummaries.map((inquiry) =>
      new Date(inquiry.createdAt).getTime(),
    )

    expect(timestamps).toEqual([...timestamps].sort((left, right) => right - left))
  })
})
