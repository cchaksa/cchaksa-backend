export const routes = {
  login: '/login',
  inquiries: '/inquiries',
  inquiryDetail: (reportId: number | string) => `/inquiries/${reportId}`,
} as const
