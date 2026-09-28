import { Navigate, Route, BrowserRouter, Routes } from 'react-router'
import { LoginPage } from '../../pages/login'
import { routes } from '../../shared/config/routes'

export function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path={routes.login} element={<LoginPage />} />
        <Route path="*" element={<Navigate to={routes.login} replace />} />
      </Routes>
    </BrowserRouter>
  )
}
