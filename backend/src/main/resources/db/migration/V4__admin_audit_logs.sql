-- V4: 관리자 행위 감사 로그 테이블
-- 도구 승인/거부, 권한 변경, 사용자 삭제 등 관리자 행위 추적

CREATE TABLE admin_audit_logs (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id        UUID NOT NULL,
    action          VARCHAR(30) NOT NULL,
    target_type     VARCHAR(50),
    target_id       UUID,
    target_name     VARCHAR(255),
    details         JSONB,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),

    CONSTRAINT fk_admin_audit_actor FOREIGN KEY (actor_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT chk_admin_audit_action CHECK (action IN (
        'TOOL_APPROVE', 'TOOL_REJECT',
        'PERMISSION_GRANT', 'PERMISSION_REVOKE',
        'USER_DELETE', 'USER_DATA_EXPORT',
        'DATASOURCE_CREATE', 'DATASOURCE_DELETE',
        'SERVER_CREATE', 'SERVER_DELETE',
        'API_KEY_REVOKE'
    ))
);

-- 행위자별 조회 인덱스
CREATE INDEX idx_admin_audit_actor ON admin_audit_logs (actor_id);
-- 행위 유형별 인덱스
CREATE INDEX idx_admin_audit_action ON admin_audit_logs (action);
-- 시간 범위 조회 인덱스
CREATE INDEX idx_admin_audit_created_at ON admin_audit_logs (created_at DESC);
-- 대상별 조회 인덱스
CREATE INDEX idx_admin_audit_target ON admin_audit_logs (target_id);
-- 복합 인덱스: 행위자 + 시간
CREATE INDEX idx_admin_audit_actor_created ON admin_audit_logs (actor_id, created_at DESC);
