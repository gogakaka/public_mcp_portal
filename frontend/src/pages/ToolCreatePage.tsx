import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useCreateTool } from '@/api/tools'
import { useUIStore } from '@/stores/uiStore'
import { FormField } from '@/components/common/FormField'
import { Breadcrumb } from '@/components/layout/Breadcrumb'
import { ToolType } from '@/types'
import { cn } from '@/lib/utils'

const steps = [
  { id: 1, label: 'Basic Info' },
  { id: 2, label: 'Connection' },
  { id: 3, label: 'Input Schema' },
  { id: 4, label: 'Response Mapping' },
  { id: 5, label: 'Review' },
]

const basicInfoSchema = z.object({
  name: z.string().min(2, 'Name must be at least 2 characters').max(100),
  description: z.string().min(10, 'Description must be at least 10 characters').max(1000),
  type: z.nativeEnum(ToolType, { errorMap: () => ({ message: 'Select a tool type' }) }),
})

const n8nConnectionSchema = z.object({
  webhookUrl: z.string().url('Valid URL is required'),
  authType: z.string().optional(),
  apiKey: z.string().optional(),
})

const cubeJsConnectionSchema = z.object({
  apiUrl: z.string().url('Valid URL is required'),
  apiKey: z.string().optional(),
  measures: z.string().optional(),
  dimensions: z.string().optional(),
})

const awsConnectionSchema = z.object({
  region: z.string().min(1, 'Region is required'),
  service: z.string().min(1, 'Service is required'),
  endpoint: z.string().url('Valid URL is required'),
  apiKey: z.string().optional(),
})

const customConnectionSchema = z.object({
  apiUrl: z.string().url('Valid URL is required'),
  authType: z.string().optional(),
  apiKey: z.string().optional(),
})

type BasicInfoForm = z.infer<typeof basicInfoSchema>

