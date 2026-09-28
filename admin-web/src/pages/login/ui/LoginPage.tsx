import { KakaoSignInButton } from '../../../features/admin-auth'
import cchaksaLogo from '../../../shared/assets/cchaksa-logo.png'
import './login-page.css'

export function LoginPage() {
  return (
    <main className="login-page">
      <section className="login-panel" aria-labelledby="login-title">
        <div className="login-brand">
          <img src={cchaksaLogo} alt="척척학사" />
          <div>
            <p>관리자 콘솔</p>
            <h1 id="login-title">척척학사</h1>
          </div>
        </div>

        <div className="login-copy">
          <h2>관리자 로그인</h2>
          <p>등록된 관리자 카카오 계정으로 로그인해 주세요.</p>
        </div>

        <KakaoSignInButton />

        <p className="login-notice">
          허용된 관리자 계정만 접근할 수 있습니다.
        </p>
      </section>
    </main>
  )
}
