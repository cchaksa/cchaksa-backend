export const routes = {
  login: '/',
  legacyLogin: '/login',
  home: '/inquiries',
  inquiries: '/inquiries',
  inquiryDetail: (reportId: string) => `/inquiries/${reportId}`,
} as const
