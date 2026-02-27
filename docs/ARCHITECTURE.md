# 아키텍처 가이드 - Universal MCP Gateway (UMG)

## 시스템 개요

UMG(Universal MCP Gateway)는 파편화된 엔터프라이즈 도구(n8n, Cube.js, AWS MCP 서버)를 단일 MCP(Model Context Protocol) 엔드포인트로 통합하여, AI 에이전트가 통합된 거버넌스 인터페이스를 통해 다양한 백엔드 시스템과 상호작용할 수 있게 해주는 중앙화 미들웨어입니다.

```
                                    Universal MCP Gateway

  AI 에이전트                  +----------------------------------+            백엔드 시스템
  (Cursor, Claude,             |                                  |
   커스텀 봇)                  |  +------- 보안 레이어 ----------+ |
                               |  | JWT 인증 | API 키 인증       | |
  +-----------+    MCP/SSE     |  | RBAC     | 속도 제한         | |
  | Cursor IDE|<-------------->|  +---------------------------+-+ |
  +-----------+    stdio       |                              |   |     +-------------+
                   브릿지      |  +------- MCP 프로토콜 ------+   |     | n8n         |
  +-----------+                |  | JSON-RPC 핸들러              |---->| 웹훅        |
  | Claude    |<--HTTP/SSE---->|  | tools/list | tools/call       |     +-------------+
  | Desktop   |                |  +---------------------------+-+ |
  +-----------+                |                              |   |     +-------------+
                               |  +------- 어댑터 레이어 -----+   |     | Cube.js     |
  +-----------+                |  | N8nAdapter                    |---->| 시맨틱      |
  | 커스텀    |<--HTTP/SSE---->|  | CubeJsAdapter                |     | 레이어      |
  | AI 에이전트|                |  | AwsRemoteMcpProxyAdapter      |     +-------------+
  +-----------+                |  +---------------------------+-+ |
                               |                              |   |     +-------------+
                               |  +------- 데이터 레이어 -----+   |     | AWS MCP     |
                               |  | PostgreSQL | Redis            |---->| 서버        |
                               |  | Flyway     | Bucket4j         |     | (SigV4)     |
                               |  +----------------------------+  |     +-------------+
                               |                                  |
                               +----------------------------------+
```

---

## 모노레포 구조

```
public_mcp_portal/
+-- backend/                    # Spring Boot 3.3+ (Java 21)
|   +-- src/main/java/com/umg/
|   |   +-- config/             # 설정 빈
|   |   +-- controller/         # REST API 컨트롤러
|   |   +-- service/            # 비즈니스 로직
|   |   +-- adapter/            # 도구 실행 어댑터
|   |   +-- mcp/                # MCP 프로토콜 처리
|   |   +-- security/           # 인증 및 인가
|   |   +-- domain/             # JPA 엔티티 및 열거형
|   |   +-- repository/         # 데이터 접근 레이어
|   |   +-- dto/                # 요청/응답 DTO
|   |   +-- exception/          # 커스텀 예외 및 핸들러
|   |   +-- util/               # 유틸리티 클래스
|   +-- src/main/resources/
|   |   +-- db/migration/       # Flyway SQL 마이그레이션
|   |   +-- application.yml     # 설정
|   +-- build.gradle
|   +-- Dockerfile
|
+-- frontend/                   # React 18+ (Vite, TypeScript)
|   +-- src/
|   |   +-- components/         # UI 컴포넌트
|   |   +-- pages/              # 라우트 페이지
|   |   +-- hooks/              # 커스텀 React 훅
|   |   +-- stores/             # Zustand 상태 관리
|   |   +-- api/                # API 클라이언트 및 React Query 훅
|   |   +-- types/              # TypeScript 타입 정의
|   |   +-- lib/                # 유틸리티 함수
|   |   +-- styles/             # 전역 CSS 및 Tailwind 설정
|   +-- package.json
|   +-- Dockerfile
|
+-- bridge/                     # Node.js CLI (stdio-to-SSE 프록시)
|   +-- src/index.ts
|   +-- package.json
|
+-- docs/                       # 문서
+-- scripts/                    # 빌드 및 배포 스크립트
+-- docker-compose.yml          # 전체 스택 구성
+-- docker-compose.dev.yml      # 개발용 인프라
```

