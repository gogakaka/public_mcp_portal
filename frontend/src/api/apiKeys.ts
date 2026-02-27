import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import api, { extractApiError } from '@/lib/api'
import { useUIStore } from '@/stores/uiStore'
import type { ApiKey, ApiKeyCreateResponse, CreateApiKeyRequest } from '@/types'

export function useApiKeys() {
  return useQuery({
    queryKey: ['apiKeys'],
    queryFn: async () => {
      const response = await api.get<ApiKey[]>('/api-keys')
      return response.data
    },
  })
}

export function useCreateApiKey() {
  const queryClient = useQueryClient()
  const addNotification = useUIStore((s) => s.addNotification)

  return useMutation({
    mutationFn: async (data: CreateApiKeyRequest) => {
      const response = await api.post<ApiKeyCreateResponse>('/api-keys', data)
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['apiKeys'] })
      addNotification({ type: 'success', message: 'API key created successfully' })
    },
    onError: (error) => {
      const apiError = extractApiError(error)
      addNotification({ type: 'error', message: apiError.message })
    },
  })
}

export function useDeleteApiKey() {
  const queryClient = useQueryClient()
  const addNotification = useUIStore((s) => s.addNotification)

  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/api-keys/${id}`)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['apiKeys'] })
      addNotification({ type: 'success', message: 'API key deleted' })
    },
    onError: (error) => {
      const apiError = extractApiError(error)
      addNotification({ type: 'error', message: apiError.message })
    },
  })
}

export function useToggleApiKey() {
  const queryClient = useQueryClient()
  const addNotification = useUIStore((s) => s.addNotification)

  return useMutation({
    mutationFn: async ({ id, active }: { id: string; active: boolean }) => {
      const response = await api.patch<ApiKey>(`/api-keys/${id}`, { active })
      return response.data
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['apiKeys'] })
      addNotification({
        type: 'success',
        message: `API key ${data.active ? 'activated' : 'deactivated'}`,
      })
    },
    onError: (error) => {
      const apiError = extractApiError(error)
      addNotification({ type: 'error', message: apiError.message })
    },
  })
}
