import { useEffect, useState } from 'react'
import {
  useAwsMcpServers,
  useCreateAwsMcpServer,
  useDeleteAwsMcpServer,
  useTestAwsMcpServerConnection,
  useSyncAwsMcpServer,
  useSyncHistory,
} from '@/api/awsMcpServers'
import { useUIStore } from '@/stores/uiStore'
import { cn, formatDateTime } from '@/lib/utils'
import { AwsAuthType, DataSourceStatus } from '@/types'
import type {
  AwsMcpServer,
  CreateAwsMcpServerRequest,
  SyncHistory,
} from '@/types'

/* ===== 상수 ===== */

type TabKey = 'servers' | 'history'

const tabs: { key: TabKey; label: string }[] = [
  { key: 'servers', label: '서버' },
  { key: 'history', label: '동기화 이력' },
]

const awsRegions = [
  'us-east-1',
  'us-east-2',
  'us-west-1',
  'us-west-2',
  'ap-northeast-1',
  'ap-northeast-2',
  'ap-northeast-3',
  'ap-southeast-1',
  'ap-southeast-2',
  'ap-south-1',
  'eu-central-1',
  'eu-west-1',
  'eu-west-2',
  'eu-west-3',
  'eu-north-1',
  'sa-east-1',
  'ca-central-1',
]

const authTypes = Object.values(AwsAuthType)

const statusBadge: Record<DataSourceStatus, string> = {
  [DataSourceStatus.ACTIVE]: 'badge-success',
  [DataSourceStatus.INACTIVE]:
    'text-2xs font-medium px-2 py-0.5 border-l border-l-current text-gray-500 bg-gray-50',
  [DataSourceStatus.ERROR]: 'badge-error',
}

/* ===== 초기 폼 값 ===== */

const emptyForm: CreateAwsMcpServerRequest = {
  name: '',
  description: '',
  endpointUrl: '',
  region: 'ap-northeast-2',
  service: 'execute-api',
  authType: AwsAuthType.IAM_KEY,
  accessKeyId: '',
  secretAccessKey: '',
  roleArn: '',
}

/* ===== 컴포넌트 ===== */

