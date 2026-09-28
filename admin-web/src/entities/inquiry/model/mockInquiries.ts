import type { InquiryDetail, InquirySummary } from './types'

export const mockInquirySummaries: InquirySummary[] = [
  {
    reportId: 1042,
    status: 'PENDING',
    category: 'PORTAL_CONNECTION',
    title: '포털 연동 중 계정 잠금 오류가 발생합니다.',
    userId: 1842,
    studentCode: '202012345',
    errorCode: 'PORTAL_ACCOUNT_LOCKED',
    createdAt: '2026-09-28T14:42:00+09:00',
  },
  {
    reportId: 1041,
    status: 'ANSWERED',
    category: 'GRADUATION_REQUIREMENT',
    title: '졸업 요건의 전공 학점이 다르게 보여요.',
    userId: 1724,
    studentCode: '202112078',
    errorCode: null,
    createdAt: '2026-09-28T11:16:00+09:00',
  },
  {
    reportId: 1040,
    status: 'PENDING',
    category: 'ACADEMIC_RECORD',
    title: '이번 학기 수강 과목이 반영되지 않았습니다.',
    userId: 1661,
    studentCode: '202209411',
    errorCode: 'SCRAPE_RESULT_NOT_FOUND',
    createdAt: '2026-09-27T19:08:00+09:00',
  },
  {
    reportId: 1039,
    status: 'ANSWERED',
    category: 'ACCOUNT',
    title: '카카오 계정을 변경한 뒤 로그인이 되지 않습니다.',
    userId: 1518,
    studentCode: null,
    errorCode: 'SOCIAL_ACCOUNT_CONFLICT',
    createdAt: '2026-09-27T13:35:00+09:00',
  },
  {
    reportId: 1038,
    status: 'PENDING',
    category: 'ETC',
    title: '복수전공 정보 수정 방법이 궁금합니다.',
    userId: 1495,
    studentCode: '202010982',
    errorCode: null,
    createdAt: '2026-09-26T17:21:00+09:00',
  },
  {
    reportId: 1037,
    status: 'ANSWERED',
    category: 'PORTAL_CONNECTION',
    title: '포털 비밀번호 변경 후 연동에 실패합니다.',
    userId: 1380,
    studentCode: '202310024',
    errorCode: 'PORTAL_LOGIN_FAILED',
    createdAt: '2026-09-26T09:04:00+09:00',
  },
]

const detailContent: Record<number, string> = {
  1042: '포털 비밀번호를 여러 번 확인한 뒤 다시 연동했지만 계정 잠금 오류가 반복됩니다. 학교 포털에서는 정상적으로 로그인할 수 있습니다.',
  1041: '졸업 요건 화면의 전공 학점과 학교 포털에서 확인한 학점이 서로 다릅니다. 어떤 정보를 기준으로 확인해야 하나요?',
  1040: '이번 학기에 수강 중인 전공 과목 두 개가 시간표와 학적 정보에 표시되지 않습니다.',
  1039: '카카오 계정을 변경한 뒤 기존 학적 정보가 연결되지 않고 로그인도 완료되지 않습니다.',
  1038: '복수전공 승인이 완료됐는데 앱에는 이전 전공만 보입니다. 직접 수정할 수 있는 방법이 궁금합니다.',
  1037: '학교 포털 비밀번호를 변경한 뒤 척척학사에서 다시 연동하려고 하면 로그인 실패 메시지가 표시됩니다.',
}

export const mockInquiryDetails: InquiryDetail[] = mockInquirySummaries.map(
  (inquiry) => ({
    ...inquiry,
    content: detailContent[inquiry.reportId],
    academicSnapshot: {
      universityName: '척척대학교',
      departmentName:
        inquiry.category === 'GRADUATION_REQUIREMENT' ? '컴퓨터공학과' : '경영학과',
      grade: 3,
      semester: 2,
    },
    answer:
      inquiry.status === 'ANSWERED'
        ? {
            content: '문의 내용을 확인했습니다. 반영 시점과 계정 상태를 점검한 뒤 안내드린 절차에 따라 다시 시도해 주세요.',
            answeredAt: '2026-09-28T15:10:00+09:00',
            answeredBy: {
              adminAccountId: 7,
              displayName: '김척척',
            },
          }
        : null,
  }),
)
