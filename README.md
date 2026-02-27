# Universal MCP Gateway (UMG)

파편화된 엔터프라이즈 도구들을 단일 MCP (Model Context Protocol) 엔드포인트로 통합하여, AI 에이전트가 통합된 거버넌스 인터페이스를 통해 다양한 백엔드 시스템과 상호작용할 수 있게 해주는 중앙화 미들웨어입니다.

---

## UMG란?

UMG는 AI 에이전트(Cursor, Claude Desktop, 커스텀 봇)와 엔터프라이즈 백엔드 시스템(n8n 워크플로우, Cube.js 분석, AWS MCP 서버) 사이의 **스마트 프록시** 역할을 합니다.

- **통합 도구 레지스트리** -- 모든 엔터프라이즈 도구를 한 곳에서 등록 및 관리
- **이중 인증** -- 웹 UI용 JWT, AI 에이전트용 API 키
- **역할 기반 접근 제어** -- 사용자별, 도구별 세분화된 권한 관리
- **Maker-Checker 승인** -- 도구 활성화 전 관리자 승인 필요
- **속도 제한** -- Redis 기반 API 키별 토큰 버킷 속도 제한
- **멱등성 보장** -- 상태 변경 도구의 중복 실행 방지
- **응답 가공** -- AI에 전송 전 대용량 API 응답 필터링 (토큰 초과 방지)
- **감사 추적** -- 모든 도구 실행을 전체 컨텍스트와 함께 로깅
- **AES-256 암호화** -- 민감한 연결 설정 데이터 저장 시 암호화

---

## 아키텍처

```
AI 에이전트            UMG 게이트웨이                  백엔드 시스템
                  +--------------------+
Cursor IDE  <---->| 보안 레이어         |
                  |  JWT + API 키      |
Claude      <---->|  RBAC + 속도 제한   |-----> n8n 웹훅
Desktop           |                    |
                  | MCP 프로토콜        |-----> Cube.js 분석
커스텀       <---->|  tools/list        |
봇                |  tools/call        |-----> AWS MCP 서버
                  |                    |       (SigV4 인증)
                  | 어댑터 레이어       |
                  |  응답 가공          |
                  +--------------------+
                  | PostgreSQL | Redis |
                  +--------------------+
```

## 기술 스택

| 레이어 | 기술 |
|--------|------|
| 백엔드 | Java 21 LTS (가상 스레드), Spring Boot 3.3+, Spring Security 6 |
| 프론트엔드 | React 18+, Vite 5, TypeScript 5, Tailwind CSS 3, React Query 5, Zustand |
| 데이터베이스 | PostgreSQL 16+ |
| 캐시 | Redis 7+ |
| 프로토콜 | MCP (Model Context Protocol) - JSON-RPC 2.0 |
| 브릿지 | Node.js CLI (stdio-to-HTTP/SSE 프록시) |
| 인프라 | Docker Compose, Kubernetes (EKS/GKE) |

---

## 빠른 시작

### 사전 요구사항

- Docker 24.0+ 및 Docker Compose 2.20+
- Java 21+ (로컬 개발용)
- Node.js 20+ (로컬 개발용)

### 1. 복제 및 설정

```bash
git clone https://github.com/your-org/public_mcp_portal.git
cd public_mcp_portal
cp .env.example .env
# .env 파일에 비밀키 설정
```

### 2. Docker Compose로 시작

```bash
docker compose up -d
```

### 3. 접속

| 서비스 | URL |
|--------|-----|
| 프론트엔드 | http://localhost:3000 |
| 백엔드 API | http://localhost:8080 |
| API 헬스체크 | http://localhost:8080/actuator/health |

기본 관리자: `admin@umg.local` / `admin123!`

### 개발 모드

```bash
# PostgreSQL과 Redis만 시작
docker compose -f docker-compose.dev.yml up -d

# 백엔드 시작 (터미널 1)
cd backend && ./gradlew bootRun --args='--spring.profiles.active=local'

# 프론트엔드 시작 (터미널 2)
cd frontend && npm install && npm run dev
```

---

## 브릿지 CLI (AI 도구 연동)

AI 도구를 UMG에 연결합니다:

```bash
# 설치
cd bridge && npm install && npm run build

# 실행
node dist/index.js --server-url http://localhost:8080 --api-key YOUR_KEY
```

### Cursor IDE 설정

`.cursor/mcp.json`에 추가:

```json
{
  "mcpServers": {
    "umg": {
      "command": "node",
      "args": ["/path/to/bridge/dist/index.js", "--server-url", "https://umg.company.com", "--api-key", "umg_key_xxx"]
    }
  }
}
```

---

## 프로젝트 구조

```
public_mcp_portal/
+-- backend/              Spring Boot 3.3+ API 서버
+-- frontend/             React 18+ 대시보드 UI
+-- bridge/               Node.js stdio-to-HTTP 브릿지 CLI
+-- docs/                 문서
|   +-- PRD.md            제품 요구사항 문서
|   +-- ARCHITECTURE.md   시스템 아키텍처 가이드
|   +-- INSTALLATION.md   설치 가이드
|   +-- RUNNING.md        실행 및 API 참조
|   +-- RELEASE.md        릴리즈 프로세스 가이드
+-- scripts/              빌드 및 배포 스크립트
+-- docker-compose.yml    프로덕션 구성
+-- docker-compose.dev.yml 개발용 인프라
```

---

## 문서

| 문서 | 설명 |
|------|------|
| [PRD](docs/PRD.md) | 제품 요구사항 및 시나리오 |
| [아키텍처](docs/ARCHITECTURE.md) | 시스템 설계 및 의사결정 |
| [설치](docs/INSTALLATION.md) | 설정 및 구성 |
| [실행](docs/RUNNING.md) | API 참조 및 운영 |
| [릴리즈](docs/RELEASE.md) | 빌드 및 배포 프로세스 |

---

## 지원 도구 타입

| 타입 | 어댑터 | 설명 |
|------|--------|------|
| N8N | N8nAdapter | n8n 웹훅 워크플로우로 HTTP POST |
| CUBE_JS | CubeJsAdapter | RLS가 적용된 Cube.js 시맨틱 레이어 쿼리 |
| AWS_REMOTE | AwsRemoteMcpProxyAdapter | SigV4 서명을 통한 AWS MCP 서버 연동 |

---

## 보안

- **인증**: 이중 모드 (웹용 JWT 세션, 에이전트용 API 키)
- **암호화**: 저장 시 민감 데이터 AES-256 암호화
- **속도 제한**: API 키별 토큰 버킷 (Bucket4j + Redis)
- **RBAC**: 역할 기반 (ADMIN/USER) + 도구 수준 권한 (EXECUTE/EDIT)
- **감사**: 추적 ID가 포함된 완전한 실행 감사 추적
- **멱등성**: Redis 기반 중복 요청 방지
- **응답 가공**: JSONPath 기반 응답 필터링으로 데이터 유출 방지

---

## 라이선스

이 프로젝트는 독점 소프트웨어입니다. 모든 권리가 보호됩니다.
