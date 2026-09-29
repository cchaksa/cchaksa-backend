import { requestJson } from '../../../shared/api/http'
import { useMockApi } from '../../../shared/config/api'
import {
  mockInquiryDetails,
  mockInquirySummaries,
} from '../model/mockInquiries'
import type {
  InquiryAnswerResult,
  InquiryDetail,
  InquiryStatus,
  InquirySummary,
} from '../model/types'

export type InquirySearchField = 'USER_ID' | 'STUDENT_CODE'

export interface InquiryListParams {
  status?: InquiryStatus
  searchField?: InquirySearchField
  query?: string
  page: number
  size: number
}

export interface InquiryPage {
  items: InquirySummary[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  hasNext: boolean
}

export function matchesExactQuery(
  inquiry: InquirySummary,
  field: InquirySearchField,
  query: string,
) {
  return field === 'USER_ID'
    ? inquiry.userId === query
    : inquiry.studentCode === query
}

async function getMockInquiryPage(params: InquiryListParams): Promise<InquiryPage> {
  const filtered = mockInquirySummaries.filter(
    (inquiry) =>
      (!params.status || inquiry.status === params.status) &&
      (!params.query ||
        (params.searchField &&
          matchesExactQuery(inquiry, params.searchField, params.query))),
  )
  const start = params.page * params.size
  return {
    items: filtered.slice(start, start + params.size),
    page: params.page,
    size: params.size,
    totalElements: filtered.length,
    totalPages: Math.max(1, Math.ceil(filtered.length / params.size)),
    hasNext: start + params.size < filtered.length,
  }
}

export function buildInquiryListUrl(params: InquiryListParams) {
  const searchParams = new URLSearchParams({
    page: String(params.page),
    size: String(params.size),
  })
  if (params.status) searchParams.set('status', params.status)
  if (params.searchField && params.query) {
    searchParams.set('searchType', params.searchField)
    searchParams.set('query', params.query)
  }
  return `/api/admin/reports?${searchParams}`
}

export const adminInquiryApi = {
  getPage: (params: InquiryListParams) =>
    useMockApi
      ? getMockInquiryPage(params)
      : requestJson<InquiryPage>(buildInquiryListUrl(params)),
  getDetail: (reportId: string) =>
    useMockApi
      ? Promise.resolve(
          mockInquiryDetails.find((inquiry) => inquiry.reportId === reportId) ?? null,
        )
      : requestJson<InquiryDetail>(`/api/admin/reports/${reportId}`),
  answer: async (reportId: string, answer: string) => {
    if (!useMockApi) {
      return requestJson<InquiryAnswerResult>(`/api/admin/reports/${reportId}/answer`, {
        method: 'POST',
        body: JSON.stringify({ answer }),
      })
    }

    const inquiry = mockInquiryDetails.find((item) => item.reportId === reportId)
    if (inquiry?.status !== 'PENDING') {
      throw new Error('답변할 수 없는 문의입니다.')
    }
    inquiry.status = 'ANSWERED'
    const answeredAt = new Date().toISOString()
    inquiry.answer = {
      answer,
      answeredAt,
      adminAccountId: '2d577d85-53d9-45a2-8b4e-c06be5975710',
      adminDisplayName: '김척척',
    }
    const summary = mockInquirySummaries.find((item) => item.reportId === reportId)
    if (summary) {
      summary.status = 'ANSWERED'
      summary.answeredAt = answeredAt
    }
    return {
      reportId,
      status: 'ANSWERED',
      answeredAt,
      adminAccountId: '2d577d85-53d9-45a2-8b4e-c06be5975710',
      adminDisplayName: '김척척',
      adminRole: 'CS_AGENT',
    }
  },
}
