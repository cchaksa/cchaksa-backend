export type InquiryStatus = 'PENDING' | 'ANSWERED'

export interface InquirySummary {
  reportId: string
  status: InquiryStatus
  title: string
  submittedUserId: string | null
  studentCode: string | null
  createdAt: string
}

export interface InquiryAnswer {
  content: string
  answeredAt: string
  answeredBy: {
    adminAccountId: string
    displayName: string
  }
}

export interface InquiryDetail extends InquirySummary {
  content: string
  academicSnapshot: {
    department: string | null
    primaryMajor: string | null
    secondaryMajor: string | null
    isTransferStudent: boolean | null
    admissionYear: number | null
    graduationRequirementStatus: string | null
  }
  answer: InquiryAnswer | null
}
