import { cn } from '@/lib/utils'
import { ToolStatus, AuditStatus } from '@/types'

interface StatusBadgeProps {
  status: string
  className?: string
}

const statusStyles: Record<string, string> = {
  [ToolStatus.ACTIVE]: 'badge-success',
  [ToolStatus.PENDING]: 'badge-warning',
  [ToolStatus.DISABLED]: 'badge-error',
  [ToolStatus.REJECTED]: 'badge-error',
  [AuditStatus.SUCCESS]: 'badge-success',
  [AuditStatus.FAILURE]: 'badge-error',
  [AuditStatus.ERROR]: 'badge-error',
  [AuditStatus.PENDING]: 'badge-warning',
  active: 'badge-success',
  inactive: 'badge-error',
  true: 'badge-success',
  false: 'badge-error',
}

export function StatusBadge({ status, className }: StatusBadgeProps) {
  const style = statusStyles[status] || 'badge-info'

  return (
    <span className={cn(style, className)}>
      {status}
    </span>
  )
}
