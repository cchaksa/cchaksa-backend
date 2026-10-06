import { ChevronLeft, ChevronRight, RefreshCw } from 'lucide-react'
import { Link, useSearchParams } from 'react-router'
import { useInquiryPage } from '../../../entities/inquiry'
import {
  InquiryFilterBar,
  type InquirySearchField,
  type InquiryStatusFilter,
} from '../../../features/inquiry-filter'
import { routes } from '../../../shared/config/routes'
import './inquiry-list-page.css'

const dateFormatter = new Intl.DateTimeFormat('ko-KR', {
  month: '2-digit',
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
})

function parseStatus(value: string | null): InquiryStatusFilter {
  return value === 'PENDING' || value === 'ANSWERED' ? value : 'ALL'
}

function parseSearchField(value: string | null): InquirySearchField {
  return value === 'STUDENT_CODE' ? value : 'USER_ID'
}

function parsePage(value: string | null) {
  const page = Number(value ?? '0')
  return Number.isInteger(page) && page >= 0 ? page : 0
}

export function InquiryListPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const status = parseStatus(searchParams.get('status'))
  const searchField = parseSearchField(searchParams.get('field'))
  const query = searchParams.get('query') ?? ''

  const page = parsePage(searchParams.get('page'))
  const inquiryQuery = useInquiryPage({
    ...(status === 'ALL' ? {} : { status }),
    ...(query ? { searchField, query } : {}),
    page,
    size: 20,
  })
  const inquiries = inquiryQuery.data?.items ?? []

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
    nextField === 'USER_ID' ? params.delete('field') : params.set('field', nextField)
    nextQuery ? params.set('query', nextQuery) : params.delete('query')
    params.delete('page')
    setSearchParams(params, { replace: true })
  }

  return (
    <div className="inquiry-list-page">
      <header className="page-heading">
        <div>
          <p>고객 지원</p>
          <h1>문의</h1>
        </div>
        <button
          type="button"
          className="refresh-button"
          title="목록 새로고침"
          disabled={inquiryQuery.isFetching}
          onClick={() => void inquiryQuery.refetch()}
        >
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
          <strong>{inquiryQuery.data?.totalElements ?? 0}건</strong>
        </div>

        <InquiryFilterBar
          status={status}
          searchField={searchField}
          query={query}
          onStatusChange={(nextStatus) => setFilters({ status: nextStatus })}
          onSearch={(field, nextQuery) => setFilters({ field, query: nextQuery })}
          onReset={() => setSearchParams({}, { replace: true })}
        />

        {inquiryQuery.isPending ? (
          <div className="inquiry-empty-state" aria-live="polite">
            <strong>문의 목록을 불러오고 있습니다.</strong>
          </div>
        ) : inquiryQuery.isError ? (
          <div className="inquiry-empty-state" role="alert">
            <strong>문의 목록을 불러오지 못했습니다.</strong>
            <button type="button" onClick={() => void inquiryQuery.refetch()}>
              다시 시도
            </button>
          </div>
        ) : inquiries.length > 0 ? (
          <div className="inquiry-table-scroll">
            <table className="inquiry-table">
              <thead>
                <tr>
                  <th>상태</th>
                  <th>문의</th>
                  <th>사용자 UUID</th>
                  <th>학번</th>
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
                    <td><code>{inquiry.userId ?? '-'}</code></td>
                    <td>{inquiry.studentCode ?? '-'}</td>
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
          <nav className="pagination" aria-label="페이지 이동">
            <button
              type="button"
              disabled={page <= 0}
              aria-label="이전 페이지"
              onClick={() => setSearchParams((params) => {
                params.set('page', String(page - 1))
                return params
              })}
            >
              <ChevronLeft aria-hidden="true" size={18} />
            </button>
            <span>{page + 1} / {inquiryQuery.data?.totalPages ?? 1}</span>
            <button
              type="button"
              disabled={page + 1 >= (inquiryQuery.data?.totalPages ?? 1)}
              aria-label="다음 페이지"
              onClick={() => setSearchParams((params) => {
                params.set('page', String(page + 1))
                return params
              })}
            >
              <ChevronRight aria-hidden="true" size={18} />
            </button>
          </nav>
        </footer>
      </section>
    </div>
  )
}
