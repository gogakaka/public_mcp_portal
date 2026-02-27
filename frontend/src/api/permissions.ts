import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import api, { extractApiError } from '@/lib/api'
import { useUIStore } from '@/stores/uiStore'
import type { Permission, GrantPermissionRequest } from '@/types'

export function usePermissions(toolId?: string) {
  const params = new URLSearchParams()
  if (toolId) params.set('toolId', toolId)

  return useQuery({
    queryKey: ['permissions', toolId],
    queryFn: async () => {
      const response = await api.get<Permission[]>(`/permissions?${params.toString()}`)
      return response.data
    },
  })
}

export function useGrantPermission() {
  const queryClient = useQueryClient()
  const addNotification = useUIStore((s) => s.addNotification)

  return useMutation({
    mutationFn: async (data: GrantPermissionRequest) => {
      const response = await api.post<Permission>('/permissions', data)
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['permissions'] })
      addNotification({ type: 'success', message: 'Permission granted' })
    },
    onError: (error) => {
      const apiError = extractApiError(error)
      addNotification({ type: 'error', message: apiError.message })
    },
  })
}

export function useRevokePermission() {
  const queryClient = useQueryClient()
  const addNotification = useUIStore((s) => s.addNotification)

  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/permissions/${id}`)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['permissions'] })
      addNotification({ type: 'success', message: 'Permission revoked' })
    },
    onError: (error) => {
      const apiError = extractApiError(error)
      addNotification({ type: 'error', message: apiError.message })
    },
  })
}
