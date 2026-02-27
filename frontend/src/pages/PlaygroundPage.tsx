import { useEffect, useState, useMemo } from 'react'
import { useTools, useExecuteTool } from '@/api/tools'
import { useUIStore } from '@/stores/uiStore'
import { LoadingSkeleton } from '@/components/common/LoadingSkeleton'
import { ErrorState } from '@/components/common/ErrorState'
import { EmptyState } from '@/components/common/EmptyState'
import { FormField } from '@/components/common/FormField'
import { ToolStatus } from '@/types'
import type { Tool } from '@/types'
import { formatExecutionTime } from '@/lib/utils'

export default function PlaygroundPage() {
  const setCurrentPage = useUIStore((s) => s.setCurrentPage)
  const addNotification = useUIStore((s) => s.addNotification)

  useEffect(() => {
    setCurrentPage('Playground')
  }, [setCurrentPage])

  /* 승인된 도구 목록 조회 */
  const {
    data: toolsData,
    isLoading: toolsLoading,
    isError: toolsError,
    refetch,
  } = useTools({
    status: ToolStatus.ACTIVE,
    pageSize: 100,
  })

  /* 선택된 도구 및 실행 관련 상태 */
  const [selectedToolId, setSelectedToolId] = useState('')
  const [params, setParams] = useState<Record<string, string>>({})
  const [result, setResult] = useState<unknown>(null)
  const [executionTime, setExecutionTime] = useState<number | null>(null)
  const [traceId, setTraceId] = useState<string | null>(null)

  const executeMutation = useExecuteTool()

  /* 선택된 도구 객체 */
  const selectedTool = useMemo<Tool | undefined>(() => {
    return toolsData?.data?.find((t) => t.id === selectedToolId)
  }, [toolsData, selectedToolId])

  /* 선택된 도구의 입력 스키마 속성 */
  const schemaProperties = useMemo(() => {
    if (!selectedTool?.inputSchema?.properties) return {}
    return selectedTool.inputSchema.properties
  }, [selectedTool])

  /* 필수 입력 필드 목록 */
  const requiredFields = useMemo(() => {
    return selectedTool?.inputSchema?.required || []
  }, [selectedTool])

  /* 도구 변경 시 파라미터 초기화 */
  useEffect(() => {
    if (selectedTool) {
      const initialParams: Record<string, string> = {}
      for (const [key, schema] of Object.entries(schemaProperties)) {
        initialParams[key] = schema.default !== undefined ? String(schema.default) : ''
      }
      setParams(initialParams)
      setResult(null)
      setExecutionTime(null)
      setTraceId(null)
    }
  }, [selectedTool, schemaProperties])

  /* 파라미터 값 변경 핸들러 */
  const handleParamChange = (key: string, value: string) => {
    setParams((prev) => ({ ...prev, [key]: value }))
  }

  /* 도구 실행 핸들러 */
  const handleExecute = () => {
    if (!selectedToolId) return

    /* 스키마 타입에 따른 파라미터 변환 */
    const parsedParams: Record<string, unknown> = {}
    for (const [key, value] of Object.entries(params)) {
      if (value === '') continue
      const schema = schemaProperties[key]
      if (schema?.type === 'number' || schema?.type === 'integer') {
        const numVal = Number(value)
        if (isNaN(numVal)) {
          addNotification({
            type: 'error',
            message: `"${key}" 필드에 유효한 숫자를 입력해주세요.`,
          })
          return
        }
        parsedParams[key] = numVal
      } else if (schema?.type === 'boolean') {
        parsedParams[key] = value === 'true'
      } else if (schema?.type === 'array' || schema?.type === 'object') {
        try {
          parsedParams[key] = JSON.parse(value)
        } catch {
          addNotification({
            type: 'error',
            message: `"${key}" 필드에 유효한 JSON을 입력해주세요.`,
          })
          return
        }
      } else {
        parsedParams[key] = value
      }
    }

    /* 필수 필드 검증 */
    for (const field of requiredFields) {
      if (!parsedParams[field] && parsedParams[field] !== false && parsedParams[field] !== 0) {
        addNotification({
          type: 'error',
          message: `필수 필드 "${field}"을(를) 입력해주세요.`,
        })
        return
      }
    }

    executeMutation.mutate(
      { toolId: selectedToolId, params: parsedParams },
      {
        onSuccess: (data) => {
          setResult(data.result)
          setExecutionTime(data.executionTimeMs)
          setTraceId(data.traceId)
        },
        onError: (error) => {
          setResult({ error: (error as Error).message || '실행 실패' })
          setExecutionTime(null)
          setTraceId(null)
        },
      },
    )
  }

  /* 결과 초기화 핸들러 */
  const handleClearResult = () => {
    setResult(null)
    setExecutionTime(null)
    setTraceId(null)
  }

  /* 결과를 클립보드에 복사 */
  const handleCopyResult = () => {
    if (result === null) return
    const text = typeof result === 'string' ? result : JSON.stringify(result, null, 2)
    navigator.clipboard.writeText(text).then(() => {
      addNotification({ type: 'success', message: '결과가 클립보드에 복사되었습니다.' })
    })
  }

  /* 로딩 상태 */
  if (toolsLoading) {
    return (
      <div className="space-y-4">
        <div className="section-header">
          <div className="h-5 w-40 bg-primary-100 animate-pulse" />
          <div className="h-3 w-64 bg-primary-100 animate-pulse mt-1" />
        </div>
        <LoadingSkeleton variant="card" rows={2} />
      </div>
    )
  }

  /* 에러 상태 */
  if (toolsError) {
    return <ErrorState message="도구 목록을 불러오는데 실패했습니다." onRetry={refetch} />
  }

  /* 빈 도구 목록 상태 */
  const hasTools = toolsData?.data && toolsData.data.length > 0

  return (
    <div className="space-y-4">
      <div className="section-header">
        <h2 className="text-base font-semibold text-text">도구 테스트</h2>
        <p className="text-2xs text-text-tertiary">
          등록된 도구를 선택하고 파라미터를 입력하여 실행 결과를 확인합니다
        </p>
      </div>

      {!hasTools ? (
        <EmptyState
          title="사용 가능한 도구 없음"
          description="현재 승인된 활성 도구가 없습니다. 먼저 도구를 등록하고 승인받으세요."
        />
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* 좌측: 도구 선택 및 파라미터 입력 */}
          <div className="space-y-4">
            {/* 도구 선택 영역 */}
            <div className="card p-4">
              <FormField label="도구 선택" required>
                <select
                  value={selectedToolId}
                  onChange={(e) => setSelectedToolId(e.target.value)}
                  className="input-field w-full"
                >
                  <option value="">도구를 선택하세요...</option>
                  {toolsData?.data?.map((tool: Tool) => (
                    <option key={tool.id} value={tool.id}>
                      {tool.name} ({tool.type})
                    </option>
                  ))}
                </select>
              </FormField>

              {/* 선택된 도구 정보 표시 */}
              {selectedTool && (
                <div className="mt-3 pt-3 border-t border-t-border">
                  <p className="text-xs text-text-secondary">{selectedTool.description}</p>
                  <div className="flex items-center gap-2 mt-2">
                    <span className="badge-info">{selectedTool.type}</span>
                    <span className="text-2xs text-text-tertiary">v{selectedTool.version}</span>
                  </div>
                </div>
              )}
            </div>

            {/* 파라미터 입력 영역 */}
            {selectedTool && (
              <div className="card p-4 space-y-3">
                <h3 className="text-xs font-semibold text-text border-b border-b-border pb-2">
                  입력 파라미터
                </h3>

                {Object.keys(schemaProperties).length === 0 ? (
                  <p className="text-xs text-text-tertiary">정의된 입력 파라미터가 없습니다</p>
                ) : (
                  Object.entries(schemaProperties).map(([key, schema]) => {
                    const isRequired = requiredFields.includes(key)
                    return (
                      <FormField
                        key={key}
                        label={key}
                        required={isRequired}
                        hint={schema.description}
                      >
                        {schema.enum ? (
                          /* enum 타입: 드롭다운 선택 */
                          <select
                            value={params[key] || ''}
                            onChange={(e) => handleParamChange(key, e.target.value)}
                            className="input-field w-full"
                          >
                            <option value="">선택하세요...</option>
                            {schema.enum.map((opt: string) => (
                              <option key={opt} value={opt}>
                                {opt}
                              </option>
                            ))}
                          </select>
                        ) : schema.type === 'boolean' ? (
                          /* boolean 타입: true/false 선택 */
                          <select
                            value={params[key] || ''}
                            onChange={(e) => handleParamChange(key, e.target.value)}
                            className="input-field w-full"
                          >
                            <option value="">선택하세요...</option>
                            <option value="true">true</option>
                            <option value="false">false</option>
                          </select>
                        ) : schema.type === 'array' || schema.type === 'object' ? (
                          /* 배열/객체 타입: JSON 입력 */
                          <textarea
                            value={params[key] || ''}
                            onChange={(e) => handleParamChange(key, e.target.value)}
                            className="input-field w-full font-mono text-xs min-h-[80px] resize-y"
                            placeholder={`${schema.type} 형식의 JSON을 입력하세요`}
                          />
                        ) : (
                          /* 기본 타입: 텍스트/숫자 입력 */
                          <input
                            type={
                              schema.type === 'number' || schema.type === 'integer'
                                ? 'number'
                                : 'text'
                            }
                            value={params[key] || ''}
                            onChange={(e) => handleParamChange(key, e.target.value)}
                            className="input-field w-full"
                            placeholder={`${key} 값을 입력하세요`}
                          />
                        )}
                      </FormField>
                    )
                  })
                )}

                {/* 실행 버튼 */}
                <div className="pt-2">
                  <button
                    onClick={handleExecute}
                    disabled={executeMutation.isPending || !selectedToolId}
                    className="btn-primary w-full disabled:opacity-50"
                  >
                    {executeMutation.isPending ? '실행 중...' : '실행'}
                  </button>
                </div>
              </div>
            )}
          </div>

          {/* 우측: 실행 결과 표시 */}
          <div className="space-y-4">
            <div className="card p-0">
              <div className="px-4 py-3 border-b border-b-border flex items-center justify-between">
                <h3 className="text-xs font-semibold text-text">실행 결과</h3>
                <div className="flex items-center gap-3">
                  {executionTime !== null && (
                    <span className="text-2xs font-mono text-text-secondary">
                      {formatExecutionTime(executionTime)}
                    </span>
                  )}
                  {result !== null && (
                    <div className="flex items-center gap-2">
                      <button
                        onClick={handleCopyResult}
                        className="text-2xs text-text-secondary hover:text-text transition-colors"
                      >
                        복사
                      </button>
                      <button
                        onClick={handleClearResult}
                        className="text-2xs text-text-secondary hover:text-text transition-colors"
                      >
                        초기화
                      </button>
                    </div>
                  )}
                </div>
              </div>
              <div className="p-4">
                {/* 추적 ID 표시 */}
                {traceId && (
                  <div className="mb-3 pb-3 border-b border-b-border">
                    <span className="text-2xs text-text-tertiary">추적 ID: </span>
                    <span className="text-2xs font-mono text-text-secondary">{traceId}</span>
                  </div>
                )}

                {/* 실행 중 상태 */}
                {executeMutation.isPending ? (
                  <div className="flex items-center justify-center py-12">
                    <p className="text-xs text-text-tertiary animate-pulse">도구 실행 중...</p>
                  </div>
                ) : result !== null ? (
                  /* 결과 표시 */
                  <pre className="text-xs font-mono text-text bg-surface-tertiary border-l-2 border-l-primary-900 p-3 overflow-x-auto whitespace-pre-wrap max-h-[500px] overflow-y-auto">
                    {typeof result === 'string' ? result : JSON.stringify(result, null, 2)}
                  </pre>
                ) : (
                  /* 빈 상태 */
                  <div className="flex items-center justify-center py-12">
                    <p className="text-xs text-text-tertiary">
                      {selectedToolId
                        ? '도구를 실행하면 결과가 여기에 표시됩니다'
                        : '도구를 선택하고 파라미터를 설정하세요'}
                    </p>
                  </div>
                )}
              </div>
            </div>

            {/* 실행 시간 상세 정보 */}
            {executionTime !== null && (
              <div className="card-subtle p-3">
                <div className="flex items-center justify-between">
                  <span className="text-2xs text-text-tertiary">실행 시간</span>
                  <span className="text-xs font-mono text-text">
                    {formatExecutionTime(executionTime)}
                  </span>
                </div>
                {traceId && (
                  <div className="flex items-center justify-between mt-1.5">
                    <span className="text-2xs text-text-tertiary">추적 ID</span>
                    <span className="text-2xs font-mono text-text-secondary">{traceId}</span>
                  </div>
                )}
                {selectedTool && (
                  <div className="flex items-center justify-between mt-1.5">
                    <span className="text-2xs text-text-tertiary">도구</span>
                    <span className="text-2xs text-text-secondary">{selectedTool.name}</span>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  )
}
