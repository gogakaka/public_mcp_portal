import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useTools } from '@/api/tools'
import { useUIStore } from '@/stores/uiStore'
import { LoadingSkeleton } from '@/components/common/LoadingSkeleton'
import { ErrorState } from '@/components/common/ErrorState'
import { EmptyState } from '@/components/common/EmptyState'
import { StatusBadge } from '@/components/common/StatusBadge'
import { Pagination } from '@/components/common/Pagination'
import { ToolType, ToolStatus } from '@/types'
import type { Tool, ToolFilters } from '@/types'
import { cn, formatDate, truncateText } from '@/lib/utils'

const toolTypes = Object.values(ToolType)
const toolStatuses = Object.values(ToolStatus)

export default function ToolsPage() {
  const setCurrentPage = useUIStore((s) => s.setCurrentPage)
  const navigate = useNavigate()

  useEffect(() => {
    setCurrentPage('Tools')
  }, [setCurrentPage])

  const [filters, setFilters] = useState<ToolFilters>({
    page: 1,
    pageSize: 12,
  })
  const [searchInput, setSearchInput] = useState('')

  const { data, isLoading, isError, refetch } = useTools(filters)

  const handleSearch = () => {
    setFilters((prev) => ({ ...prev, search: searchInput || undefined, page: 1 }))
  }

  const handleSearchKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') handleSearch()
  }

  const handleTypeFilter = (type: string) => {
    setFilters((prev) => ({
      ...prev,
      type: type ? (type as ToolType) : undefined,
      page: 1,
    }))
  }

  const handleStatusFilter = (status: string) => {
    setFilters((prev) => ({
      ...prev,
      status: status ? (status as ToolStatus) : undefined,
      page: 1,
    }))
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <div className="section-header">
          <h2 className="text-base font-semibold text-text">Tool Registry</h2>
          <p className="text-2xs text-text-tertiary">Manage and discover MCP tools</p>
        </div>
        <button onClick={() => navigate('/tools/new')} className="btn-primary text-xs self-start">
          Register New Tool
        </button>
      </div>

      <div className="card p-3">
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="flex-1 flex gap-2">
            <input
              type="text"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              onKeyDown={handleSearchKeyDown}
              placeholder="Search tools..."
              className="input-field flex-1"
            />
            <button onClick={handleSearch} className="btn-secondary text-xs px-3">
              Search
            </button>
          </div>
          <div className="flex gap-2">
            <select
              value={filters.type || ''}
              onChange={(e) => handleTypeFilter(e.target.value)}
              className="input-field text-xs"
            >
              <option value="">All Types</option>
              {toolTypes.map((type) => (
                <option key={type} value={type}>
                  {type}
                </option>
              ))}
            </select>
            <select
              value={filters.status || ''}
              onChange={(e) => handleStatusFilter(e.target.value)}
              className="input-field text-xs"
            >
              <option value="">All Statuses</option>
              {toolStatuses.map((status) => (
                <option key={status} value={status}>
                  {status}
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {isLoading && <LoadingSkeleton variant="card" rows={6} />}

      {isError && <ErrorState message="Failed to load tools" onRetry={refetch} />}

      {data && data.data.length === 0 && (
        <EmptyState
          title="No tools found"
          description="No tools match your current filters, or no tools have been registered yet."
          actionLabel="Register New Tool"
          onAction={() => navigate('/tools/new')}
        />
      )}

      {data && data.data.length > 0 && (
        <>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {data.data.map((tool: Tool) => (
              <div
                key={tool.id}
                onClick={() => navigate(`/tools/${tool.id}`)}
                className={cn(
                  'card p-4 cursor-pointer hover:bg-surface-secondary transition-colors',
                )}
              >
                <div className="flex items-start justify-between gap-2 mb-2">
                  <h3 className="text-sm font-semibold text-text truncate">{tool.name}</h3>
                  <StatusBadge status={tool.status} />
                </div>
                <div className="flex items-center gap-2 mb-2">
                  <span className="badge-info">{tool.type}</span>
                  {tool.version > 1 && (
                    <span className="text-2xs text-text-tertiary">v{tool.version}</span>
                  )}
                </div>
                <p className="text-xs text-text-secondary mb-3 leading-relaxed">
                  {truncateText(tool.description, 120)}
                </p>
                <div className="flex items-center justify-between text-2xs text-text-tertiary">
                  <span>{tool.ownerName || 'Unknown'}</span>
                  <span>{formatDate(tool.createdAt)}</span>
                </div>
              </div>
            ))}
          </div>

          {data.totalPages > 1 && (
            <div className="flex justify-center pt-2">
              <Pagination
                currentPage={data.page}
                totalPages={data.totalPages}
                onPageChange={(page) => setFilters((prev) => ({ ...prev, page }))}
              />
            </div>
          )}
        </>
      )}
    </div>
  )
}
