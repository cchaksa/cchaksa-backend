import { Navigate, Route, BrowserRouter, Routes } from 'react-router'
import { InquiryListPage } from '../../pages/inquiries'
import { LoginPage } from '../../pages/login'
import { routes } from '../../shared/config/routes'
import { AdminShell } from '../../widgets/admin-shell'

export function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path={routes.login} element={<LoginPage />} />
        <Route element={<AdminShell />}>
          <Route index element={<Navigate to={routes.inquiries} replace />} />
          <Route path={routes.inquiries} element={<InquiryListPage />} />
        </Route>
        <Route path="*" element={<Navigate to={routes.inquiries} replace />} />
      </Routes>
    </BrowserRouter>
  )
}
