import { useState, type ReactNode } from 'react'
import { cn } from '@/lib/utils'
import { Pagination } from './Pagination'

export interface Column<T> {
  key: string
  header: string
  render: (item: T) => ReactNode
  sortable?: boolean
  className?: string
}

interface DataTableProps<T> {
  columns: Column<T>[]
  data: T[]
  keyExtractor: (item: T) => string
  onRowClick?: (item: T) => void
  currentPage?: number
  totalPages?: number
  onPageChange?: (page: number) => void
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
  onSort?: (key: string) => void
  className?: string
  emptyMessage?: string
}

export function DataTable<T>({
  columns,
  data,
  keyExtractor,
  onRowClick,
  currentPage,
  totalPages,
  onPageChange,
  sortBy,
  sortOrder,
  onSort,
  className,
  emptyMessage = 'No data available',
}: DataTableProps<T>) {
  const [hoveredSort, setHoveredSort] = useState<string | null>(null)

  const handleSort = (key: string) => {
    if (onSort) {
      onSort(key)
    }
  }

  const getSortIndicator = (key: string) => {
    if (sortBy !== key) {
      return hoveredSort === key ? ' ^' : ''
    }
    return sortOrder === 'asc' ? ' ^' : ' v'
  }

  if (data.length === 0) {
    return (
      <div className="card p-8 text-center">
        <p className="text-sm text-text-secondary">{emptyMessage}</p>
      </div>
    )
  }

  return (
    <div className={cn('bg-white', className)}>
      <div className="overflow-x-auto">
        <table className="w-full">
          <thead>
            <tr className="border-b-2 border-b-primary-900">
              {columns.map((col) => (
                <th
                  key={col.key}
                  className={cn(
                    'text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3',
                    col.sortable && 'cursor-pointer select-none hover:text-text',
                    col.className,
                  )}
                  onClick={() => col.sortable && handleSort(col.key)}
                  onMouseEnter={() => col.sortable && setHoveredSort(col.key)}
                  onMouseLeave={() => setHoveredSort(null)}
                >
                  {col.header}
                  {col.sortable && (
                    <span className="text-text-tertiary">{getSortIndicator(col.key)}</span>
                  )}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {data.map((item) => (
              <tr
                key={keyExtractor(item)}
                className={cn(
                  'table-row',
                  onRowClick && 'cursor-pointer',
                )}
                onClick={() => onRowClick?.(item)}
              >
                {columns.map((col) => (
                  <td key={col.key} className={cn('px-4 py-3 text-sm', col.className)}>
                    {col.render(item)}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {currentPage && totalPages && onPageChange && (
        <div className="px-4 py-3 border-b border-b-border flex justify-between items-center">
          <span className="text-xs text-text-secondary">
            Page {currentPage} of {totalPages}
          </span>
          <Pagination
            currentPage={currentPage}
            totalPages={totalPages}
            onPageChange={onPageChange}
          />
        </div>
      )}
    </div>
  )
}
