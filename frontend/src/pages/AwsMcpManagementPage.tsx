import { useEffect, useState } from 'react'
import { useUIStore } from '@/stores/uiStore'
import { cn, formatDateTime } from '@/lib/utils'
import {
  useAwsMcpServers,
  useCreateAwsMcpServer,
  useDeleteAwsMcpServer,
  useTestAwsMcpServerConnection,
  useSyncAwsMcpServer,
  useSyncHistory,
} from '@/api/awsMcpServers'
import type {
  AwsAuthType,
  DataSourceStatus,
  AwsMcpServer,
  CreateAwsMcpServerRequest,
} from '@/types'

const AWS_REGIONS = [
  'us-east-1',
  'us-east-2',
  'us-west-1',
  'us-west-2',
  'eu-west-1',
  'eu-west-2',
  'eu-central-1',
  'ap-northeast-1',
  'ap-northeast-2',
  'ap-southeast-1',
  'ap-southeast-2',
]

const statusBadge = (status: DataSourceStatus) => {
  switch (status) {
    case 'ACTIVE':
      return 'badge-success'
    case 'INACTIVE':
      return 'text-xs px-2 py-0.5 bg-gray-100 text-gray-600'
    case 'ERROR':
      return 'badge-error'
    default:
      return 'text-xs px-2 py-0.5 bg-gray-100 text-gray-600'
  }
}

type Tab = 'servers' | 'history'

