import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import api, { extractApiError } from '@/lib/api'
import { useUIStore } from '@/stores/uiStore'
import type {
  CubeSchema,
  PaginatedResponse,
  CreateCubeSchemaRequest,
  UpdateCubeSchemaRequest,
  ValidationResult,
  CubeMeta,
  SchemaStatus,
  PaginationParams,
} from '@/types'

interface SchemaFilters extends PaginationParams {
  status?: SchemaStatus
  datasourceId?: string
}

export function useCubeSchemas(filters: SchemaFilters = {}) {
  const params = new URLSearchParams()
  if (filters.page) params.set('page', String(filters.page))
  if (filters.pageSize) params.set('size', String(filters.pageSize))
  if (filters.status) params.set('status', filters.status)
  if (filters.datasourceId) params.set('datasourceId', filters.datasourceId)

  return useQuery({
    queryKey: ['cubeSchemas', filters],
    queryFn: async () => {
      const response = await api.get<PaginatedResponse<CubeSchema>>(
        `/cube/schemas?${params.toString()}`
      )
      return response.data
    },
  })
}

export function useCubeSchema(id: string | undefined) {
  return useQuery({
    queryKey: ['cubeSchema', id],
    queryFn: async () => {
      const response = await api.get<CubeSchema>(`/cube/schemas/${id}`)
      return response.data
    },
    enabled: !!id,
  })
}

export function useCreateCubeSchema() {
  const queryClient = useQueryClient()
  const addNotification = useUIStore((s) => s.addNotification)

  return useMutation({
    mutationFn: async (data: CreateCubeSchemaRequest) => {
      const response = await api.post<CubeSchema>('/cube/schemas', data)
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cubeSchemas'] })
      addNotification({ type: 'success', message: '스키마가 생성되었습니다' })
    },
    onError: (error) => {
      addNotification({ type: 'error', message: extractApiError(error).message })
    },
  })
}

export function useUpdateCubeSchema() {
  const queryClient = useQueryClient()
  const addNotification = useUIStore((s) => s.addNotification)

  return useMutation({
    mutationFn: async ({ id, ...data }: UpdateCubeSchemaRequest & { id: string }) => {
      const response = await api.put<CubeSchema>(`/cube/schemas/${id}`, data)
      return response.data
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['cubeSchemas'] })
      queryClient.invalidateQueries({ queryKey: ['cubeSchema', data.id] })
      addNotification({ type: 'success', message: '스키마가 수정되었습니다' })
    },
    onError: (error) => {
      addNotification({ type: 'error', message: extractApiError(error).message })
    },
  })
}

export function useDeleteCubeSchema() {
  const queryClient = useQueryClient()
  const addNotification = useUIStore((s) => s.addNotification)

  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/cube/schemas/${id}`)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cubeSchemas'] })
      addNotification({ type: 'success', message: '스키마가 삭제되었습니다' })
    },
    onError: (error) => {
      addNotification({ type: 'error', message: extractApiError(error).message })
    },
  })
}

export function useActivateCubeSchema() {
  const queryClient = useQueryClient()
  const addNotification = useUIStore((s) => s.addNotification)

  return useMutation({
    mutationFn: async (id: string) => {
      const response = await api.post<CubeSchema>(`/cube/schemas/${id}/activate`)
      return response.data
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['cubeSchemas'] })
      queryClient.invalidateQueries({ queryKey: ['cubeSchema', data.id] })
      addNotification({ type: 'success', message: '스키마가 활성화되었습니다' })
    },
    onError: (error) => {
      addNotification({ type: 'error', message: extractApiError(error).message })
    },
  })
}

export function useArchiveCubeSchema() {
  const queryClient = useQueryClient()
  const addNotification = useUIStore((s) => s.addNotification)

  return useMutation({
    mutationFn: async (id: string) => {
      const response = await api.post<CubeSchema>(`/cube/schemas/${id}/archive`)
      return response.data
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['cubeSchemas'] })
      queryClient.invalidateQueries({ queryKey: ['cubeSchema', data.id] })
      addNotification({ type: 'success', message: '스키마가 아카이브되었습니다' })
    },
    onError: (error) => {
      addNotification({ type: 'error', message: extractApiError(error).message })
    },
  })
}

export function useValidateCubeSchema() {
  return useMutation({
    mutationFn: async (id: string) => {
      const response = await api.post<ValidationResult>(`/cube/schemas/${id}/validate`)
      return response.data
    },
  })
}

export function useCubeMeta() {
  return useQuery({
    queryKey: ['cubeMeta'],
    queryFn: async () => {
      const response = await api.get<CubeMeta[]>('/cube/schemas/meta')
      return response.data
    },
  })
}
