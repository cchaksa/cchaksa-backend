import { MessageCircle } from 'lucide-react'
import { useNavigate } from 'react-router'
import { useAdminSignIn } from '../../../entities/admin-session'
import { routes } from '../../../shared/config/routes'
import './kakao-sign-in-button.css'

export function KakaoSignInButton() {
  const navigate = useNavigate()
  const signIn = useAdminSignIn()

  const handleSignIn = () => {
    signIn.mutate(undefined, {
      onSuccess: () => navigate(routes.inquiries, { replace: true }),
    })
  }

  return (
    <div>
      <button
        className="kakao-sign-in-button"
        type="button"
        disabled={signIn.isPending}
        onClick={handleSignIn}
      >
        <MessageCircle aria-hidden="true" size={20} strokeWidth={2.2} />
        <span>{signIn.isPending ? '로그인 준비 중' : '카카오로 로그인'}</span>
      </button>
      {signIn.isError && (
        <p className="kakao-sign-in-error" role="alert">
          로그인을 시작하지 못했습니다. 잠시 후 다시 시도해 주세요.
        </p>
      )}
    </div>
  )
}
