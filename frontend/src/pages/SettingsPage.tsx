import { useEffect, useState, useMemo } from 'react'
import { useUsers, useUpdateUserRole } from '@/api/auth'
import { useUIStore } from '@/stores/uiStore'
import { useAuthStore } from '@/stores/authStore'
import { LoadingSkeleton } from '@/components/common/LoadingSkeleton'
import { ErrorState } from '@/components/common/ErrorState'
import { EmptyState } from '@/components/common/EmptyState'
import { StatusBadge } from '@/components/common/StatusBadge'
import { ConfirmDialog } from '@/components/common/ConfirmDialog'
import { UserRole } from '@/types'
import type { User } from '@/types'
import { formatDate } from '@/lib/utils'

/* 사용자 역할 한글 표시 매핑 */
const roleLabels: Record<string, string> = {
  [UserRole.ADMIN]: '관리자',
  [UserRole.DEVELOPER]: '개발자',
  [UserRole.VIEWER]: '뷰어',
}

export default function SettingsPage() {
  const setCurrentPage = useUIStore((s) => s.setCurrentPage)
  const currentUser = useAuthStore((s) => s.user)

  useEffect(() => {
    setCurrentPage('Settings')
  }, [setCurrentPage])

  /* 사용자 목록 조회 */
  const { data: users, isLoading, isError, refetch } = useUsers()
  const updateRoleMutation = useUpdateUserRole()

  /* 역할 변경 확인 대화상자 상태 */
  const [roleChange, setRoleChange] = useState<{ user: User; newRole: UserRole } | null>(null)

  /* 검색 필터 상태 */
  const [searchQuery, setSearchQuery] = useState('')

  /* 검색 필터 적용된 사용자 목록 */
  const filteredUsers = useMemo(() => {
    if (!users) return []
    if (!searchQuery.trim()) return users
    const query = searchQuery.toLowerCase()
    return users.filter(
      (user: User) =>
        user.name.toLowerCase().includes(query) ||
        user.email.toLowerCase().includes(query) ||
        user.department.toLowerCase().includes(query),
    )
  }, [users, searchQuery])

  /* 역할 변경 핸들러 */
  const handleRoleChange = (user: User, newRole: string) => {
    if (newRole === user.role) return
    setRoleChange({ user, newRole: newRole as UserRole })
  }

  /* 역할 변경 확인 */
  const confirmRoleChange = () => {
    if (!roleChange) return
    updateRoleMutation.mutate(
      {
        userId: roleChange.user.id,
        role: roleChange.newRole,
      },
      {
        onSettled: () => {
          setRoleChange(null)
        },
      },
    )
  }

  /* 활성 사용자 수 계산 */
  const activeUserCount = useMemo(() => {
    if (!users) return 0
    return users.filter((u: User) => u.active).length
  }, [users])

  /* 역할별 사용자 수 계산 */
  const usersByRole = useMemo(() => {
    if (!users) return {}
    const counts: Record<string, number> = {}
    users.forEach((u: User) => {
      counts[u.role] = (counts[u.role] || 0) + 1
    })
    return counts
  }, [users])

  /* 로딩 상태 */
  if (isLoading) {
    return (
      <div className="space-y-6">
        <div className="section-header">
          <div className="h-5 w-32 bg-primary-100 animate-pulse" />
          <div className="h-3 w-56 bg-primary-100 animate-pulse mt-1" />
        </div>
        <LoadingSkeleton variant="stat" />
        <LoadingSkeleton variant="table" rows={5} />
      </div>
    )
  }

  /* 에러 상태 */
  if (isError) {
    return <ErrorState message="설정 정보를 불러오는데 실패했습니다." onRetry={refetch} />
  }

  return (
    <div className="space-y-6">
      {/* 페이지 헤더 */}
      <div className="section-header">
        <h2 className="text-base font-semibold text-text">설정</h2>
        <p className="text-2xs text-text-tertiary">시스템 관리 및 사용자 관리</p>
      </div>

      {/* 사용자 통계 요약 */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="stat-block pt-4">
          <p className="text-2xs text-text-secondary uppercase tracking-wider font-medium">
            전체 사용자
          </p>
          <p className="text-2xl font-bold text-text mt-1">{users?.length || 0}</p>
          <p className="text-2xs text-text-tertiary mt-0.5">등록된 사용자</p>
        </div>
        <div className="stat-block pt-4">
          <p className="text-2xs text-text-secondary uppercase tracking-wider font-medium">
            활성 사용자
          </p>
          <p className="text-2xl font-bold text-text mt-1">{activeUserCount}</p>
          <p className="text-2xs text-text-tertiary mt-0.5">현재 활성 상태</p>
        </div>
        {Object.entries(usersByRole).map(([role, count]) => (
          <div key={role} className="stat-block pt-4">
            <p className="text-2xs text-text-secondary uppercase tracking-wider font-medium">
              {roleLabels[role] || role}
            </p>
            <p className="text-2xl font-bold text-text mt-1">{count}</p>
            <p className="text-2xs text-text-tertiary mt-0.5">{role} 역할 사용자</p>
          </div>
        ))}
      </div>

      {/* 사용자 관리 테이블 */}
      <div className="card p-0">
        <div className="px-4 py-3 border-b border-b-border flex flex-col sm:flex-row sm:items-center sm:justify-between gap-2">
          <div>
            <h3 className="text-xs font-semibold text-text">사용자 관리</h3>
            <p className="text-2xs text-text-tertiary">사용자 역할 및 접근 권한을 관리합니다</p>
          </div>
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="이름, 이메일, 부서 검색..."
            className="input-field text-xs w-full sm:w-56"
          />
        </div>

        {filteredUsers.length === 0 ? (
          <EmptyState
            title="사용자를 찾을 수 없습니다"
            description={
              searchQuery
                ? '검색 조건에 맞는 사용자가 없습니다. 검색어를 변경해 보세요.'
                : '등록된 사용자가 없습니다.'
            }
            className="border-l-0 border-b-0"
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b-2 border-b-primary-900">
                  <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                    이름
                  </th>
                  <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                    이메일
                  </th>
                  <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                    부서
                  </th>
                  <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                    역할
                  </th>
                  <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                    상태
                  </th>
                  <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                    가입일
                  </th>
                </tr>
              </thead>
              <tbody>
                {filteredUsers.map((user: User) => {
                  const isSelf = user.id === currentUser?.id
                  return (
                    <tr key={user.id} className="table-row">
                      <td className="px-4 py-3">
                        <p className="text-sm text-text font-medium">{user.name}</p>
                        {isSelf && (
                          <span className="text-2xs text-text-tertiary">(본인)</span>
                        )}
                      </td>
                      <td className="px-4 py-3 text-xs text-text-secondary">{user.email}</td>
                      <td className="px-4 py-3 text-xs text-text-secondary">{user.department}</td>
                      <td className="px-4 py-3">
                        {isSelf ? (
                          /* 본인의 역할은 변경 불가 */
                          <span className="badge-info">
                            {roleLabels[user.role] || user.role}
                          </span>
                        ) : (
                          /* 다른 사용자의 역할은 드롭다운으로 변경 가능 */
                          <select
                            value={user.role}
                            onChange={(e) => handleRoleChange(user, e.target.value)}
                            className="input-field text-xs py-1"
                          >
                            {Object.values(UserRole).map((role) => (
                              <option key={role} value={role}>
                                {roleLabels[role] || role}
                              </option>
                            ))}
                          </select>
                        )}
                      </td>
                      <td className="px-4 py-3">
                        <StatusBadge status={user.active ? 'active' : 'inactive'} />
                      </td>
                      <td className="px-4 py-3 text-xs text-text-tertiary">
                        {formatDate(user.createdAt)}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* 시스템 정보 */}
      <div className="card p-0">
        <div className="px-4 py-3 border-b border-b-border">
          <h3 className="text-xs font-semibold text-text">시스템 정보</h3>
          <p className="text-2xs text-text-tertiary">현재 시스템 구성 및 상태 정보</p>
        </div>
        <div className="p-4 space-y-3">
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            <div className="border-l-2 border-l-primary-900 pl-3">
              <p className="text-2xs text-text-tertiary uppercase tracking-wider font-medium">
                버전
              </p>
              <p className="text-sm text-text mt-0.5">1.0.0</p>
            </div>
            <div className="border-l-2 border-l-primary-900 pl-3">
              <p className="text-2xs text-text-tertiary uppercase tracking-wider font-medium">
                API 엔드포인트
              </p>
              <p className="text-sm font-mono text-text mt-0.5">/api</p>
            </div>
            <div className="border-l-2 border-l-primary-900 pl-3">
              <p className="text-2xs text-text-tertiary uppercase tracking-wider font-medium">
                환경
              </p>
              <p className="text-sm text-text mt-0.5">Production</p>
            </div>
            <div className="border-l-2 border-l-primary-900 pl-3">
              <p className="text-2xs text-text-tertiary uppercase tracking-wider font-medium">
                프론트엔드
              </p>
              <p className="text-sm text-text mt-0.5">React 18 + TypeScript</p>
            </div>
            <div className="border-l-2 border-l-primary-900 pl-3">
              <p className="text-2xs text-text-tertiary uppercase tracking-wider font-medium">
                상태 관리
              </p>
              <p className="text-sm text-text mt-0.5">Zustand + React Query</p>
            </div>
            <div className="border-l-2 border-l-primary-900 pl-3">
              <p className="text-2xs text-text-tertiary uppercase tracking-wider font-medium">
                현재 사용자
              </p>
              <p className="text-sm text-text mt-0.5">
                {currentUser?.name || currentUser?.email || '--'}
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* 역할 변경 확인 대화상자 */}
      <ConfirmDialog
        isOpen={!!roleChange}
        onClose={() => setRoleChange(null)}
        onConfirm={confirmRoleChange}
        title="역할 변경 확인"
        message={
          roleChange
            ? `${roleChange.user.name}의 역할을 ${roleLabels[roleChange.user.role] || roleChange.user.role}에서 ${roleLabels[roleChange.newRole] || roleChange.newRole}(으)로 변경하시겠습니까?`
            : ''
        }
        confirmLabel="변경"
        isLoading={updateRoleMutation.isPending}
      />
    </div>
  )
}
