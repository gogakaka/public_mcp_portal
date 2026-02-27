import { useEffect, useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useApiKeys, useCreateApiKey, useDeleteApiKey, useToggleApiKey } from '@/api/apiKeys'
import { useUIStore } from '@/stores/uiStore'
import { LoadingSkeleton } from '@/components/common/LoadingSkeleton'
import { ErrorState } from '@/components/common/ErrorState'
import { EmptyState } from '@/components/common/EmptyState'
import { Modal } from '@/components/common/Modal'
import { FormField } from '@/components/common/FormField'
import { ConfirmDialog } from '@/components/common/ConfirmDialog'
import { StatusBadge } from '@/components/common/StatusBadge'
import { formatDate, formatRelativeTime, copyToClipboard } from '@/lib/utils'
import type { ApiKey, ApiKeyCreateResponse } from '@/types'

const createKeySchema = z.object({
  name: z.string().min(2, 'Name must be at least 2 characters').max(50),
  rateLimit: z.coerce.number().int().min(1).max(10000).optional(),
  rateLimitWindow: z.string().optional(),
  expiresAt: z.string().optional(),
})

type CreateKeyForm = z.infer<typeof createKeySchema>

export default function ApiKeysPage() {
  const setCurrentPage = useUIStore((s) => s.setCurrentPage)
  const addNotification = useUIStore((s) => s.addNotification)

  useEffect(() => {
    setCurrentPage('API Keys')
  }, [setCurrentPage])

  const [showCreateModal, setShowCreateModal] = useState(false)
  const [createdKey, setCreatedKey] = useState<ApiKeyCreateResponse | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<ApiKey | null>(null)

  const { data: apiKeys, isLoading, isError, refetch } = useApiKeys()
  const createMutation = useCreateApiKey()
  const deleteMutation = useDeleteApiKey()
  const toggleMutation = useToggleApiKey()

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<CreateKeyForm>({
    resolver: zodResolver(createKeySchema),
    defaultValues: {
      rateLimit: 100,
      rateLimitWindow: '1m',
    },
  })

  const onCreateSubmit = (data: CreateKeyForm) => {
    createMutation.mutate(
      {
        name: data.name,
        rateLimit: data.rateLimit,
        rateLimitWindow: data.rateLimitWindow,
        expiresAt: data.expiresAt || undefined,
      },
      {
        onSuccess: (result) => {
          setCreatedKey(result)
          setShowCreateModal(false)
          reset()
        },
      },
    )
  }

  const handleCopyKey = async (key: string) => {
    await copyToClipboard(key)
    addNotification({ type: 'success', message: 'API key copied to clipboard' })
  }

  const handleDelete = () => {
    if (!deleteTarget) return
    deleteMutation.mutate(deleteTarget.id)
    setDeleteTarget(null)
  }

  const handleToggle = (key: ApiKey) => {
    toggleMutation.mutate({ id: key.id, active: !key.active })
  }

  if (isLoading) {
    return <LoadingSkeleton variant="table" rows={5} />
  }

  if (isError) {
    return <ErrorState message="Failed to load API keys" onRetry={refetch} />
  }

  return (
    <div className="space-y-4">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <div className="section-header">
          <h2 className="text-base font-semibold text-text">API Keys</h2>
          <p className="text-2xs text-text-tertiary">Manage your API access keys</p>
        </div>
        <button onClick={() => setShowCreateModal(true)} className="btn-primary text-xs self-start">
          Create New Key
        </button>
      </div>

      {createdKey && (
        <div className="border-l-2 border-b border-l-emerald-600 border-b-border bg-emerald-50 p-4">
          <h3 className="text-xs font-semibold text-emerald-800 mb-2">
            API Key Created Successfully
          </h3>
          <p className="text-2xs text-emerald-700 mb-3">
            Copy this key now. It will not be shown again.
          </p>
          <div className="flex items-center gap-2">
            <code className="text-xs font-mono bg-white px-3 py-1.5 border-l border-b border-l-emerald-600 border-b-border flex-1 break-all">
              {createdKey.rawKey}
            </code>
            <button
              onClick={() => handleCopyKey(createdKey.rawKey)}
              className="btn-primary text-xs shrink-0"
            >
              Copy
            </button>
          </div>
          <button
            onClick={() => setCreatedKey(null)}
            className="text-2xs text-emerald-600 hover:text-emerald-700 mt-3 transition-colors"
          >
            Dismiss
          </button>
        </div>
      )}

      {apiKeys && apiKeys.length === 0 ? (
        <EmptyState
          title="No API keys"
          description="Create an API key to authenticate your requests to the gateway."
          actionLabel="Create New Key"
          onAction={() => setShowCreateModal(true)}
        />
      ) : (
        <div className="card p-0">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="border-b-2 border-b-primary-900">
                  <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                    Name
                  </th>
                  <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                    Key Prefix
                  </th>
                  <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                    Rate Limit
                  </th>
                  <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                    Expires
                  </th>
                  <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                    Status
                  </th>
                  <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                    Last Used
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
                {apiKeys?.map((key: ApiKey) => (
                  <tr key={key.id} className="table-row">
                    <td className="px-4 py-3 text-sm font-medium text-text">{key.name}</td>
                    <td className="px-4 py-3 text-xs font-mono text-text-secondary">
                      {key.keyPrefix}...
                    </td>
                    <td className="px-4 py-3 text-xs text-text-secondary">
                      {key.rateLimit}/{key.rateLimitWindow}
                    </td>
                    <td className="px-4 py-3 text-xs text-text-secondary">
                      {key.expiresAt ? formatDate(key.expiresAt) : 'Never'}
                    </td>
                    <td className="px-4 py-3">
                      <StatusBadge status={key.active ? 'active' : 'inactive'} />
                    </td>
                    <td className="px-4 py-3 text-xs text-text-tertiary">
                      {key.lastUsedAt ? formatRelativeTime(key.lastUsedAt) : 'Never'}
                    </td>
                    <td className="px-4 py-3 text-xs text-text-tertiary">
                      {formatDate(key.createdAt)}
                    </td>
                    <td className="px-4 py-3 text-right">
                      <div className="flex items-center justify-end gap-2">
                        <button
                          onClick={() => handleToggle(key)}
                          className="text-2xs text-text-secondary hover:text-text transition-colors"
                        >
                          {key.active ? 'Deactivate' : 'Activate'}
                        </button>
                        <button
                          onClick={() => setDeleteTarget(key)}
                          className="text-2xs text-red-600 hover:text-red-700 transition-colors"
                        >
                          Delete
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <Modal
        isOpen={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        title="Create API Key"
      >
        <form onSubmit={handleSubmit(onCreateSubmit)} className="space-y-4">
          <FormField label="Key Name" error={errors.name?.message} required>
            <input
              {...register('name')}
              className="input-field w-full"
              placeholder="Production Key"
            />
          </FormField>
          <FormField label="Rate Limit" error={errors.rateLimit?.message} hint="Requests per window">
            <input
              {...register('rateLimit')}
              type="number"
              className="input-field w-full"
              placeholder="100"
            />
          </FormField>
          <FormField label="Rate Limit Window" error={errors.rateLimitWindow?.message}>
            <select {...register('rateLimitWindow')} className="input-field w-full">
              <option value="1m">1 Minute</option>
              <option value="5m">5 Minutes</option>
              <option value="1h">1 Hour</option>
              <option value="1d">1 Day</option>
            </select>
          </FormField>
          <FormField label="Expires At" error={errors.expiresAt?.message} hint="Leave blank for no expiration">
            <input
              {...register('expiresAt')}
              type="date"
              className="input-field w-full"
            />
          </FormField>
          <div className="flex justify-end gap-3 pt-2">
            <button type="button" onClick={() => setShowCreateModal(false)} className="btn-secondary text-xs">
              Cancel
            </button>
            <button type="submit" disabled={createMutation.isPending} className="btn-primary text-xs disabled:opacity-50">
              {createMutation.isPending ? 'Creating...' : 'Create Key'}
            </button>
          </div>
        </form>
      </Modal>

      <ConfirmDialog
        isOpen={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        onConfirm={handleDelete}
        title="Delete API Key"
        message={`Are you sure you want to delete the key "${deleteTarget?.name}"? This action cannot be undone and will immediately revoke access.`}
        confirmLabel="Delete Key"
        variant="danger"
        isLoading={deleteMutation.isPending}
      />
    </div>
  )
}
