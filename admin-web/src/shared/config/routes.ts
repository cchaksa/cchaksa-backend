export const routes = {
  login: '/login',
  inquiries: '/inquiries',
  inquiryDetail: (reportId: string) => `/inquiries/${reportId}`,
} as const
