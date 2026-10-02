import { ArrowLeft, CircleAlert } from 'lucide-react'
import { Link, useParams } from 'react-router'
import {
  InquiryAnswerPanel,
  useAnswerInquiry,
  useInquiryDetail,
} from '../../../entities/inquiry'
import { AnswerInquiryForm } from '../../../features/answer-inquiry'
import { routes } from '../../../shared/config/routes'
import './inquiry-detail-page.css'

const dateFormatter = new Intl.DateTimeFormat('ko-KR', {
  dateStyle: 'medium',
  timeStyle: 'short',
})

export function InquiryDetailPage() {
  const { reportId } = useParams()
  const resolvedReportId = reportId ?? ''
  const inquiryQuery = useInquiryDetail(resolvedReportId)
  const answerMutation = useAnswerInquiry(resolvedReportId)
  const inquiry = inquiryQuery.data

  if (inquiryQuery.isPending && resolvedReportId) {
    return <div className="detail-state">문의 내용을 불러오고 있습니다.</div>
  }

  if (inquiryQuery.isError) {
    return (
      <div className="detail-state" role="alert">
        <strong>문의 내용을 불러오지 못했습니다.</strong>
        <button type="button" onClick={() => void inquiryQuery.refetch()}>
          다시 시도
        </button>
      </div>
    )
  }

  if (!inquiry) {
    return (
      <div className="inquiry-not-found">
        <CircleAlert aria-hidden="true" size={30} />
        <h1>문의를 찾을 수 없습니다.</h1>
        <Link to={routes.inquiries}>문의 목록으로 돌아가기</Link>
      </div>
    )
  }

  return (
    <div className="inquiry-detail-page">
      <Link className="back-link" to={routes.inquiries}>
        <ArrowLeft aria-hidden="true" size={18} />
        문의 목록
      </Link>

      <article className="inquiry-article">
        <header className="inquiry-article-header">
          <div className="inquiry-article-status">
            <span className={`status-badge status-${inquiry.status.toLowerCase()}`}>
              {inquiry.status === 'PENDING' ? '답변 필요' : '답변 완료'}
            </span>
            <span>문의 #{inquiry.reportId}</span>
          </div>
          <h1>{inquiry.title}</h1>
          <p>{dateFormatter.format(new Date(inquiry.createdAt))}</p>
        </header>

        <div className="inquiry-article-body">
          <p>{inquiry.content}</p>
        </div>

        <dl className="inquiry-metadata">
          <div>
            <dt>사용자 UUID</dt>
            <dd><code>{inquiry.submitter.submittedUserId ?? '-'}</code></dd>
          </div>
          <div>
            <dt>학번</dt>
            <dd>{inquiry.submitter.studentCode ?? '-'}</dd>
          </div>
          <div>
            <dt>소속 학과</dt>
            <dd>{inquiry.submitter.departmentName ?? '-'}</dd>
          </div>
          <div>
            <dt>주전공</dt>
            <dd>{inquiry.submitter.primaryMajorName ?? '-'}</dd>
          </div>
          <div>
            <dt>복수전공</dt>
            <dd>{inquiry.submitter.secondaryMajorName ?? '-'}</dd>
          </div>
          <div>
            <dt>편입 여부</dt>
            <dd>
              {inquiry.submitter.transferStudent === null
                ? '-'
                : inquiry.submitter.transferStudent
                  ? '편입'
                  : '일반'}
            </dd>
          </div>
          <div>
            <dt>입학 연도</dt>
            <dd>{inquiry.submitter.admissionYear ?? '-'}</dd>
          </div>
          <div>
            <dt>졸업요건 상태</dt>
            <dd>{inquiry.submitter.graduationRequirementStatus ?? '-'}</dd>
          </div>
        </dl>
      </article>

      {inquiry.status === 'PENDING' ? (
        <AnswerInquiryForm
          isSubmitting={answerMutation.isPending}
          errorMessage={
            answerMutation.isError
              ? '답변을 등록하지 못했습니다. 문의 상태를 확인한 뒤 다시 시도해 주세요.'
              : undefined
          }
          onSubmit={(answer) => answerMutation.mutateAsync(answer).then(() => undefined)}
        />
      ) : inquiry.answer ? (
        <InquiryAnswerPanel answer={inquiry.answer} />
      ) : null}
    </div>
  )
}
