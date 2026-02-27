package com.umg.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umg.domain.entity.CubeDataSource;
import com.umg.domain.entity.User;
import com.umg.domain.enums.CubeDbType;
import com.umg.domain.enums.DataSourceStatus;
import com.umg.dto.CubeDataSourceDto;
import com.umg.repository.CubeDataSourceRepository;
import com.umg.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.*;

/**
 * Cube.js 데이터소스를 관리하는 서비스.
 *
 * <p>데이터소스 CRUD, 연결 테스트, 테이블 인트로스펙션 기능을 제공합니다.
 * DB 연결 정보는 AES-256 암호화되어 엔티티에 저장됩니다.</p>
 */
@Service
public class CubeDataSourceService {

    private static final Logger log = LoggerFactory.getLogger(CubeDataSourceService.class);

    private final CubeDataSourceRepository cubeDataSourceRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * 생성자 주입.
     *
     * @param cubeDataSourceRepository 데이터소스 저장소
     * @param userRepository           사용자 저장소
     * @param objectMapper             JSON 직렬화/역직렬화를 위한 ObjectMapper
     */
    public CubeDataSourceService(CubeDataSourceRepository cubeDataSourceRepository,
                                 UserRepository userRepository,
                                 ObjectMapper objectMapper) {
        this.cubeDataSourceRepository = cubeDataSourceRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 새 데이터소스를 생성한다.
     *
     * <p>요청에 포함된 호스트, 포트, 데이터베이스, 사용자, 비밀번호 정보를
     * JSON으로 조합하여 {@code connectionConfig}에 저장합니다.</p>
     *
     * @param request 생성 요청 DTO
     * @param userId  생성자 사용자 ID
     * @return 생성된 데이터소스 응답 DTO
     */
    @Transactional
    public CubeDataSourceDto.Response create(CubeDataSourceDto.CreateRequest request, UUID userId) {
        String connectionConfig = buildConnectionConfig(
                request.host(),
                request.port(),
                request.database(),
                request.username(),
                request.password()
        );

        CubeDataSource dataSource = CubeDataSource.builder()
                .name(request.name())
                .description(request.description())
                .dbType(request.dbType())
                .connectionConfig(connectionConfig)
                .status(DataSourceStatus.ACTIVE)
                .createdBy(userId)
                .build();

        CubeDataSource saved = cubeDataSourceRepository.save(dataSource);
        log.info("데이터소스 '{}' 생성 완료 (생성자: {})", saved.getName(), userId);
        return toResponse(saved);
    }

    /**
     * 기존 데이터소스를 수정한다.
     *
     * <p>null이 아닌 필드만 업데이트하며, 연결 관련 필드가 변경된 경우
     * {@code connectionConfig} JSON을 재구성합니다.</p>
     *
     * @param id      데이터소스 ID
     * @param request 수정 요청 DTO
     * @return 수정된 데이터소스 응답 DTO
     * @throws RuntimeException 데이터소스를 찾을 수 없는 경우
     */
    @Transactional
    public CubeDataSourceDto.Response update(UUID id, CubeDataSourceDto.UpdateRequest request) {
        CubeDataSource dataSource = cubeDataSourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("데이터소스를 찾을 수 없습니다. ID: " + id));

        if (request.name() != null) {
            dataSource.setName(request.name());
        }
        if (request.description() != null) {
            dataSource.setDescription(request.description());
        }

        // 연결 정보 필드 중 하나라도 변경되었으면 connectionConfig를 재구성
        boolean connectionChanged = request.host() != null
                || request.port() != null
                || request.database() != null
                || request.username() != null
                || request.password() != null;

        if (connectionChanged) {
            Map<String, Object> currentConfig = parseConnectionConfig(dataSource.getConnectionConfig());

            String host = request.host() != null
                    ? request.host() : (String) currentConfig.get("host");
            int port = request.port() != null
                    ? request.port() : ((Number) currentConfig.get("port")).intValue();
            String database = request.database() != null
                    ? request.database() : (String) currentConfig.get("database");
            String username = request.username() != null
                    ? request.username() : (String) currentConfig.get("username");
            String password = request.password() != null
                    ? request.password() : (String) currentConfig.get("password");

            dataSource.setConnectionConfig(buildConnectionConfig(host, port, database, username, password));
        }

        CubeDataSource updated = cubeDataSourceRepository.save(dataSource);
        log.info("데이터소스 '{}' 수정 완료", updated.getName());
        return toResponse(updated);
    }

    /**
     * 데이터소스를 삭제한다.
     *
     * @param id 삭제할 데이터소스 ID
     * @throws RuntimeException 데이터소스를 찾을 수 없는 경우
     */
    @Transactional
    public void delete(UUID id) {
        CubeDataSource dataSource = cubeDataSourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("데이터소스를 찾을 수 없습니다. ID: " + id));

