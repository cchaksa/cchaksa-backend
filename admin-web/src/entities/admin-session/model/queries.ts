import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { adminSessionApi } from '../api/adminSessionApi'

export const adminSessionQueryKey = ['admin-session'] as const

export function useAdminSession() {
  return useQuery({
    queryKey: adminSessionQueryKey,
    queryFn: adminSessionApi.getSession,
    retry: false,
    staleTime: 5 * 60 * 1000,
  })
}

export function useAdminSignOut() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: adminSessionApi.signOut,
    onSuccess: () => queryClient.removeQueries({ queryKey: adminSessionQueryKey }),
  })
}
