export enum UserRole {
  ADMIN = 'ADMIN',
  DEVELOPER = 'DEVELOPER',
  VIEWER = 'VIEWER',
}

export enum ToolType {
  N8N = 'N8N',
  CUBE_JS = 'CUBE_JS',
  AWS_REMOTE = 'AWS_REMOTE',
  CUSTOM = 'CUSTOM',
}

export enum AuthType {
  API_KEY = 'API_KEY',
  BEARER_TOKEN = 'BEARER_TOKEN',
  BASIC = 'BASIC',
  NONE = 'NONE',
}

export enum ToolStatus {
  ACTIVE = 'ACTIVE',
  PENDING = 'PENDING',
  DISABLED = 'DISABLED',
  REJECTED = 'REJECTED',
}

export enum AccessLevel {
  FULL = 'FULL',
  READ_ONLY = 'READ_ONLY',
  RESTRICTED = 'RESTRICTED',
}

export enum AuditStatus {
  SUCCESS = 'SUCCESS',
  FAILURE = 'FAILURE',
  ERROR = 'ERROR',
  PENDING = 'PENDING',
}

export interface User {
  id: string
  email: string
  name: string
  role: UserRole
  department: string
  active: boolean
  createdAt: string
  updatedAt: string
}

export interface ApiKey {
  id: string
  name: string
  keyPrefix: string
  userId: string
  userName?: string
  rateLimit: number
  rateLimitWindow: string
  expiresAt: string | null
  active: boolean
  lastUsedAt: string | null
  createdAt: string
}

export interface ApiKeyCreateResponse {
  id: string
  name: string
  rawKey: string
  keyPrefix: string
  expiresAt: string | null
  createdAt: string
}

export interface ConnectionConfig {
  webhookUrl?: string
  apiUrl?: string
  apiKey?: string
  authType?: AuthType
  region?: string
  service?: string
  endpoint?: string
  measures?: string[]
  dimensions?: string[]
  headers?: Record<string, string>
  [key: string]: unknown
}

export interface InputSchemaField {
  name: string
  type: string
  description: string
  required: boolean
  default?: unknown
  enum?: string[]
}

export interface InputSchema {
  type: string
  properties: Record<string, {
    type: string
    description?: string
    default?: unknown
    enum?: string[]
  }>
  required?: string[]
}

export interface Tool {
  id: string
  name: string
  description: string
  type: ToolType
  status: ToolStatus
  connectionConfig: ConnectionConfig
  inputSchema: InputSchema
  responseMapping: string
  ownerId: string
  ownerName?: string
  version: number
  createdAt: string
  updatedAt: string
}

export interface Permission {
  id: string
  userId: string
  userName?: string
  userEmail?: string
  toolId: string
  toolName?: string
  accessLevel: AccessLevel
  grantedBy: string
  grantedByName?: string
  expiresAt: string | null
  createdAt: string
}

export interface AuditLog {
  id: string
  traceId: string
  userId: string
  userName?: string
  toolId: string
  toolName?: string
  status: AuditStatus
  inputParams: Record<string, unknown>
  outputSummary: string
  executionTimeMs: number
  errorMessage: string | null
  ipAddress: string
  createdAt: string
}

export interface DashboardStats {
  totalTools: number
  activeTools: number
  totalApiKeys: number
  activeApiKeys: number
  totalRequests24h: number
  errorRate24h: number
  toolsByType: Record<string, number>
  requestTrend: {
    date: string
    requests: number
    errors: number
  }[]
  recentActivity: AuditLog[]
}

export interface PaginatedResponse<T> {
  data: T[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

export interface PaginationParams {
  page?: number
  pageSize?: number
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
}

export interface ToolFilters extends PaginationParams {
  type?: ToolType
  status?: ToolStatus
  search?: string
}

export interface AuditLogFilters extends PaginationParams {
  userId?: string
  toolId?: string
  status?: AuditStatus
  startDate?: string
  endDate?: string
}

export interface LoginRequest {
  email: string
  password: string
}

export interface LoginResponse {
  token: string
  user: User
}

export interface RegisterRequest {
  email: string
  name: string
  password: string
  department: string
}

export interface CreateToolRequest {
  name: string
  description: string
  type: ToolType
  connectionConfig: ConnectionConfig
  inputSchema: InputSchema
  responseMapping: string
}

export interface UpdateToolRequest extends Partial<CreateToolRequest> {
  id: string
}

export interface CreateApiKeyRequest {
  name: string
  rateLimit?: number
  rateLimitWindow?: string
  expiresAt?: string
}

export interface GrantPermissionRequest {
  userId: string
  toolId: string
  accessLevel: AccessLevel
  expiresAt?: string
}

export interface ExecuteToolRequest {
  toolId: string
  params: Record<string, unknown>
}

export interface ExecuteToolResponse {
  traceId: string
  result: unknown
  executionTimeMs: number
}
