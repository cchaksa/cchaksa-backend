export type InquiryStatus = 'PENDING' | 'ANSWERED'

export interface InquirySummary {
  reportId: string
  status: InquiryStatus
  title: string
  userId: string | null
  studentCode: string | null
  createdAt: string
  answeredAt: string | null
}

export interface InquirySubmitterSnapshot {
  submittedUserId: string | null
  departmentId: number | null
  departmentName: string | null
  studentCode: string | null
  primaryMajorId: number | null
  primaryMajorName: string | null
  secondaryMajorId: number | null
  secondaryMajorName: string | null
  transferStudent: boolean | null
  admissionYear: number | null
  graduationRequirementStatus: string | null
}

export interface InquiryAnswer {
  answer: string
  answeredAt: string
  adminAccountId: string
  adminDisplayName: string
}

export interface InquiryDetail {
  reportId: string
  status: InquiryStatus
  title: string
  content: string
  userId: string | null
  createdAt: string
  updatedAt: string
  submitter: InquirySubmitterSnapshot
  answer: InquiryAnswer | null
}

export interface InquiryAnswerResult {
  reportId: string
  status: 'ANSWERED'
  answeredAt: string
  adminAccountId: string
  adminDisplayName: string
  adminRole: 'ADMIN' | 'CS_AGENT'
}
