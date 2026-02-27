import { useEffect, useState } from 'react'
import {
  useCubeDataSources,
  useCreateCubeDataSource,
  useDeleteCubeDataSource,
  useTestCubeDataSourceConnection,
} from '@/api/cubeDataSources'
import {
  useCubeSchemas,
  useCreateCubeSchema,
  useDeleteCubeSchema,
  useActivateCubeSchema,
  useArchiveCubeSchema,
  useValidateCubeSchema,
  useCubeMeta,
} from '@/api/cubeSchemas'
import { useUIStore } from '@/stores/uiStore'
import { cn, formatDateTime } from '@/lib/utils'
import {
  CubeDbType,
  DataSourceStatus,
  SchemaStatus,
} from '@/types'
import type {
  CubeDataSource,
  CubeSchema,
  CubeMeta,
  CreateCubeDataSourceRequest,
  CreateCubeSchemaRequest,
} from '@/types'

/* ===== 상수 ===== */

type TabKey = 'datasources' | 'schemas' | 'meta'

const tabs: { key: TabKey; label: string }[] = [
  { key: 'datasources', label: '데이터소스' },
  { key: 'schemas', label: '스키마' },
  { key: 'meta', label: '메타 탐색기' },
]

const dbTypes = Object.values(CubeDbType)

const dsStatusBadge: Record<DataSourceStatus, string> = {
  [DataSourceStatus.ACTIVE]: 'badge-success',
  [DataSourceStatus.INACTIVE]: 'text-2xs font-medium px-2 py-0.5 border-l border-l-current text-gray-500 bg-gray-50',
  [DataSourceStatus.ERROR]: 'badge-error',
}

const schemaStatusBadge: Record<SchemaStatus, string> = {
  [SchemaStatus.DRAFT]: 'badge-warning',
  [SchemaStatus.ACTIVE]: 'badge-success',
  [SchemaStatus.ARCHIVED]: 'text-2xs font-medium px-2 py-0.5 border-l border-l-current text-gray-500 bg-gray-50',
}

/* ===== 초기 폼 값 ===== */

const emptyDsForm: CreateCubeDataSourceRequest = {
  name: '',
  description: '',
  dbType: CubeDbType.POSTGRESQL,
  host: '',
  port: 5432,
  database: '',
  username: '',
  password: '',
}

const emptySchemaForm: CreateCubeSchemaRequest = {
  datasourceId: '',
  name: '',
  description: '',
  schemaDefinition: '',
}

/* ===== 컴포넌트 ===== */

