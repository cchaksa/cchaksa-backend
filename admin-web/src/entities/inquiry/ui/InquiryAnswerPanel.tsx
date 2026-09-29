import { CheckCircle2, UserRound } from 'lucide-react'
import type { InquiryAnswer } from '../model/types'
import './inquiry-answer-panel.css'

const dateFormatter = new Intl.DateTimeFormat('ko-KR', {
  dateStyle: 'medium',
  timeStyle: 'short',
})

interface InquiryAnswerPanelProps {
  answer: InquiryAnswer
}

export function InquiryAnswerPanel({ answer }: InquiryAnswerPanelProps) {
  return (
    <section className="inquiry-answer-panel" aria-labelledby="registered-answer-title">
      <header>
        <div className="registered-answer-title">
          <CheckCircle2 aria-hidden="true" size={20} />
          <div>
            <p>CS 답변</p>
            <h2 id="registered-answer-title">등록된 답변</h2>
          </div>
        </div>
        <div className="answer-audit">
          <UserRound aria-hidden="true" size={16} />
          <span>{answer.answeredBy.displayName}</span>
          <code>{answer.answeredBy.adminAccountId}</code>
          <time dateTime={answer.answeredAt}>
            {dateFormatter.format(new Date(answer.answeredAt))}
          </time>
        </div>
      </header>
      <p className="registered-answer-content">{answer.content}</p>
      <footer>답변이 완료된 문의에는 추가 답변을 등록할 수 없습니다.</footer>
    </section>
  )
}
