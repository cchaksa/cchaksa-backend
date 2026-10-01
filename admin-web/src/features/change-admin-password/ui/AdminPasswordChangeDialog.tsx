import { X } from 'lucide-react'
import {
  type FormEvent,
  type RefObject,
  useEffect,
  useRef,
  useState,
} from 'react'
import { useAdminPasswordChange } from '../../../entities/admin-session'
import { ApiError } from '../../../shared/api/http'
import './admin-password-change-dialog.css'

interface AdminPasswordChangeDialogProps {
  isOpen: boolean
  onClose: () => void
  returnFocusRef: RefObject<HTMLButtonElement | null>
}

function isBlank(value: string) {
  return value.trim().length === 0
}

function passwordErrorMessage(error: Error | null) {
  if (error instanceof ApiError && error.code === 'A14') {
    return '기존 비밀번호가 일치하지 않습니다.'
  }
  if (error instanceof ApiError && error.code === 'A15') {
    return '기존 비밀번호와 다른 비밀번호를 입력해 주세요.'
  }
  if (error instanceof ApiError && error.code === 'C01') {
    return '입력한 비밀번호를 확인해 주세요.'
  }
  if (error instanceof ApiError && error.code === 'C04') {
    return '요청을 확인할 수 없습니다. 페이지를 새로고침해 주세요.'
  }
  return '비밀번호를 변경하지 못했습니다. 잠시 후 다시 시도해 주세요.'
}

export function AdminPasswordChangeDialog({
  isOpen,
  onClose,
  returnFocusRef,
}: AdminPasswordChangeDialogProps) {
  const dialogRef = useRef<HTMLDialogElement>(null)
  const currentPasswordRef = useRef<HTMLInputElement>(null)
  const changePassword = useAdminPasswordChange()
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [validationError, setValidationError] = useState<string | null>(null)
  const [successMessage, setSuccessMessage] = useState<string | null>(null)

  const clearSensitiveFields = () => {
    setCurrentPassword('')
    setNewPassword('')
    setConfirmation('')
  }

  useEffect(() => {
    const dialog = dialogRef.current
    if (!dialog) return

    if (isOpen && !dialog.open) {
      dialog.showModal()
      currentPasswordRef.current?.focus()
    } else if (!isOpen && dialog.open) {
      dialog.close()
    }
  }, [isOpen])

  const closeDialog = () => {
    if (changePassword.isPending) return
    clearSensitiveFields()
    setValidationError(null)
    setSuccessMessage(null)
    changePassword.reset()
    onClose()
    requestAnimationFrame(() => returnFocusRef.current?.focus())
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setSuccessMessage(null)
    if (
      isBlank(currentPassword) ||
      isBlank(newPassword) ||
      isBlank(confirmation)
    ) {
      setValidationError('모든 비밀번호 입력란을 작성해 주세요.')
      return
    }
    if (newPassword !== confirmation) {
      setValidationError('변경 비밀번호가 서로 일치하지 않습니다.')
      return
    }

    setValidationError(null)
    try {
      await changePassword.execute({ currentPassword, newPassword })
      clearSensitiveFields()
      setSuccessMessage('비밀번호를 변경했습니다.')
      currentPasswordRef.current?.focus()
    } catch {
      clearSensitiveFields()
      currentPasswordRef.current?.focus()
    }
  }

  return (
    <dialog
      ref={dialogRef}
      className="password-dialog"
      aria-labelledby="password-dialog-title"
      aria-describedby="password-dialog-description"
      onCancel={(event) => {
        event.preventDefault()
        closeDialog()
      }}
      onClick={(event) => {
        if (event.target === event.currentTarget) closeDialog()
      }}
      onKeyDown={(event) => {
        if (event.key === 'Escape') closeDialog()
      }}
    >
      <div className="password-dialog-panel">
        <div className="password-dialog-header">
          <div>
            <h2 id="password-dialog-title">비밀번호 변경</h2>
            <p id="password-dialog-description">
              변경 후 다른 기기의 관리자 세션은 종료됩니다.
            </p>
          </div>
          <button
            type="button"
            className="password-dialog-close"
            aria-label="비밀번호 변경 닫기"
            disabled={changePassword.isPending}
            onClick={closeDialog}
          >
            <X aria-hidden="true" size={20} />
          </button>
        </div>

        <form className="password-dialog-form" onSubmit={handleSubmit} noValidate>
          <label htmlFor="current-admin-password">기존 비밀번호</label>
          <input
            ref={currentPasswordRef}
            id="current-admin-password"
            type="password"
            autoComplete="current-password"
            maxLength={256}
            value={currentPassword}
            disabled={changePassword.isPending}
            onChange={(event) => setCurrentPassword(event.target.value)}
          />

          <label htmlFor="new-admin-password">변경 비밀번호</label>
          <input
            id="new-admin-password"
            type="password"
            autoComplete="new-password"
            maxLength={256}
            value={newPassword}
            disabled={changePassword.isPending}
            onChange={(event) => setNewPassword(event.target.value)}
          />

          <label htmlFor="confirm-admin-password">변경 비밀번호 재확인</label>
          <input
            id="confirm-admin-password"
            type="password"
            autoComplete="new-password"
            maxLength={256}
            value={confirmation}
            disabled={changePassword.isPending}
            onChange={(event) => setConfirmation(event.target.value)}
          />

          {(validationError || changePassword.error) && (
            <p className="password-dialog-error" role="alert">
              {validationError ?? passwordErrorMessage(changePassword.error)}
            </p>
          )}
          {successMessage && (
            <p className="password-dialog-success" role="status">
              {successMessage}
            </p>
          )}

          <div className="password-dialog-actions">
            <button type="button" onClick={closeDialog}>
              취소
            </button>
            <button type="submit" disabled={changePassword.isPending}>
              {changePassword.isPending ? '변경 중' : '변경'}
            </button>
          </div>
        </form>
      </div>
    </dialog>
  )
}
