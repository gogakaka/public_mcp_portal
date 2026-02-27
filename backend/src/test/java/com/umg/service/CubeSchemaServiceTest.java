package com.umg.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umg.domain.entity.CubeDataSource;
import com.umg.domain.entity.CubeSchema;
import com.umg.domain.entity.User;
import com.umg.domain.enums.CubeDbType;
import com.umg.domain.enums.DataSourceStatus;
import com.umg.domain.enums.SchemaStatus;
import com.umg.dto.CubeSchemaDto;
import com.umg.repository.CubeDataSourceRepository;
import com.umg.repository.CubeSchemaRepository;
import com.umg.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CubeSchemaService 단위 테스트")
class CubeSchemaServiceTest {

    @Mock
    private CubeSchemaRepository cubeSchemaRepository;

    @Mock
    private CubeDataSourceRepository cubeDataSourceRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private CubeSchemaService service;

    private UUID userId;
    private UUID datasourceId;
    private UUID schemaId;
    private CubeDataSource sampleDataSource;
    private CubeSchema sampleSchema;
    private User sampleUser;

    private static final String VALID_SCHEMA_DEFINITION = """
            {"cube":"Orders","measures":{"count":{"type":"count"}},"dimensions":{"status":{"sql":"status","type":"string"}}}""";

    private static final String INVALID_SCHEMA_NO_CUBE = """
            {"measures":{"count":{"type":"count"}}}""";

    private static final String INVALID_SCHEMA_NO_MEASURES_OR_DIMENSIONS = """
            {"cube":"Orders"}""";

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        datasourceId = UUID.randomUUID();
        schemaId = UUID.randomUUID();

        sampleUser = new User();
        sampleUser.setId(userId);
        sampleUser.setName("테스트 사용자");

        sampleDataSource = CubeDataSource.builder()
                .id(datasourceId)
                .name("test-ds")
                .dbType(CubeDbType.POSTGRESQL)
                .status(DataSourceStatus.ACTIVE)
                .build();

