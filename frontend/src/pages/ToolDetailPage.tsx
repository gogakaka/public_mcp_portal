import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { useTool, useApproveTool, useRejectTool, useDeleteTool } from '@/api/tools'
import { usePermissions } from '@/api/permissions'
import { useAuditLogs } from '@/api/auditLogs'
import { useAuthStore } from '@/stores/authStore'
import { useUIStore } from '@/stores/uiStore'
import { LoadingSkeleton } from '@/components/common/LoadingSkeleton'
import { ErrorState } from '@/components/common/ErrorState'
import { StatusBadge } from '@/components/common/StatusBadge'
import { ConfirmDialog } from '@/components/common/ConfirmDialog'
import { Breadcrumb } from '@/components/layout/Breadcrumb'
import { ToolStatus, UserRole } from '@/types'
import { formatDate, formatRelativeTime, formatExecutionTime, maskSensitiveValue } from '@/lib/utils'

export default function ToolDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const setCurrentPage = useUIStore((s) => s.setCurrentPage)
  const user = useAuthStore((s) => s.user)
  const isAdmin = user?.role === UserRole.ADMIN

  const [showDeleteConfirm, setShowDeleteConfirm] = useState(false)
  const [showSchemaRaw, setShowSchemaRaw] = useState(false)

  useEffect(() => {
    setCurrentPage('Tool Detail')
  }, [setCurrentPage])

  const { data: tool, isLoading, isError, refetch } = useTool(id)
  const { data: permissions } = usePermissions(id)
  const { data: auditLogs } = useAuditLogs({ toolId: id, pageSize: 10 })

  const approveMutation = useApproveTool()
  const rejectMutation = useRejectTool()
  const deleteMutation = useDeleteTool()

  if (isLoading) {
    return (
      <div className="space-y-4">
        <LoadingSkeleton variant="text" rows={2} />
        <LoadingSkeleton variant="card" rows={1} />
      </div>
    )
  }

  if (isError || !tool) {
    return <ErrorState message="Failed to load tool details" onRetry={refetch} />
  }

  const connectionFields = Object.entries(tool.connectionConfig || {}).filter(
    ([, value]) => value !== undefined && value !== null && value !== '',
  )

  const sensitiveKeys = ['apiKey', 'token', 'secret', 'password', 'credentials']

  const handleDelete = () => {
    deleteMutation.mutate(tool.id, {
      onSuccess: () => navigate('/tools'),
    })
  }

  return (
    <div className="space-y-6">
      <Breadcrumb
        items={[
          { label: 'Tools', path: '/tools' },
          { label: tool.name },
        ]}
      />

      <div className="flex flex-col sm:flex-row sm:items-start sm:justify-between gap-4">
        <div className="section-header">
          <h2 className="text-lg font-bold text-text">{tool.name}</h2>
          <div className="flex items-center gap-2 mt-1">
            <StatusBadge status={tool.status} />
            <span className="badge-info">{tool.type}</span>
            <span className="text-2xs text-text-tertiary">v{tool.version}</span>
          </div>
        </div>
        <div className="flex items-center gap-2 self-start">
          {isAdmin && tool.status === ToolStatus.PENDING && (
            <>
              <button
                onClick={() => approveMutation.mutate(tool.id)}
                disabled={approveMutation.isPending}
                className="btn-primary text-xs"
              >
                {approveMutation.isPending ? 'Approving...' : 'Approve'}
              </button>
              <button
                onClick={() => rejectMutation.mutate(tool.id)}
                disabled={rejectMutation.isPending}
                className="bg-red-700 text-white px-4 py-2 text-xs font-medium hover:bg-red-800 transition-colors"
              >
                {rejectMutation.isPending ? 'Rejecting...' : 'Reject'}
              </button>
            </>
          )}
          <button
            onClick={() => setShowDeleteConfirm(true)}
            className="text-xs text-red-600 hover:text-red-700 px-3 py-2 transition-colors"
          >
            Delete
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <div className="lg:col-span-2 space-y-6">
          <div className="card p-0">
            <div className="px-4 py-3 border-b border-b-border">
              <h3 className="text-xs font-semibold text-text">Tool Information</h3>
            </div>
            <div className="p-4 space-y-3">
              <div>
                <p className="text-2xs text-text-tertiary uppercase tracking-wider font-medium mb-0.5">
                  Description
                </p>
                <p className="text-sm text-text leading-relaxed">{tool.description}</p>
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <p className="text-2xs text-text-tertiary uppercase tracking-wider font-medium mb-0.5">
                    Owner
                  </p>
                  <p className="text-sm text-text">{tool.ownerName || tool.ownerId}</p>
                </div>
                <div>
                  <p className="text-2xs text-text-tertiary uppercase tracking-wider font-medium mb-0.5">
                    Created
                  </p>
                  <p className="text-sm text-text">{formatDate(tool.createdAt)}</p>
                </div>
                <div>
                  <p className="text-2xs text-text-tertiary uppercase tracking-wider font-medium mb-0.5">
                    Updated
                  </p>
                  <p className="text-sm text-text">{formatRelativeTime(tool.updatedAt)}</p>
                </div>
                <div>
                  <p className="text-2xs text-text-tertiary uppercase tracking-wider font-medium mb-0.5">
                    Version
                  </p>
                  <p className="text-sm text-text">{tool.version}</p>
                </div>
              </div>
            </div>
          </div>

          <div className="card p-0">
            <div className="px-4 py-3 border-b border-b-border">
              <h3 className="text-xs font-semibold text-text">Connection Configuration</h3>
            </div>
            <div className="p-4">
              {connectionFields.length > 0 ? (
                <div className="space-y-2">
                  {connectionFields.map(([key, value]) => {
                    const isSensitive = sensitiveKeys.some((sk) =>
                      key.toLowerCase().includes(sk.toLowerCase()),
                    )
                    const displayValue = isSensitive
                      ? maskSensitiveValue(String(value))
                      : Array.isArray(value)
                        ? value.join(', ')
                        : typeof value === 'object'
                          ? JSON.stringify(value)
                          : String(value)
                    return (
                      <div key={key} className="flex items-start gap-3 py-1.5 border-b border-b-border last:border-0">
                        <span className="text-xs font-mono text-text-secondary w-36 shrink-0">
                          {key}
                        </span>
                        <span className="text-xs font-mono text-text break-all">
                          {displayValue}
                        </span>
                      </div>
                    )
                  })}
                </div>
              ) : (
                <p className="text-xs text-text-tertiary">No connection configuration</p>
              )}
            </div>
          </div>

          <div className="card p-0">
            <div className="px-4 py-3 border-b border-b-border flex items-center justify-between">
              <h3 className="text-xs font-semibold text-text">Input Schema</h3>
              <button
                onClick={() => setShowSchemaRaw(!showSchemaRaw)}
                className="text-2xs text-text-secondary hover:text-text transition-colors"
              >
                {showSchemaRaw ? 'Show Formatted' : 'Show Raw JSON'}
              </button>
            </div>
            <div className="p-4">
              {showSchemaRaw ? (
                <pre className="text-xs font-mono text-text bg-surface-tertiary p-3 overflow-x-auto whitespace-pre-wrap">
                  {JSON.stringify(tool.inputSchema, null, 2)}
                </pre>
              ) : tool.inputSchema?.properties ? (
                <div className="space-y-2">
                  {Object.entries(tool.inputSchema.properties).map(([name, schema]) => {
                    const isRequired = tool.inputSchema.required?.includes(name)
                    return (
                      <div key={name} className="flex items-start gap-3 py-1.5 border-b border-b-border last:border-0">
                        <span className="text-xs font-mono text-text w-36 shrink-0">
                          {name}
                          {isRequired && <span className="text-red-600 ml-0.5">*</span>}
                        </span>
                        <span className="text-2xs text-text-secondary">{schema.type}</span>
                        {schema.description && (
                          <span className="text-2xs text-text-tertiary">{schema.description}</span>
                        )}
                      </div>
                    )
                  })}
                </div>
              ) : (
                <p className="text-xs text-text-tertiary">No input schema defined</p>
              )}
            </div>
          </div>

          {tool.responseMapping && (
            <div className="card p-0">
              <div className="px-4 py-3 border-b border-b-border">
                <h3 className="text-xs font-semibold text-text">Response Mapping</h3>
              </div>
              <div className="p-4">
                <pre className="text-xs font-mono text-text bg-surface-tertiary p-3 overflow-x-auto whitespace-pre-wrap">
                  {tool.responseMapping}
                </pre>
              </div>
            </div>
          )}
        </div>

        <div className="space-y-6">
          <div className="card p-0">
            <div className="px-4 py-3 border-b border-b-border">
              <h3 className="text-xs font-semibold text-text">Permissions</h3>
              <p className="text-2xs text-text-tertiary">Users with access</p>
            </div>
            <div className="p-2">
              {permissions && permissions.length > 0 ? (
                <div className="space-y-0">
                  {permissions.map((perm) => (
                    <div
                      key={perm.id}
                      className="px-2 py-2 border-b border-b-border last:border-0 flex items-center justify-between"
                    >
                      <div>
                        <p className="text-xs text-text">{perm.userName || perm.userId}</p>
                        <p className="text-2xs text-text-tertiary">{perm.userEmail}</p>
                      </div>
                      <span className="badge-info">{perm.accessLevel}</span>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-xs text-text-tertiary text-center py-4">No permissions set</p>
              )}
            </div>
          </div>

          <div className="card p-0">
            <div className="px-4 py-3 border-b border-b-border">
              <h3 className="text-xs font-semibold text-text">Recent Logs</h3>
              <p className="text-2xs text-text-tertiary">Last 10 executions</p>
            </div>
            <div className="p-2">
              {auditLogs?.data && auditLogs.data.length > 0 ? (
                <div className="space-y-0">
                  {auditLogs.data.map((log) => (
                    <div
                      key={log.id}
                      className="px-2 py-2 border-b border-b-border last:border-0"
                    >
                      <div className="flex items-center justify-between mb-0.5">
                        <StatusBadge status={log.status} />
                        <span className="text-2xs text-text-tertiary">
                          {formatRelativeTime(log.createdAt)}
                        </span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-2xs text-text-secondary">
                          {log.userName || log.userId}
                        </span>
                        <span className="text-2xs font-mono text-text-tertiary">
                          {formatExecutionTime(log.executionTimeMs)}
                        </span>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <p className="text-xs text-text-tertiary text-center py-4">No executions yet</p>
              )}
            </div>
          </div>
        </div>
      </div>

      <ConfirmDialog
        isOpen={showDeleteConfirm}
        onClose={() => setShowDeleteConfirm(false)}
        onConfirm={handleDelete}
        title="Delete Tool"
        message={`Are you sure you want to delete "${tool.name}"? This action cannot be undone.`}
        confirmLabel="Delete"
        variant="danger"
        isLoading={deleteMutation.isPending}
      />
    </div>
  )
}
