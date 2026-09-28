import { Send } from 'lucide-react'
import { useState } from 'react'
import './answer-inquiry-form.css'

const MAX_ANSWER_LENGTH = 2000

interface AnswerInquiryFormProps {
  onSubmit: (answer: string) => void
}

export function AnswerInquiryForm({ onSubmit }: AnswerInquiryFormProps) {
  const [answer, setAnswer] = useState('')
  const normalizedAnswer = answer.trim()

  return (
    <section className="answer-composer" aria-labelledby="answer-title">
      <div className="answer-composer-heading">
        <div>
          <p>CS 답변</p>
          <h2 id="answer-title">답변 작성</h2>
        </div>
        <span>답변 등록 후에는 수정할 수 없습니다.</span>
      </div>

      <label htmlFor="inquiry-answer">답변 내용</label>
      <textarea
        id="inquiry-answer"
        value={answer}
        maxLength={MAX_ANSWER_LENGTH}
        onChange={(event) => setAnswer(event.target.value)}
        placeholder="사용자에게 전달할 답변을 입력해 주세요."
        rows={8}
      />

      <div className="answer-composer-footer">
        <span aria-live="polite">
          {answer.length.toLocaleString()} / {MAX_ANSWER_LENGTH.toLocaleString()}자
        </span>
        <button
          type="button"
          disabled={!normalizedAnswer}
          onClick={() => onSubmit(normalizedAnswer)}
        >
          <Send aria-hidden="true" size={17} />
          답변 등록
        </button>
      </div>
    </section>
  )
}