export default function AwsMcpManagementPage() {
  useEffect(() => {
    useUIStore.getState().setCurrentPage('AWS MCP')
  }, [])

  /* 탭 상태 */
  const [activeTab, setActiveTab] = useState<TabKey>('servers')

  /* ---------- 서버 ---------- */
  const [showForm, setShowForm] = useState(false)
  const [form, setForm] = useState<CreateAwsMcpServerRequest>({ ...emptyForm })

  const {
    data: serverData,
    isLoading: serverLoading,
    isError: serverError,
    refetch: serverRefetch,
  } = useAwsMcpServers()
  const createServer = useCreateAwsMcpServer()
  const deleteServer = useDeleteAwsMcpServer()
  const testConnection = useTestAwsMcpServerConnection()
  const syncServer = useSyncAwsMcpServer()

  const handleFormChange = (
    field: keyof CreateAwsMcpServerRequest,
    value: string,
  ) => {
    setForm((prev) => ({ ...prev, [field]: value }))
  }

  const handleCreate = () => {
    if (!form.name || !form.endpointUrl || !form.region) return
    if (form.authType === AwsAuthType.IAM_KEY && (!form.accessKeyId || !form.secretAccessKey)) return
    if (form.authType === AwsAuthType.IAM_ROLE && !form.roleArn) return

    const payload: CreateAwsMcpServerRequest = {
      name: form.name,
      description: form.description || undefined,
      endpointUrl: form.endpointUrl,
      region: form.region,
      service: form.service || 'execute-api',
      authType: form.authType,
    }

    if (form.authType === AwsAuthType.IAM_KEY) {
      payload.accessKeyId = form.accessKeyId
      payload.secretAccessKey = form.secretAccessKey
    } else {
      payload.roleArn = form.roleArn
    }

    createServer.mutate(payload, {
      onSuccess: () => {
        setShowForm(false)
        setForm({ ...emptyForm })
      },
    })
  }

  /* ---------- 동기화 이력 ---------- */
  const [selectedServerId, setSelectedServerId] = useState<string>('')
  const [historyPage, setHistoryPage] = useState(0)

  const {
    data: historyData,
    isLoading: historyLoading,
    isError: historyError,
    refetch: historyRefetch,
  } = useSyncHistory(selectedServerId || undefined, historyPage)

  const serverList: AwsMcpServer[] = serverData?.data ?? []

  return (
    <div className="space-y-4">
      {/* 페이지 헤더 */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <div className="section-header">
          <h2 className="text-base font-semibold text-text">AWS MCP 서버 관리</h2>
          <p className="text-2xs text-text-tertiary">
            AWS MCP 서버 등록 및 도구 동기화
          </p>
        </div>
      </div>

      {/* 탭 */}
      <div className="flex gap-0 border-b-2 border-b-primary-900">
        {tabs.map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            className={cn(
              'px-4 py-2 text-xs font-medium transition-colors',
              activeTab === tab.key
                ? 'bg-primary-900 text-white'
                : 'bg-white text-text-secondary hover:bg-surface-tertiary',
            )}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* ==================== 서버 탭 ==================== */}
      {activeTab === 'servers' && (
        <div className="space-y-4">
          <div className="flex justify-end">
            <button
              onClick={() => setShowForm((prev) => !prev)}
              className="btn-primary text-xs"
            >
              {showForm ? '취소' : '서버 등록'}
            </button>
          </div>

          {/* 인라인 생성 폼 */}
          {showForm && (
            <div className="card p-4 space-y-3">
              <h3 className="text-xs font-semibold text-text border-l-2 border-l-primary-900 pl-2">
                새 AWS MCP 서버
              </h3>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                <div>
                  <label className="text-2xs text-text-secondary block mb-1">
                    이름 *
                  </label>
                  <input
                    type="text"
                    value={form.name}
                    onChange={(e) => handleFormChange('name', e.target.value)}
                    placeholder="서버 이름"
                    className="input-field w-full text-xs"
                  />
                </div>
                <div>
                  <label className="text-2xs text-text-secondary block mb-1">
                    설명
                  </label>
                  <input
                    type="text"
                    value={form.description}
                    onChange={(e) =>
                      handleFormChange('description', e.target.value)
                    }
                    placeholder="서버 설명"
                    className="input-field w-full text-xs"
                  />
                </div>
                <div>
                  <label className="text-2xs text-text-secondary block mb-1">
                    Endpoint URL *
                  </label>
                  <input
                    type="text"
                    value={form.endpointUrl}
                    onChange={(e) =>
                      handleFormChange('endpointUrl', e.target.value)
                    }
                    placeholder="https://xxx.execute-api.ap-northeast-2.amazonaws.com/mcp"
                    className="input-field w-full text-xs"
                  />
                </div>
                <div>
                  <label className="text-2xs text-text-secondary block mb-1">
                    리전 *
                  </label>
                  <select
                    value={form.region}
                    onChange={(e) => handleFormChange('region', e.target.value)}
                    className="input-field w-full text-xs"
                  >
                    {awsRegions.map((r) => (
                      <option key={r} value={r}>
                        {r}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="text-2xs text-text-secondary block mb-1">
                    서비스
                  </label>
                  <input
                    type="text"
                    value={form.service}
                    onChange={(e) =>
                      handleFormChange('service', e.target.value)
                    }
                    placeholder="execute-api"
                    className="input-field w-full text-xs"
                  />
                </div>
                <div>
                  <label className="text-2xs text-text-secondary block mb-1">
                    인증 방식 *
                  </label>
                  <select
                    value={form.authType}
                    onChange={(e) =>
                      handleFormChange('authType', e.target.value)
                    }
                    className="input-field w-full text-xs"
                  >
                    {authTypes.map((t) => (
                      <option key={t} value={t}>
                        {t}
                      </option>
                    ))}
                  </select>
                </div>

                {/* IAM_KEY 필드 */}
                {form.authType === AwsAuthType.IAM_KEY && (
                  <>
                    <div>
                      <label className="text-2xs text-text-secondary block mb-1">
                        Access Key ID *
                      </label>
                      <input
                        type="text"
                        value={form.accessKeyId}
                        onChange={(e) =>
                          handleFormChange('accessKeyId', e.target.value)
                        }
                        placeholder="AKIAIOSFODNN7EXAMPLE"
                        className="input-field w-full text-xs"
                      />
                    </div>
                    <div>
                      <label className="text-2xs text-text-secondary block mb-1">
                        Secret Access Key *
                      </label>
                      <input
                        type="password"
                        value={form.secretAccessKey}
                        onChange={(e) =>
                          handleFormChange('secretAccessKey', e.target.value)
                        }
                        placeholder="wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"
                        className="input-field w-full text-xs"
                      />
                    </div>
                  </>
                )}

                {/* IAM_ROLE 필드 */}
                {form.authType === AwsAuthType.IAM_ROLE && (
                  <div className="sm:col-span-2">
                    <label className="text-2xs text-text-secondary block mb-1">
                      Role ARN *
                    </label>
                    <input
                      type="text"
                      value={form.roleArn}
                      onChange={(e) =>
                        handleFormChange('roleArn', e.target.value)
                      }
                      placeholder="arn:aws:iam::123456789012:role/MyRole"
                      className="input-field w-full text-xs"
                    />
                  </div>
                )}
              </div>
              <div className="flex justify-end gap-2 pt-1">
                <button
                  onClick={() => {
                    setShowForm(false)
                    setForm({ ...emptyForm })
                  }}
                  className="btn-secondary text-xs"
                >
                  취소
                </button>
                <button
                  onClick={handleCreate}
                  disabled={createServer.isPending}
                  className="btn-primary text-xs disabled:opacity-50"
                >
                  {createServer.isPending ? '등록 중...' : '등록'}
                </button>
              </div>
            </div>
          )}

          {/* 로딩 / 에러 */}
          {serverLoading && (
            <div className="card p-6 text-center text-xs text-text-tertiary">
              불러오는 중...
            </div>
          )}
          {serverError && (
            <div className="card p-6 text-center">
              <p className="text-xs text-red-600 mb-2">
                서버 목록을 불러오지 못했습니다
              </p>
              <button
                onClick={() => serverRefetch()}
                className="btn-secondary text-xs"
              >
                재시도
              </button>
            </div>
          )}

          {/* 서버 목록 비어있을 때 */}
          {serverData && serverData.data.length === 0 && !showForm && (
            <div className="card p-6 text-center">
              <p className="text-sm text-text mb-1">
                등록된 서버가 없습니다
              </p>
              <p className="text-2xs text-text-tertiary">
                위의 "서버 등록" 버튼으로 새 AWS MCP 서버를 등록하세요.
              </p>
            </div>
          )}

          {/* 서버 테이블 */}
          {serverData && serverData.data.length > 0 && (
            <div className="card p-0">
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="border-b-2 border-b-primary-900">
                      <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                        이름
                      </th>
                      <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                        Endpoint
                      </th>
                      <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                        리전
                      </th>
                      <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                        상태
                      </th>
                      <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                        동기화 도구
                      </th>
                      <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                        마지막 동기화
                      </th>
                      <th className="text-right text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                        작업
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {serverData.data.map((server: AwsMcpServer) => (
                      <tr key={server.id} className="table-row">
                        <td className="px-4 py-3">
                          <p className="text-sm font-medium text-text">
                            {server.name}
                          </p>
                          {server.description && (
                            <p className="text-2xs text-text-tertiary">
                              {server.description}
                            </p>
                          )}
                        </td>
                        <td className="px-4 py-3 text-xs text-text-secondary max-w-[200px] truncate">
                          {server.endpointUrl}
                        </td>
                        <td className="px-4 py-3 text-xs text-text-secondary">
                          {server.region}
                        </td>
                        <td className="px-4 py-3">
                          <span className={statusBadge[server.status]}>
                            {server.status}
                          </span>
                        </td>
                        <td className="px-4 py-3 text-xs text-text-secondary">
                          {server.syncedToolCount}개
                        </td>
                        <td className="px-4 py-3 text-xs text-text-tertiary">
                          {formatDateTime(server.lastSyncedAt)}
                        </td>
                        <td className="px-4 py-3 text-right">
                          <div className="flex items-center justify-end gap-2">
                            <button
                              onClick={() => testConnection.mutate(server.id)}
                              disabled={testConnection.isPending}
                              className="text-2xs text-text-secondary hover:text-text transition-colors"
                            >
                              {testConnection.isPending
                                ? '테스트 중...'
                                : '연결 테스트'}
                            </button>
                            <button
                              onClick={() => syncServer.mutate(server.id)}
                              disabled={syncServer.isPending}
                              className="text-2xs text-emerald-600 hover:text-emerald-700 transition-colors"
                            >
                              {syncServer.isPending
                                ? '동기화 중...'
                                : '도구 동기화'}
                            </button>
                            <button
                              onClick={() => {
                                if (
                                  window.confirm(
                                    `"${server.name}" 서버를 삭제하시겠습니까?`,
                                  )
                                ) {
                                  deleteServer.mutate(server.id)
                                }
                              }}
                              className="text-2xs text-red-600 hover:text-red-700 transition-colors"
                            >
                              삭제
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
        </div>
      )}

      {/* ==================== 동기화 이력 탭 ==================== */}
      {activeTab === 'history' && (
        <div className="space-y-4">
          {/* 서버 선택 */}
          <div className="card p-3">
            <div className="flex flex-col sm:flex-row sm:items-center gap-3">
              <label className="text-xs text-text-secondary whitespace-nowrap">
                서버 선택
              </label>
              <select
                value={selectedServerId}
                onChange={(e) => {
                  setSelectedServerId(e.target.value)
                  setHistoryPage(0)
                }}
                className="input-field text-xs flex-1"
              >
                <option value="">-- 서버를 선택하세요 --</option>
                {serverList.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name} ({s.region})
                  </option>
                ))}
              </select>
            </div>
          </div>

          {/* 서버 미선택 */}
          {!selectedServerId && (
            <div className="card p-6 text-center">
              <p className="text-sm text-text mb-1">
                서버를 선택하세요
              </p>
              <p className="text-2xs text-text-tertiary">
                동기화 이력을 조회할 서버를 위 드롭다운에서 선택하세요.
              </p>
            </div>
          )}

          {/* 로딩 / 에러 */}
          {selectedServerId && historyLoading && (
            <div className="card p-6 text-center text-xs text-text-tertiary">
              불러오는 중...
            </div>
          )}
          {selectedServerId && historyError && (
            <div className="card p-6 text-center">
              <p className="text-xs text-red-600 mb-2">
                동기화 이력을 불러오지 못했습니다
              </p>
              <button
                onClick={() => historyRefetch()}
                className="btn-secondary text-xs"
              >
                재시도
              </button>
            </div>
          )}

          {/* 이력 비어있을 때 */}
          {selectedServerId &&
            historyData &&
            historyData.data.length === 0 && (
              <div className="card p-6 text-center">
                <p className="text-sm text-text mb-1">
                  동기화 이력이 없습니다
                </p>
                <p className="text-2xs text-text-tertiary">
                  서버 탭에서 "도구 동기화"를 실행하면 이력이 기록됩니다.
                </p>
              </div>
            )}

          {/* 이력 테이블 */}
          {selectedServerId &&
            historyData &&
            historyData.data.length > 0 && (
              <>
                <div className="card p-0">
                  <div className="overflow-x-auto">
                    <table className="w-full">
                      <thead>
                        <tr className="border-b-2 border-b-primary-900">
                          <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                            상태
                          </th>
                          <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                            발견
                          </th>
                          <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                            생성
                          </th>
                          <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                            업데이트
                          </th>
                          <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                            오류 메시지
                          </th>
                          <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                            일시
                          </th>
                        </tr>
                      </thead>
                      <tbody>
                        {historyData.data.map((row: SyncHistory) => (
                          <tr key={row.id} className="table-row">
                            <td className="px-4 py-3">
                              <span
                                className={cn(
                                  row.status === 'SUCCESS'
                                    ? 'badge-success'
                                    : row.status === 'PENDING'
                                      ? 'badge-warning'
                                      : 'badge-error',
                                )}
                              >
                                {row.status}
                              </span>
                            </td>
                            <td className="px-4 py-3 text-xs text-text-secondary">
                              {row.toolsDiscovered}개
                            </td>
                            <td className="px-4 py-3 text-xs text-text-secondary">
                              {row.toolsCreated}개
                            </td>
                            <td className="px-4 py-3 text-xs text-text-secondary">
                              {row.toolsUpdated}개
                            </td>
                            <td className="px-4 py-3 text-xs text-text-tertiary max-w-[250px] truncate">
                              {row.errorMessage || '--'}
                            </td>
                            <td className="px-4 py-3 text-xs text-text-tertiary">
                              {formatDateTime(row.createdAt)}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>

                {/* 페이지네이션 */}
                {historyData.totalPages > 1 && (
                  <div className="flex items-center justify-center gap-2 pt-2">
                    <button
                      onClick={() => setHistoryPage((p) => Math.max(0, p - 1))}
                      disabled={historyPage === 0}
                      className={cn(
                        'px-3 py-1.5 text-xs font-medium transition-colors',
                        historyPage === 0
                          ? 'text-text-tertiary cursor-not-allowed'
                          : 'text-text hover:bg-surface-tertiary',
                      )}
                    >
                      이전
                    </button>
                    <span className="text-xs text-text-secondary">
                      {historyPage + 1} / {historyData.totalPages}
                    </span>
                    <button
                      onClick={() =>
                        setHistoryPage((p) =>
                          Math.min(historyData.totalPages - 1, p + 1),
                        )
                      }
                      disabled={historyPage >= historyData.totalPages - 1}
                      className={cn(
                        'px-3 py-1.5 text-xs font-medium transition-colors',
                        historyPage >= historyData.totalPages - 1
                          ? 'text-text-tertiary cursor-not-allowed'
                          : 'text-text hover:bg-surface-tertiary',
                      )}
                    >
                      다음
                    </button>
                  </div>
                )}
              </>
            )}
        </div>
      )}
    </div>
  )
}
