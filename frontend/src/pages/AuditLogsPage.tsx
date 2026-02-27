import { useEffect, useState } from 'react'
import { useAuditLogs } from '@/api/auditLogs'
import { useUIStore } from '@/stores/uiStore'
import { LoadingSkeleton } from '@/components/common/LoadingSkeleton'
import { ErrorState } from '@/components/common/ErrorState'
import { EmptyState } from '@/components/common/EmptyState'
import { StatusBadge } from '@/components/common/StatusBadge'
import { Pagination } from '@/components/common/Pagination'
import { Modal } from '@/components/common/Modal'
import { AuditStatus } from '@/types'
import type { AuditLog, AuditLogFilters } from '@/types'
import { formatDateTime, formatExecutionTime } from '@/lib/utils'

export default function AuditLogsPage() {
  const setCurrentPage = useUIStore((s) => s.setCurrentPage)

  useEffect(() => {
    setCurrentPage('Audit Logs')
  }, [setCurrentPage])

  const [filters, setFilters] = useState<AuditLogFilters>({
    page: 1,
    pageSize: 20,
  })
  const [selectedLog, setSelectedLog] = useState<AuditLog | null>(null)

  const { data, isLoading, isError, refetch } = useAuditLogs(filters)

  const handleFilterChange = (key: keyof AuditLogFilters, value: string) => {
    setFilters((prev) => ({
      ...prev,
      [key]: value || undefined,
      page: 1,
    }))
  }

  if (isLoading) {
    return <LoadingSkeleton variant="table" rows={10} />
  }

  if (isError) {
    return <ErrorState message="Failed to load audit logs" onRetry={refetch} />
  }

  return (
    <div className="space-y-4">
      <div className="section-header">
        <h2 className="text-base font-semibold text-text">Audit Logs</h2>
        <p className="text-2xs text-text-tertiary">Track all tool executions and system events</p>
      </div>

      <div className="card p-3">
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="flex gap-2 flex-1">
            <input
              type="date"
              value={filters.startDate || ''}
              onChange={(e) => handleFilterChange('startDate', e.target.value)}
              className="input-field text-xs"
              placeholder="Start date"
            />
            <input
              type="date"
              value={filters.endDate || ''}
              onChange={(e) => handleFilterChange('endDate', e.target.value)}
              className="input-field text-xs"
              placeholder="End date"
            />
          </div>
          <div className="flex gap-2">
            <input
              type="text"
              value={filters.userId || ''}
              onChange={(e) => handleFilterChange('userId', e.target.value)}
              className="input-field text-xs"
              placeholder="User ID"
            />
            <input
              type="text"
              value={filters.toolId || ''}
              onChange={(e) => handleFilterChange('toolId', e.target.value)}
              className="input-field text-xs"
              placeholder="Tool ID"
            />
            <select
              value={filters.status || ''}
              onChange={(e) => handleFilterChange('status', e.target.value)}
              className="input-field text-xs"
            >
              <option value="">All Statuses</option>
              {Object.values(AuditStatus).map((status) => (
                <option key={status} value={status}>
                  {status}
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {data && data.data.length === 0 ? (
        <EmptyState
          title="No audit logs found"
          description="No logs match your current filters, or no tool executions have occurred yet."
        />
      ) : (
        <>
          <div className="card p-0">
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b-2 border-b-primary-900">
                    <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                      Trace ID
                    </th>
                    <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                      User
                    </th>
                    <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                      Tool
                    </th>
                    <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                      Status
                    </th>
                    <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                      Duration
                    </th>
                    <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                      Timestamp
                    </th>
                    <th className="text-right text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                      Details
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {data?.data.map((log: AuditLog) => (
                    <tr key={log.id} className="table-row">
                      <td className="px-4 py-3 text-xs font-mono text-text-secondary">
                        {log.traceId?.slice(0, 12) || '--'}
                      </td>
                      <td className="px-4 py-3 text-xs text-text">
                        {log.userName || log.userId}
                      </td>
                      <td className="px-4 py-3 text-xs text-text">
                        {log.toolName || log.toolId}
                      </td>
                      <td className="px-4 py-3">
                        <StatusBadge status={log.status} />
                      </td>
                      <td className="px-4 py-3 text-xs font-mono text-text-secondary">
                        {formatExecutionTime(log.executionTimeMs)}
                      </td>
                      <td className="px-4 py-3 text-xs text-text-tertiary">
                        {formatDateTime(log.createdAt)}
                      </td>
                      <td className="px-4 py-3 text-right">
                        <button
                          onClick={() => setSelectedLog(log)}
                          className="text-2xs text-text-secondary hover:text-text transition-colors"
                        >
                          View
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {data && data.totalPages > 1 && (
            <div className="flex justify-between items-center">
              <span className="text-xs text-text-secondary">
                {data.total} total entries
              </span>
              <Pagination
                currentPage={data.page}
                totalPages={data.totalPages}
                onPageChange={(page) => setFilters((prev) => ({ ...prev, page }))}
              />
            </div>
          )}
        </>
      )}

      <Modal
        isOpen={!!selectedLog}
        onClose={() => setSelectedLog(null)}
        title="Audit Log Details"
        size="lg"
      >
        {selectedLog && (
          <div className="space-y-4">
            <div className="grid grid-cols-2 gap-3">
              <div>
                <p className="text-2xs text-text-tertiary uppercase tracking-wider font-medium mb-0.5">
                  Trace ID
                </p>
                <p className="text-xs font-mono text-text">{selectedLog.traceId}</p>
              </div>
              <div>
                <p className="text-2xs text-text-tertiary uppercase tracking-wider font-medium mb-0.5">
                  Status
                </p>
                <StatusBadge status={selectedLog.status} />
              </div>
              <div>
                <p className="text-2xs text-text-tertiary uppercase tracking-wider font-medium mb-0.5">
                  User
                </p>
                <p className="text-xs text-text">{selectedLog.userName || selectedLog.userId}</p>
              </div>
              <div>
                <p className="text-2xs text-text-tertiary uppercase tracking-wider font-medium mb-0.5">
                  Tool
                </p>
                <p className="text-xs text-text">{selectedLog.toolName || selectedLog.toolId}</p>
              </div>
              <div>
                <p className="text-2xs text-text-tertiary uppercase tracking-wider font-medium mb-0.5">
                  Execution Time
                </p>
                <p className="text-xs font-mono text-text">
                  {formatExecutionTime(selectedLog.executionTimeMs)}
                </p>
              </div>
              <div>
                <p className="text-2xs text-text-tertiary uppercase tracking-wider font-medium mb-0.5">
                  IP Address
                </p>
                <p className="text-xs font-mono text-text">{selectedLog.ipAddress || '--'}</p>
              </div>
              <div>
                <p className="text-2xs text-text-tertiary uppercase tracking-wider font-medium mb-0.5">
                  Timestamp
                </p>
                <p className="text-xs text-text">{formatDateTime(selectedLog.createdAt)}</p>
              </div>
            </div>

            {selectedLog.errorMessage && (
              <div>
                <p className="text-2xs text-text-tertiary uppercase tracking-wider font-medium mb-1">
                  Error Message
                </p>
                <pre className="text-xs font-mono text-red-700 bg-red-50 border-l-2 border-l-red-600 p-3 whitespace-pre-wrap">
                  {selectedLog.errorMessage}
                </pre>
              </div>
            )}

            <div>
              <p className="text-2xs text-text-tertiary uppercase tracking-wider font-medium mb-1">
                Input Parameters
              </p>
              <pre className="text-xs font-mono text-text bg-surface-tertiary border-l-2 border-l-primary-900 p-3 overflow-x-auto whitespace-pre-wrap max-h-64 overflow-y-auto">
                {JSON.stringify(selectedLog.inputParams, null, 2)}
              </pre>
            </div>

            {selectedLog.outputSummary && (
              <div>
                <p className="text-2xs text-text-tertiary uppercase tracking-wider font-medium mb-1">
                  Output Summary
                </p>
                <pre className="text-xs font-mono text-text bg-surface-tertiary border-l-2 border-l-primary-900 p-3 overflow-x-auto whitespace-pre-wrap">
                  {selectedLog.outputSummary}
                </pre>
              </div>
            )}
          </div>
        )}
      </Modal>
    </div>
  )
}
