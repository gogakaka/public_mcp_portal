import { Suspense, lazy } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { AppLayout } from '@/components/layout/AppLayout'
import { ProtectedRoute } from '@/components/auth/ProtectedRoute'
import { LoadingSkeleton } from '@/components/common/LoadingSkeleton'

/* 페이지 컴포넌트 지연 로딩 */
const LoginPage = lazy(() => import('@/pages/LoginPage'))
const RegisterPage = lazy(() => import('@/pages/RegisterPage'))
const DashboardPage = lazy(() => import('@/pages/DashboardPage'))
const ToolsPage = lazy(() => import('@/pages/ToolsPage'))
const ToolDetailPage = lazy(() => import('@/pages/ToolDetailPage'))
const ToolCreatePage = lazy(() => import('@/pages/ToolCreatePage'))
const ApiKeysPage = lazy(() => import('@/pages/ApiKeysPage'))
const PermissionsPage = lazy(() => import('@/pages/PermissionsPage'))
const AuditLogsPage = lazy(() => import('@/pages/AuditLogsPage'))
const PlaygroundPage = lazy(() => import('@/pages/PlaygroundPage'))
const SettingsPage = lazy(() => import('@/pages/SettingsPage'))
const CubeManagementPage = lazy(() => import('@/pages/CubeManagementPage'))
const AwsMcpManagementPage = lazy(() => import('@/pages/AwsMcpManagementPage'))

/* 지연 로딩 중 표시할 폴백 컴포넌트 */
function PageFallback() {
  return (
    <div className="p-6">
      <LoadingSkeleton variant="card" rows={3} />
    </div>
  )
}

export default function App() {
  return (
    <Suspense fallback={<PageFallback />}>
      <Routes>
        {/* 공개 라우트 - 인증 불필요 */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/register" element={<RegisterPage />} />

        {/* 보호된 라우트 - 인증 필요, AppLayout으로 감싸기 */}
        <Route
          element={
            <ProtectedRoute>
              <AppLayout />
            </ProtectedRoute>
          }
        >
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/tools" element={<ToolsPage />} />
          <Route path="/tools/new" element={<ToolCreatePage />} />
          <Route path="/tools/:id" element={<ToolDetailPage />} />
          <Route path="/api-keys" element={<ApiKeysPage />} />
          <Route path="/permissions" element={<PermissionsPage />} />
          <Route path="/audit-logs" element={<AuditLogsPage />} />
          <Route path="/playground" element={<PlaygroundPage />} />
          <Route path="/cube" element={<CubeManagementPage />} />
          <Route path="/aws-mcp" element={<AwsMcpManagementPage />} />
          <Route path="/settings" element={<SettingsPage />} />
        </Route>

        {/* 루트 경로를 대시보드로 리다이렉트 */}
        <Route path="/" element={<Navigate to="/dashboard" replace />} />

        {/* 알 수 없는 경로를 대시보드로 리다이렉트 */}
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </Suspense>
  )
}
