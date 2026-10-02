import { useQueryClient } from '@tanstack/react-query'
import { useEffect, useRef } from 'react'
import { useLocation, useNavigate } from 'react-router'
import { configureSessionExpiredHandler } from '../../../shared/api/http'
import { routes } from '../../../shared/config/routes'

export function AdminSessionExpiryCoordinator() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const location = useLocation()
  const handlingRef = useRef(false)

  useEffect(() => {
    if (location.pathname !== routes.login) handlingRef.current = false
  }, [location.pathname])

  useEffect(() => {
    configureSessionExpiredHandler(() => {
      if (handlingRef.current) return
      handlingRef.current = true
      void queryClient.cancelQueries().finally(() => {
        queryClient.clear()
        navigate(routes.login, {
          replace: true,
          state: { sessionExpired: true },
        })
      })
    })
    return () => configureSessionExpiredHandler(null)
  }, [navigate, queryClient])

  return null
}
