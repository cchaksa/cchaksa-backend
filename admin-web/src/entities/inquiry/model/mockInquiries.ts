import type { InquiryDetail, InquirySummary } from './types'

export const mockInquiryDetails: InquiryDetail[] = [
  {
    reportId: '8f0d5c40-7a87-4cc5-a0ef-4e165f4d5e41',
    status: 'PENDING',
    title: '포털 연동 중 계정 잠금 오류가 발생합니다.',
    submittedUserId: '7f6d4516-6e8c-43c0-99f4-f6fe4daf47b8',
    studentCode: '202012345',
    createdAt: '2026-09-28T14:42:00+09:00',
    content:
      '포털 비밀번호를 여러 번 확인한 뒤 다시 연동했지만 계정 잠금 오류가 반복됩니다. 학교 포털에서는 정상적으로 로그인할 수 있습니다.',
    academicSnapshot: {
      department: '소프트웨어융합대학',
      primaryMajor: '컴퓨터공학과',
      secondaryMajor: null,
      isTransferStudent: false,
      admissionYear: 2020,
      graduationRequirementStatus: 'IN_PROGRESS',
    },
    answer: null,
  },
  {
    reportId: 'aa2a4022-05b4-48e1-93d2-ea058c939b0e',
    status: 'ANSWERED',
    title: '졸업 요건의 전공 학점이 다르게 보여요.',
    submittedUserId: '9234b1c9-da7a-47f0-b8bc-09c12b380467',
    studentCode: '202112078',
    createdAt: '2026-09-28T11:16:00+09:00',
    content:
      '졸업 요건 화면의 전공 학점과 학교 포털에서 확인한 학점이 서로 다릅니다. 어떤 정보를 기준으로 확인해야 하나요?',
    academicSnapshot: {
      department: '소프트웨어융합대학',
      primaryMajor: '컴퓨터공학과',
      secondaryMajor: '경영학과',
      isTransferStudent: false,
      admissionYear: 2021,
      graduationRequirementStatus: 'IN_PROGRESS',
    },
    answer: {
      content:
        '문의 내용을 확인했습니다. 반영 시점과 계정 상태를 점검한 뒤 안내드린 절차에 따라 다시 시도해 주세요.',
      answeredAt: '2026-09-28T15:10:00+09:00',
      answeredBy: {
        adminAccountId: '2d577d85-53d9-45a2-8b4e-c06be5975710',
        displayName: '김척척',
      },
    },
  },
  {
    reportId: '71ac507f-2403-4038-8816-bca4b79ca29f',
    status: 'PENDING',
    title: '이번 학기 수강 과목이 반영되지 않았습니다.',
    submittedUserId: '603720e7-ee8b-4af8-b095-3e2755d3e710',
    studentCode: '202209411',
    createdAt: '2026-09-27T19:08:00+09:00',
    content:
      '이번 학기에 수강 중인 전공 과목 두 개가 시간표와 학적 정보에 표시되지 않습니다.',
    academicSnapshot: {
      department: '경상대학',
      primaryMajor: '경영학과',
      secondaryMajor: null,
      isTransferStudent: null,
      admissionYear: 2022,
      graduationRequirementStatus: null,
    },
    answer: null,
  },
  {
    reportId: '27ed7854-52b4-4f98-8918-4e94619f56af',
    status: 'ANSWERED',
    title: '카카오 계정을 변경한 뒤 로그인이 되지 않습니다.',
    submittedUserId: '5689fd5c-cfc9-48cd-a8d0-c29d02fa8051',
    studentCode: null,
    createdAt: '2026-09-27T13:35:00+09:00',
    content:
      '카카오 계정을 변경한 뒤 기존 학적 정보가 연결되지 않고 로그인도 완료되지 않습니다.',
    academicSnapshot: {
      department: null,
      primaryMajor: null,
      secondaryMajor: null,
      isTransferStudent: null,
      admissionYear: null,
      graduationRequirementStatus: null,
    },
    answer: {
      content: '등록된 계정 정보를 확인했습니다. 관리자 안내에 따라 다시 로그인해 주세요.',
      answeredAt: '2026-09-27T15:02:00+09:00',
      answeredBy: {
        adminAccountId: '6dd9b769-fe2f-478f-9cf4-898e1629efae',
        displayName: '이학사',
      },
    },
  },
]

export const mockInquirySummaries: InquirySummary[] = mockInquiryDetails.map(
  (inquiry) => ({
    reportId: inquiry.reportId,
    status: inquiry.status,
    title: inquiry.title,
    submittedUserId: inquiry.submittedUserId,
    studentCode: inquiry.studentCode,
    createdAt: inquiry.createdAt,
  }),
)
