import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import api, { extractApiError } from '@/lib/api'
import { useUIStore } from '@/stores/uiStore'
import type {
  Tool,
  PaginatedResponse,
  ToolFilters,
  CreateToolRequest,
  UpdateToolRequest,
  ExecuteToolRequest,
  ExecuteToolResponse,
} from '@/types'

export function useTools(filters: ToolFilters = {}) {
  const params = new URLSearchParams()
  if (filters.page) params.set('page', String(filters.page))
  if (filters.pageSize) params.set('pageSize', String(filters.pageSize))
  if (filters.sortBy) params.set('sortBy', filters.sortBy)
  if (filters.sortOrder) params.set('sortOrder', filters.sortOrder)
  if (filters.type) params.set('type', filters.type)
  if (filters.status) params.set('status', filters.status)
  if (filters.search) params.set('search', filters.search)

  return useQuery({
    queryKey: ['tools', filters],
    queryFn: async () => {
      const response = await api.get<PaginatedResponse<Tool>>(`/tools?${params.toString()}`)
      return response.data
    },
  })
}

export function useTool(id: string | undefined) {
  return useQuery({
    queryKey: ['tool', id],
    queryFn: async () => {
      const response = await api.get<Tool>(`/tools/${id}`)
      return response.data
    },
    enabled: !!id,
  })
}

export function useCreateTool() {
  const queryClient = useQueryClient()
  const addNotification = useUIStore((s) => s.addNotification)

  return useMutation({
    mutationFn: async (data: CreateToolRequest) => {
      const response = await api.post<Tool>('/tools', data)
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tools'] })
      addNotification({ type: 'success', message: 'Tool created successfully' })
    },
    onError: (error) => {
      const apiError = extractApiError(error)
      addNotification({ type: 'error', message: apiError.message })
    },
  })
}

export function useUpdateTool() {
  const queryClient = useQueryClient()
  const addNotification = useUIStore((s) => s.addNotification)

  return useMutation({
    mutationFn: async (data: UpdateToolRequest) => {
      const { id, ...rest } = data
      const response = await api.put<Tool>(`/tools/${id}`, rest)
      return response.data
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['tools'] })
      queryClient.invalidateQueries({ queryKey: ['tool', data.id] })
      addNotification({ type: 'success', message: 'Tool updated successfully' })
    },
    onError: (error) => {
      const apiError = extractApiError(error)
      addNotification({ type: 'error', message: apiError.message })
    },
  })
}

export function useDeleteTool() {
  const queryClient = useQueryClient()
  const addNotification = useUIStore((s) => s.addNotification)

  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/tools/${id}`)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['tools'] })
      addNotification({ type: 'success', message: 'Tool deleted successfully' })
    },
    onError: (error) => {
      const apiError = extractApiError(error)
      addNotification({ type: 'error', message: apiError.message })
    },
  })
}

export function useApproveTool() {
  const queryClient = useQueryClient()
  const addNotification = useUIStore((s) => s.addNotification)

  return useMutation({
    mutationFn: async (id: string) => {
      const response = await api.post<Tool>(`/tools/${id}/approve`)
      return response.data
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['tools'] })
      queryClient.invalidateQueries({ queryKey: ['tool', data.id] })
      addNotification({ type: 'success', message: 'Tool approved' })
    },
    onError: (error) => {
      const apiError = extractApiError(error)
      addNotification({ type: 'error', message: apiError.message })
    },
  })
}

export function useRejectTool() {
  const queryClient = useQueryClient()
  const addNotification = useUIStore((s) => s.addNotification)

  return useMutation({
    mutationFn: async (id: string) => {
      const response = await api.post<Tool>(`/tools/${id}/reject`)
      return response.data
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['tools'] })
      queryClient.invalidateQueries({ queryKey: ['tool', data.id] })
      addNotification({ type: 'success', message: 'Tool rejected' })
    },
    onError: (error) => {
      const apiError = extractApiError(error)
      addNotification({ type: 'error', message: apiError.message })
    },
  })
}

export function useExecuteTool() {
  const addNotification = useUIStore((s) => s.addNotification)

  return useMutation({
    mutationFn: async (data: ExecuteToolRequest) => {
      const response = await api.post<ExecuteToolResponse>(`/tools/${data.toolId}/execute`, {
        params: data.params,
      })
      return response.data
    },
    onError: (error) => {
      const apiError = extractApiError(error)
      addNotification({ type: 'error', message: apiError.message })
    },
  })
}