export default function ToolCreatePage() {
  const navigate = useNavigate()
  const setCurrentPage = useUIStore((s) => s.setCurrentPage)
  const createTool = useCreateTool()

  const [currentStep, setCurrentStep] = useState(1)
  const [basicInfo, setBasicInfo] = useState<BasicInfoForm | null>(null)
  const [connectionConfig, setConnectionConfig] = useState<Record<string, unknown>>({})
  const [inputSchemaText, setInputSchemaText] = useState(
    JSON.stringify(
      {
        type: 'object',
        properties: {
          query: { type: 'string', description: 'Search query' },
        },
        required: ['query'],
      },
      null,
      2,
    ),
  )
  const [inputSchemaError, setInputSchemaError] = useState('')
  const [responseMapping, setResponseMapping] = useState('$.data')

  useEffect(() => {
    setCurrentPage('Register Tool')
  }, [setCurrentPage])

  const {
    register: registerBasic,
    handleSubmit: handleBasicSubmit,
    watch: watchBasic,
    formState: { errors: basicErrors },
  } = useForm<BasicInfoForm>({
    resolver: zodResolver(basicInfoSchema),
    defaultValues: basicInfo || undefined,
  })

  const selectedType = watchBasic('type')

  const getConnectionSchema = () => {
    switch (selectedType || basicInfo?.type) {
      case ToolType.N8N:
        return n8nConnectionSchema
      case ToolType.CUBE_JS:
        return cubeJsConnectionSchema
      case ToolType.AWS_REMOTE:
        return awsConnectionSchema
      default:
        return customConnectionSchema
    }
  }

  const {
    register: registerConnection,
    handleSubmit: handleConnectionSubmit,
    formState: { errors: connectionErrors },
  } = useForm({
    resolver: zodResolver(getConnectionSchema()),
    defaultValues: connectionConfig,
  })

  const onBasicSubmit = (data: BasicInfoForm) => {
    setBasicInfo(data)
    setCurrentStep(2)
  }

  const onConnectionSubmit = (data: Record<string, unknown>) => {
    const cleaned: Record<string, unknown> = {}
    for (const [key, value] of Object.entries(data)) {
      if (key === 'measures' || key === 'dimensions') {
        const strVal = value as string
        if (strVal) {
          cleaned[key] = strVal.split(',').map((s: string) => s.trim()).filter(Boolean)
        }
      } else if (value !== undefined && value !== '') {
        cleaned[key] = value
      }
    }
    setConnectionConfig(cleaned)
    setCurrentStep(3)
  }

  const handleSchemaStep = () => {
    try {
      JSON.parse(inputSchemaText)
      setInputSchemaError('')
      setCurrentStep(4)
    } catch {
      setInputSchemaError('Invalid JSON. Please check your schema.')
    }
  }

  const handleResponseStep = () => {
    setCurrentStep(5)
  }

  const handleFinalSubmit = () => {
    if (!basicInfo) return

    let parsedSchema
    try {
      parsedSchema = JSON.parse(inputSchemaText)
    } catch {
      return
    }

    createTool.mutate(
      {
        name: basicInfo.name,
        description: basicInfo.description,
        type: basicInfo.type,
        connectionConfig,
        inputSchema: parsedSchema,
        responseMapping,
      },
      {
        onSuccess: (tool) => {
          navigate(`/tools/${tool.id}`)
        },
      },
    )
  }

  const renderConnectionFields = () => {
    const toolType = basicInfo?.type
    const errs = connectionErrors as Record<string, { message?: string }>

    switch (toolType) {
      case ToolType.N8N:
        return (
          <>
            <FormField label="Webhook URL" error={errs.webhookUrl?.message} required>
              <input
                {...registerConnection('webhookUrl')}
                className="input-field w-full"
                placeholder="https://n8n.example.com/webhook/..."
              />
            </FormField>
            <FormField label="Auth Type" error={errs.authType?.message}>
              <select {...registerConnection('authType')} className="input-field w-full">
                <option value="">None</option>
                <option value="API_KEY">API Key</option>
                <option value="BEARER_TOKEN">Bearer Token</option>
              </select>
            </FormField>
            <FormField label="API Key" error={errs.apiKey?.message} hint="Optional authentication key">
              <input
                {...registerConnection('apiKey')}
                type="password"
                className="input-field w-full"
                placeholder="Enter API key"
              />
            </FormField>
          </>
        )

      case ToolType.CUBE_JS:
        return (
          <>
            <FormField label="API URL" error={errs.apiUrl?.message} required>
              <input
                {...registerConnection('apiUrl')}
                className="input-field w-full"
                placeholder="https://cubejs.example.com/cubejs-api/v1"
              />
            </FormField>
            <FormField label="API Key" error={errs.apiKey?.message}>
              <input
                {...registerConnection('apiKey')}
                type="password"
                className="input-field w-full"
                placeholder="Cube.js API token"
              />
            </FormField>
            <FormField
              label="Measures"
              error={errs.measures?.message}
              hint="Comma-separated list: Orders.count, Orders.totalAmount"
            >
              <input
                {...registerConnection('measures')}
                className="input-field w-full"
                placeholder="Orders.count, Orders.totalAmount"
              />
            </FormField>
            <FormField
              label="Dimensions"
              error={errs.dimensions?.message}
              hint="Comma-separated list: Orders.status, Orders.createdAt"
            >
              <input
                {...registerConnection('dimensions')}
                className="input-field w-full"
                placeholder="Orders.status, Orders.createdAt"
              />
            </FormField>
          </>
        )

      case ToolType.AWS_REMOTE:
        return (
          <>
            <FormField label="AWS Region" error={errs.region?.message} required>
              <input
                {...registerConnection('region')}
                className="input-field w-full"
                placeholder="us-east-1"
              />
            </FormField>
            <FormField label="Service" error={errs.service?.message} required>
              <input
                {...registerConnection('service')}
                className="input-field w-full"
                placeholder="lambda"
              />
            </FormField>
            <FormField label="Endpoint" error={errs.endpoint?.message} required>
              <input
                {...registerConnection('endpoint')}
                className="input-field w-full"
                placeholder="https://lambda.us-east-1.amazonaws.com/..."
              />
            </FormField>
            <FormField label="API Key" error={errs.apiKey?.message}>
              <input
                {...registerConnection('apiKey')}
                type="password"
                className="input-field w-full"
                placeholder="AWS access key"
              />
            </FormField>
          </>
        )

      default:
        return (
          <>
            <FormField label="API URL" error={errs.apiUrl?.message} required>
              <input
                {...registerConnection('apiUrl')}
                className="input-field w-full"
                placeholder="https://api.example.com/v1"
              />
            </FormField>
            <FormField label="Auth Type" error={errs.authType?.message}>
              <select {...registerConnection('authType')} className="input-field w-full">
                <option value="">None</option>
                <option value="API_KEY">API Key</option>
                <option value="BEARER_TOKEN">Bearer Token</option>
                <option value="BASIC">Basic Auth</option>
              </select>
            </FormField>
            <FormField label="API Key" error={errs.apiKey?.message}>
              <input
                {...registerConnection('apiKey')}
                type="password"
                className="input-field w-full"
                placeholder="Enter API key"
              />
            </FormField>
          </>
        )
    }
  }

  return (
    <div className="space-y-6 max-w-3xl">
      <Breadcrumb
        items={[
          { label: 'Tools', path: '/tools' },
          { label: 'Register New Tool' },
        ]}
      />

      <div className="section-header">
        <h2 className="text-lg font-bold text-text">Register New Tool</h2>
        <p className="text-2xs text-text-tertiary">Step {currentStep} of {steps.length}</p>
      </div>

      <div className="flex items-center gap-0 mb-6">
        {steps.map((step, index) => (
          <div key={step.id} className="flex items-center">
            {index > 0 && (
              <div
                className={cn(
                  'h-px w-6 sm:w-10',
                  step.id <= currentStep ? 'bg-primary-900' : 'bg-border',
                )}
              />
            )}
            <div className="flex items-center gap-1.5">
              <div
                className={cn(
                  'w-5 h-5 flex items-center justify-center text-2xs font-medium',
                  step.id === currentStep
                    ? 'bg-primary-900 text-white'
                    : step.id < currentStep
                      ? 'bg-primary-900 text-white'
                      : 'bg-surface-tertiary text-text-tertiary',
                )}
              >
                {step.id < currentStep ? '-' : step.id}
              </div>
              <span
                className={cn(
                  'text-2xs hidden sm:inline',
                  step.id === currentStep
                    ? 'text-text font-medium'
                    : 'text-text-tertiary',
                )}
              >
                {step.label}
              </span>
            </div>
          </div>
        ))}
      </div>

      {currentStep === 1 && (
        <form onSubmit={handleBasicSubmit(onBasicSubmit)} className="card p-6 space-y-4">
          <FormField label="Tool Name" error={basicErrors.name?.message} required>
            <input
              {...registerBasic('name')}
              className="input-field w-full"
              placeholder="My Analysis Tool"
            />
          </FormField>

          <FormField label="Description" error={basicErrors.description?.message} required>
            <textarea
              {...registerBasic('description')}
              className="input-field w-full min-h-[80px] resize-y"
              placeholder="Describe what this tool does..."
              rows={3}
            />
          </FormField>

          <FormField label="Tool Type" error={basicErrors.type?.message} required>
            <select {...registerBasic('type')} className="input-field w-full">
              <option value="">Select type...</option>
              {Object.values(ToolType).map((type) => (
                <option key={type} value={type}>
                  {type}
                </option>
              ))}
            </select>
          </FormField>

          <div className="flex justify-end pt-2">
            <button type="submit" className="btn-primary text-xs">
              Next: Connection Config
            </button>
          </div>
        </form>
      )}

      {currentStep === 2 && (
        <form onSubmit={handleConnectionSubmit(onConnectionSubmit)} className="card p-6 space-y-4">
          <div className="mb-2">
            <h3 className="text-sm font-semibold text-text">
              Connection Configuration for {basicInfo?.type}
            </h3>
            <p className="text-2xs text-text-tertiary mt-0.5">
              Configure how UMG connects to this tool
            </p>
          </div>
          {renderConnectionFields()}
          <div className="flex justify-between pt-2">
            <button
              type="button"
              onClick={() => setCurrentStep(1)}
              className="btn-secondary text-xs"
            >
              Back
            </button>
            <button type="submit" className="btn-primary text-xs">
              Next: Input Schema
            </button>
          </div>
        </form>
      )}

      {currentStep === 3 && (
        <div className="card p-6 space-y-4">
          <div className="mb-2">
            <h3 className="text-sm font-semibold text-text">Input Schema Definition</h3>
            <p className="text-2xs text-text-tertiary mt-0.5">
              Define the JSON schema for tool input parameters
            </p>
          </div>
          <FormField label="JSON Schema" error={inputSchemaError}>
            <textarea
              value={inputSchemaText}
              onChange={(e) => {
                setInputSchemaText(e.target.value)
                setInputSchemaError('')
              }}
              className="input-field w-full min-h-[240px] font-mono text-xs resize-y"
              spellCheck={false}
            />
          </FormField>
          <div className="flex justify-between pt-2">
            <button
              type="button"
              onClick={() => setCurrentStep(2)}
              className="btn-secondary text-xs"
            >
              Back
            </button>
            <button
              type="button"
              onClick={handleSchemaStep}
              className="btn-primary text-xs"
            >
              Next: Response Mapping
            </button>
          </div>
        </div>
      )}

      {currentStep === 4 && (
        <div className="card p-6 space-y-4">
          <div className="mb-2">
            <h3 className="text-sm font-semibold text-text">Response Mapping Rule</h3>
            <p className="text-2xs text-text-tertiary mt-0.5">
              Define how to extract data from the tool response (JSONPath expression)
            </p>
          </div>
          <FormField label="Response Mapping" hint="Use JSONPath syntax, e.g., $.data, $.results[0]">
            <input
              value={responseMapping}
              onChange={(e) => setResponseMapping(e.target.value)}
              className="input-field w-full font-mono"
              placeholder="$.data"
            />
          </FormField>
          <div className="flex justify-between pt-2">
            <button
              type="button"
              onClick={() => setCurrentStep(3)}
              className="btn-secondary text-xs"
            >
              Back
            </button>
            <button
              type="button"
              onClick={handleResponseStep}
              className="btn-primary text-xs"
            >
              Next: Review
            </button>
          </div>
        </div>
      )}

      {currentStep === 5 && (
        <div className="card p-6 space-y-6">
          <div className="mb-2">
            <h3 className="text-sm font-semibold text-text">Review and Submit</h3>
            <p className="text-2xs text-text-tertiary mt-0.5">
              Review your tool configuration before submitting
            </p>
          </div>

          <div className="space-y-4">
            <div className="border-l-2 border-l-primary-900 pl-3">
              <p className="text-2xs text-text-tertiary uppercase tracking-wider font-medium">
                Basic Information
              </p>
              <div className="mt-2 space-y-1">
                <p className="text-sm text-text">
                  <span className="text-text-secondary">Name:</span> {basicInfo?.name}
                </p>
                <p className="text-sm text-text">
                  <span className="text-text-secondary">Type:</span> {basicInfo?.type}
                </p>
                <p className="text-sm text-text">
                  <span className="text-text-secondary">Description:</span> {basicInfo?.description}
                </p>
              </div>
            </div>

            <div className="border-l-2 border-l-primary-900 pl-3">
              <p className="text-2xs text-text-tertiary uppercase tracking-wider font-medium">
                Connection Configuration
              </p>
              <pre className="mt-2 text-xs font-mono text-text bg-surface-tertiary p-2 overflow-x-auto">
                {JSON.stringify(connectionConfig, null, 2)}
              </pre>
            </div>

            <div className="border-l-2 border-l-primary-900 pl-3">
              <p className="text-2xs text-text-tertiary uppercase tracking-wider font-medium">
                Input Schema
              </p>
              <pre className="mt-2 text-xs font-mono text-text bg-surface-tertiary p-2 overflow-x-auto">
                {inputSchemaText}
              </pre>
            </div>

            <div className="border-l-2 border-l-primary-900 pl-3">
              <p className="text-2xs text-text-tertiary uppercase tracking-wider font-medium">
                Response Mapping
              </p>
              <p className="mt-2 text-xs font-mono text-text">{responseMapping}</p>
            </div>
          </div>

          <div className="flex justify-between pt-2">
            <button
              type="button"
              onClick={() => setCurrentStep(4)}
              className="btn-secondary text-xs"
            >
              Back
            </button>
            <button
              type="button"
              onClick={handleFinalSubmit}
              disabled={createTool.isPending}
              className="btn-primary text-xs disabled:opacity-50"
            >
              {createTool.isPending ? 'Creating...' : 'Submit Tool'}
            </button>
          </div>
        </div>
      )}
    </div>
  )
}
