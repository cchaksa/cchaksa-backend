import { requestJson } from '../../../shared/api/http'
import { useMockApi } from '../../../shared/config/api'
import { mockInquiryDetails } from '../model/mockInquiries'
import type {
  InquiryDetail,
  InquiryStatus,
  InquirySummary,
} from '../model/types'

export type InquirySearchField =
  | 'ALL'
  | 'USER_ID'
  | 'STUDENT_CODE'
  | 'ERROR_CODE'

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
}

function matchesQuery(
  inquiry: InquirySummary,
  field: InquirySearchField,
  query: string,
) {
  const normalized = query.toLowerCase()
  const values: Record<InquirySearchField, string[]> = {
    ALL: [String(inquiry.userId), inquiry.studentCode ?? '', inquiry.errorCode ?? ''],
    USER_ID: [String(inquiry.userId)],
    STUDENT_CODE: [inquiry.studentCode ?? ''],
    ERROR_CODE: [inquiry.errorCode ?? ''],
  }
  return values[field].some((value) => value.toLowerCase().includes(normalized))
}

async function getMockInquiryPage(params: InquiryListParams): Promise<InquiryPage> {
  const filtered = mockInquiryDetails.filter(
    (inquiry) =>
      (!params.status || inquiry.status === params.status) &&
      (!params.query || matchesQuery(inquiry, params.searchField ?? 'ALL', params.query)),
  )
  const start = params.page * params.size
  return {
    items: filtered.slice(start, start + params.size),
    page: params.page,
    size: params.size,
    totalElements: filtered.length,
    totalPages: Math.max(1, Math.ceil(filtered.length / params.size)),
  }
}

function buildListUrl(params: InquiryListParams) {
  const searchParams = new URLSearchParams({
    page: String(params.page),
    size: String(params.size),
  })
  if (params.status) searchParams.set('status', params.status)
  if (params.searchField) searchParams.set('searchField', params.searchField)
  if (params.query) searchParams.set('query', params.query)
  return `/api/admin/reports?${searchParams}`
}

export const adminInquiryApi = {
  getPage: (params: InquiryListParams) =>
    useMockApi
      ? getMockInquiryPage(params)
      : requestJson<InquiryPage>(buildListUrl(params)),
  getDetail: (reportId: number) =>
    useMockApi
      ? Promise.resolve(
          mockInquiryDetails.find((inquiry) => inquiry.reportId === reportId) ?? null,
        )
      : requestJson<InquiryDetail>(`/api/admin/reports/${reportId}`),
  answer: async (reportId: number, answer: string) => {
    if (!useMockApi) {
      return requestJson<InquiryDetail>(`/api/admin/reports/${reportId}/answer`, {
        method: 'POST',
        body: JSON.stringify({ answer }),
      })
    }

    const inquiry = mockInquiryDetails.find((item) => item.reportId === reportId)
    if (!inquiry || inquiry.status !== 'PENDING') {
      throw new Error('답변할 수 없는 문의입니다.')
    }
    inquiry.status = 'ANSWERED'
    inquiry.answer = {
      content: answer,
      answeredAt: new Date().toISOString(),
      answeredBy: { adminAccountId: 7, displayName: '김척척' },
    }
    return inquiry
  },
}
