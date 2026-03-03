package com.umg.domain.enums;

/**
 * 관리자 행위 유형 (감사 로그용).
 */
public enum AdminAction {
    TOOL_APPROVE,
    TOOL_REJECT,
    PERMISSION_GRANT,
    PERMISSION_REVOKE,
    USER_DELETE,
    USER_DATA_EXPORT,
    DATASOURCE_CREATE,
    DATASOURCE_DELETE,
    SERVER_CREATE,
    SERVER_DELETE,
    API_KEY_REVOKE
}