---

## 백엔드 아키텍처

### 레이어드 아키텍처

```
요청 흐름:

HTTP 요청
    |
    v
[보안 필터 체인]
    | JWT 인증 (웹 UI)
    | API 키 인증 (에이전트)
    | 속도 제한 필터
    v
[컨트롤러 레이어]
    | 요청 유효성 검증
    | DTO 매핑
    v
[서비스 레이어]
    | 비즈니스 로직
    | 권한 검사
    | 감사 로깅 (비동기)
    v
[어댑터 레이어]              [리포지토리 레이어]
    | 도구 실행                   | JPA/Hibernate
    | 응답 가공                   | PostgreSQL
    v                             v
[외부 시스템]               [데이터베이스]
    | n8n, Cube.js, AWS          | 사용자, 도구
    |                             | 권한, 감사
```

### 보안 아키텍처

**이중 인증 체인:**

1. **웹 UI 경로** (`/api/auth/**`, `/api/tools/**` 등)
   - JWT 토큰 기반 세션
   - 로그인 시 JWT 반환, 브라우저 localStorage에 저장
   - 각 요청에 `Authorization: Bearer {jwt}` 포함

2. **에이전트 경로** (`/api/mcp/**`)
   - API 키 기반 인증
   - `X-API-Key` 헤더로 키 전송
   - 키는 SHA-256 해시하여 DB와 비교

**인가:**
- 역할 기반: ADMIN은 모든 도구 관리, 승인/거부 가능
- 권한 기반: 사용자는 도구별 명시적 EXECUTE 권한 필요
- 컨텍스트 인식: Cube.js RLS를 위해 사용자 부서 정보 주입

**데이터 보호:**
- 민감한 연결 설정은 저장 시 AES-256 암호화
- API 키는 SHA-256 해시(단방향)로 저장
- JWT 시크릿 및 AES 키는 환경 변수로 주입

### 도구 어댑터 패턴

```java
interface ToolExecutor {
    CompletableFuture<Object> execute(Tool tool, Map<String, Object> params);
    boolean supports(ToolType type);
}

// 팩토리가 올바른 어댑터를 선택
class ToolExecutorFactory {
    ToolExecutor getExecutor(ToolType type);
}
```

**어댑터 타입:**

| 어댑터 | 프로토콜 | 인증 | 용도 |
|--------|---------|------|------|
| N8nAdapter | HTTP POST | Bearer 토큰 | n8n 웹훅 워크플로우 |
| CubeJsAdapter | HTTP GET/POST | 토큰 + RLS 헤더 | Cube.js 시맨틱 쿼리 |
| AwsRemoteMcpProxyAdapter | HTTP + SigV4 | AWS IAM 역할 | AWS MCP 서버 |

### MCP 프로토콜 구현

UMG는 Model Context Protocol (MCP) 사양을 구현합니다:

**지원 메서드:**
- `initialize` - 핸드셰이크 및 기능 협상
- `tools/list` - 인증된 사용자가 접근 가능한 도구 목록 반환
- `tools/call` - 제공된 인자로 도구 실행

**전송:**
- HTTP POST - 요청-응답 (동기)
- SSE (Server-Sent Events) - 스트리밍 응답

**응답 가공:**
외부 시스템의 도구 응답이 클 수 있습니다. UMG는 도구별로 설정된 JSONPath 또는 JQ 규칙을 적용하여 관련 데이터만 추출한 후 AI 에이전트에 반환합니다. 이를 통해 LLM 토큰 한도 초과를 방지합니다.

### 속도 제한

