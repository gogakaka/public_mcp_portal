import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import api, { extractApiError } from '@/lib/api'
import { useUIStore } from '@/stores/uiStore'
import type {
  CubeDataSource,
  PaginatedResponse,
  CreateCubeDataSourceRequest,
  UpdateCubeDataSourceRequest,
  ConnectionTestResult,
  DataSourceStatus,
  PaginationParams,
} from '@/types'

interface DataSourceFilters extends PaginationParams {
  status?: DataSourceStatus
}

export function useCubeDataSources(filters: DataSourceFilters = {}) {
  const params = new URLSearchParams()
  if (filters.page) params.set('page', String(filters.page))
  if (filters.pageSize) params.set('size', String(filters.pageSize))
  if (filters.status) params.set('status', filters.status)

  return useQuery({
    queryKey: ['cubeDataSources', filters],
    queryFn: async () => {
      const response = await api.get<PaginatedResponse<CubeDataSource>>(
        `/cube/datasources?${params.toString()}`
      )
      return response.data
    },
  })
}

export function useCubeDataSource(id: string | undefined) {
  return useQuery({
    queryKey: ['cubeDataSource', id],
    queryFn: async () => {
      const response = await api.get<CubeDataSource>(`/cube/datasources/${id}`)
      return response.data
    },
    enabled: !!id,
  })
}

export function useCreateCubeDataSource() {
  const queryClient = useQueryClient()
  const addNotification = useUIStore((s) => s.addNotification)

  return useMutation({
    mutationFn: async (data: CreateCubeDataSourceRequest) => {
      const response = await api.post<CubeDataSource>('/cube/datasources', data)
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cubeDataSources'] })
      addNotification({ type: 'success', message: '데이터소스가 생성되었습니다' })
    },
    onError: (error) => {
      addNotification({ type: 'error', message: extractApiError(error).message })
    },
  })
}

export function useUpdateCubeDataSource() {
  const queryClient = useQueryClient()
  const addNotification = useUIStore((s) => s.addNotification)

  return useMutation({
    mutationFn: async ({ id, ...data }: UpdateCubeDataSourceRequest & { id: string }) => {
      const response = await api.put<CubeDataSource>(`/cube/datasources/${id}`, data)
      return response.data
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['cubeDataSources'] })
      queryClient.invalidateQueries({ queryKey: ['cubeDataSource', data.id] })
      addNotification({ type: 'success', message: '데이터소스가 수정되었습니다' })
    },
    onError: (error) => {
      addNotification({ type: 'error', message: extractApiError(error).message })
    },
  })
}

export function useDeleteCubeDataSource() {
  const queryClient = useQueryClient()
  const addNotification = useUIStore((s) => s.addNotification)

  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/cube/datasources/${id}`)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cubeDataSources'] })
      addNotification({ type: 'success', message: '데이터소스가 삭제되었습니다' })
    },
    onError: (error) => {
      addNotification({ type: 'error', message: extractApiError(error).message })
    },
  })
}

export function useTestCubeDataSourceConnection() {
  const addNotification = useUIStore((s) => s.addNotification)

  return useMutation({
    mutationFn: async (id: string) => {
      const response = await api.post<ConnectionTestResult>(`/cube/datasources/${id}/test`)
      return response.data
    },
    onSuccess: (result) => {
      if (result.success) {
        addNotification({ type: 'success', message: `연결 성공 (${result.responseTimeMs}ms)` })
      } else {
        addNotification({ type: 'error', message: `연결 실패: ${result.message}` })
      }
    },
    onError: (error) => {
      addNotification({ type: 'error', message: extractApiError(error).message })
    },
  })
}

export function useIntrospectTables(id: string | undefined) {
  return useQuery({
    queryKey: ['cubeDataSourceTables', id],
    queryFn: async () => {
      const response = await api.get<Record<string, unknown[]>>(`/cube/datasources/${id}/tables`)
      return response.data
    },
    enabled: false,
  })
}
