-- V3: Cube.js 내부 통합 및 AWS MCP 서버 관리 테이블
-- Phase 7: Cube.js 데이터소스/스키마 관리, AWS MCP 서버 레지스트리

-- =============================================
-- Cube.js 데이터소스 테이블
-- =============================================
CREATE TABLE cube_data_sources (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                VARCHAR(200) NOT NULL,
    description         TEXT,
    db_type             VARCHAR(30) NOT NULL,
    connection_config   TEXT NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_tested_at      TIMESTAMP WITH TIME ZONE,
    created_by          UUID NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT uk_cube_ds_name UNIQUE (name),
    CONSTRAINT fk_cube_ds_creator FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_cube_ds_db_type CHECK (db_type IN ('POSTGRESQL', 'MYSQL', 'BIGQUERY', 'REDSHIFT', 'SNOWFLAKE', 'CLICKHOUSE')),
    CONSTRAINT chk_cube_ds_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'ERROR'))
);

CREATE INDEX idx_cube_ds_status ON cube_data_sources (status);
CREATE INDEX idx_cube_ds_db_type ON cube_data_sources (db_type);
CREATE INDEX idx_cube_ds_created_by ON cube_data_sources (created_by);

-- =============================================
-- Cube.js 스키마(모델) 테이블
-- =============================================
CREATE TABLE cube_schemas (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    datasource_id       UUID NOT NULL,
    name                VARCHAR(200) NOT NULL,
    description         TEXT,
    schema_definition   TEXT NOT NULL,
    version             INTEGER NOT NULL DEFAULT 1,
    status              VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_by          UUID NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT uk_cube_schema_name UNIQUE (name),
    CONSTRAINT fk_cube_schema_ds FOREIGN KEY (datasource_id) REFERENCES cube_data_sources(id) ON DELETE CASCADE,
    CONSTRAINT fk_cube_schema_creator FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_cube_schema_status CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED'))
);

CREATE INDEX idx_cube_schema_ds ON cube_schemas (datasource_id);
CREATE INDEX idx_cube_schema_status ON cube_schemas (status);
CREATE INDEX idx_cube_schema_created_by ON cube_schemas (created_by);

-- =============================================
-- AWS MCP 서버 레지스트리 테이블
-- =============================================
CREATE TABLE aws_mcp_servers (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                    VARCHAR(200) NOT NULL,
    description             TEXT,
    endpoint_url            VARCHAR(2000) NOT NULL,
    region                  VARCHAR(50) NOT NULL DEFAULT 'us-east-1',
    service                 VARCHAR(100) NOT NULL DEFAULT 'execute-api',
    auth_type               VARCHAR(20) NOT NULL DEFAULT 'IAM_KEY',
    credentials_config      TEXT NOT NULL,
    status                  VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    synced_tool_count       INTEGER NOT NULL DEFAULT 0,
    last_synced_at          TIMESTAMP WITH TIME ZONE,
    last_health_check_at    TIMESTAMP WITH TIME ZONE,
    created_by              UUID NOT NULL,
    created_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT uk_aws_mcp_name UNIQUE (name),
    CONSTRAINT fk_aws_mcp_creator FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT chk_aws_mcp_auth CHECK (auth_type IN ('IAM_KEY', 'IAM_ROLE')),
    CONSTRAINT chk_aws_mcp_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'ERROR'))
);

CREATE INDEX idx_aws_mcp_status ON aws_mcp_servers (status);
CREATE INDEX idx_aws_mcp_created_by ON aws_mcp_servers (created_by);

-- =============================================
-- AWS MCP 동기화 이력 테이블
-- =============================================
CREATE TABLE aws_mcp_sync_history (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    server_id           UUID NOT NULL,
    status              VARCHAR(20) NOT NULL,
    tools_discovered    INTEGER NOT NULL DEFAULT 0,
    tools_created       INTEGER NOT NULL DEFAULT 0,
    tools_updated       INTEGER NOT NULL DEFAULT 0,
    error_message       TEXT,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_sync_history_server FOREIGN KEY (server_id) REFERENCES aws_mcp_servers(id) ON DELETE CASCADE,
    CONSTRAINT chk_sync_status CHECK (status IN ('SUCCESS', 'FAIL'))
);

CREATE INDEX idx_sync_history_server ON aws_mcp_sync_history (server_id);
CREATE INDEX idx_sync_history_created ON aws_mcp_sync_history (created_at DESC);

-- =============================================
-- tools 테이블에 외부 연결 컬럼 추가
-- =============================================
ALTER TABLE tools ADD COLUMN cube_datasource_id UUID;
ALTER TABLE tools ADD COLUMN aws_server_id UUID;

ALTER TABLE tools ADD CONSTRAINT fk_tools_cube_ds
    FOREIGN KEY (cube_datasource_id) REFERENCES cube_data_sources(id) ON DELETE SET NULL;
ALTER TABLE tools ADD CONSTRAINT fk_tools_aws_server
    FOREIGN KEY (aws_server_id) REFERENCES aws_mcp_servers(id) ON DELETE SET NULL;

CREATE INDEX idx_tools_cube_ds ON tools (cube_datasource_id) WHERE cube_datasource_id IS NOT NULL;
CREATE INDEX idx_tools_aws_server ON tools (aws_server_id) WHERE aws_server_id IS NOT NULL;

-- =============================================
-- updated_at 자동 갱신 트리거
-- =============================================
CREATE TRIGGER trg_cube_ds_updated_at
    BEFORE UPDATE ON cube_data_sources
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_cube_schema_updated_at
    BEFORE UPDATE ON cube_schemas
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER trg_aws_mcp_updated_at
    BEFORE UPDATE ON aws_mcp_servers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
