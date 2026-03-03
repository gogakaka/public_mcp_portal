package com.umg.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.umg.domain.entity.CubeDataSource;
import com.umg.domain.entity.CubeSchema;
import com.umg.domain.entity.User;
import com.umg.domain.enums.SchemaStatus;
import com.umg.dto.CubeSchemaDto;
import com.umg.config.CacheConfig;
import com.umg.repository.CubeDataSourceRepository;
import com.umg.repository.CubeSchemaRepository;
import com.umg.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Cube.js 스키마(모델)를 관리하는 서비스.
 *
 * <p>스키마 CRUD, 상태 전환(DRAFT -> ACTIVE -> ARCHIVED),
 * 스키마 정의 유효성 검증, 메타데이터 조회 기능을 제공합니다.
 * 스키마 정의는 JSON 형식으로 저장되며, cube 이름, measures,
 * dimensions 등의 Cube.js 모델 정보를 포함합니다.</p>
 */
@Service
public class CubeSchemaService {

    private static final Logger log = LoggerFactory.getLogger(CubeSchemaService.class);

    private final CubeSchemaRepository cubeSchemaRepository;
    private final CubeDataSourceRepository cubeDataSourceRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * 생성자 주입.
     *
     * @param cubeSchemaRepository     스키마 저장소
     * @param cubeDataSourceRepository 데이터소스 저장소
     * @param userRepository           사용자 저장소
     * @param objectMapper             JSON 직렬화/역직렬화를 위한 ObjectMapper
     */
    public CubeSchemaService(CubeSchemaRepository cubeSchemaRepository,
                             CubeDataSourceRepository cubeDataSourceRepository,
                             UserRepository userRepository,
                             ObjectMapper objectMapper) {
        this.cubeSchemaRepository = cubeSchemaRepository;
        this.cubeDataSourceRepository = cubeDataSourceRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * 새 스키마를 생성한다.
     *
     * <p>요청에 포함된 데이터소스 ID의 존재 여부를 검증한 뒤,
     * 버전 1, 상태 DRAFT로 스키마를 생성합니다.</p>
     *
     * @param request 생성 요청 DTO
     * @param userId  생성자 사용자 ID
     * @return 생성된 스키마 응답 DTO
     * @throws RuntimeException 데이터소스를 찾을 수 없는 경우
     */
    @Transactional
    public CubeSchemaDto.Response create(CubeSchemaDto.CreateRequest request, UUID userId) {
        cubeDataSourceRepository.findById(request.datasourceId())
                .orElseThrow(() -> new RuntimeException(
                        "데이터소스를 찾을 수 없습니다. ID: " + request.datasourceId()));

        CubeSchema schema = CubeSchema.builder()
                .datasourceId(request.datasourceId())
                .name(request.name())
                .description(request.description())
                .schemaDefinition(request.schemaDefinition())
                .version(1)
                .status(SchemaStatus.DRAFT)
                .createdBy(userId)
                .build();

        CubeSchema saved = cubeSchemaRepository.save(schema);
        log.info("스키마 '{}' 생성 완료 (생성자: {})", saved.getName(), userId);
        return toResponse(saved);
    }

    /**
     * 기존 스키마를 수정한다.
     *
     * <p>null이 아닌 필드만 업데이트하며, 스키마 정의(schemaDefinition)가
     * 변경된 경우 버전을 1 증가시킵니다.</p>
     *
     * @param id      스키마 ID
     * @param request 수정 요청 DTO
     * @return 수정된 스키마 응답 DTO
     * @throws RuntimeException 스키마를 찾을 수 없는 경우
     */
    @Transactional
    public CubeSchemaDto.Response update(UUID id, CubeSchemaDto.UpdateRequest request) {
        CubeSchema schema = cubeSchemaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("스키마를 찾을 수 없습니다. ID: " + id));

        if (request.name() != null) {
            schema.setName(request.name());
        }
        if (request.description() != null) {
            schema.setDescription(request.description());
        }
        if (request.schemaDefinition() != null) {
            schema.setSchemaDefinition(request.schemaDefinition());
            schema.setVersion(schema.getVersion() + 1);
        }

        CubeSchema updated = cubeSchemaRepository.save(schema);
        log.info("스키마 '{}' 수정 완료 (버전: {})", updated.getName(), updated.getVersion());
        return toResponse(updated);
    }

    /**
     * 스키마를 삭제한다.
     *
     * @param id 삭제할 스키마 ID
     * @throws RuntimeException 스키마를 찾을 수 없는 경우
     */
    @Transactional
    public void delete(UUID id) {
        CubeSchema schema = cubeSchemaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("스키마를 찾을 수 없습니다. ID: " + id));

