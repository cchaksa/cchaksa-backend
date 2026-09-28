import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  adminInquiryApi,
  type InquiryListParams,
} from '../api/adminInquiryApi'

export const inquiryQueryKeys = {
  all: ['admin-inquiries'] as const,
  list: (params: InquiryListParams) => [...inquiryQueryKeys.all, 'list', params] as const,
  detail: (reportId: number) => [...inquiryQueryKeys.all, 'detail', reportId] as const,
}

export function useInquiryPage(params: InquiryListParams) {
  return useQuery({
    queryKey: inquiryQueryKeys.list(params),
    queryFn: () => adminInquiryApi.getPage(params),
  })
}

export function useInquiryDetail(reportId: number) {
  return useQuery({
    queryKey: inquiryQueryKeys.detail(reportId),
    queryFn: () => adminInquiryApi.getDetail(reportId),
    enabled: Number.isInteger(reportId) && reportId > 0,
  })
}

export function useAnswerInquiry(reportId: number) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (answer: string) => adminInquiryApi.answer(reportId, answer),
    onSuccess: (inquiry) => {
      queryClient.setQueryData(inquiryQueryKeys.detail(reportId), inquiry)
      void queryClient.invalidateQueries({ queryKey: inquiryQueryKeys.all })
    },
  })
}
