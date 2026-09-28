import { MessageCircle } from 'lucide-react'
import { adminAuthConfig } from '../../../shared/config/adminAuth'
import './kakao-sign-in-button.css'

export function KakaoSignInButton() {
  return (
    <a className="kakao-sign-in-button" href={adminAuthConfig.signInPath}>
      <MessageCircle aria-hidden="true" size={20} strokeWidth={2.2} />
      <span>카카오로 로그인</span>
    </a>
  )
}
