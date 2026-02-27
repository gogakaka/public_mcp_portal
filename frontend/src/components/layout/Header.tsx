import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/stores/authStore'
import { useUIStore } from '@/stores/uiStore'

export function Header() {
  const currentPage = useUIStore((s) => s.currentPage)
  const toggleSidebar = useUIStore((s) => s.toggleSidebar)
  const logout = useAuthStore((s) => s.logout)
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <header className="bg-white border-b border-b-border px-4 sm:px-6 py-3 flex items-center justify-between">
      <div className="flex items-center gap-3">
        <button
          onClick={toggleSidebar}
          className="lg:hidden text-sm text-text-secondary hover:text-text px-2 py-1 transition-colors"
        >
          Menu
        </button>
        <h2 className="text-sm font-semibold text-text">{currentPage}</h2>
      </div>
      <div className="flex items-center gap-4">
        <button
          onClick={handleLogout}
          className="text-xs text-text-secondary hover:text-text transition-colors"
        >
          Sign Out
        </button>
      </div>
    </header>
  )
}
