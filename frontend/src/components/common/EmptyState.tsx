import { cn } from '@/lib/utils'

interface EmptyStateProps {
  title: string
  description?: string
  actionLabel?: string
  onAction?: () => void
  className?: string
}

export function EmptyState({
  title,
  description,
  actionLabel,
  onAction,
  className,
}: EmptyStateProps) {
  return (
    <div
      className={cn(
        'card p-8 flex flex-col items-center justify-center text-center min-h-[200px]',
        className,
      )}
    >
      <h3 className="text-sm font-medium text-text mb-1">{title}</h3>
      {description && (
        <p className="text-xs text-text-secondary max-w-md mb-4">{description}</p>
      )}
      {actionLabel && onAction && (
        <button onClick={onAction} className="btn-primary text-xs">
          {actionLabel}
        </button>
      )}
    </div>
  )
}