        cubeDataSourceRepository.delete(dataSource);
        log.info("데이터소스 '{}' 삭제 완료", dataSource.getName());
    }

    /**
     * ID로 데이터소스를 조회한다.
     *
     * @param id 조회할 데이터소스 ID
     * @return 데이터소스 응답 DTO
     * @throws RuntimeException 데이터소스를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public CubeDataSourceDto.Response findById(UUID id) {
        CubeDataSource dataSource = cubeDataSourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("데이터소스를 찾을 수 없습니다. ID: " + id));
        return toResponse(dataSource);
    }

    /**
     * 모든 데이터소스를 페이징하여 조회한다.
     *
     * @param pageable 페이징 정보
     * @return 데이터소스 응답 DTO 페이지
     */
    @Transactional(readOnly = true)
    public Page<CubeDataSourceDto.Response> findAll(Pageable pageable) {
        return cubeDataSourceRepository.findAll(pageable)
                .map(this::toResponse);
    }

    /**
     * 상태별 데이터소스를 페이징하여 조회한다.
     *
     * @param status   필터링할 상태
     * @param pageable 페이징 정보
     * @return 데이터소스 응답 DTO 페이지
     */
    @Transactional(readOnly = true)
    public Page<CubeDataSourceDto.Response> findByStatus(DataSourceStatus status, Pageable pageable) {
        return cubeDataSourceRepository.findByStatus(status, pageable)
                .map(this::toResponse);
    }

    /**
     * 데이터소스의 DB 연결을 테스트한다.
     *
     * <p>저장된 연결 정보를 파싱하여 JDBC 연결을 시도하고,
     * 응답 시간을 측정합니다. 테스트 결과에 따라 데이터소스의
     * {@code lastTestedAt}과 {@code status}를 갱신합니다.</p>
     *
     * @param id 테스트할 데이터소스 ID
     * @return 연결 테스트 결과 DTO
     * @throws RuntimeException 데이터소스를 찾을 수 없는 경우
     */
    @Transactional
    public CubeDataSourceDto.ConnectionTestResult testConnection(UUID id) {
        CubeDataSource dataSource = cubeDataSourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("데이터소스를 찾을 수 없습니다. ID: " + id));

        Map<String, Object> config = parseConnectionConfig(dataSource.getConnectionConfig());

        String host = (String) config.get("host");
        int port = ((Number) config.get("port")).intValue();
        String database = (String) config.get("database");
        String username = (String) config.get("username");
        String password = (String) config.get("password");

        String jdbcUrl = buildJdbcUrl(dataSource.getDbType(), host, port, database);

        long startTime = System.currentTimeMillis();
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            long responseTimeMs = System.currentTimeMillis() - startTime;

            // 연결 유효성 확인
            if (connection.isValid(5)) {
                dataSource.setLastTestedAt(Instant.now());
                dataSource.setStatus(DataSourceStatus.ACTIVE);
                cubeDataSourceRepository.save(dataSource);

                log.info("데이터소스 '{}' 연결 테스트 성공 (응답시간: {}ms)", dataSource.getName(), responseTimeMs);
                return new CubeDataSourceDto.ConnectionTestResult(true, "연결 성공", responseTimeMs);
            } else {
                dataSource.setLastTestedAt(Instant.now());
                dataSource.setStatus(DataSourceStatus.ERROR);
                cubeDataSourceRepository.save(dataSource);

                log.warn("데이터소스 '{}' 연결 테스트 실패: 연결이 유효하지 않음", dataSource.getName());
                return new CubeDataSourceDto.ConnectionTestResult(false, "연결이 유효하지 않습니다.", null);
            }
        } catch (Exception e) {
            long responseTimeMs = System.currentTimeMillis() - startTime;

            dataSource.setLastTestedAt(Instant.now());
            dataSource.setStatus(DataSourceStatus.ERROR);
            cubeDataSourceRepository.save(dataSource);

            log.error("데이터소스 '{}' 연결 테스트 실패: {}", dataSource.getName(), e.getMessage());
            return new CubeDataSourceDto.ConnectionTestResult(false, "연결 실패: " + e.getMessage(), responseTimeMs);
        }
    }

    /**
     * 데이터소스에 연결하여 사용 가능한 테이블과 컬럼 정보를 조회한다.
     *
     * <p>JDBC 메타데이터를 사용하여 테이블 목록과 각 테이블의 컬럼 정보를
     * 인트로스펙션합니다. 반환되는 Map의 키는 테이블 이름이고,
     * 값은 컬럼 정보(이름, 타입, nullable 여부 등) 리스트입니다.</p>
     *
     * @param id 인트로스펙션할 데이터소스 ID
     * @return 테이블명을 키로, 컬럼 정보 리스트를 값으로 가지는 Map
     * @throws RuntimeException 데이터소스를 찾을 수 없거나 연결에 실패한 경우
     */
    @Transactional(readOnly = true)
    public Map<String, List<Map<String, Object>>> introspectTables(UUID id) {
        CubeDataSource dataSource = cubeDataSourceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("데이터소스를 찾을 수 없습니다. ID: " + id));

        Map<String, Object> config = parseConnectionConfig(dataSource.getConnectionConfig());

        String host = (String) config.get("host");
        int port = ((Number) config.get("port")).intValue();
        String database = (String) config.get("database");
        String username = (String) config.get("username");
        String password = (String) config.get("password");

        String jdbcUrl = buildJdbcUrl(dataSource.getDbType(), host, port, database);

        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();

        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password)) {
            DatabaseMetaData metaData = connection.getMetaData();

            // 테이블 목록 조회 (TABLE 유형만)
            try (ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    String tableSchema = tables.getString("TABLE_SCHEM");

                    List<Map<String, Object>> columns = new ArrayList<>();

                    // 각 테이블의 컬럼 정보 조회
                    try (ResultSet cols = metaData.getColumns(null, tableSchema, tableName, "%")) {
                        while (cols.next()) {
                            Map<String, Object> columnInfo = new LinkedHashMap<>();
                            columnInfo.put("columnName", cols.getString("COLUMN_NAME"));
                            columnInfo.put("dataType", cols.getString("TYPE_NAME"));
                            columnInfo.put("columnSize", cols.getInt("COLUMN_SIZE"));
                            columnInfo.put("nullable", "YES".equals(cols.getString("IS_NULLABLE")));
                            columnInfo.put("ordinalPosition", cols.getInt("ORDINAL_POSITION"));
                            columns.add(columnInfo);
                        }
                    }

                    result.put(tableName, columns);
                }
            }

            log.info("데이터소스 '{}' 테이블 인트로스펙션 완료: {}개 테이블 발견",
                    dataSource.getName(), result.size());

        } catch (Exception e) {
            log.error("데이터소스 '{}' 인트로스펙션 실패: {}", dataSource.getName(), e.getMessage());
            throw new RuntimeException("테이블 인트로스펙션에 실패했습니다: " + e.getMessage(), e);
        }

        return result;
    }

    // ========================= 헬퍼 메서드 =========================

    /**
     * 엔티티를 응답 DTO로 변환한다.
     *
     * <p>생성자 이름은 {@link UserRepository}를 통해 별도 조회합니다.
     * 사용자가 존재하지 않을 경우 "알 수 없음"으로 표시합니다.</p>
     *
     * @param entity 변환할 데이터소스 엔티티
     * @return 응답 DTO
     */
    private CubeDataSourceDto.Response toResponse(CubeDataSource entity) {
        String creatorName = userRepository.findById(entity.getCreatedBy())
                .map(User::getName)
                .orElse("알 수 없음");

        return new CubeDataSourceDto.Response(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getDbType(),
                entity.getStatus(),
                entity.getLastTestedAt(),
                entity.getCreatedBy(),
                creatorName,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    /**
     * 연결 정보를 JSON 문자열로 조합한다.
     *
     * @param host     호스트 주소
     * @param port     포트 번호
     * @param database 데이터베이스 이름
     * @param username 사용자 이름
     * @param password 비밀번호
     * @return JSON 형식의 연결 설정 문자열
     */
    private String buildConnectionConfig(String host, int port, String database,
                                         String username, String password) {
        try {
            Map<String, Object> config = new LinkedHashMap<>();
            config.put("host", host);
            config.put("port", port);
            config.put("database", database);
            config.put("username", username);
            config.put("password", password);
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("연결 설정 JSON 생성에 실패했습니다.", e);
        }
    }

    /**
     * JSON 문자열을 Map으로 파싱한다.
     *
     * @param json 파싱할 JSON 문자열
     * @return 파싱된 Map
     */
    private Map<String, Object> parseConnectionConfig(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException("연결 설정 JSON 파싱에 실패했습니다.", e);
        }
    }

    /**
     * DB 유형에 따라 JDBC URL을 생성한다.
     *
     * @param dbType   데이터베이스 유형
     * @param host     호스트 주소
     * @param port     포트 번호
     * @param database 데이터베이스 이름
     * @return JDBC 연결 URL
     * @throws RuntimeException 지원하지 않는 DB 유형인 경우
     */
    private String buildJdbcUrl(CubeDbType dbType, String host, int port, String database) {
        return switch (dbType) {
            case POSTGRESQL -> "jdbc:postgresql://" + host + ":" + port + "/" + database;
            case MYSQL -> "jdbc:mysql://" + host + ":" + port + "/" + database;
            case REDSHIFT -> "jdbc:redshift://" + host + ":" + port + "/" + database;
            case SNOWFLAKE -> "jdbc:snowflake://" + host + ":" + port + "/?db=" + database;
            case CLICKHOUSE -> "jdbc:clickhouse://" + host + ":" + port + "/" + database;
            case BIGQUERY -> "jdbc:bigquery://https://www.googleapis.com/bigquery/v2:443;ProjectId=" + database;
        };
    }
}