        sampleSchema = CubeSchema.builder()
                .id(schemaId)
                .datasourceId(datasourceId)
                .name("orders-schema")
                .description("주문 스키마")
                .schemaDefinition(VALID_SCHEMA_DEFINITION)
                .version(1)
                .status(SchemaStatus.DRAFT)
                .createdBy(userId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("유효한 요청으로 스키마를 생성할 수 있다")
        void shouldCreateSchema() {
            CubeSchemaDto.CreateRequest request = new CubeSchemaDto.CreateRequest(
                    datasourceId, "new-schema", "설명", VALID_SCHEMA_DEFINITION
            );

            when(cubeDataSourceRepository.findById(datasourceId)).thenReturn(Optional.of(sampleDataSource));
            when(cubeSchemaRepository.save(any(CubeSchema.class))).thenAnswer(inv -> {
                CubeSchema s = inv.getArgument(0);
                s.setId(schemaId);
                s.setCreatedAt(Instant.now());
                s.setUpdatedAt(Instant.now());
                return s;
            });
            when(cubeDataSourceRepository.findById(datasourceId)).thenReturn(Optional.of(sampleDataSource));
            when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

            CubeSchemaDto.Response result = service.create(request, userId);

            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("new-schema");
            assertThat(result.version()).isEqualTo(1);
            assertThat(result.status()).isEqualTo(SchemaStatus.DRAFT);
        }

        @Test
        @DisplayName("존재하지 않는 데이터소스 ID로 생성하면 예외가 발생한다")
        void shouldThrowExceptionForNonExistentDatasource() {
            UUID nonExistentId = UUID.randomUUID();
            CubeSchemaDto.CreateRequest request = new CubeSchemaDto.CreateRequest(
                    nonExistentId, "schema", null, VALID_SCHEMA_DEFINITION
            );

            when(cubeDataSourceRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(request, userId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("데이터소스를 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("schemaDefinition 변경 시 버전이 증가한다")
        void shouldIncrementVersionWhenSchemaDefinitionChanges() {
            String newDefinition = """
                    {"cube":"Products","measures":{"total":{"type":"sum","sql":"price"}},"dimensions":{"name":{"sql":"name","type":"string"}}}""";

            CubeSchemaDto.UpdateRequest request = new CubeSchemaDto.UpdateRequest(
                    null, null, newDefinition
            );

            when(cubeSchemaRepository.findById(schemaId)).thenReturn(Optional.of(sampleSchema));
            when(cubeSchemaRepository.save(any(CubeSchema.class))).thenReturn(sampleSchema);
            when(cubeDataSourceRepository.findById(datasourceId)).thenReturn(Optional.of(sampleDataSource));
            when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

            service.update(schemaId, request);

            verify(cubeSchemaRepository).save(argThat(s ->
                    s.getVersion() == 2 && s.getSchemaDefinition().equals(newDefinition)
            ));
        }

        @Test
        @DisplayName("이름만 변경하면 버전이 증가하지 않는다")
        void shouldNotIncrementVersionWhenOnlyNameChanges() {
            CubeSchemaDto.UpdateRequest request = new CubeSchemaDto.UpdateRequest(
                    "renamed", null, null
            );

            when(cubeSchemaRepository.findById(schemaId)).thenReturn(Optional.of(sampleSchema));
            when(cubeSchemaRepository.save(any(CubeSchema.class))).thenReturn(sampleSchema);
            when(cubeDataSourceRepository.findById(datasourceId)).thenReturn(Optional.of(sampleDataSource));
            when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

            service.update(schemaId, request);

            verify(cubeSchemaRepository).save(argThat(s ->
                    s.getVersion() == 1 && s.getName().equals("renamed")
            ));
        }
    }

    @Nested
    @DisplayName("activate()")
    class Activate {

        @Test
        @DisplayName("유효한 스키마를 활성화할 수 있다")
        void shouldActivateValidSchema() {
            when(cubeSchemaRepository.findById(schemaId)).thenReturn(Optional.of(sampleSchema));
            when(cubeSchemaRepository.save(any(CubeSchema.class))).thenReturn(sampleSchema);
            when(cubeDataSourceRepository.findById(datasourceId)).thenReturn(Optional.of(sampleDataSource));
            when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

            service.activate(schemaId);

            verify(cubeSchemaRepository).save(argThat(s ->
                    s.getStatus() == SchemaStatus.ACTIVE
            ));
        }

        @Test
        @DisplayName("유효하지 않은 스키마를 활성화하면 예외가 발생한다")
        void shouldThrowExceptionForInvalidSchema() {
            sampleSchema.setSchemaDefinition(INVALID_SCHEMA_NO_CUBE);

            when(cubeSchemaRepository.findById(schemaId)).thenReturn(Optional.of(sampleSchema));

            assertThatThrownBy(() -> service.activate(schemaId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("유효성 검증 실패");
        }

        @Test
        @DisplayName("measures/dimensions가 없는 스키마를 활성화하면 예외가 발생한다")
        void shouldThrowExceptionForSchemaWithoutMeasuresOrDimensions() {
            sampleSchema.setSchemaDefinition(INVALID_SCHEMA_NO_MEASURES_OR_DIMENSIONS);

            when(cubeSchemaRepository.findById(schemaId)).thenReturn(Optional.of(sampleSchema));

            assertThatThrownBy(() -> service.activate(schemaId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("유효성 검증 실패");
        }
    }

    @Nested
    @DisplayName("archive()")
    class Archive {

        @Test
        @DisplayName("스키마를 보관 처리할 수 있다")
        void shouldArchiveSchema() {
            when(cubeSchemaRepository.findById(schemaId)).thenReturn(Optional.of(sampleSchema));
            when(cubeSchemaRepository.save(any(CubeSchema.class))).thenReturn(sampleSchema);
            when(cubeDataSourceRepository.findById(datasourceId)).thenReturn(Optional.of(sampleDataSource));
            when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

            service.archive(schemaId);

            verify(cubeSchemaRepository).save(argThat(s ->
                    s.getStatus() == SchemaStatus.ARCHIVED
            ));
        }
    }

    @Nested
    @DisplayName("validate()")
    class Validate {

        @Test
        @DisplayName("유효한 스키마 정의는 검증을 통과한다")
        void shouldPassValidation() {
            when(cubeSchemaRepository.findById(schemaId)).thenReturn(Optional.of(sampleSchema));

            CubeSchemaDto.ValidationResult result = service.validate(schemaId);

            assertThat(result.valid()).isTrue();
            assertThat(result.message()).contains("통과");
        }

        @Test
        @DisplayName("cube 필드가 없으면 검증에 실패한다")
        void shouldFailValidationWithoutCubeField() {
            sampleSchema.setSchemaDefinition(INVALID_SCHEMA_NO_CUBE);
            when(cubeSchemaRepository.findById(schemaId)).thenReturn(Optional.of(sampleSchema));

            CubeSchemaDto.ValidationResult result = service.validate(schemaId);

            assertThat(result.valid()).isFalse();
            assertThat(result.message()).contains("cube");
        }

        @Test
        @DisplayName("빈 스키마 정의는 검증에 실패한다")
        void shouldFailValidationWithEmptyDefinition() {
            sampleSchema.setSchemaDefinition("");
            when(cubeSchemaRepository.findById(schemaId)).thenReturn(Optional.of(sampleSchema));

            CubeSchemaDto.ValidationResult result = service.validate(schemaId);

            assertThat(result.valid()).isFalse();
        }

        @Test
        @DisplayName("잘못된 JSON은 검증에 실패한다")
        void shouldFailValidationWithInvalidJson() {
            sampleSchema.setSchemaDefinition("not valid json{{{");
            when(cubeSchemaRepository.findById(schemaId)).thenReturn(Optional.of(sampleSchema));

            CubeSchemaDto.ValidationResult result = service.validate(schemaId);

            assertThat(result.valid()).isFalse();
            assertThat(result.message()).contains("JSON");
        }
    }

    @Nested
    @DisplayName("getMeta()")
    class GetMeta {

        @Test
        @DisplayName("활성 스키마의 메타 정보를 조회할 수 있다")
        void shouldReturnMetaForActiveSchemas() {
            sampleSchema.setStatus(SchemaStatus.ACTIVE);
            when(cubeSchemaRepository.findByStatusOrderByNameAsc(SchemaStatus.ACTIVE))
                    .thenReturn(List.of(sampleSchema));

            List<Map<String, Object>> meta = service.getMeta();

            assertThat(meta).hasSize(1);
            assertThat(meta.get(0).get("cubeName")).isEqualTo("Orders");
            assertThat(meta.get(0).get("schemaName")).isEqualTo("orders-schema");
            assertThat((List<?>) meta.get(0).get("measures")).contains("count");
            assertThat((List<?>) meta.get(0).get("dimensions")).contains("status");
        }

        @Test
        @DisplayName("활성 스키마가 없으면 빈 목록을 반환한다")
        void shouldReturnEmptyListWhenNoActiveSchemas() {
            when(cubeSchemaRepository.findByStatusOrderByNameAsc(SchemaStatus.ACTIVE))
                    .thenReturn(List.of());

            List<Map<String, Object>> meta = service.getMeta();

            assertThat(meta).isEmpty();
        }
    }
}