        cubeSchemaRepository.delete(schema);
        log.info("스키마 '{}' 삭제 완료", schema.getName());
    }

    /**
     * ID로 스키마를 조회한다.
     *
     * @param id 조회할 스키마 ID
     * @return 스키마 응답 DTO
     * @throws RuntimeException 스키마를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public CubeSchemaDto.Response findById(UUID id) {
        CubeSchema schema = cubeSchemaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("스키마를 찾을 수 없습니다. ID: " + id));
        return toResponse(schema);
    }

    /**
     * 모든 스키마를 페이징하여 조회한다.
     *
     * @param pageable 페이징 정보
     * @return 스키마 응답 DTO 페이지
     */
    @Transactional(readOnly = true)
    public Page<CubeSchemaDto.Response> findAll(Pageable pageable) {
        return cubeSchemaRepository.findAll(pageable)
                .map(this::toResponse);
    }

    /**
     * 데이터소스 ID로 스키마를 페이징하여 조회한다.
     *
     * @param datasourceId 데이터소스 ID
     * @param pageable     페이징 정보
     * @return 스키마 응답 DTO 페이지
     */
    @Transactional(readOnly = true)
    public Page<CubeSchemaDto.Response> findByDatasourceId(UUID datasourceId, Pageable pageable) {
        return cubeSchemaRepository.findByDatasourceId(datasourceId, pageable)
                .map(this::toResponse);
    }

    /**
     * 상태별 스키마를 페이징하여 조회한다.
     *
     * @param status   필터링할 스키마 상태
     * @param pageable 페이징 정보
     * @return 스키마 응답 DTO 페이지
     */
    @Transactional(readOnly = true)
    public Page<CubeSchemaDto.Response> findByStatus(SchemaStatus status, Pageable pageable) {
        return cubeSchemaRepository.findByStatus(status, pageable)
                .map(this::toResponse);
    }

    /**
     * 스키마를 활성화(ACTIVE) 상태로 전환한다.
     *
     * <p>활성화 전에 스키마 정의의 유효성을 검증합니다.
     * 유효하지 않은 스키마는 활성화할 수 없습니다.</p>
     *
     * @param id 활성화할 스키마 ID
     * @return 활성화된 스키마 응답 DTO
     * @throws RuntimeException 스키마를 찾을 수 없거나 유효성 검증에 실패한 경우
     */
    @CacheEvict(value = CacheConfig.CACHE_CUBE_SCHEMA_META, allEntries = true)
    @Transactional
    public CubeSchemaDto.Response activate(UUID id) {
        CubeSchema schema = cubeSchemaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("스키마를 찾을 수 없습니다. ID: " + id));

        CubeSchemaDto.ValidationResult validationResult = validateSchemaDefinition(schema.getSchemaDefinition());
        if (!validationResult.valid()) {
            throw new RuntimeException(
                    "스키마를 활성화할 수 없습니다. 유효성 검증 실패: " + validationResult.message());
        }

        schema.setStatus(SchemaStatus.ACTIVE);
        CubeSchema activated = cubeSchemaRepository.save(schema);
        log.info("스키마 '{}' 활성화 완료", activated.getName());
        return toResponse(activated);
    }

    /**
     * 스키마를 보관(ARCHIVED) 상태로 전환한다.
     *
     * @param id 보관할 스키마 ID
     * @return 보관된 스키마 응답 DTO
     * @throws RuntimeException 스키마를 찾을 수 없는 경우
     */
    @CacheEvict(value = CacheConfig.CACHE_CUBE_SCHEMA_META, allEntries = true)
    @Transactional
    public CubeSchemaDto.Response archive(UUID id) {
        CubeSchema schema = cubeSchemaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("스키마를 찾을 수 없습니다. ID: " + id));

        schema.setStatus(SchemaStatus.ARCHIVED);
        CubeSchema archived = cubeSchemaRepository.save(schema);
        log.info("스키마 '{}' 보관 처리 완료", archived.getName());
        return toResponse(archived);
    }

    /**
     * 스키마 정의의 유효성을 검증한다.
     *
     * <p>스키마 정의를 JSON으로 파싱하여 다음 항목을 확인합니다:
     * <ul>
     *   <li>{@code cube} 필드가 존재하는지</li>
     *   <li>{@code measures} 또는 {@code dimensions} 중 하나 이상 존재하는지</li>
     * </ul>
     * </p>
     *
     * @param id 검증할 스키마 ID
     * @return 유효성 검증 결과 DTO
     * @throws RuntimeException 스키마를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public CubeSchemaDto.ValidationResult validate(UUID id) {
        CubeSchema schema = cubeSchemaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("스키마를 찾을 수 없습니다. ID: " + id));

        return validateSchemaDefinition(schema.getSchemaDefinition());
    }

    /**
     * 모든 활성(ACTIVE) 스키마의 메타데이터를 조회한다.
     *
     * <p>각 활성 스키마의 정의를 파싱하여 cube 이름, measures, dimensions
     * 정보를 추출합니다. Cube.js 시맨틱 레이어의 메타 정보를 제공하는 데 사용됩니다.</p>
     *
     * @return 활성 스키마의 cube 이름, measures, dimensions 목록
     */
    @Cacheable(value = CacheConfig.CACHE_CUBE_SCHEMA_META)
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMeta() {
        List<CubeSchema> activeSchemas = cubeSchemaRepository.findByStatusOrderByNameAsc(SchemaStatus.ACTIVE);
        List<Map<String, Object>> metaList = new ArrayList<>();

        for (CubeSchema schema : activeSchemas) {
            try {
                Map<String, Object> definition = objectMapper.readValue(
                        schema.getSchemaDefinition(),
                        new TypeReference<Map<String, Object>>() {});

                Map<String, Object> cubeMeta = new LinkedHashMap<>();
                cubeMeta.put("cubeName", definition.get("cube"));
                cubeMeta.put("schemaId", schema.getId());
                cubeMeta.put("schemaName", schema.getName());

                // measures 추출
                Object measuresObj = definition.get("measures");
                if (measuresObj instanceof Map<?, ?> measuresMap) {
                    cubeMeta.put("measures", new ArrayList<>(measuresMap.keySet()));
                } else {
                    cubeMeta.put("measures", Collections.emptyList());
                }

                // dimensions 추출
                Object dimensionsObj = definition.get("dimensions");
                if (dimensionsObj instanceof Map<?, ?> dimensionsMap) {
                    cubeMeta.put("dimensions", new ArrayList<>(dimensionsMap.keySet()));
                } else {
                    cubeMeta.put("dimensions", Collections.emptyList());
                }

                metaList.add(cubeMeta);
            } catch (JsonProcessingException e) {
                log.warn("스키마 '{}' 정의 파싱 실패, 메타 목록에서 제외합니다: {}",
                        schema.getName(), e.getMessage());
            }
        }

        log.info("활성 스키마 메타데이터 조회 완료: {}개 cube", metaList.size());
        return metaList;
    }

    // ========================= 헬퍼 메서드 =========================

    /**
     * 엔티티를 응답 DTO로 변환한다.
     *
     * <p>데이터소스 이름은 {@link CubeDataSourceRepository}를 통해,
     * 생성자 이름은 {@link UserRepository}를 통해 별도 조회합니다.
     * 각각 존재하지 않을 경우 "알 수 없음"으로 표시합니다.</p>
     *
     * @param entity 변환할 스키마 엔티티
     * @return 응답 DTO
     */
    private CubeSchemaDto.Response toResponse(CubeSchema entity) {
        String datasourceName = cubeDataSourceRepository.findById(entity.getDatasourceId())
                .map(CubeDataSource::getName)
                .orElse("알 수 없음");

        String creatorName = userRepository.findById(entity.getCreatedBy())
                .map(User::getName)
                .orElse("알 수 없음");

        return new CubeSchemaDto.Response(
                entity.getId(),
                entity.getDatasourceId(),
                datasourceName,
                entity.getName(),
                entity.getDescription(),
                entity.getSchemaDefinition(),
                entity.getVersion(),
                entity.getStatus(),
                entity.getCreatedBy(),
                creatorName,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    /**
     * 스키마 정의 문자열의 유효성을 검증한다.
     *
     * <p>JSON으로 파싱한 뒤 다음 조건을 확인합니다:
     * <ol>
     *   <li>{@code cube} 필드가 존재하고 비어있지 않은지</li>
     *   <li>{@code measures} 또는 {@code dimensions} 중 하나 이상 존재하는지</li>
     * </ol>
     * </p>
     *
     * @param schemaDefinition 검증할 스키마 정의 JSON 문자열
     * @return 유효성 검증 결과 DTO
     */
    private CubeSchemaDto.ValidationResult validateSchemaDefinition(String schemaDefinition) {
        if (schemaDefinition == null || schemaDefinition.isBlank()) {
            return new CubeSchemaDto.ValidationResult(false, "스키마 정의가 비어있습니다.");
        }

        Map<String, Object> definition;
        try {
            definition = objectMapper.readValue(
                    schemaDefinition,
                    new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            return new CubeSchemaDto.ValidationResult(false,
                    "스키마 정의를 JSON으로 파싱할 수 없습니다: " + e.getMessage());
        }

        // cube 필드 검증
        Object cubeField = definition.get("cube");
        if (cubeField == null || cubeField.toString().isBlank()) {
            return new CubeSchemaDto.ValidationResult(false,
                    "필수 필드 'cube'가 누락되었거나 비어있습니다.");
        }

        // measures 또는 dimensions 존재 여부 검증
        boolean hasMeasures = definition.containsKey("measures")
                && definition.get("measures") instanceof Map<?, ?> measuresMap
                && !measuresMap.isEmpty();

        boolean hasDimensions = definition.containsKey("dimensions")
                && definition.get("dimensions") instanceof Map<?, ?> dimensionsMap
                && !dimensionsMap.isEmpty();

        if (!hasMeasures && !hasDimensions) {
            return new CubeSchemaDto.ValidationResult(false,
                    "'measures' 또는 'dimensions' 중 하나 이상이 정의되어야 합니다.");
        }

        return new CubeSchemaDto.ValidationResult(true, "스키마 유효성 검증 통과.");
    }
}
