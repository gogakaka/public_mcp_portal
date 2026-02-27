import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import api, { extractApiError } from '@/lib/api'
import { useUIStore } from '@/stores/uiStore'
import type {
  AwsMcpServer,
  PaginatedResponse,
  CreateAwsMcpServerRequest,
  UpdateAwsMcpServerRequest,
  AwsConnectionTestResult,
  SyncResult,
  SyncHistory,
  DataSourceStatus,
  PaginationParams,
} from '@/types'

interface ServerFilters extends PaginationParams {
  status?: DataSourceStatus
}

export function useAwsMcpServers(filters: ServerFilters = {}) {
  const params = new URLSearchParams()
  if (filters.page) params.set('page', String(filters.page))
  if (filters.pageSize) params.set('size', String(filters.pageSize))
  if (filters.status) params.set('status', filters.status)

  return useQuery({
    queryKey: ['awsMcpServers', filters],
    queryFn: async () => {
      const response = await api.get<PaginatedResponse<AwsMcpServer>>(
        `/aws-mcp/servers?${params.toString()}`
      )
      return response.data
    },
  })
}

export function useAwsMcpServer(id: string | undefined) {
  return useQuery({
    queryKey: ['awsMcpServer', id],
    queryFn: async () => {
      const response = await api.get<AwsMcpServer>(`/aws-mcp/servers/${id}`)
      return response.data
    },
    enabled: !!id,
  })
}

export function useCreateAwsMcpServer() {
  const queryClient = useQueryClient()
  const addNotification = useUIStore((s) => s.addNotification)

  return useMutation({
    mutationFn: async (data: CreateAwsMcpServerRequest) => {
      const response = await api.post<AwsMcpServer>('/aws-mcp/servers', data)
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['awsMcpServers'] })
      addNotification({ type: 'success', message: 'AWS MCP 서버가 등록되었습니다' })
    },
    onError: (error) => {
      addNotification({ type: 'error', message: extractApiError(error).message })
    },
  })
}

export function useUpdateAwsMcpServer() {
  const queryClient = useQueryClient()
  const addNotification = useUIStore((s) => s.addNotification)

  return useMutation({
    mutationFn: async ({ id, ...data }: UpdateAwsMcpServerRequest & { id: string }) => {
      const response = await api.put<AwsMcpServer>(`/aws-mcp/servers/${id}`, data)
      return response.data
    },
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: ['awsMcpServers'] })
      queryClient.invalidateQueries({ queryKey: ['awsMcpServer', data.id] })
      addNotification({ type: 'success', message: 'AWS MCP 서버가 수정되었습니다' })
    },
    onError: (error) => {
      addNotification({ type: 'error', message: extractApiError(error).message })
    },
  })
}

export function useDeleteAwsMcpServer() {
  const queryClient = useQueryClient()
  const addNotification = useUIStore((s) => s.addNotification)

  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/aws-mcp/servers/${id}`)
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['awsMcpServers'] })
      addNotification({ type: 'success', message: 'AWS MCP 서버가 삭제되었습니다' })
    },
    onError: (error) => {
      addNotification({ type: 'error', message: extractApiError(error).message })
    },
  })
}

export function useTestAwsMcpServerConnection() {
  const queryClient = useQueryClient()
  const addNotification = useUIStore((s) => s.addNotification)

  return useMutation({
    mutationFn: async (id: string) => {
      const response = await api.post<AwsConnectionTestResult>(`/aws-mcp/servers/${id}/test`)
      return response.data
    },
    onSuccess: (result, id) => {
      queryClient.invalidateQueries({ queryKey: ['awsMcpServer', id] })
      if (result.success) {
        addNotification({
          type: 'success',
          message: `연결 성공: ${result.serverName} (${result.protocolVersion}, ${result.responseTimeMs}ms)`,
        })
      } else {
        addNotification({ type: 'error', message: `연결 실패: ${result.message}` })
      }
    },
    onError: (error) => {
      addNotification({ type: 'error', message: extractApiError(error).message })
    },
  })
}

export function useSyncAwsMcpServer() {
  const queryClient = useQueryClient()
  const addNotification = useUIStore((s) => s.addNotification)

  return useMutation({
    mutationFn: async (id: string) => {
      const response = await api.post<SyncResult>(`/aws-mcp/servers/${id}/sync`)
      return response.data
    },
    onSuccess: (result, id) => {
      queryClient.invalidateQueries({ queryKey: ['awsMcpServers'] })
      queryClient.invalidateQueries({ queryKey: ['awsMcpServer', id] })
      queryClient.invalidateQueries({ queryKey: ['syncHistory', id] })
      queryClient.invalidateQueries({ queryKey: ['tools'] })
      if (result.success) {
        addNotification({
          type: 'success',
          message: `동기화 완료: ${result.toolsDiscovered}개 발견, ${result.toolsCreated}개 생성, ${result.toolsUpdated}개 업데이트`,
        })
      } else {
        addNotification({ type: 'error', message: `동기화 실패: ${result.message}` })
      }
    },
    onError: (error) => {
      addNotification({ type: 'error', message: extractApiError(error).message })
    },
  })
}

export function useSyncHistory(serverId: string | undefined, page = 0) {
  return useQuery({
    queryKey: ['syncHistory', serverId, page],
    queryFn: async () => {
      const response = await api.get<PaginatedResponse<SyncHistory>>(
        `/aws-mcp/servers/${serverId}/sync-history?page=${page}&size=10`
      )
      return response.data
    },
    enabled: !!serverId,
  })
}
