import { NavLink } from 'react-router-dom'
import { cn } from '@/lib/utils'
import { useAuthStore } from '@/stores/authStore'
import { useUIStore } from '@/stores/uiStore'
import { UserRole } from '@/types'

interface NavItem {
  label: string
  path: string
  adminOnly?: boolean
}

const navItems: NavItem[] = [
  { label: 'Dashboard', path: '/dashboard' },
  { label: 'Tools', path: '/tools' },
  { label: 'API Keys', path: '/api-keys' },
  { label: 'Permissions', path: '/permissions' },
  { label: 'Audit Logs', path: '/audit-logs' },
  { label: 'Playground', path: '/playground' },
  { label: 'Cube.js', path: '/cube', adminOnly: true },
  { label: 'AWS MCP', path: '/aws-mcp', adminOnly: true },
  { label: 'Settings', path: '/settings', adminOnly: true },
]

export function Sidebar() {
  const user = useAuthStore((s) => s.user)
  const sidebarOpen = useUIStore((s) => s.sidebarOpen)
  const setSidebarOpen = useUIStore((s) => s.setSidebarOpen)
  const isAdmin = user?.role === UserRole.ADMIN

  const filteredItems = navItems.filter((item) => !item.adminOnly || isAdmin)

  return (
    <>
      {sidebarOpen && (
        <div
          className="fixed inset-0 bg-primary-950/30 z-20 lg:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}
      <aside
        className={cn(
          'fixed top-0 left-0 h-full bg-white border-r border-r-border z-30 w-56 transition-transform duration-200 lg:translate-x-0 lg:static lg:z-0',
          sidebarOpen ? 'translate-x-0' : '-translate-x-full',
        )}
      >
        <div className="flex flex-col h-full">
          <div className="px-5 py-5 border-b border-b-border">
            <h1 className="text-base font-bold text-text tracking-tight">UMG</h1>
            <p className="text-2xs text-text-tertiary mt-0.5">Universal MCP Gateway</p>
          </div>

          <nav className="flex-1 py-3 px-2 space-y-0.5 overflow-y-auto">
            {filteredItems.map((item) => (
              <NavLink
                key={item.path}
                to={item.path}
                onClick={() => setSidebarOpen(false)}
                className={({ isActive }) =>
                  cn(
                    'block px-3 py-2 text-sm transition-colors',
                    isActive
                      ? 'nav-item-active font-medium text-text'
                      : 'nav-item text-text-secondary hover:text-text',
                  )
                }
              >
                {item.label}
              </NavLink>
            ))}
          </nav>

          <div className="px-5 py-4 border-t border-t-border">
            <p className="text-2xs text-text-tertiary">Signed in as</p>
            <p className="text-xs font-medium text-text truncate">{user?.name || user?.email}</p>
            <p className="text-2xs text-text-tertiary">{user?.role}</p>
          </div>
        </div>
      </aside>
    </>
  )
}
