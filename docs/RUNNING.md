# 실행 가이드 - Universal MCP Gateway (UMG)

## 시스템 시작

### 프로덕션 모드

```bash
# 전체 서비스 시작
docker compose up -d

# 로그 확인
docker compose logs -f

# 전체 서비스 중지
docker compose down
```

### 개발 모드

```bash
# 터미널 1: 인프라
docker compose -f docker-compose.dev.yml up -d

# 터미널 2: 백엔드
cd backend && ./gradlew bootRun --args='--spring.profiles.active=local'

# 터미널 3: 프론트엔드
cd frontend && npm run dev
```

---

## API 참조

### 인증

#### 로그인 (웹 UI)

```bash
POST /api/auth/login
Content-Type: application/json

{
  "email": "admin@umg.local",
  "password": "admin123!"
}
```

응답:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "user": {
    "id": "uuid",
    "email": "admin@umg.local",
    "name": "Admin",
    "role": "ADMIN"
  }
}
```

#### API 키 인증 (에이전트)

```bash
# 요청 헤더에 API 키 포함
curl -H "X-API-Key: umg_key_xxxxx" http://localhost:8080/api/mcp
```

### 도구 관리

#### 도구 목록 조회

```bash
GET /api/tools?page=0&size=20&status=APPROVED&type=N8N
Authorization: Bearer {token}
```

#### 도구 생성

```bash
POST /api/tools
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "production-metrics",
  "description": "MES 시스템에서 생산 라인 지표를 조회합니다",
  "toolType": "N8N",
  "connectionConfig": {
    "webhookUrl": "https://n8n.internal/webhook/abc123",
    "authToken": "bearer-token-here"
  },
  "authType": "BEARER",
  "inputSchema": {
    "type": "object",
    "properties": {
      "lineId": { "type": "string", "description": "생산 라인 ID" },
      "metric": { "type": "string", "enum": ["temperature", "output", "defectRate"] }
    },
    "required": ["lineId", "metric"]
  },
  "responseMappingRule": "$.data.metrics[*]",
  "isPublic": false,
  "isIdempotent": true
}
```

#### 도구 승인 (관리자)

```bash
POST /api/tools/{toolId}/approve
Authorization: Bearer {token}
```

### API 키 관리

#### API 키 생성

```bash
POST /api/keys
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "Cursor IDE - 개발 노트북",
  "rateLimitPerMin": 60,
  "expiresAt": "2026-12-31T23:59:59Z"
}
```

응답 (원본 키는 한 번만 표시됨):
```json
{
  "id": "uuid",
  "name": "Cursor IDE - 개발 노트북",
  "key": "umg_key_a1b2c3d4e5f6...",
  "rateLimitPerMin": 60,
  "expiresAt": "2026-12-31T23:59:59Z"
}
```

### MCP 프로토콜

#### 초기화

```bash
POST /api/mcp
X-API-Key: umg_key_xxxxx
Content-Type: application/json

{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "initialize",
  "params": {
    "protocolVersion": "2024-11-05",
    "capabilities": {},
    "clientInfo": {
      "name": "cursor",
      "version": "1.0.0"
    }
  }
}
```

#### 사용 가능한 도구 목록

```bash
POST /api/mcp
X-API-Key: umg_key_xxxxx
Content-Type: application/json

{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "tools/list",
  "params": {}
}
```

응답:
```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "result": {
    "tools": [
      {
        "name": "production-metrics",
        "description": "MES 시스템에서 생산 라인 지표를 조회합니다",
        "inputSchema": {
          "type": "object",
          "properties": {
            "lineId": { "type": "string" },
            "metric": { "type": "string", "enum": ["temperature", "output", "defectRate"] }
          },
          "required": ["lineId", "metric"]
        }
      }
    ]
  }
}
```

#### 도구 호출

```bash
POST /api/mcp
X-API-Key: umg_key_xxxxx
Content-Type: application/json

