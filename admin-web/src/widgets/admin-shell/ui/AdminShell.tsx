import {
  ChevronDown,
  KeyRound,
  LogOut,
  Menu,
  MessageSquareText,
  X,
} from 'lucide-react'
import { useEffect, useRef, useState } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router'
import { useAdminSession, useAdminSignOut } from '../../../entities/admin-session'
import { AdminPasswordChangeDialog } from '../../../features/change-admin-password'
import cchaksaLogo from '../../../shared/assets/cchaksa-logo.png'
import { routes } from '../../../shared/config/routes'
import './admin-shell.css'

export function AdminShell() {
  const [isMenuOpen, setIsMenuOpen] = useState(false)
  const [isProfileOpen, setIsProfileOpen] = useState(false)
  const [isPasswordDialogOpen, setIsPasswordDialogOpen] = useState(false)
  const profileAreaRef = useRef<HTMLDivElement>(null)
  const profileButtonRef = useRef<HTMLButtonElement>(null)
  const passwordMenuItemRef = useRef<HTMLButtonElement>(null)
  const navigate = useNavigate()
  const session = useAdminSession()
  const signOut = useAdminSignOut()
  const displayName = session.data?.displayName ?? '관리자'
  const roleLabel = session.data?.adminRole === 'ADMIN' ? 'Admin' : 'CS Agent'
  const initials = displayName.slice(0, 2).toUpperCase()

  useEffect(() => {
    if (!isProfileOpen) return
    passwordMenuItemRef.current?.focus()
    const closeOnPointerDown = (event: PointerEvent) => {
      if (!profileAreaRef.current?.contains(event.target as Node)) {
        setIsProfileOpen(false)
      }
    }
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setIsProfileOpen(false)
        profileButtonRef.current?.focus()
      }
    }
    document.addEventListener('pointerdown', closeOnPointerDown)
    document.addEventListener('keydown', closeOnEscape)
    return () => {
      document.removeEventListener('pointerdown', closeOnPointerDown)
      document.removeEventListener('keydown', closeOnEscape)
    }
  }, [isProfileOpen])

  const handleSignOut = async () => {
    setIsProfileOpen(false)
    try {
      const signedOut = await signOut.execute()
      if (signedOut) navigate(routes.login, { replace: true })
    } catch {
      // The menu displays the sanitized command error below.
    }
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
          <div ref={profileAreaRef} className="admin-profile-area">
            <button
              ref={profileButtonRef}
              type="button"
              className="admin-profile"
              aria-expanded={isProfileOpen}
              aria-controls="admin-profile-menu"
              onClick={() => setIsProfileOpen((value) => !value)}
            >
              <span className="admin-avatar">{initials}</span>
              <span className="admin-profile-copy">
                <strong>{displayName}</strong>
                <small>{roleLabel}</small>
              </span>
              <ChevronDown
                className="admin-profile-chevron"
                aria-hidden="true"
                size={16}
              />
            </button>
            {isProfileOpen && (
              <nav
                id="admin-profile-menu"
                className="admin-profile-menu"
                aria-label="관리자 계정 메뉴"
              >
                <button
                  ref={passwordMenuItemRef}
                  type="button"
                  onClick={() => {
                    setIsProfileOpen(false)
                    setIsPasswordDialogOpen(true)
                  }}
                >
                  <KeyRound aria-hidden="true" size={17} />
                  비밀번호 변경
                </button>
                <button
                  type="button"
                  disabled={signOut.isPending}
                  onClick={() => void handleSignOut()}
                >
                  <LogOut aria-hidden="true" size={17} />
                  {signOut.isPending ? '로그아웃 중' : '로그아웃'}
                </button>
                {signOut.error && (
                  <p className="profile-menu-error" role="alert">
                    로그아웃하지 못했습니다.
                  </p>
                )}
              </nav>
            )}
          </div>
        </header>

        {signOut.error && (
          <p className="admin-command-error" role="alert">
            로그아웃하지 못했습니다. 잠시 후 다시 시도해 주세요.
          </p>
        )}

        <main className="admin-content">
          <Outlet />
        </main>
      </div>

      <AdminPasswordChangeDialog
        isOpen={isPasswordDialogOpen}
        returnFocusRef={profileButtonRef}
        onClose={() => setIsPasswordDialogOpen(false)}
      />
    </div>
  )
}
