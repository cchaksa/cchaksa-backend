import { Search, X } from 'lucide-react'
import { type FormEvent, useEffect, useState } from 'react'
import type { InquirySearchField, InquiryStatusFilter } from '../model/types'
import './inquiry-filter-bar.css'

interface InquiryFilterBarProps {
  status: InquiryStatusFilter
  searchField: InquirySearchField
  query: string
  onStatusChange: (status: InquiryStatusFilter) => void
  onSearch: (searchField: InquirySearchField, query: string) => void
  onReset: () => void
}

const statusOptions: Array<{ label: string; value: InquiryStatusFilter }> = [
  { label: '전체', value: 'ALL' },
  { label: '답변 필요', value: 'PENDING' },
  { label: '답변 완료', value: 'ANSWERED' },
]

export function InquiryFilterBar({
  status,
  searchField,
  query,
  onStatusChange,
  onSearch,
  onReset,
}: InquiryFilterBarProps) {
  const [draftField, setDraftField] = useState(searchField)
  const [draftQuery, setDraftQuery] = useState(query)

  useEffect(() => {
    setDraftField(searchField)
    setDraftQuery(query)
  }, [query, searchField])

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    onSearch(draftField, draftQuery.trim())
  }

  return (
    <div className="inquiry-filter-bar">
      <div className="status-segments" aria-label="문의 답변 상태">
        {statusOptions.map((option) => (
          <button
            key={option.value}
            className={status === option.value ? 'is-active' : undefined}
            type="button"
            aria-pressed={status === option.value}
            onClick={() => onStatusChange(option.value)}
          >
            {option.label}
          </button>
        ))}
      </div>

      <form className="inquiry-search" role="search" onSubmit={handleSubmit}>
        <label className="sr-only" htmlFor="inquiry-search-field">
          검색 대상
        </label>
        <select
          id="inquiry-search-field"
          value={draftField}
          onChange={(event) =>
            setDraftField(event.target.value as InquirySearchField)
          }
        >
          <option value="ALL">통합 검색</option>
          <option value="USER_ID">사용자 ID</option>
          <option value="STUDENT_CODE">학번</option>
          <option value="ERROR_CODE">오류 코드</option>
        </select>

        <label className="sr-only" htmlFor="inquiry-search-query">
          검색어
        </label>
        <div className="inquiry-search-input">
          <Search aria-hidden="true" size={18} />
          <input
            id="inquiry-search-query"
            type="search"
            value={draftQuery}
            placeholder="사용자, 학번 또는 오류 코드 검색"
            onChange={(event) => setDraftQuery(event.target.value)}
          />
        </div>

        <button className="search-submit" type="submit">
          검색
        </button>
        {(status !== 'ALL' || query) && (
          <button
            className="search-reset"
            type="button"
            title="필터 초기화"
            aria-label="필터 초기화"
            onClick={onReset}
          >
            <X aria-hidden="true" size={18} />
          </button>
        )}
      </form>
    </div>
  )
}
