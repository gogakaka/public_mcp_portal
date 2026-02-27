-- V2: 초기 시드 데이터
-- 기본 관리자 계정 및 샘플 도구 데이터

-- =============================================
-- 기본 관리자 사용자
-- 비밀번호: admin123! (BCrypt 해시)
-- =============================================
INSERT INTO users (id, email, name, password_hash, department, role)
VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'admin@umg.local',
    'UMG 관리자',
    '$2a$12$LJ3m4ys3uz8J8oNMQF.bCeNjF0qRVHpPOSzJuMJsBKHVLlVjqy6f2',
    'IT',
    'ADMIN'
);

-- 샘플 일반 사용자
INSERT INTO users (id, email, name, password_hash, department, role)
VALUES (
    'a0000000-0000-0000-0000-000000000002',
    'user@umg.local',
    '샘플 사용자',
    '$2a$12$LJ3m4ys3uz8J8oNMQF.bCeNjF0qRVHpPOSzJuMJsBKHVLlVjqy6f2',
    '개발팀',
    'USER'
);

-- =============================================
-- 샘플 도구: N8N 타입
-- =============================================
INSERT INTO tools (id, name, description, tool_type, connection_config, auth_type, input_schema, status, is_public, is_idempotent, owner_id)
VALUES (
    'b0000000-0000-0000-0000-000000000001',
    'production-metrics',
    'MES 시스템에서 생산 라인 지표를 조회합니다. 온도, 생산량, 불량률 등의 실시간 데이터를 반환합니다.',
    'N8N',
    NULL,
    'BEARER',
    '{
        "type": "object",
        "properties": {
            "lineId": {
                "type": "string",
                "description": "생산 라인 ID (예: LINE-001)"
            },
            "metric": {
                "type": "string",
                "enum": ["temperature", "output", "defectRate"],
                "description": "조회할 지표 종류"
            }
        },
        "required": ["lineId", "metric"]
    }'::jsonb,
    'APPROVED',
    true,
    true,
    'a0000000-0000-0000-0000-000000000001'
);

-- =============================================
-- 샘플 도구: CUBE_JS 타입
-- =============================================
INSERT INTO tools (id, name, description, tool_type, connection_config, auth_type, input_schema, status, is_public, is_idempotent, owner_id)
VALUES (
    'b0000000-0000-0000-0000-000000000002',
    'finance-report',
    '재무 데이터를 조회합니다. 부서별 예산 대비 지출, 월별 매출 추이 등을 분석합니다. 부서 기반 RLS가 적용됩니다.',
    'CUBE_JS',
    NULL,
    'BEARER',
    '{
        "type": "object",
        "properties": {
            "measures": {
                "type": "array",
                "items": {"type": "string"},
                "description": "조회할 측정값 목록 (예: [\"Orders.totalAmount\"])"
            },
            "dimensions": {
                "type": "array",
                "items": {"type": "string"},
                "description": "그룹핑 차원 목록 (예: [\"Orders.department\"])"
            },
            "dateRange": {
                "type": "string",
                "description": "조회 기간 (예: \"last 3 months\")"
            }
        },
        "required": ["measures"]
    }'::jsonb,
    'APPROVED',
    true,
    true,
    'a0000000-0000-0000-0000-000000000001'
);

-- =============================================
-- 샘플 도구: AWS_REMOTE 타입
-- =============================================
INSERT INTO tools (id, name, description, tool_type, connection_config, auth_type, input_schema, status, is_public, is_idempotent, owner_id)
VALUES (
    'b0000000-0000-0000-0000-000000000003',
    'cloudwatch-logs',
    'AWS CloudWatch에서 로그를 조회하고 분석합니다. 서버 장애 원인 파악, 로그 패턴 분석 등에 활용됩니다.',
    'AWS_REMOTE',
    NULL,
    'AWS_SIGV4',
    '{
        "type": "object",
        "properties": {
            "logGroup": {
                "type": "string",
                "description": "CloudWatch 로그 그룹 이름"
            },
            "query": {
                "type": "string",
                "description": "CloudWatch Insights 쿼리"
            },
            "timeRange": {
                "type": "string",
                "description": "조회 시간 범위 (예: \"1h\", \"24h\")"
            }
        },
        "required": ["logGroup", "query"]
    }'::jsonb,
    'APPROVED',
    false,
    true,
    'a0000000-0000-0000-0000-000000000001'
);

-- =============================================
-- 샘플 권한: 사용자에게 도구 실행 권한 부여
-- =============================================
INSERT INTO permissions (tool_id, user_id, access_level)
VALUES
    ('b0000000-0000-0000-0000-000000000001', 'a0000000-0000-0000-0000-000000000002', 'EXECUTE'),
    ('b0000000-0000-0000-0000-000000000002', 'a0000000-0000-0000-0000-000000000002', 'EXECUTE');