{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "tools/call",
  "params": {
    "name": "production-metrics",
    "arguments": {
      "lineId": "LINE-003",
      "metric": "temperature"
    }
  }
}
```

#### SSE 스트리밍

```bash
GET /api/mcp/sse
X-API-Key: umg_key_xxxxx
Accept: text/event-stream
```

### Cube.js 데이터소스 관리 (관리자 전용)

#### 데이터소스 목록 조회

```bash
GET /api/cube/datasources?status=ACTIVE&page=0&size=20
Authorization: Bearer {token}
```

#### 데이터소스 생성

```bash
POST /api/cube/datasources
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "analytics-postgres",
  "description": "분석용 PostgreSQL 데이터소스",
  "dbType": "POSTGRESQL",
  "host": "analytics-db.internal",
  "port": 5432,
  "database": "analytics",
  "username": "cube_reader",
  "password": "secure-password"
}
```

응답 (201 Created):
```json
{
  "id": "uuid",
  "name": "analytics-postgres",
  "dbType": "POSTGRESQL",
  "status": "ACTIVE",
  "lastTestedAt": null,
  "creatorName": "Admin",
  "createdAt": "2026-02-27T10:00:00Z"
}
```

#### 연결 테스트

```bash
POST /api/cube/datasources/{id}/test
Authorization: Bearer {token}
```

응답:
```json
{
  "success": true,
  "message": "연결 성공 (PostgreSQL 16.2)",
  "responseTimeMs": 42
}
```

#### 테이블 인트로스펙션

```bash
GET /api/cube/datasources/{id}/tables
Authorization: Bearer {token}
```

응답:
```json
{
  "orders": [
    {"columnName": "id", "dataType": "uuid", "isNullable": false},
    {"columnName": "amount", "dataType": "numeric", "isNullable": true}
  ]
}
```

### Cube.js 스키마 관리 (관리자 전용)

#### 스키마 생성

```bash
POST /api/cube/schemas
Authorization: Bearer {token}
Content-Type: application/json

{
  "datasourceId": "datasource-uuid",
  "name": "orders-cube",
  "description": "주문 분석용 큐브 스키마",
  "schemaDefinition": "{\"cube\":\"Orders\",\"measures\":{\"count\":{\"type\":\"count\"}},\"dimensions\":{\"status\":{\"sql\":\"status\",\"type\":\"string\"}}}"
}
```

#### 스키마 유효성 검증

```bash
POST /api/cube/schemas/{id}/validate
Authorization: Bearer {token}
```

#### 스키마 활성화 / 아카이브

```bash
POST /api/cube/schemas/{id}/activate
POST /api/cube/schemas/{id}/archive
Authorization: Bearer {token}
```

#### 활성 스키마 메타 조회

```bash
GET /api/cube/schemas/meta
Authorization: Bearer {token}
```

응답:
```json
[
  {
    "schemaName": "orders-cube",
    "cubeName": "Orders",
    "measures": ["count"],
    "dimensions": ["status"]
  }
]
```

### AWS MCP 서버 관리 (관리자 전용)

#### 서버 등록

```bash
POST /api/aws-mcp/servers
Authorization: Bearer {token}
Content-Type: application/json

{
  "name": "redshift-mcp",
  "description": "AWS Redshift MCP 서버",
  "endpointUrl": "https://mcp-redshift.us-east-1.amazonaws.com",
  "region": "us-east-1",
  "authType": "IAM_KEY",
  "accessKeyId": "AKIAIOSFODNN7EXAMPLE",
  "secretAccessKey": "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"
}
```

#### 연결 테스트

```bash
POST /api/aws-mcp/servers/{id}/test
Authorization: Bearer {token}
```

응답:
```json
{
  "success": true,
  "message": "연결 성공",
  "protocolVersion": "2024-11-05",
  "serverName": "aws-redshift-mcp-server",
  "responseTimeMs": 320
}
```

#### 도구 동기화

```bash
POST /api/aws-mcp/servers/{id}/sync
Authorization: Bearer {token}
```

응답:
```json
{
  "success": true,
  "message": "동기화 완료: 10개 발견, 7개 생성, 3개 업데이트",
  "toolsDiscovered": 10,
  "toolsCreated": 7,
  "toolsUpdated": 3
}
```

#### 동기화 이력 조회

```bash
GET /api/aws-mcp/servers/{id}/sync-history?page=0&size=20
Authorization: Bearer {token}
```

### 권한 관리

#### 권한 부여

```bash
POST /api/permissions
Authorization: Bearer {token}
Content-Type: application/json

