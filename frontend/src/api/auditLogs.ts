import { useQuery } from '@tanstack/react-query'
import api from '@/lib/api'
import type { AuditLog, PaginatedResponse, AuditLogFilters } from '@/types'

export function useAuditLogs(filters: AuditLogFilters = {}) {
  const params = new URLSearchParams()
  if (filters.page) params.set('page', String(filters.page))
  if (filters.pageSize) params.set('pageSize', String(filters.pageSize))
  if (filters.sortBy) params.set('sortBy', filters.sortBy)
  if (filters.sortOrder) params.set('sortOrder', filters.sortOrder)
  if (filters.userId) params.set('userId', filters.userId)
  if (filters.toolId) params.set('toolId', filters.toolId)
  if (filters.status) params.set('status', filters.status)
  if (filters.startDate) params.set('startDate', filters.startDate)
  if (filters.endDate) params.set('endDate', filters.endDate)

  return useQuery({
    queryKey: ['auditLogs', filters],
    queryFn: async () => {
      const response = await api.get<PaginatedResponse<AuditLog>>(
        `/audit-logs?${params.toString()}`,
      )
      return response.data
    },
  })
}

export function useAuditLog(id: string | undefined) {
  return useQuery({
    queryKey: ['auditLog', id],
    queryFn: async () => {
      const response = await api.get<AuditLog>(`/audit-logs/${id}`)
      return response.data
    },
    enabled: !!id,
  })
}
