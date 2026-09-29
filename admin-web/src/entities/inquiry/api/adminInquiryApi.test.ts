import { afterEach, describe, expect, it, vi } from 'vitest'
import { configureCsrfTokenProvider } from '../../../shared/api/http'
import { mockInquirySummaries } from '../model/mockInquiries'
import {
  adminInquiryApi,
  buildInquiryListUrl,
  matchesExactQuery,
} from './adminInquiryApi'

afterEach(() => {
  configureCsrfTokenProvider(() => null)
  vi.unstubAllGlobals()
})

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
      '/api/admin/reports?page=0&size=20&status=PENDING&searchType=USER_ID&query=7f6d4516-6e8c-43c0-99f4-f6fe4daf47b8',
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

  it('reads the detail submitter shape from SuccessResponse data', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        Response.json({
          success: true,
          data: {
            reportId: '8f0d5c40-7a87-4cc5-a0ef-4e165f4d5e41',
            status: 'PENDING',
            title: '문의 제목',
            content: '문의 본문',
            userId: '7f6d4516-6e8c-43c0-99f4-f6fe4daf47b8',
            createdAt: '2026-09-28T05:42:00Z',
            updatedAt: '2026-09-28T05:42:00Z',
            submitter: {
              submittedUserId: '7f6d4516-6e8c-43c0-99f4-f6fe4daf47b8',
              departmentId: 1,
              departmentName: '컴퓨터공학과',
              studentCode: '20201234',
              primaryMajorId: 2,
              primaryMajorName: '컴퓨터공학',
              secondaryMajorId: null,
              secondaryMajorName: null,
              transferStudent: false,
              admissionYear: 2020,
              graduationRequirementStatus: 'AVAILABLE',
            },
            answer: null,
          },
        }),
      ),
    )

    const detail = await adminInquiryApi.getDetail(
      '8f0d5c40-7a87-4cc5-a0ef-4e165f4d5e41',
    )

    expect(detail?.submitter.departmentName).toBe('컴퓨터공학과')
    expect(detail?.submitter.transferStudent).toBe(false)
  })

  it('posts an answer with the server CSRF header and reads AnswerResponse', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      Response.json({
        success: true,
        data: {
          reportId: '8f0d5c40-7a87-4cc5-a0ef-4e165f4d5e41',
          status: 'ANSWERED',
          answeredAt: '2026-09-29T12:00:00Z',
          adminAccountId: '2d577d85-53d9-45a2-8b4e-c06be5975710',
          adminDisplayName: '김척척',
          adminRole: 'CS_AGENT',
        },
      }),
    )
    vi.stubGlobal('fetch', fetchMock)
    configureCsrfTokenProvider(() => ({
      name: 'X-XSRF-TOKEN',
      value: 'csrf-token',
    }))

    const result = await adminInquiryApi.answer(
      '8f0d5c40-7a87-4cc5-a0ef-4e165f4d5e41',
      '답변 내용',
    )

    const headers = new Headers(fetchMock.mock.calls[0][1]?.headers)
    expect(headers.get('X-XSRF-TOKEN')).toBe('csrf-token')
    expect(result.adminDisplayName).toBe('김척척')
  })
})