{
  "toolId": "tool-uuid",
  "userId": "user-uuid",
  "accessLevel": "EXECUTE"
}
```

### 감사 로그

#### 감사 로그 조회

```bash
GET /api/audit-logs?userId={uuid}&toolId={uuid}&status=SUCCESS&from=2026-01-01&to=2026-12-31&page=0&size=50
Authorization: Bearer {token}
```

### 대시보드

#### 대시보드 통계 조회

```bash
GET /api/dashboard/stats
Authorization: Bearer {token}
```

응답:
```json
{
  "totalTools": 42,
  "activeApiKeys": 15,
  "requestsLast24h": 12450,
  "errorRate": 0.023,
  "toolsByType": {
    "N8N": 18,
    "CUBE_JS": 12,
    "AWS_REMOTE": 12
  },
  "recentActivity": [...]
}
```

---

## 브릿지 CLI 사용법

브릿지 CLI는 로컬 AI 도구(Cursor, VS Code 등)를 UMG 서버에 연결합니다.

### 설정

```bash
# 기본 사용법
umg-bridge --server-url https://umg.company.com --api-key umg_key_xxxxx

# 커스텀 옵션
umg-bridge \
  --server-url https://umg.company.com \
  --api-key umg_key_xxxxx \
  --timeout 30000 \
  --retry-count 3
```

### MCP 클라이언트 설정

Cursor IDE의 경우, `.cursor/mcp.json`에 추가:

```json
{
  "mcpServers": {
    "umg": {
      "command": "npx",
      "args": ["-y", "umg-bridge", "--server-url", "https://umg.company.com", "--api-key", "umg_key_xxxxx"]
    }
  }
}
```

Claude Desktop의 경우, `claude_desktop_config.json`에 추가:

```json
{
  "mcpServers": {
    "umg": {
      "command": "npx",
      "args": ["-y", "umg-bridge"],
      "env": {
        "UMG_SERVER_URL": "https://umg.company.com",
        "UMG_API_KEY": "umg_key_xxxxx"
      }
    }
  }
}
```

---

## 모니터링

### Actuator 엔드포인트

| 엔드포인트 | 설명 |
|-----------|------|
| `/actuator/health` | 헬스체크 (DB, Redis, 디스크) |
| `/actuator/info` | 애플리케이션 정보 |
| `/actuator/metrics` | Micrometer 메트릭 |
| `/actuator/prometheus` | Prometheus 형식 메트릭 |

### 주요 메트릭

| 메트릭 | 설명 |
|--------|------|
| `umg.tools.execution.count` | 전체 도구 실행 횟수 |
| `umg.tools.execution.duration` | 도구 실행 지연시간 |
| `umg.tools.execution.errors` | 도구 실행 오류 횟수 |
| `umg.mcp.connections.active` | 활성 SSE 연결 수 |
| `umg.rate-limit.rejected` | 속도 제한된 요청 수 |

### 로그 형식

MDC 추적 ID를 포함한 구조화된 JSON 로깅:

```json
{
  "timestamp": "2026-02-27T10:30:00.000Z",
  "level": "INFO",
  "traceId": "abc-123-def-456",
  "userId": "user-uuid",
  "logger": "com.umg.service.McpService",
  "message": "도구 실행 성공",
  "toolName": "production-metrics",
  "executionTimeMs": 234
}
```

---

## 운영 작업

### 데이터베이스 백업

```bash
# 백업
docker compose exec postgres pg_dump -U umg umg > backup_$(date +%Y%m%d).sql

# 복원
docker compose exec -T postgres psql -U umg umg < backup_20260227.sql
```

### Redis 캐시 초기화

```bash
# 전체 캐시 삭제
docker compose exec redis redis-cli FLUSHDB

# 특정 패턴 삭제
docker compose exec redis redis-cli --scan --pattern "rate-limit:*" | xargs redis-cli DEL
```

### 로그 로테이션

로그는 기본적으로 stdout/stderr로 출력됩니다 (Docker가 로테이션 관리). 파일 기반 로깅의 경우, `application.yml`에서 설정:

```yaml
logging:
  file:
    name: /var/log/umg/application.log
    max-size: 100MB
    max-history: 30
```