export default function AwsMcpManagementPage() {
  const [tab, setTab] = useState<Tab>('servers')
  const [showForm, setShowForm] = useState(false)
  const [historyServerId, setHistoryServerId] = useState<string>('')
  const [form, setForm] = useState<CreateAwsMcpServerRequest>({
    name: '',
    endpointUrl: '',
    region: 'us-east-1',
    service: 'execute-api',
    authType: 'IAM_KEY' as AwsAuthType,
  })

  useEffect(() => {
    useUIStore.getState().setCurrentPage('AWS MCP')
  }, [])

  const { data: serversData, isLoading } = useAwsMcpServers()
  const createServer = useCreateAwsMcpServer()
  const deleteServer = useDeleteAwsMcpServer()
  const testConnection = useTestAwsMcpServerConnection()
  const syncTools = useSyncAwsMcpServer()
  const { data: historyData } = useSyncHistory(historyServerId || undefined)

  const servers: AwsMcpServer[] = (serversData as any)?.content || serversData?.data || []

  const handleCreate = () => {
    createServer.mutate(form, {
      onSuccess: () => {
        setShowForm(false)
        setForm({
          name: '',
          endpointUrl: '',
          region: 'us-east-1',
          service: 'execute-api',
          authType: 'IAM_KEY' as AwsAuthType,
        })
      },
    })
  }

  const handleDelete = (id: string, name: string) => {
    if (window.confirm(`"${name}" 서버를 삭제하시겠습니까?`)) {
      deleteServer.mutate(id)
    }
  }

  return (
    <div className="p-6 space-y-6">
      <div className="section-header">
        <h2 className="text-lg font-bold text-text tracking-tight">AWS MCP 서버 관리</h2>
        <p className="text-xs text-text-tertiary mt-1">
          AWS 원격 MCP 서버 등록, 연결 테스트, 도구 동기화
        </p>
      </div>

      {/* 탭 */}
      <div className="flex gap-0 border-b border-b-border">
        {([
          { key: 'servers' as Tab, label: '서버' },
          { key: 'history' as Tab, label: '동기화 이력' },
        ]).map((t) => (
          <button
            key={t.key}
            onClick={() => setTab(t.key)}
            className={cn(
              'px-4 py-2 text-sm transition-colors border-b-2 -mb-px',
              tab === t.key
                ? 'border-b-text text-text font-medium'
                : 'border-b-transparent text-text-secondary hover:text-text'
            )}
          >
            {t.label}
          </button>
        ))}
      </div>

      {/* 서버 탭 */}
      {tab === 'servers' && (
        <div className="space-y-4">
          <div className="flex justify-end">
            <button
              onClick={() => setShowForm(!showForm)}
              className="btn-primary text-sm px-4 py-2"
            >
              {showForm ? '취소' : '서버 등록'}
            </button>
          </div>

          {showForm && (
            <div className="card p-4 space-y-3">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs text-text-secondary mb-1">이름</label>
                  <input
                    className="input-field w-full text-sm px-3 py-2"
                    value={form.name}
                    onChange={(e) => setForm({ ...form, name: e.target.value })}
                    placeholder="cloudwatch-mcp"
                  />
                </div>
                <div>
                  <label className="block text-xs text-text-secondary mb-1">엔드포인트 URL</label>
                  <input
                    className="input-field w-full text-sm px-3 py-2"
                    value={form.endpointUrl}
                    onChange={(e) => setForm({ ...form, endpointUrl: e.target.value })}
                    placeholder="https://mcp.us-east-1.amazonaws.com"
                  />
                </div>
                <div>
                  <label className="block text-xs text-text-secondary mb-1">리전</label>
                  <select
                    className="input-field w-full text-sm px-3 py-2"
                    value={form.region}
                    onChange={(e) => setForm({ ...form, region: e.target.value })}
                  >
                    {AWS_REGIONS.map((r) => (
                      <option key={r} value={r}>{r}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-xs text-text-secondary mb-1">서비스</label>
                  <input
                    className="input-field w-full text-sm px-3 py-2"
                    value={form.service || ''}
                    onChange={(e) => setForm({ ...form, service: e.target.value })}
                    placeholder="execute-api"
                  />
                </div>
                <div>
                  <label className="block text-xs text-text-secondary mb-1">인증 방식</label>
                  <select
                    className="input-field w-full text-sm px-3 py-2"
                    value={form.authType}
                    onChange={(e) => setForm({ ...form, authType: e.target.value as AwsAuthType })}
                  >
                    <option value="IAM_KEY">IAM Access Key</option>
                    <option value="IAM_ROLE">IAM Role ARN</option>
                  </select>
                </div>
                <div>
                  <label className="block text-xs text-text-secondary mb-1">설명</label>
                  <input
                    className="input-field w-full text-sm px-3 py-2"
                    value={form.description || ''}
                    onChange={(e) => setForm({ ...form, description: e.target.value })}
                    placeholder="CloudWatch 로그 분석 MCP"
                  />
                </div>
                {form.authType === 'IAM_KEY' && (
                  <>
                    <div>
                      <label className="block text-xs text-text-secondary mb-1">Access Key ID</label>
                      <input
                        className="input-field w-full text-sm px-3 py-2"
                        value={form.accessKeyId || ''}
                        onChange={(e) => setForm({ ...form, accessKeyId: e.target.value })}
                        placeholder="AKIA..."
                      />
                    </div>
                    <div>
                      <label className="block text-xs text-text-secondary mb-1">Secret Access Key</label>
                      <input
                        type="password"
                        className="input-field w-full text-sm px-3 py-2"
                        value={form.secretAccessKey || ''}
                        onChange={(e) => setForm({ ...form, secretAccessKey: e.target.value })}
                      />
                    </div>
                  </>
                )}
                {form.authType === 'IAM_ROLE' && (
                  <div className="md:col-span-2">
                    <label className="block text-xs text-text-secondary mb-1">Role ARN</label>
                    <input
                      className="input-field w-full text-sm px-3 py-2"
                      value={form.roleArn || ''}
                      onChange={(e) => setForm({ ...form, roleArn: e.target.value })}
                      placeholder="arn:aws:iam::123456789012:role/McpRole"
                    />
                  </div>
                )}
              </div>
              <div className="flex justify-end">
                <button
                  onClick={handleCreate}
                  disabled={createServer.isPending || !form.name || !form.endpointUrl}
                  className="btn-primary text-sm px-4 py-2 disabled:opacity-50"
                >
                  {createServer.isPending ? '등록 중...' : '등록'}
                </button>
              </div>
            </div>
          )}

          {isLoading ? (
            <div className="text-sm text-text-secondary py-8 text-center">로딩 중...</div>
          ) : servers.length === 0 ? (
            <div className="text-sm text-text-secondary py-8 text-center">
              등록된 AWS MCP 서버가 없습니다
            </div>
          ) : (
            <div className="space-y-0">
              {/* 테이블 헤더 */}
              <div className="grid grid-cols-12 gap-2 px-4 py-2 text-2xs uppercase tracking-wider text-text-tertiary border-b border-b-border">
                <div className="col-span-2">이름</div>
                <div className="col-span-3">엔드포인트</div>
                <div className="col-span-1">리전</div>
                <div className="col-span-1">상태</div>
                <div className="col-span-1">도구 수</div>
                <div className="col-span-2">마지막 동기화</div>
                <div className="col-span-2">작업</div>
              </div>

              {servers.map((server) => (
                <div key={server.id} className="table-row grid grid-cols-12 gap-2 px-4 py-3 items-center">
                  <div className="col-span-2">
                    <p className="text-sm font-medium text-text">{server.name}</p>
                    {server.description && (
                      <p className="text-2xs text-text-tertiary truncate">{server.description}</p>
                    )}
                  </div>
                  <div className="col-span-3 text-xs text-text-secondary truncate">
                    {server.endpointUrl}
                  </div>
                  <div className="col-span-1 text-xs text-text-secondary">{server.region}</div>
                  <div className="col-span-1">
                    <span className={statusBadge(server.status as DataSourceStatus)}>
                      {server.status}
                    </span>
                  </div>
                  <div className="col-span-1 text-xs text-text-secondary tabular-nums">
                    {server.syncedToolCount}
                  </div>
                  <div className="col-span-2 text-xs text-text-tertiary">
                    {server.lastSyncedAt ? formatDateTime(server.lastSyncedAt) : '-'}
                  </div>
                  <div className="col-span-2 flex gap-2 flex-wrap">
                    <button
                      onClick={() => testConnection.mutate(server.id)}
                      disabled={testConnection.isPending}
                      className="btn-secondary text-2xs px-2 py-1"
                    >
                      연결 테스트
                    </button>
                    <button
                      onClick={() => syncTools.mutate(server.id)}
                      disabled={syncTools.isPending}
                      className="btn-primary text-2xs px-2 py-1"
                    >
                      동기화
                    </button>
                    <button
                      onClick={() => handleDelete(server.id, server.name)}
                      className="text-2xs px-2 py-1 text-red-600 hover:text-red-800"
                    >
                      삭제
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* 동기화 이력 탭 */}
      {tab === 'history' && (
        <div className="space-y-4">
          <div>
            <label className="block text-xs text-text-secondary mb-1">서버 선택</label>
            <select
              className="input-field text-sm px-3 py-2 w-64"
              value={historyServerId}
              onChange={(e) => setHistoryServerId(e.target.value)}
            >
              <option value="">서버를 선택하세요</option>
              {servers.map((s) => (
                <option key={s.id} value={s.id}>{s.name}</option>
              ))}
            </select>
          </div>

          {historyServerId && historyData && (
            <div className="space-y-0">
              <div className="grid grid-cols-12 gap-2 px-4 py-2 text-2xs uppercase tracking-wider text-text-tertiary border-b border-b-border">
                <div className="col-span-1">상태</div>
                <div className="col-span-2">발견</div>
                <div className="col-span-2">생성</div>
                <div className="col-span-2">업데이트</div>
                <div className="col-span-3">오류</div>
                <div className="col-span-2">시간</div>
              </div>

              {((historyData as any)?.content || historyData?.data || []).map((h: any) => (
                <div key={h.id} className="table-row grid grid-cols-12 gap-2 px-4 py-3 items-center">
                  <div className="col-span-1">
                    <span className={h.status === 'SUCCESS' ? 'badge-success' : 'badge-error'}>
                      {h.status}
                    </span>
                  </div>
                  <div className="col-span-2 text-xs tabular-nums">{h.toolsDiscovered}개</div>
                  <div className="col-span-2 text-xs tabular-nums">{h.toolsCreated}개</div>
                  <div className="col-span-2 text-xs tabular-nums">{h.toolsUpdated}개</div>
                  <div className="col-span-3 text-xs text-text-tertiary truncate">
                    {h.errorMessage || '-'}
                  </div>
                  <div className="col-span-2 text-xs text-text-tertiary">
                    {formatDateTime(h.createdAt)}
                  </div>
                </div>
              ))}

              {((historyData as any)?.content || historyData?.data || []).length === 0 && (
                <div className="text-sm text-text-secondary py-8 text-center">
                  동기화 이력이 없습니다
                </div>
              )}
            </div>
          )}

          {!historyServerId && (
            <div className="text-sm text-text-secondary py-8 text-center">
              서버를 선택하여 동기화 이력을 확인하세요
            </div>
          )}
        </div>
      )}
    </div>
  )
}
