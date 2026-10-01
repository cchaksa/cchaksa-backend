import { type FormEvent, useState } from 'react'
import { useNavigate } from 'react-router'
import {
  useAdminCsrfBootstrap,
  useAdminSignIn,
} from '../../../entities/admin-session'
import { ApiError } from '../../../shared/api/http'
import { routes } from '../../../shared/config/routes'
import './admin-sign-in-form.css'

function isBlank(value: string) {
  return value.trim().length === 0
}

function signInErrorMessage(error: Error | null) {
  if (error instanceof ApiError && error.code === 'A13') {
    return '아이디 또는 비밀번호를 확인해 주세요.'
  }
  if (error instanceof ApiError && error.code === 'C01') {
    return '입력한 로그인 정보를 확인해 주세요.'
  }
  return '로그인하지 못했습니다. 잠시 후 다시 시도해 주세요.'
}

export function AdminSignInForm() {
  const navigate = useNavigate()
  const csrf = useAdminCsrfBootstrap()
  const signIn = useAdminSignIn()
  const [loginId, setLoginId] = useState('')
  const [password, setPassword] = useState('')
  const [validationError, setValidationError] = useState<string | null>(null)

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (isBlank(loginId) || isBlank(password)) {
      setValidationError('아이디와 비밀번호를 모두 입력해 주세요.')
      return
    }
    setValidationError(null)
    try {
      const session = await signIn.execute({ loginId, password })
      setLoginId('')
      setPassword('')
      if (session) navigate(routes.inquiries, { replace: true })
    } catch {
      setLoginId('')
      setPassword('')
    }
  }

  if (csrf.error) {
    return (
      <div className="sign-in-preparation" role="alert">
        <p>로그인 보안 정보를 준비하지 못했습니다.</p>
        <button type="button" onClick={csrf.retry}>
          다시 시도
        </button>
      </div>
    )
  }

  return (
    <form className="admin-sign-in-form" onSubmit={handleSubmit} noValidate>
      <label htmlFor="admin-login-id">아이디</label>
      <input
        id="admin-login-id"
        name="loginId"
        type="text"
        autoComplete="username"
        maxLength={255}
        value={loginId}
        disabled={!csrf.isReady || signIn.isPending}
        onChange={(event) => setLoginId(event.target.value)}
      />

      <label htmlFor="admin-login-password">비밀번호</label>
      <input
        id="admin-login-password"
        name="password"
        type="password"
        autoComplete="current-password"
        maxLength={256}
        value={password}
        disabled={!csrf.isReady || signIn.isPending}
        onChange={(event) => setPassword(event.target.value)}
      />

      {(validationError || signIn.error) && (
        <p className="admin-sign-in-error" role="alert">
          {validationError ?? signInErrorMessage(signIn.error)}
        </p>
      )}

      <button type="submit" disabled={!csrf.isReady || signIn.isPending}>
        {csrf.isPending
          ? '로그인 준비 중'
          : signIn.isPending
            ? '로그인 중'
            : '로그인'}
      </button>
    </form>
  )
}