```
요청 --> [속도제한필터] --> Bucket4j/Redis 확인
                                    |
                              +-----+-----+
                              |           |
                           허용         거부
                              |           |
                           계속       429 응답
```

- Bucket4j를 통한 토큰 버킷 알고리즘
- 분산 배포를 위해 Redis에 상태 저장
- API 키별 설정 가능 (`rate_limit_per_min` 필드)
- 점진적 리필과 함께 버스트 용량 제공

### 멱등성

상태 변경 작업(쓰기 도구)의 경우:
1. 클라이언트가 `Idempotency-Key` 헤더 전송
2. UMG가 Redis에서 기존 키 확인
3. 발견 시: 캐시된 응답 반환 (재실행 안 함)
4. 미발견 시: 도구 실행, 24시간 TTL로 응답 캐시

---

## 프론트엔드 아키텍처

### 컴포넌트 아키텍처

```
App
+-- AppLayout
|   +-- Sidebar (네비게이션)
|   +-- Header (페이지 제목, 사용자 정보)
|   +-- 메인 콘텐츠 영역
|       +-- 페이지
|           +-- DashboardPage (대시보드)
|           +-- ToolsPage / ToolDetailPage / ToolCreatePage (도구)
|           +-- ApiKeysPage (API 키)
|           +-- PermissionsPage (권한)
|           +-- AuditLogsPage (감사 로그)
|           +-- PlaygroundPage (플레이그라운드)
|           +-- SettingsPage (설정)
```

### 상태 관리

| 레이어 | 기술 | 용도 |
|--------|------|------|
| 서버 상태 | React Query (TanStack) | API 데이터, 캐싱, 리페칭 |
| 클라이언트 상태 | Zustand | 인증 상태, UI 설정 |
| 폼 상태 | React Hook Form + Zod | 폼 유효성 검증, 제출 |
| URL 상태 | React Router | 페이지 라우팅, 쿼리 파라미터 |

### 디자인 시스템

