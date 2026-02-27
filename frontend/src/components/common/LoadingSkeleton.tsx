import { cn } from '@/lib/utils'

interface LoadingSkeletonProps {
  rows?: number
  className?: string
  variant?: 'card' | 'table' | 'stat' | 'text'
}

function SkeletonPulse({ className }: { className?: string }) {
  return (
    <div
      className={cn(
        'animate-pulse bg-primary-100',
        className,
      )}
    />
  )
}

export function LoadingSkeleton({ rows = 3, className, variant = 'card' }: LoadingSkeletonProps) {
  if (variant === 'stat') {
    return (
      <div className={cn('grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4', className)}>
        {Array.from({ length: 4 }).map((_, i) => (
          <div key={i} className="stat-block pt-4">
            <SkeletonPulse className="h-3 w-20 mb-2" />
            <SkeletonPulse className="h-8 w-16 mb-1" />
            <SkeletonPulse className="h-3 w-24" />
          </div>
        ))}
      </div>
    )
  }

  if (variant === 'table') {
    return (
      <div className={cn('space-y-0', className)}>
        <div className="table-row px-4 py-3 flex gap-4">
          {Array.from({ length: 5 }).map((_, i) => (
            <SkeletonPulse key={i} className="h-3 flex-1" />
          ))}
        </div>
        {Array.from({ length: rows }).map((_, i) => (
          <div key={i} className="table-row px-4 py-3 flex gap-4">
            {Array.from({ length: 5 }).map((_, j) => (
              <SkeletonPulse key={j} className="h-3 flex-1" />
            ))}
          </div>
        ))}
      </div>
    )
  }

  if (variant === 'text') {
    return (
      <div className={cn('space-y-3', className)}>
        {Array.from({ length: rows }).map((_, i) => (
          <SkeletonPulse key={i} className={cn('h-3', i === rows - 1 ? 'w-2/3' : 'w-full')} />
        ))}
      </div>
    )
  }

  return (
    <div className={cn('grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4', className)}>
      {Array.from({ length: rows }).map((_, i) => (
        <div key={i} className="card p-4 space-y-3">
          <SkeletonPulse className="h-4 w-3/4" />
          <SkeletonPulse className="h-3 w-1/2" />
          <SkeletonPulse className="h-3 w-full" />
          <SkeletonPulse className="h-3 w-2/3" />
        </div>
      ))}
    </div>
  )
}
