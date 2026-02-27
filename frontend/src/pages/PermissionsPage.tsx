import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { usePermissions, useGrantPermission, useRevokePermission } from '@/api/permissions'
import { useTools } from '@/api/tools'
import { useUsers } from '@/api/auth'
import { useUIStore } from '@/stores/uiStore'
import { LoadingSkeleton } from '@/components/common/LoadingSkeleton'
import { ErrorState } from '@/components/common/ErrorState'
import { EmptyState } from '@/components/common/EmptyState'
import { Modal } from '@/components/common/Modal'
import { FormField } from '@/components/common/FormField'
import { ConfirmDialog } from '@/components/common/ConfirmDialog'
import { AccessLevel } from '@/types'
import type { Permission } from '@/types'
import { formatDate } from '@/lib/utils'

const grantSchema = z.object({
  userId: z.string().min(1, 'Select a user'),
  toolId: z.string().min(1, 'Select a tool'),
  accessLevel: z.nativeEnum(AccessLevel, { errorMap: () => ({ message: 'Select access level' }) }),
  expiresAt: z.string().optional(),
})

type GrantForm = z.infer<typeof grantSchema>

export default function PermissionsPage() {
  const setCurrentPage = useUIStore((s) => s.setCurrentPage)

  useEffect(() => {
    setCurrentPage('Permissions')
  }, [setCurrentPage])

  const [showGrantModal, setShowGrantModal] = useState(false)
  const [revokeTarget, setRevokeTarget] = useState<Permission | null>(null)

  const { data: permissions, isLoading, isError, refetch } = usePermissions()
  const { data: toolsData } = useTools({ pageSize: 100 })
  const { data: users } = useUsers()
  const grantMutation = useGrantPermission()
  const revokeMutation = useRevokePermission()

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<GrantForm>({
    resolver: zodResolver(grantSchema),
    defaultValues: {
      accessLevel: AccessLevel.READ_ONLY,
    },
  })

  const onGrantSubmit = (data: GrantForm) => {
    grantMutation.mutate(
      {
        userId: data.userId,
        toolId: data.toolId,
        accessLevel: data.accessLevel,
        expiresAt: data.expiresAt || undefined,
      },
      {
        onSuccess: () => {
          setShowGrantModal(false)
          reset()
        },
      },
    )
  }

  const handleRevoke = () => {
    if (!revokeTarget) return
    revokeMutation.mutate(revokeTarget.id)
    setRevokeTarget(null)
  }

  if (isLoading) {
    return <LoadingSkeleton variant="table" rows={5} />
  }

  if (isError) {
    return <ErrorState message="Failed to load permissions" onRetry={refetch} />
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <div className="section-header">
          <h2 className="text-base font-semibold text-text">Permissions</h2>
          <p className="text-2xs text-text-tertiary">Manage user-tool access control</p>
        </div>
        <button onClick={() => setShowGrantModal(true)} className="btn-primary text-xs self-start">
          Grant Permission
        </button>
      </div>

      {permissions && permissions.length === 0 ? (
        <EmptyState
          title="No permissions configured"
          description="Grant permissions to allow users to access specific tools."
          actionLabel="Grant Permission"
          onAction={() => setShowGrantModal(true)}
        />
      ) : (
        <div className="card p-0">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b-2 border-b-primary-900">
                  <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                    User
                  </th>
                  <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                    Tool
                  </th>
                  <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                    Access Level
                  </th>
                  <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                    Granted By
                  </th>
                  <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                    Expires
                  </th>
                  <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                    Created
                  </th>
                  <th className="text-right text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody>
                {permissions?.map((perm: Permission) => (
                  <tr key={perm.id} className="table-row">
                    <td className="px-4 py-3">
                      <p className="text-sm text-text">{perm.userName || perm.userId}</p>
                      {perm.userEmail && (
                        <p className="text-2xs text-text-tertiary">{perm.userEmail}</p>
                      )}
                    </td>
                    <td className="px-4 py-3 text-sm text-text">
                      {perm.toolName || perm.toolId}
                    </td>
                    <td className="px-4 py-3">
                      <span className="badge-info">{perm.accessLevel}</span>
                    </td>
                    <td className="px-4 py-3 text-xs text-text-secondary">
                      {perm.grantedByName || perm.grantedBy}
                    </td>
                    <td className="px-4 py-3 text-xs text-text-secondary">
                      {perm.expiresAt ? formatDate(perm.expiresAt) : 'Never'}
                    </td>
                    <td className="px-4 py-3 text-xs text-text-tertiary">
                      {formatDate(perm.createdAt)}
                    </td>
                    <td className="px-4 py-3 text-right">
                      <button
                        onClick={() => setRevokeTarget(perm)}
                        className="text-2xs text-red-600 hover:text-red-700 transition-colors"
                      >
                        Revoke
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <Modal
        isOpen={showGrantModal}
        onClose={() => setShowGrantModal(false)}
        title="Grant Permission"
      >
        <form onSubmit={handleSubmit(onGrantSubmit)} className="space-y-4">
          <FormField label="User" error={errors.userId?.message} required>
            <select {...register('userId')} className="input-field w-full">
              <option value="">Select user...</option>
              {users?.map((user) => (
                <option key={user.id} value={user.id}>
                  {user.name} ({user.email})
                </option>
              ))}
            </select>
          </FormField>
          <FormField label="Tool" error={errors.toolId?.message} required>
            <select {...register('toolId')} className="input-field w-full">
              <option value="">Select tool...</option>
              {toolsData?.data?.map((tool) => (
                <option key={tool.id} value={tool.id}>
                  {tool.name} ({tool.type})
                </option>
              ))}
            </select>
          </FormField>
          <FormField label="Access Level" error={errors.accessLevel?.message} required>
            <select {...register('accessLevel')} className="input-field w-full">
              {Object.values(AccessLevel).map((level) => (
                <option key={level} value={level}>
                  {level}
                </option>
              ))}
            </select>
          </FormField>
          <FormField label="Expires At" error={errors.expiresAt?.message} hint="Leave blank for no expiration">
            <input {...register('expiresAt')} type="date" className="input-field w-full" />
          </FormField>
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => setShowGrantModal(false)} className="btn-secondary text-xs">
              Cancel
            </button>
            <button type="submit" disabled={grantMutation.isPending} className="btn-primary text-xs disabled:opacity-50">
              {grantMutation.isPending ? 'Granting...' : 'Grant Permission'}
            </button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        isOpen={!!revokeTarget}
        onClose={() => setRevokeTarget(null)}
        onConfirm={handleRevoke}
        title="Revoke Permission"
        message={`Are you sure you want to revoke ${revokeTarget?.userName || revokeTarget?.userId}'s access to ${revokeTarget?.toolName || revokeTarget?.toolId}?`}
        confirmLabel="Revoke"
        variant="danger"
        isLoading={revokeMutation.isPending}
      />
    </div>
  )
}
