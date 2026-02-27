# 설치 가이드 - Universal MCP Gateway (UMG)

## 사전 요구사항

### 필수 소프트웨어

| 소프트웨어 | 버전 | 용도 |
|-----------|------|------|
| Java JDK | 21 LTS+ | 백엔드 런타임 (가상 스레드) |
| Node.js | 20 LTS+ | 프론트엔드 빌드 및 브릿지 CLI |
| Docker | 24.0+ | 컨테이너 오케스트레이션 |
| Docker Compose | 2.20+ | 멀티 컨테이너 관리 |
| Git | 2.40+ | 버전 관리 |

### 선택 소프트웨어

| 소프트웨어 | 버전 | 용도 |
|-----------|------|------|
| PostgreSQL | 16+ | 직접 DB 접근 (Docker 미사용 시) |
| Redis | 7+ | 직접 캐시 접근 (Docker 미사용 시) |
| kubectl | 1.28+ | 쿠버네티스 배포 |
| Helm | 3.13+ | 쿠버네티스 패키지 관리 |

---

## 빠른 시작 (Docker Compose)

### 1. 저장소 복제

```bash
git clone https://github.com/your-org/public_mcp_portal.git
cd public_mcp_portal
```

### 2. 환경 설정

```bash
cp .env.example .env
```

`.env` 파일을 편집하여 다음 값을 설정합니다:

```bash
# 중요: 프로덕션에서는 반드시 변경하세요
UMG_AES_SECRET_KEY=32자리-AES-비밀키
UMG_JWT_SECRET=32자리-JWT-비밀키
POSTGRES_PASSWORD=안전한-DB-비밀번호
```

### 3. 전체 서비스 시작

```bash
docker compose up -d
```

시작되는 서비스:
- **PostgreSQL 16** - 포트 `5432`
- **Redis 7** - 포트 `6379`
- **UMG 백엔드** - 포트 `8080`
- **UMG 프론트엔드** - 포트 `3000`

### 4. 설치 확인

```bash
# 모든 컨테이너 실행 상태 확인
docker compose ps

# 백엔드 헬스체크
curl http://localhost:8080/actuator/health

# 프론트엔드 접속
open http://localhost:3000
```

### 5. 기본 계정

| 역할 | 이메일 | 비밀번호 |
|------|--------|---------|
| 관리자 | admin@umg.local | admin123! |

**중요**: 첫 로그인 후 반드시 기본 관리자 비밀번호를 변경하세요.

---

## 개발 환경 설정 (로컬)

활발한 개발을 위해 인프라는 Docker로, 애플리케이션은 로컬에서 실행합니다.

### 1. 인프라만 시작

```bash
docker compose -f docker-compose.dev.yml up -d
```

PostgreSQL과 Redis만 시작됩니다.

### 2. 백엔드 설정

```bash
cd backend

# 프로젝트 빌드
./gradlew build

# 로컬 프로파일로 실행
./gradlew bootRun --args='--spring.profiles.active=local'
```

백엔드가 `http://localhost:8080`에서 시작됩니다.

### 3. 프론트엔드 설정

```bash
cd frontend

# 의존성 설치
npm install

# 개발 서버 시작
npm run dev
```

프론트엔드가 `http://localhost:5173`에서 핫 리로드와 함께 시작됩니다.

### 4. 브릿지 CLI 설정 (선택)

```bash
cd bridge

# 의존성 설치
npm install

# 빌드
npm run build

# 연결 테스트
node dist/index.js --server-url http://localhost:8080 --api-key YOUR_API_KEY
```

---

## 데이터베이스 설정

### 자동 (권장)

Flyway 마이그레이션이 백엔드 시작 시 자동으로 실행됩니다. 수동 작업이 필요 없습니다.

### 수동 설정

PostgreSQL을 수동으로 설정해야 하는 경우:

```bash
# PostgreSQL 접속
psql -h localhost -U umg -d umg

# 확장 기능 (초기화 스크립트에 의해 자동 생성)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
```

### 데이터베이스 마이그레이션

마이그레이션 파일 위치: `backend/src/main/resources/db/migration/`

| 마이그레이션 | 설명 |
|-------------|------|
| V1__init_schema.sql | 핵심 테이블 (users, tools, api_keys, permissions, audit_logs) |
| V2__seed_data.sql | 기본 관리자 및 샘플 데이터 |

---

## 설정 참조

### 백엔드 설정 (application.yml)

| 속성 | 기본값 | 설명 |
|------|-------|------|
| `server.port` | 8080 | HTTP 서버 포트 |
| `spring.threads.virtual.enabled` | true | Java 21 가상 스레드 활성화 |
| `spring.datasource.url` | jdbc:postgresql://localhost:5432/umg | 데이터베이스 URL |
| `spring.data.redis.host` | localhost | Redis 호스트 |
| `umg.security.aes-secret-key` | - | AES-256 암호화 키 (32자) |
| `umg.security.jwt-secret` | - | JWT 서명 비밀키 (32자 이상) |
| `umg.security.jwt-expiration` | 86400000 | JWT 토큰 유효기간 (밀리초) |
| `umg.rate-limit.default-per-minute` | 60 | 기본 API 속도 제한 |
| `umg.rate-limit.burst-capacity` | 10 | 속도 제한 버스트 용량 |

### 환경 변수

모든 Spring Boot 속성은 환경 변수로 재정의할 수 있습니다:

```bash
# 데이터베이스
SPRING_DATASOURCE_URL=jdbc:postgresql://host:5432/umg
SPRING_DATASOURCE_USERNAME=umg
SPRING_DATASOURCE_PASSWORD=secret

# Redis
SPRING_DATA_REDIS_HOST=redis-host
SPRING_DATA_REDIS_PORT=6379

# 보안
UMG_AES_SECRET_KEY=32자리-키
UMG_JWT_SECRET=32자리-비밀키
```

---

## 문제 해결

### 자주 발생하는 문제

**포트 충돌**
```bash
# 포트 사용 여부 확인
lsof -i :5432  # PostgreSQL
lsof -i :6379  # Redis
lsof -i :8080  # 백엔드
lsof -i :3000  # 프론트엔드
```

**데이터베이스 연결 거부**
```bash
# PostgreSQL 실행 상태 확인
docker compose ps postgres
docker compose logs postgres
```

**Redis 연결 거부**
```bash
# Redis 실행 상태 확인
docker compose ps redis
docker compose logs redis
```

**백엔드 시작 실패**
```bash
# 로그 확인
docker compose logs backend

# 일반적 해결: DB 준비 대기 후 재시작
docker compose restart backend
```

**프론트엔드 빌드 실패**
```bash
# node_modules 삭제 후 재설치
cd frontend
rm -rf node_modules
npm install
```

### 헬스체크

```bash
# 백엔드 상태
curl http://localhost:8080/actuator/health

# 백엔드 정보
curl http://localhost:8080/actuator/info

# Redis 연결 상태 (백엔드 경유)
curl http://localhost:8080/actuator/health/redis
```

---

## 다음 단계

- [실행 가이드](./RUNNING.md) - UMG 사용 및 운영 방법
- [릴리즈 가이드](./RELEASE.md) - 릴리즈 빌드 및 배포 방법
- [아키텍처](./ARCHITECTURE.md) - 시스템 아키텍처 개요
