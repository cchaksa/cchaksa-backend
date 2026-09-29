export const routes = {
  login: '/login',
  loginCallback: '/login/callback',
  inquiries: '/inquiries',
  inquiryDetail: (reportId: string) => `/inquiries/${reportId}`,
} as const
