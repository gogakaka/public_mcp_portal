import { cn } from '@/lib/utils'

interface ErrorStateProps {
  message?: string
  onRetry?: () => void
  className?: string
}

export function ErrorState({
  message = 'Something went wrong. Please try again.',
  onRetry,
  className,
}: ErrorStateProps) {
  return (
    <div
      className={cn(
        'border-l-2 border-b border-l-red-600 border-b-border bg-white p-6 flex flex-col items-center justify-center text-center min-h-[200px]',
        className,
      )}
    >
      <h3 className="text-sm font-medium text-red-700 mb-1">Error</h3>
      <p className="text-xs text-text-secondary max-w-md mb-4">{message}</p>
      {onRetry && (
        <button onClick={onRetry} className="btn-secondary text-xs">
          Try Again
        </button>
      )}
    </div>
  )
}
