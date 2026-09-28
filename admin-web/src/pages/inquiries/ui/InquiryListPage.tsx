import { ChevronLeft, ChevronRight, RefreshCw } from 'lucide-react'
import { useMemo } from 'react'
import { Link, useSearchParams } from 'react-router'
import {
  mockInquirySummaries,
  type InquiryCategory,
  type InquirySummary,
} from '../../../entities/inquiry'
import {
  InquiryFilterBar,
  type InquirySearchField,
  type InquiryStatusFilter,
} from '../../../features/inquiry-filter'
import { routes } from '../../../shared/config/routes'
import './inquiry-list-page.css'

const categoryLabels: Record<InquiryCategory, string> = {
  PORTAL_CONNECTION: '포털 연동',
  GRADUATION_REQUIREMENT: '졸업 요건',
  ACADEMIC_RECORD: '학적 정보',
  ACCOUNT: '계정',
  ETC: '기타',
}

const dateFormatter = new Intl.DateTimeFormat('ko-KR', {
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
})

function matchesQuery(
  inquiry: InquirySummary,
  field: InquirySearchField,
  query: string,
) {
  if (!query) return true
  const normalized = query.toLowerCase()
  const values: Record<InquirySearchField, string[]> = {
    ALL: [
      String(inquiry.userId),
      inquiry.studentCode ?? '',
      inquiry.errorCode ?? '',
    ],
    USER_ID: [String(inquiry.userId)],
    STUDENT_CODE: [inquiry.studentCode ?? ''],
    ERROR_CODE: [inquiry.errorCode ?? ''],
  }

  return values[field].some((value) => value.toLowerCase().includes(normalized))
}

export function InquiryListPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const status = (searchParams.get('status') ?? 'ALL') as InquiryStatusFilter
  const searchField = (searchParams.get('field') ?? 'ALL') as InquirySearchField
  const query = searchParams.get('query') ?? ''

  const inquiries = useMemo(
    () =>
      mockInquirySummaries.filter(
        (inquiry) =>
          (status === 'ALL' || inquiry.status === status) &&
          matchesQuery(inquiry, searchField, query),
      ),
    [query, searchField, status],
  )

  const setFilters = (next: {
    status?: InquiryStatusFilter
    field?: InquirySearchField
    query?: string
  }) => {
    const params = new URLSearchParams(searchParams)
    const nextStatus = next.status ?? status
    const nextField = next.field ?? searchField
    const nextQuery = next.query ?? query

    nextStatus === 'ALL' ? params.delete('status') : params.set('status', nextStatus)
    nextField === 'ALL' ? params.delete('field') : params.set('field', nextField)
    nextQuery ? params.set('query', nextQuery) : params.delete('query')
    setSearchParams(params, { replace: true })
  }

  return (
    <div className="inquiry-list-page">
      <header className="page-heading">
        <div>
          <p>고객 지원</p>
          <h1>문의</h1>
        </div>
        <button type="button" className="refresh-button" title="목록 새로고침">
          <RefreshCw aria-hidden="true" size={18} />
          <span>새로고침</span>
        </button>
      </header>

      <section className="inquiry-table-panel" aria-labelledby="inquiry-list-title">
        <div className="panel-title-row">
          <div>
            <h2 id="inquiry-list-title">문의 목록</h2>
            <p>최근 접수된 순서로 표시됩니다.</p>
          </div>
          <strong>{inquiries.length}건</strong>
        </div>

        <InquiryFilterBar
          status={status}
          searchField={searchField}
          query={query}
          onStatusChange={(nextStatus) => setFilters({ status: nextStatus })}
          onSearch={(field, nextQuery) => setFilters({ field, query: nextQuery })}
          onReset={() => setSearchParams({}, { replace: true })}
        />

        {inquiries.length > 0 ? (
          <div className="inquiry-table-scroll">
            <table className="inquiry-table">
              <thead>
                <tr>
                  <th>상태</th>
                  <th>문의</th>
                  <th>분류</th>
                  <th>사용자 ID</th>
                  <th>학번</th>
                  <th>오류 코드</th>
                  <th>접수일</th>
                </tr>
              </thead>
              <tbody>
                {inquiries.map((inquiry) => (
                  <tr key={inquiry.reportId}>
                    <td>
                      <span className={`status-badge status-${inquiry.status.toLowerCase()}`}>
                        {inquiry.status === 'PENDING' ? '답변 필요' : '답변 완료'}
                      </span>
                    </td>
                    <td className="inquiry-title-cell">
                      <Link to={routes.inquiryDetail(inquiry.reportId)}>
                        <strong>{inquiry.title}</strong>
                        <span>#{inquiry.reportId}</span>
                      </Link>
                    </td>
                    <td>{categoryLabels[inquiry.category]}</td>
                    <td>{inquiry.userId}</td>
                    <td>{inquiry.studentCode ?? '-'}</td>
                    <td>
                      {inquiry.errorCode ? (
                        <code>{inquiry.errorCode}</code>
                      ) : (
                        '-'
                      )}
                    </td>
                    <td>{dateFormatter.format(new Date(inquiry.createdAt))}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="inquiry-empty-state">
            <strong>조건에 맞는 문의가 없습니다.</strong>
            <p>검색어나 답변 상태를 변경해 주세요.</p>
          </div>
        )}

        <footer className="table-footer">
          <p>페이지당 20개</p>
          <div className="pagination" aria-label="페이지 이동">
            <button type="button" disabled aria-label="이전 페이지">
              <ChevronLeft aria-hidden="true" size={18} />
            </button>
            <span>1 / 1</span>
            <button type="button" disabled aria-label="다음 페이지">
              <ChevronRight aria-hidden="true" size={18} />
            </button>
          </div>
        </footer>
      </section>
    </div>
  )
}
