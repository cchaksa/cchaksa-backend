import { useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router'
import {
  cancelAdminSignIn,
  useAdminSignInCallback,
} from '../../../entities/admin-session'
import cchaksaLogo from '../../../shared/assets/cchaksa-logo.png'
import { routes } from '../../../shared/config/routes'
import '../../login/ui/login-page.css'

export function KakaoSignInCallbackPage() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const signIn = useAdminSignInCallback()
  const authorizationCode = searchParams.get('code')
  const state = searchParams.get('state')
  const providerError = searchParams.has('error')
  const invalidCallback = providerError || !authorizationCode || !state

  useEffect(() => {
    if (invalidCallback) {
      cancelAdminSignIn()
      return
    }

    let cancelled = false
    void Promise.resolve().then(() => {
      if (cancelled) return
      signIn.mutate(
        { authorizationCode, state },
        {
          onSuccess: () => navigate(routes.inquiries, { replace: true }),
        },
      )
    })

    return () => {
      cancelled = true
    }
  }, [authorizationCode, invalidCallback, navigate, signIn.mutate, state])

  const hasError = invalidCallback || signIn.isError

  return (
    <main className="login-page">
      <section className="login-panel" aria-labelledby="login-callback-title">
        <div className="login-brand">
          <img src={cchaksaLogo} alt="척척학사" />
          <div>
            <p>관리자 콘솔</p>
            <h1 id="login-callback-title">척척학사</h1>
          </div>
        </div>

        <div className="login-copy" aria-live="polite">
          <h2>{hasError ? '로그인 실패' : '로그인 확인 중'}</h2>
          <p>
            {hasError
              ? '카카오 로그인을 완료하지 못했습니다. 다시 시도해 주세요.'
              : '관리자 계정과 권한을 확인하고 있습니다.'}
          </p>
        </div>

        {hasError && (
          <button
            className="kakao-sign-in-button"
            type="button"
            onClick={() => navigate(routes.login, { replace: true })}
          >
            로그인으로 돌아가기
          </button>
        )}
      </section>
    </main>
  )
}
