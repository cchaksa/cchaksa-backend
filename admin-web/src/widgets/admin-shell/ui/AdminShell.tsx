import { ChevronDown, LogOut, Menu, MessageSquareText, X } from 'lucide-react'
import { useState } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router'
import { useAdminSession, useAdminSignOut } from '../../../entities/admin-session'
import cchaksaLogo from '../../../shared/assets/cchaksa-logo.png'
import { routes } from '../../../shared/config/routes'
import './admin-shell.css'

export function AdminShell() {
  const [isMenuOpen, setIsMenuOpen] = useState(false)
  const navigate = useNavigate()
  const session = useAdminSession()
  const signOut = useAdminSignOut()
  const displayName = session.data?.displayName ?? '관리자'
  const roleLabel = session.data?.adminRole === 'ADMIN' ? 'Admin' : 'CS Agent'
  const initials = displayName.slice(0, 2).toUpperCase()

  const handleSignOut = () => {
    signOut.mutate(undefined, {
      onSuccess: () => navigate(routes.login, { replace: true }),
    })
  }

  return (
    <div className="admin-layout">
      <aside className={`admin-sidebar ${isMenuOpen ? 'is-open' : ''}`}>
        <div className="sidebar-brand">
          <img src={cchaksaLogo} alt="" />
          <div>
            <strong>척척학사</strong>
            <span>ADMIN</span>
          </div>
          <button
            type="button"
            className="sidebar-close"
            aria-label="메뉴 닫기"
            onClick={() => setIsMenuOpen(false)}
          >
            <X aria-hidden="true" size={20} />
          </button>
        </div>

        <nav className="sidebar-nav" aria-label="관리자 메뉴">
          <p>고객 지원</p>
          <NavLink
            to={routes.inquiries}
            onClick={() => setIsMenuOpen(false)}
          >
            <MessageSquareText aria-hidden="true" size={19} />
            <span>문의</span>
          </NavLink>
        </nav>

        <button
          className="sidebar-logout"
          type="button"
          disabled={signOut.isPending}
          onClick={handleSignOut}
        >
          <LogOut aria-hidden="true" size={19} />
          <span>{signOut.isPending ? '로그아웃 중' : '로그아웃'}</span>
        </button>
      </aside>

      {isMenuOpen && (
        <button
          type="button"
          className="sidebar-backdrop"
          aria-label="메뉴 닫기"
          onClick={() => setIsMenuOpen(false)}
        />
      )}

      <div className="admin-main">
        <header className="admin-topbar">
          <button
            type="button"
            className="menu-button"
            aria-label="메뉴 열기"
            onClick={() => setIsMenuOpen(true)}
          >
            <Menu aria-hidden="true" size={22} />
          </button>
          <span className="topbar-context">관리자 콘솔</span>
          <button type="button" className="admin-profile">
            <span className="admin-avatar">{initials}</span>
            <span className="admin-profile-copy">
              <strong>{displayName}</strong>
              <small>{roleLabel}</small>
            </span>
            <ChevronDown aria-hidden="true" size={16} />
          </button>
        </header>

        <main className="admin-content">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
