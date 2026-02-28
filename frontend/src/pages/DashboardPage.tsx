import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts'
import { useDashboardStats } from '@/api/dashboard'
import { useUIStore } from '@/stores/uiStore'
import { LoadingSkeleton } from '@/components/common/LoadingSkeleton'
import { ErrorState } from '@/components/common/ErrorState'
import { StatusBadge } from '@/components/common/StatusBadge'
import { formatNumber, formatRelativeTime, formatExecutionTime } from '@/lib/utils'
import type { AuditLog } from '@/types'

export default function DashboardPage() {
  const setCurrentPage = useUIStore((s) => s.setCurrentPage)
  const navigate = useNavigate()

  useEffect(() => {
    setCurrentPage('Dashboard')
  }, [setCurrentPage])

  const { data: stats, isLoading, isError, refetch } = useDashboardStats()

  if (isLoading) {
    return (
      <div className="space-y-6">
        <LoadingSkeleton variant="stat" />
        <LoadingSkeleton variant="table" rows={5} />
      </div>
    )
  }

  if (isError || !stats) {
    return <ErrorState message="Failed to load dashboard data" onRetry={refetch} />
  }

  const statBlocks = [
    {
      label: 'Total Tools',
      value: formatNumber(stats.totalTools),
      detail: `${stats.activeTools} active`,
    },
    {
      label: 'Active API Keys',
      value: formatNumber(stats.activeApiKeys),
      detail: `of ${stats.totalApiKeys} total`,
    },
    {
      label: 'Requests (24h)',
      value: formatNumber(stats.totalRequests24h),
      detail: 'last 24 hours',
    },
    {
      label: 'Error Rate',
      value: `${stats.errorRate24h.toFixed(1)}%`,
      detail: 'last 24 hours',
    },
  ]

  const toolsByType = Object.entries(stats.toolsByType || {})

  return (
    <div className="space-y-6">
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {statBlocks.map((stat) => (
          <div key={stat.label} className="stat-block pt-4">
            <p className="text-2xs text-text-secondary uppercase tracking-wider font-medium">
              {stat.label}
            </p>
            <p className="text-2xl font-bold text-text mt-1">{stat.value}</p>
            <p className="text-2xs text-text-tertiary mt-0.5">{stat.detail}</p>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 card p-0">
          <div className="px-4 py-3 border-b border-b-border">
            <h3 className="text-xs font-semibold text-text">Request Trend</h3>
            <p className="text-2xs text-text-tertiary">Requests over time</p>
          </div>
          <div className="p-4 h-[280px]">
            {stats.requestTrend && stats.requestTrend.length > 0 ? (
              <ResponsiveContainer width="100%" height="100%">
                <LineChart data={stats.requestTrend}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e5e5e5" />
                  <XAxis
                    dataKey="date"
                    tick={{ fontSize: 10, fill: '#999999' }}
                    tickLine={false}
                    axisLine={{ stroke: '#e5e5e5' }}
                  />
                  <YAxis
                    tick={{ fontSize: 10, fill: '#999999' }}
                    tickLine={false}
                    axisLine={{ stroke: '#e5e5e5' }}
                  />
                  <Tooltip
                    contentStyle={{
                      border: 'none',
                      borderLeft: '2px solid #1a1a1a',
                      borderBottom: '1px solid #e5e5e5',
                      borderRadius: 0,
                      fontSize: 12,
                      boxShadow: 'none',
                    }}
                  />
                  <Line
                    type="monotone"
                    dataKey="requests"
                    stroke="#1a1a1a"
                    strokeWidth={1.5}
                    dot={false}
                    name="Requests"
                  />
                  <Line
                    type="monotone"
                    dataKey="errors"
                    stroke="#dc2626"
                    strokeWidth={1.5}
                    dot={false}
                    name="Errors"
                  />
                </LineChart>
              </ResponsiveContainer>
            ) : (
              <div className="h-full flex items-center justify-center">
                <p className="text-xs text-text-tertiary">No trend data available</p>
              </div>
            )}
          </div>
        </div>

        <div className="card p-0">
          <div className="px-4 py-3 border-b border-b-border">
            <h3 className="text-xs font-semibold text-text">Tools by Type</h3>
            <p className="text-2xs text-text-tertiary">Distribution</p>
          </div>
          <div className="p-4 space-y-3">
            {toolsByType.length > 0 ? (
              toolsByType.map(([type, count]) => {
                const total = stats.totalTools || 1
                const pct = Math.round((count / total) * 100)
                return (
                  <div key={type}>
                    <div className="flex items-center justify-between mb-1">
                      <span className="text-xs text-text">{type}</span>
                      <span className="text-2xs text-text-secondary">{count} ({pct}%)</span>
                    </div>
                    <div className="h-1.5 bg-surface-tertiary">
                      <div
                        className="h-full bg-primary-900 transition-all"
                        style={{ width: `${pct}%` }}
                      />
                    </div>
                  </div>
                )
              })
            ) : (
              <p className="text-xs text-text-tertiary text-center py-4">No tools registered</p>
            )}
          </div>
        </div>
      </div>

      <div className="card p-0">
        <div className="px-4 py-3 border-b border-b-border flex items-center justify-between">
          <div>
            <h3 className="text-xs font-semibold text-text">Recent Activity</h3>
            <p className="text-2xs text-text-tertiary">Last 10 operations</p>
          </div>
          <button
            onClick={() => navigate('/audit-logs')}
            className="text-2xs text-text-secondary hover:text-text transition-colors"
          >
            View All
          </button>
        </div>
        {stats.recentActivity && stats.recentActivity.length > 0 ? (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b border-b-border">
                  <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-2">
                    Trace ID
                  </th>
                  <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-2">
                    User
                  </th>
                  <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-2">
                    Tool
                  </th>
                  <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-2">
                    Status
                  </th>
                  <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-2">
                    Duration
                  </th>
                  <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-2">
                    Time
                  </th>
                </tr>
              </thead>
              <tbody>
                {stats.recentActivity.map((log: AuditLog) => (
                  <tr key={log.id} className="table-row">
                    <td className="px-4 py-2 text-xs font-mono text-text-secondary">
                      {log.traceId?.slice(0, 8) || '--'}
                    </td>
                    <td className="px-4 py-2 text-xs text-text">
                      {log.userName || log.userId}
                    </td>
                    <td className="px-4 py-2 text-xs text-text">
                      {log.toolName || log.toolId}
                    </td>
                    <td className="px-4 py-2">
                      <StatusBadge status={log.status} />
                    </td>
                    <td className="px-4 py-2 text-xs text-text-secondary font-mono">
                      {formatExecutionTime(log.executionTimeMs)}
                    </td>
                    <td className="px-4 py-2 text-xs text-text-tertiary">
                      {formatRelativeTime(log.createdAt)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="p-8 text-center">
            <p className="text-xs text-text-tertiary">No recent activity</p>
          </div>
        )}
      </div>
    </div>
  )
}
