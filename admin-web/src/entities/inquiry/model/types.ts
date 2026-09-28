export type InquiryStatus = 'PENDING' | 'ANSWERED'

export type InquiryCategory =
  | 'PORTAL_CONNECTION'
  | 'GRADUATION_REQUIREMENT'
  | 'ACADEMIC_RECORD'
  | 'ACCOUNT'
  | 'ETC'

export interface InquirySummary {
  reportId: number
  status: InquiryStatus
  category: InquiryCategory
  title: string
  userId: number
  studentCode: string | null
  errorCode: string | null
  createdAt: string
}
