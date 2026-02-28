package com.umg.domain.enums;

/**
 * Cube.js 데이터소스가 연결할 수 있는 데이터베이스 유형.
 */
public enum CubeDbType {
    POSTGRESQL,
    MYSQL,
    BIGQUERY,
    REDSHIFT,
    SNOWFLAKE,
    CLICKHOUSE
}