export default function CubeManagementPage() {
  useEffect(() => {
    useUIStore.getState().setCurrentPage('Cube.js')
  }, [])

  /* 탭 상태 */
  const [activeTab, setActiveTab] = useState<TabKey>('datasources')

  /* ---------- 데이터소스 ---------- */
  const [showDsForm, setShowDsForm] = useState(false)
  const [dsForm, setDsForm] = useState<CreateCubeDataSourceRequest>({ ...emptyDsForm })

  const { data: dsData, isLoading: dsLoading, isError: dsError, refetch: dsRefetch } = useCubeDataSources()
  const createDs = useCreateCubeDataSource()
  const deleteDs = useDeleteCubeDataSource()
  const testDs = useTestCubeDataSourceConnection()

  const handleDsFormChange = (field: keyof CreateCubeDataSourceRequest, value: string | number) => {
    setDsForm((prev) => ({ ...prev, [field]: value }))
  }

  const handleCreateDs = () => {
    if (!dsForm.name || !dsForm.host || !dsForm.database || !dsForm.username) return
    createDs.mutate(
      { ...dsForm, port: Number(dsForm.port) },
      {
        onSuccess: () => {
          setShowDsForm(false)
          setDsForm({ ...emptyDsForm })
        },
      },
    )
  }

  /* ---------- 스키마 ---------- */
  const [showSchemaForm, setShowSchemaForm] = useState(false)
  const [schemaForm, setSchemaForm] = useState<CreateCubeSchemaRequest>({ ...emptySchemaForm })

  const { data: schemaData, isLoading: schemaLoading, isError: schemaError, refetch: schemaRefetch } = useCubeSchemas()
  const createSchema = useCreateCubeSchema()
  const deleteSchema = useDeleteCubeSchema()
  const activateSchema = useActivateCubeSchema()
  const archiveSchema = useArchiveCubeSchema()
  const validateSchema = useValidateCubeSchema()

  const handleSchemaFormChange = (field: keyof CreateCubeSchemaRequest, value: string) => {
    setSchemaForm((prev) => ({ ...prev, [field]: value }))
  }

  const handleCreateSchema = () => {
    if (!schemaForm.datasourceId || !schemaForm.name || !schemaForm.schemaDefinition) return
    createSchema.mutate(schemaForm, {
      onSuccess: () => {
        setShowSchemaForm(false)
        setSchemaForm({ ...emptySchemaForm })
      },
    })
  }

  /* 활성 데이터소스 목록 (스키마 폼 셀렉트용) */
  const activeDataSources = (dsData?.data ?? []).filter(
    (ds: CubeDataSource) => ds.status === DataSourceStatus.ACTIVE,
  )

  /* ---------- 메타 탐색기 ---------- */
  const { data: metaList, isLoading: metaLoading, isError: metaError, refetch: metaRefetch } = useCubeMeta()
  const [expandedCubes, setExpandedCubes] = useState<Set<string>>(new Set())

  const toggleCube = (cubeName: string) => {
    setExpandedCubes((prev) => {
      const next = new Set(prev)
      if (next.has(cubeName)) {
        next.delete(cubeName)
      } else {
        next.add(cubeName)
      }
      return next
    })
  }

  return (
    <div className="space-y-4">
      {/* 페이지 헤더 */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <div className="section-header">
          <h2 className="text-base font-semibold text-text">Cube.js 관리</h2>
          <p className="text-2xs text-text-tertiary">데이터소스 및 스키마 관리</p>
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

      {/* ==================== 데이터소스 탭 ==================== */}
      {activeTab === 'datasources' && (
        <div className="space-y-4">
          <div className="flex justify-end">
            <button
              onClick={() => setShowDsForm((prev) => !prev)}
              className="btn-primary text-xs"
            >
              {showDsForm ? '취소' : '데이터소스 추가'}
            </button>
          </div>

          {/* 인라인 생성 폼 */}
          {showDsForm && (
            <div className="card p-4 space-y-3">
              <h3 className="text-xs font-semibold text-text border-l-2 border-l-primary-900 pl-2">
                새 데이터소스
              </h3>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                <div>
                  <label className="text-2xs text-text-secondary block mb-1">이름 *</label>
                  <input
                    type="text"
                    value={dsForm.name}
                    onChange={(e) => handleDsFormChange('name', e.target.value)}
                    placeholder="데이터소스 이름"
                    className="input-field w-full text-xs"
                  />
                </div>
                <div>
                  <label className="text-2xs text-text-secondary block mb-1">설명</label>
                  <input
                    type="text"
                    value={dsForm.description}
                    onChange={(e) => handleDsFormChange('description', e.target.value)}
                    placeholder="설명"
                    className="input-field w-full text-xs"
                  />
                </div>
                <div>
                  <label className="text-2xs text-text-secondary block mb-1">DB 타입 *</label>
                  <select
                    value={dsForm.dbType}
                    onChange={(e) => handleDsFormChange('dbType', e.target.value)}
                    className="input-field w-full text-xs"
                  >
                    {dbTypes.map((t) => (
                      <option key={t} value={t}>{t}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="text-2xs text-text-secondary block mb-1">호스트 *</label>
                  <input
                    type="text"
                    value={dsForm.host}
                    onChange={(e) => handleDsFormChange('host', e.target.value)}
                    placeholder="localhost"
                    className="input-field w-full text-xs"
                  />
                </div>
                <div>
                  <label className="text-2xs text-text-secondary block mb-1">포트 *</label>
                  <input
                    type="number"
                    value={dsForm.port}
                    onChange={(e) => handleDsFormChange('port', Number(e.target.value))}
                    placeholder="5432"
                    className="input-field w-full text-xs"
                  />
                </div>
                <div>
                  <label className="text-2xs text-text-secondary block mb-1">데이터베이스 *</label>
                  <input
                    type="text"
                    value={dsForm.database}
                    onChange={(e) => handleDsFormChange('database', e.target.value)}
                    placeholder="mydb"
                    className="input-field w-full text-xs"
                  />
                </div>
                <div>
                  <label className="text-2xs text-text-secondary block mb-1">사용자명 *</label>
                  <input
                    type="text"
                    value={dsForm.username}
                    onChange={(e) => handleDsFormChange('username', e.target.value)}
                    placeholder="username"
                    className="input-field w-full text-xs"
                  />
                </div>
                <div>
                  <label className="text-2xs text-text-secondary block mb-1">비밀번호 *</label>
                  <input
                    type="password"
                    value={dsForm.password}
                    onChange={(e) => handleDsFormChange('password', e.target.value)}
                    placeholder="password"
                    className="input-field w-full text-xs"
                  />
                </div>
              </div>
              <div className="flex justify-end gap-2 pt-1">
                <button
                  onClick={() => {
                    setShowDsForm(false)
                    setDsForm({ ...emptyDsForm })
                  }}
                  className="btn-secondary text-xs"
                >
                  취소
                </button>
                <button
                  onClick={handleCreateDs}
                  disabled={createDs.isPending}
                  className="btn-primary text-xs disabled:opacity-50"
                >
                  {createDs.isPending ? '생성 중...' : '생성'}
                </button>
              </div>
            </div>
          )}

          {/* 로딩 / 에러 */}
          {dsLoading && (
            <div className="card p-6 text-center text-xs text-text-tertiary">불러오는 중...</div>
          )}
          {dsError && (
            <div className="card p-6 text-center">
              <p className="text-xs text-red-600 mb-2">데이터소스를 불러오지 못했습니다</p>
              <button onClick={() => dsRefetch()} className="btn-secondary text-xs">재시도</button>
            </div>
          )}

          {/* 데이터소스 테이블 */}
          {dsData && dsData.data.length === 0 && !showDsForm && (
            <div className="card p-6 text-center">
              <p className="text-sm text-text mb-1">등록된 데이터소스가 없습니다</p>
              <p className="text-2xs text-text-tertiary">위의 "데이터소스 추가" 버튼으로 새 데이터소스를 등록하세요.</p>
            </div>
          )}

          {dsData && dsData.data.length > 0 && (
            <div className="card p-0">
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="border-b-2 border-b-primary-900">
                      <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                        이름
                      </th>
                      <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                        DB 타입
                      </th>
                      <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                        상태
                      </th>
                      <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                        마지막 테스트
                      </th>
                      <th className="text-right text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                        작업
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {dsData.data.map((ds: CubeDataSource) => (
                      <tr key={ds.id} className="table-row">
                        <td className="px-4 py-3">
                          <p className="text-sm font-medium text-text">{ds.name}</p>
                          {ds.description && (
                            <p className="text-2xs text-text-tertiary">{ds.description}</p>
                          )}
                        </td>
                        <td className="px-4 py-3 text-xs text-text-secondary">{ds.dbType}</td>
                        <td className="px-4 py-3">
                          <span className={dsStatusBadge[ds.status]}>{ds.status}</span>
                        </td>
                        <td className="px-4 py-3 text-xs text-text-tertiary">
                          {formatDateTime(ds.lastTestedAt)}
                        </td>
                        <td className="px-4 py-3 text-right">
                          <div className="flex items-center justify-end gap-2">
                            <button
                              onClick={() => testDs.mutate(ds.id)}
                              disabled={testDs.isPending}
                              className="text-2xs text-text-secondary hover:text-text transition-colors"
                            >
                              {testDs.isPending ? '테스트 중...' : '연결 테스트'}
                            </button>
                            <button
                              onClick={() => {
                                if (window.confirm(`"${ds.name}" 데이터소스를 삭제하시겠습니까?`)) {
                                  deleteDs.mutate(ds.id)
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

      {/* ==================== 스키마 탭 ==================== */}
      {activeTab === 'schemas' && (
        <div className="space-y-4">
          <div className="flex justify-end">
            <button
              onClick={() => setShowSchemaForm((prev) => !prev)}
              className="btn-primary text-xs"
            >
              {showSchemaForm ? '취소' : '스키마 추가'}
            </button>
          </div>

          {/* 인라인 생성 폼 */}
          {showSchemaForm && (
            <div className="card p-4 space-y-3">
              <h3 className="text-xs font-semibold text-text border-l-2 border-l-primary-900 pl-2">
                새 스키마
              </h3>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <div>
                  <label className="text-2xs text-text-secondary block mb-1">데이터소스 *</label>
                  <select
                    value={schemaForm.datasourceId}
                    onChange={(e) => handleSchemaFormChange('datasourceId', e.target.value)}
                    className="input-field w-full text-xs"
                  >
                    <option value="">데이터소스 선택</option>
                    {activeDataSources.map((ds: CubeDataSource) => (
                      <option key={ds.id} value={ds.id}>{ds.name}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="text-2xs text-text-secondary block mb-1">이름 *</label>
                  <input
                    type="text"
                    value={schemaForm.name}
                    onChange={(e) => handleSchemaFormChange('name', e.target.value)}
                    placeholder="스키마 이름"
                    className="input-field w-full text-xs"
                  />
                </div>
                <div className="sm:col-span-2">
                  <label className="text-2xs text-text-secondary block mb-1">설명</label>
                  <input
                    type="text"
                    value={schemaForm.description}
                    onChange={(e) => handleSchemaFormChange('description', e.target.value)}
                    placeholder="스키마 설명"
                    className="input-field w-full text-xs"
                  />
                </div>
                <div className="sm:col-span-2">
                  <label className="text-2xs text-text-secondary block mb-1">스키마 정의 *</label>
                  <textarea
                    value={schemaForm.schemaDefinition}
                    onChange={(e) => handleSchemaFormChange('schemaDefinition', e.target.value)}
                    placeholder="cube(`Orders`, { ... })"
                    rows={8}
                    className="input-field w-full text-xs font-mono resize-y"
                  />
                </div>
              </div>
              <div className="flex justify-end gap-2 pt-1">
                <button
                  onClick={() => {
                    setShowSchemaForm(false)
                    setSchemaForm({ ...emptySchemaForm })
                  }}
                  className="btn-secondary text-xs"
                >
                  취소
                </button>
                <button
                  onClick={handleCreateSchema}
                  disabled={createSchema.isPending}
                  className="btn-primary text-xs disabled:opacity-50"
                >
                  {createSchema.isPending ? '생성 중...' : '생성'}
                </button>
              </div>
            </div>
          )}

          {/* 로딩 / 에러 */}
          {schemaLoading && (
            <div className="card p-6 text-center text-xs text-text-tertiary">불러오는 중...</div>
          )}
          {schemaError && (
            <div className="card p-6 text-center">
              <p className="text-xs text-red-600 mb-2">스키마를 불러오지 못했습니다</p>
              <button onClick={() => schemaRefetch()} className="btn-secondary text-xs">재시도</button>
            </div>
          )}

          {/* 스키마 테이블 */}
          {schemaData && schemaData.data.length === 0 && !showSchemaForm && (
            <div className="card p-6 text-center">
              <p className="text-sm text-text mb-1">등록된 스키마가 없습니다</p>
              <p className="text-2xs text-text-tertiary">위의 "스키마 추가" 버튼으로 새 스키마를 등록하세요.</p>
            </div>
          )}

          {schemaData && schemaData.data.length > 0 && (
            <div className="card p-0">
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead>
                    <tr className="border-b-2 border-b-primary-900">
                      <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                        이름
                      </th>
                      <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                        데이터소스
                      </th>
                      <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                        상태
                      </th>
                      <th className="text-left text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                        버전
                      </th>
                      <th className="text-right text-2xs font-semibold text-text-secondary uppercase tracking-wider px-4 py-3">
                        작업
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {schemaData.data.map((schema: CubeSchema) => (
                      <tr key={schema.id} className="table-row">
                        <td className="px-4 py-3">
                          <p className="text-sm font-medium text-text">{schema.name}</p>
                          {schema.description && (
                            <p className="text-2xs text-text-tertiary">{schema.description}</p>
                          )}
                        </td>
                        <td className="px-4 py-3 text-xs text-text-secondary">
                          {schema.datasourceName}
                        </td>
                        <td className="px-4 py-3">
                          <span className={schemaStatusBadge[schema.status]}>{schema.status}</span>
                        </td>
                        <td className="px-4 py-3 text-xs text-text-tertiary">v{schema.version}</td>
                        <td className="px-4 py-3 text-right">
                          <div className="flex items-center justify-end gap-2">
                            <button
                              onClick={() => {
                                validateSchema.mutate(schema.id, {
                                  onSuccess: (result) => {
                                    useUIStore.getState().addNotification({
                                      type: result.valid ? 'success' : 'error',
                                      message: result.valid
                                        ? '스키마 검증 성공'
                                        : `검증 실패: ${result.message}`,
                                    })
                                  },
                                })
                              }}
                              disabled={validateSchema.isPending}
                              className="text-2xs text-text-secondary hover:text-text transition-colors"
                            >
                              검증
                            </button>
                            {schema.status !== SchemaStatus.ACTIVE && (
                              <button
                                onClick={() => activateSchema.mutate(schema.id)}
                                disabled={activateSchema.isPending}
                                className="text-2xs text-emerald-600 hover:text-emerald-700 transition-colors"
                              >
                                활성화
                              </button>
                            )}
                            {schema.status === SchemaStatus.ACTIVE && (
                              <button
                                onClick={() => archiveSchema.mutate(schema.id)}
                                disabled={archiveSchema.isPending}
                                className="text-2xs text-amber-600 hover:text-amber-700 transition-colors"
                              >
                                아카이브
                              </button>
                            )}
                            <button
                              onClick={() => {
                                if (window.confirm(`"${schema.name}" 스키마를 삭제하시겠습니까?`)) {
                                  deleteSchema.mutate(schema.id)
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

      {/* ==================== 메타 탐색기 탭 ==================== */}
      {activeTab === 'meta' && (
        <div className="space-y-4">
          {metaLoading && (
            <div className="card p-6 text-center text-xs text-text-tertiary">불러오는 중...</div>
          )}
          {metaError && (
            <div className="card p-6 text-center">
              <p className="text-xs text-red-600 mb-2">메타 정보를 불러오지 못했습니다</p>
              <button onClick={() => metaRefetch()} className="btn-secondary text-xs">재시도</button>
            </div>
          )}

          {metaList && metaList.length === 0 && (
            <div className="card p-6 text-center">
              <p className="text-sm text-text mb-1">활성 큐브가 없습니다</p>
              <p className="text-2xs text-text-tertiary">스키마를 활성화하면 이곳에 큐브 메타 정보가 표시됩니다.</p>
            </div>
          )}

          {metaList && metaList.length > 0 && (
            <div className="space-y-2">
              {metaList.map((cube: CubeMeta) => {
                const isExpanded = expandedCubes.has(cube.cubeName)
                return (
                  <div key={cube.cubeName} className="card p-0">
                    <button
                      onClick={() => toggleCube(cube.cubeName)}
                      className="w-full text-left px-4 py-3 flex items-center justify-between hover:bg-surface-tertiary transition-colors"
                    >
                      <div>
                        <p className="text-sm font-medium text-text">{cube.cubeName}</p>
                        <p className="text-2xs text-text-tertiary">
                          {cube.schemaName} -- measures: {cube.measures.length}, dimensions: {cube.dimensions.length}
                        </p>
                      </div>
                      <span className="text-xs text-text-tertiary">
                        {isExpanded ? '[-]' : '[+]'}
                      </span>
                    </button>

                    {isExpanded && (
                      <div className="border-t border-t-border px-4 py-3 space-y-3">
                        {/* Measures */}
                        <div>
                          <p className="text-2xs font-semibold text-text-secondary uppercase tracking-wider mb-1">
                            Measures
                          </p>
                          {cube.measures.length === 0 ? (
                            <p className="text-2xs text-text-tertiary">없음</p>
                          ) : (
                            <div className="flex flex-wrap gap-1">
                              {cube.measures.map((m) => (
                                <span key={m} className="badge-info">{m}</span>
                              ))}
                            </div>
                          )}
                        </div>

                        {/* Dimensions */}
                        <div>
                          <p className="text-2xs font-semibold text-text-secondary uppercase tracking-wider mb-1">
                            Dimensions
                          </p>
                          {cube.dimensions.length === 0 ? (
                            <p className="text-2xs text-text-tertiary">없음</p>
                          ) : (
                            <div className="flex flex-wrap gap-1">
                              {cube.dimensions.map((d) => (
                                <span key={d} className="badge-warning">{d}</span>
                              ))}
                            </div>
                          )}
                        </div>
                      </div>
                    )}
                  </div>
                )
              })}
            </div>
          )}
        </div>
      )}
    </div>
  )
}
