import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useState } from 'react'
import {
  beginAdminSessionTransition,
  type ApiError,
} from '../../../shared/api/http'
import { adminSessionApi } from '../api/adminSessionApi'
import type {
  AdminPasswordChangeInput,
  AdminSignInInput,
} from './types'

export const adminSessionQueryKey = ['admin-session'] as const

interface CommandState {
  isPending: boolean
  error: ApiError | Error | null
}

const idleCommandState: CommandState = {
  isPending: false,
  error: null,
}

export function useAdminSession() {
  return useQuery({
    queryKey: adminSessionQueryKey,
    queryFn: adminSessionApi.getSession,
    retry: false,
    staleTime: 5 * 60 * 1000,
  })
}

export function useAdminCsrfBootstrap() {
  const [attempt, setAttempt] = useState(0)
  const [state, setState] = useState<CommandState>({
    isPending: true,
    error: null,
  })

  useEffect(() => {
    void attempt
    let cancelled = false
    setState({ isPending: true, error: null })
    void adminSessionApi.bootstrapCsrf().then(
      () => {
        if (!cancelled) setState(idleCommandState)
      },
      (error: unknown) => {
        if (!cancelled) {
          setState({
            isPending: false,
            error: error instanceof Error ? error : new Error('CSRF 준비 실패'),
          })
        }
      },
    )
    return () => {
      cancelled = true
    }
  }, [attempt])

  return {
    ...state,
    isReady: !state.isPending && state.error === null,
    retry: () => setAttempt((value) => value + 1),
  }
}

export function useAdminSignIn() {
  const queryClient = useQueryClient()
  const [state, setState] = useState<CommandState>(idleCommandState)

  const execute = useCallback(
    async (input: AdminSignInInput) => {
      if (state.isPending) return null
      setState({ isPending: true, error: null })
      try {
        const session = await adminSessionApi.signIn(input)
        queryClient.setQueryData(adminSessionQueryKey, session)
        setState(idleCommandState)
        return session
      } catch (error) {
        const safeError = error instanceof Error ? error : new Error('로그인 실패')
        setState({ isPending: false, error: safeError })
        throw safeError
      }
    },
    [queryClient, state.isPending],
  )

  return { ...state, execute, reset: () => setState(idleCommandState) }
}

export function useAdminPasswordChange() {
  const queryClient = useQueryClient()
  const [state, setState] = useState<CommandState>(idleCommandState)

  const execute = useCallback(
    async (input: AdminPasswordChangeInput) => {
      if (state.isPending) return null
      const transition = beginAdminSessionTransition()
      setState({ isPending: true, error: null })
      try {
        await queryClient.cancelQueries()
        await adminSessionApi.changePassword(input)
        const session = await adminSessionApi.verifySession()
        transition.commit()
        queryClient.setQueryData(adminSessionQueryKey, session)
        setState(idleCommandState)
        return session
      } catch (error) {
        transition.cancel()
        const safeError =
          error instanceof Error ? error : new Error('비밀번호 변경 실패')
        setState({ isPending: false, error: safeError })
        throw safeError
      }
    },
    [queryClient, state.isPending],
  )

  return { ...state, execute, reset: () => setState(idleCommandState) }
}

export function useAdminSignOut() {
  const queryClient = useQueryClient()
  const [state, setState] = useState<CommandState>(idleCommandState)

  const execute = useCallback(async () => {
    if (state.isPending) return false
    const transition = beginAdminSessionTransition()
    setState({ isPending: true, error: null })
    try {
      await queryClient.cancelQueries()
      await adminSessionApi.signOut()
      transition.commit()
      queryClient.clear()
      setState(idleCommandState)
      return true
    } catch (error) {
      transition.cancel()
      const safeError = error instanceof Error ? error : new Error('로그아웃 실패')
      setState({ isPending: false, error: safeError })
      throw safeError
    }
  }, [queryClient, state.isPending])

  return { ...state, execute, reset: () => setState(idleCommandState) }
}
