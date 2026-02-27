-- V1: 초기 스키마 생성
-- Universal MCP Gateway (UMG) 데이터베이스 스키마

-- UUID 생성 확장
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =============================================
-- 사용자 테이블
-- =============================================
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL,
    name            VARCHAR(100) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    department      VARCHAR(100),
    role            VARCHAR(20) NOT NULL DEFAULT 'USER',
    is_active       BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'USER'))
);

-- 이메일 검색 인덱스 (활성 사용자만)
CREATE INDEX idx_users_email_active ON users (email) WHERE is_active = true;
-- 부서별 검색 인덱스
CREATE INDEX idx_users_department ON users (department);

-- =============================================
-- API 키 테이블
-- =============================================
CREATE TABLE api_keys (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL,
    key_hash            VARCHAR(255) NOT NULL,
    name                VARCHAR(100) NOT NULL,
    rate_limit_per_min  INTEGER NOT NULL DEFAULT 60,
    expires_at          TIMESTAMP WITH TIME ZONE,
    is_active           BOOLEAN NOT NULL DEFAULT true,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_api_keys_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_api_keys_hash UNIQUE (key_hash)
);

-- 키 해시 조회 인덱스
CREATE INDEX idx_api_keys_hash ON api_keys (key_hash) WHERE is_active = true;
-- 사용자별 활성 키 인덱스
CREATE INDEX idx_api_keys_user_active ON api_keys (user_id) WHERE is_active = true;

-- =============================================
-- 도구 테이블
-- =============================================
CREATE TABLE tools (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                    VARCHAR(200) NOT NULL,
    description             TEXT NOT NULL,
    tool_type               VARCHAR(30) NOT NULL,
    connection_config       TEXT,
    auth_type               VARCHAR(30) NOT NULL DEFAULT 'NONE',
    input_schema            JSONB,
    response_mapping_rule   TEXT,
    status                  VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    is_public               BOOLEAN NOT NULL DEFAULT false,
    is_idempotent           BOOLEAN NOT NULL DEFAULT false,
    owner_id                UUID NOT NULL,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_tools_owner FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_tools_type CHECK (tool_type IN ('N8N', 'CUBE_JS', 'AWS_REMOTE')),
    CONSTRAINT chk_tools_auth_type CHECK (auth_type IN ('BEARER', 'AWS_SIGV4', 'NONE')),
    CONSTRAINT chk_tools_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED'))
);

-- 상태별 도구 조회 인덱스
CREATE INDEX idx_tools_status ON tools (status);
-- 공개 도구 인덱스
CREATE INDEX idx_tools_public ON tools (is_public) WHERE is_public = true AND status = 'APPROVED';
-- 소유자별 도구 인덱스
CREATE INDEX idx_tools_owner ON tools (owner_id);
-- 도구 타입별 인덱스
CREATE INDEX idx_tools_type ON tools (tool_type);
-- 도구 이름 검색 인덱스
CREATE INDEX idx_tools_name ON tools (name);

-- =============================================
-- 권한 테이블
-- =============================================
CREATE TABLE permissions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tool_id         UUID NOT NULL,
    user_id         UUID NOT NULL,
    access_level    VARCHAR(20) NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_permissions_tool FOREIGN KEY (tool_id) REFERENCES tools(id) ON DELETE CASCADE,
    CONSTRAINT fk_permissions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_permissions_tool_user UNIQUE (tool_id, user_id),
    CONSTRAINT chk_permissions_level CHECK (access_level IN ('EXECUTE', 'EDIT'))
);

-- 사용자별 권한 조회 인덱스
CREATE INDEX idx_permissions_user ON permissions (user_id);
-- 도구별 권한 조회 인덱스
CREATE INDEX idx_permissions_tool ON permissions (tool_id);

-- =============================================
-- 감사 로그 테이블
-- =============================================
CREATE TABLE audit_logs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    trace_id            VARCHAR(100),
    user_id             UUID,
    tool_id             UUID,
    tool_name           VARCHAR(200),
    input_params        JSONB,
    status              VARCHAR(20) NOT NULL,
    error_message       TEXT,
    execution_time_ms   BIGINT,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_audit_logs_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_audit_logs_tool FOREIGN KEY (tool_id) REFERENCES tools(id) ON DELETE SET NULL,
    CONSTRAINT chk_audit_status CHECK (status IN ('SUCCESS', 'FAIL'))
);

-- 시간 범위 조회 인덱스 (감사 로그의 핵심 쿼리)
CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at DESC);
-- 사용자별 감사 로그 인덱스
CREATE INDEX idx_audit_logs_user ON audit_logs (user_id);
-- 도구별 감사 로그 인덱스
CREATE INDEX idx_audit_logs_tool ON audit_logs (tool_id);
-- 추적 ID 인덱스
CREATE INDEX idx_audit_logs_trace_id ON audit_logs (trace_id);
-- 상태별 인덱스
CREATE INDEX idx_audit_logs_status ON audit_logs (status);
-- 복합 인덱스: 사용자 + 시간 범위 (가장 빈번한 쿼리 패턴)
CREATE INDEX idx_audit_logs_user_created ON audit_logs (user_id, created_at DESC);

-- =============================================
-- updated_at 자동 갱신 트리거
-- =============================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_tools_updated_at
    BEFORE UPDATE ON tools
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
