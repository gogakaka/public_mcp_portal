import { cn } from '@/lib/utils'
import { useUIStore, type Notification } from '@/stores/uiStore'

const typeStyles: Record<Notification['type'], string> = {
  success: 'border-l-emerald-600 bg-emerald-50 text-emerald-800',
  error: 'border-l-red-600 bg-red-50 text-red-800',
  warning: 'border-l-amber-600 bg-amber-50 text-amber-800',
  info: 'border-l-blue-600 bg-blue-50 text-blue-800',
}

function ToastItem({ notification }: { notification: Notification }) {
  const removeNotification = useUIStore((s) => s.removeNotification)

  return (
    <div
      className={cn(
        'border-l-2 border-b border-b-border px-4 py-3 flex items-start justify-between gap-3 min-w-[300px] max-w-[400px] animate-in',
        typeStyles[notification.type],
      )}
    >
      <div className="flex-1 min-w-0">
        <p className="text-xs font-medium capitalize">{notification.type}</p>
        <p className="text-xs mt-0.5 opacity-90">{notification.message}</p>
      </div>
      <button
        onClick={() => removeNotification(notification.id)}
        className="text-2xs opacity-60 hover:opacity-100 transition-opacity shrink-0 pt-0.5"
      >
        Dismiss
      </button>
    </div>
  )
}

export function ToastContainer() {
  const notifications = useUIStore((s) => s.notifications)

  if (notifications.length === 0) return null

  return (
    <div className="fixed bottom-4 right-4 z-[100] flex flex-col gap-2">
      {notifications.map((notification) => (
        <ToastItem key={notification.id} notification={notification} />
      ))}
    </div>
  )
}
