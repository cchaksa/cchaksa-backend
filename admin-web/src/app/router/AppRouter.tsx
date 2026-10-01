import { Navigate, Route, BrowserRouter, Routes } from 'react-router'
import {
  AdminSessionExpiryCoordinator,
  RequireAdmin,
} from '../../features/admin-auth'
import { InquiryDetailPage } from '../../pages/inquiry-detail'
import { InquiryListPage } from '../../pages/inquiries'
import { LoginPage } from '../../pages/login'
import { routes } from '../../shared/config/routes'
import { AdminShell } from '../../widgets/admin-shell'

export function AppRouter() {
  return (
    <BrowserRouter>
      <AdminSessionExpiryCoordinator />
      <Routes>
        <Route path={routes.login} element={<LoginPage />} />
        <Route element={<RequireAdmin />}>
          <Route element={<AdminShell />}>
            <Route index element={<Navigate to={routes.inquiries} replace />} />
            <Route path={routes.inquiries} element={<InquiryListPage />} />
            <Route
              path={`${routes.inquiries}/:reportId`}
              element={<InquiryDetailPage />}
            />
          </Route>
        </Route>
        <Route path="*" element={<Navigate to={routes.inquiries} replace />} />
      </Routes>
    </BrowserRouter>
  )
}
