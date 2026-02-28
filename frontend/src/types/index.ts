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

/* ===== Cube.js 데이터소스 ===== */

export enum CubeDbType {
  POSTGRESQL = 'POSTGRESQL',
  MYSQL = 'MYSQL',
  BIGQUERY = 'BIGQUERY',
  REDSHIFT = 'REDSHIFT',
  SNOWFLAKE = 'SNOWFLAKE',
  CLICKHOUSE = 'CLICKHOUSE',
}

export enum DataSourceStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  ERROR = 'ERROR',
}

export enum SchemaStatus {
  DRAFT = 'DRAFT',
  ACTIVE = 'ACTIVE',
  ARCHIVED = 'ARCHIVED',
}

export enum AwsAuthType {
  IAM_KEY = 'IAM_KEY',
  IAM_ROLE = 'IAM_ROLE',
}

export interface CubeDataSource {
  id: string
  name: string
  description: string
  dbType: CubeDbType
  status: DataSourceStatus
  lastTestedAt: string | null
  createdBy: string
  creatorName: string
  createdAt: string
  updatedAt: string
}

export interface CreateCubeDataSourceRequest {
  name: string
  description?: string
  dbType: CubeDbType
  host: string
  port: number
  database: string
  username: string
  password: string
}

export interface UpdateCubeDataSourceRequest {
  name?: string
  description?: string
  host?: string
  port?: number
  database?: string
  username?: string
  password?: string
}

export interface ConnectionTestResult {
  success: boolean
  message: string
  responseTimeMs: number | null
}

export interface CubeSchema {
  id: string
  datasourceId: string
  datasourceName: string
  name: string
  description: string
  schemaDefinition: string
  version: number
  status: SchemaStatus
  createdBy: string
  creatorName: string
  createdAt: string
  updatedAt: string
}

export interface CreateCubeSchemaRequest {
  datasourceId: string
  name: string
  description?: string
  schemaDefinition: string
}

export interface UpdateCubeSchemaRequest {
  name?: string
  description?: string
  schemaDefinition?: string
}

export interface ValidationResult {
  valid: boolean
  message: string
}

export interface CubeMeta {
  cubeName: string
  schemaId: string
  schemaName: string
  measures: string[]
  dimensions: string[]
}

/* ===== AWS MCP 서버 ===== */

export interface AwsMcpServer {
  id: string
  name: string
  description: string
  endpointUrl: string
  region: string
  service: string
  authType: AwsAuthType
  status: DataSourceStatus
  syncedToolCount: number
  lastSyncedAt: string | null
  lastHealthCheckAt: string | null
  createdBy: string
  creatorName: string
  createdAt: string
  updatedAt: string
}

export interface CreateAwsMcpServerRequest {
  name: string
  description?: string
  endpointUrl: string
  region: string
  service?: string
  authType: AwsAuthType
  accessKeyId?: string
  secretAccessKey?: string
  roleArn?: string
}

export interface UpdateAwsMcpServerRequest {
  name?: string
  description?: string
  endpointUrl?: string
  region?: string
  service?: string
  accessKeyId?: string
  secretAccessKey?: string
  roleArn?: string
}

export interface AwsConnectionTestResult {
  success: boolean
  message: string
  protocolVersion: string | null
  serverName: string | null
  responseTimeMs: number | null
}

export interface SyncResult {
  success: boolean
  message: string
  toolsDiscovered: number
  toolsCreated: number
  toolsUpdated: number
}

export interface SyncHistory {
  id: string
  serverId: string
  status: string
  toolsDiscovered: number
  toolsCreated: number
  toolsUpdated: number
  errorMessage: string | null
  createdAt: string
}