**색상 팔레트:**
- 배경: 화이트 (#ffffff), 오프화이트 (#fafafa, #f5f5f5)
- 텍스트: 진한 회색 (#1a1a1a), 중간 회색 (#666666), 연한 회색 (#999999)
- 강조: 진한 회색 (#1a1a1a) - 주요 액션용

**테두리 시스템:**
- 부분 테두리: 왼쪽 + 하단만 (시그니처 디자인 패턴)
- 강한 왼쪽 테두리 (#1a1a1a) - 활성/강조 상태
- 미세한 하단 테두리 (#e5e5e5) - 구분선

**타이포그래피:**
- 폰트: Inter (산세리프), JetBrains Mono (모노스페이스)
- 스케일: text-2xs, text-xs, text-sm, text-base, text-lg, text-xl, text-2xl
- 아이콘이나 이모지 없음 - 텍스트 라벨만 사용

---

## 데이터베이스 아키텍처

### 엔티티 관계

```
USERS
  |-- 1:N --> API_KEYS (인증 토큰)
  |-- 1:N --> PERMISSIONS (도구 접근 권한)
  |-- 1:N --> TOOLS (소유자로서)
  |-- 1:N --> AUDIT_LOGS (행위자로서)

TOOLS
  |-- 1:N --> PERMISSIONS (접근 가능한 사용자)
  |-- 1:N --> AUDIT_LOGS (실행 이력)
```

### 주요 설계 결정

**UUID 기본 키:**
- 모든 테이블에 UUID v4를 기본 키로 사용
- ID 열거 공격 방지
- 분산 ID 생성 가능

**JSONB 컬럼:**
- `connection_config`: 다양한 어댑터 타입에 유연한 스키마
- `input_schema`: 도구 파라미터의 JSON Schema 정의
- `input_params`: 실행 파라미터의 완전한 감사 추적

**저장 시 암호화:**
- `connection_config`는 JPA AttributeConverter를 통해 AES-256 암호화
- API 키는 SHA-256 해시(비가역)로 저장

**인덱싱 전략:**
- 자주 필터링하는 컬럼에 B-tree 인덱스
- 일반적인 쿼리 패턴에 복합 인덱스
- 활성 레코드에 부분 인덱스

---

## 확장성 고려사항

### 수평 확장

```
                    로드 밸런서 (ALB/Nginx)
                         |
              +----------+----------+
              |          |          |
          백엔드-1   백엔드-2   백엔드-3
              |          |          |
              +----------+----------+
                         |
              +----------+----------+
              |                     |
          PostgreSQL            Redis
          (프라이머리 +        (클러스터)
           읽기 레플리카)
```

- **백엔드**: 무상태(JWT, 서버 세션 없음) - 수평 확장 가능
- **PostgreSQL**: 읽기 집약적 쿼리를 위한 프라이머리 + 읽기 레플리카
- **Redis**: 속도 제한과 캐싱을 위한 클러스터 모드
- **가상 스레드**: 인스턴스당 수천 개의 동시 SSE 연결 처리

### 성능 목표

| 메트릭 | 목표 |
|--------|------|
| API 응답 시간 (p50) | < 100ms |
| API 응답 시간 (p99) | < 500ms |
| 도구 실행 (p50) | < 2초 |
| 도구 실행 (p99) | < 15초 |
| 동시 SSE 연결 | 인스턴스당 5,000개 |
| 초당 요청 수 | 인스턴스당 1,000개 |

### 캐싱 전략

| 캐시 | TTL | 용도 |
|------|-----|------|
| 도구 정의 | 5분 | tools/list의 DB 읽기 감소 |
| 사용자 권한 | 2분 | 인증 확인의 DB 읽기 감소 |
| 속도 제한 버킷 | 슬라이딩 | 토큰 버킷 상태 |
| 멱등성 키 | 24시간 | 중복 요청 방지 |
| API 키 조회 | 5분 | 인증의 DB 읽기 감소 |

---

## 보안 모델

### 심층 방어

1. **네트워크 레이어**: HTTPS, CORS, CSP 헤더
2. **인증**: JWT + API 키 이중 인증
3. **인가**: RBAC + 도구 수준 권한
4. **데이터 보호**: 저장 시 AES-256 암호화, 전송 시 TLS
5. **속도 제한**: 키별 토큰 버킷
6. **감사 추적**: 모든 도구 실행을 전체 컨텍스트와 함께 로깅
7. **입력 유효성 검증**: Zod (프론트엔드) + Bean Validation (백엔드)
8. **응답 가공**: AI 에이전트로의 민감 데이터 유출 방지

### 위협 모델

| 위협 | 대응 |
|------|------|
| API 키 도용 | 키 해시 저장, 교체 가능, 만료일 설정 |
| 권한 상승 | 컨트롤러가 아닌 서비스 레이어에서 권한 검사 |
| AI를 통한 데이터 유출 | 응답 가공으로 데이터 노출 제한 |
| DDoS | API 키별 속도 제한, 요청 타임아웃 |
| SQL 인젝션 | JPA를 통한 파라미터화된 쿼리, 원시 SQL 사용 안 함 |
| XSS | React 자동 이스케이프, CSP 헤더 |
| 리플레이 공격 | 상태 변경 작업에 멱등성 키 적용 |

---

## 관측성

### 로깅

- 구조화된 JSON 형식
- MDC TraceId 주입으로 분산 추적
- 로그 레벨: ERROR (알림), WARN (이상), INFO (비즈니스 이벤트), DEBUG (개발)

### 메트릭

- Micrometer + Prometheus 엔드포인트
- 도구 실행, MCP 연결, 속도 제한에 대한 커스텀 메트릭
- JVM 메트릭 (GC, 메모리, 스레드)

### 헬스체크

- Spring Actuator 헬스 인디케이터
- 데이터베이스 연결 상태 확인
- Redis 연결 상태 확인
- 어댑터 상태에 대한 커스텀 인디케이터
