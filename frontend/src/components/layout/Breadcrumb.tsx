import { Link } from 'react-router-dom'
import { cn } from '@/lib/utils'

export interface BreadcrumbItem {
  label: string
  path?: string
}

interface BreadcrumbProps {
  items: BreadcrumbItem[]
  className?: string
}

export function Breadcrumb({ items, className }: BreadcrumbProps) {
  return (
    <nav className={cn('flex items-center gap-1.5 text-xs', className)}>
      {items.map((item, index) => {
        const isLast = index === items.length - 1
        return (
          <span key={index} className="flex items-center gap-1.5">
            {index > 0 && <span className="text-text-tertiary">/</span>}
            {item.path && !isLast ? (
              <Link
                to={item.path}
                className="text-text-secondary hover:text-text transition-colors"
              >
                {item.label}
              </Link>
            ) : (
              <span className={cn(isLast ? 'text-text font-medium' : 'text-text-secondary')}>
                {item.label}
              </span>
            )}
          </span>
        )
      })}
    </nav>
  )
}
