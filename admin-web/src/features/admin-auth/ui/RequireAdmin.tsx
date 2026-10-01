import { Navigate, Outlet } from 'react-router'
import { useAdminSession } from '../../../entities/admin-session'
import { ApiError } from '../../../shared/api/http'
import { routes } from '../../../shared/config/routes'

export function RequireAdmin() {
  const session = useAdminSession()

  if (session.isPending) {
    return <main className="route-status">관리자 세션을 확인하고 있습니다.</main>
  }

  if (
    session.error instanceof ApiError &&
    [401, 403].includes(session.error.status)
  ) {
    return (
      <Navigate
        to={routes.login}
        replace
        state={
          session.error.code === 'A05' ? { sessionExpired: true } : undefined
        }
      />
    )
  }

  if (session.isError) {
    return (
      <main className="route-status" role="alert">
        <strong>관리자 세션을 확인하지 못했습니다.</strong>
        <button type="button" onClick={() => void session.refetch()}>
          다시 시도
        </button>
      </main>
    )
  }

  return <Outlet />
}
