package com.umg.controller;

import com.umg.domain.enums.SchemaStatus;
import com.umg.dto.CubeSchemaDto;
import com.umg.dto.PageResponse;
import com.umg.security.SecurityUtils;
import com.umg.service.CubeSchemaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CubeSchemaController 단위 테스트")
class CubeSchemaControllerTest {

    @Mock
    private CubeSchemaService cubeSchemaService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private CubeSchemaController controller;

    private UUID userId;
    private UUID schemaId;
    private UUID datasourceId;
    private CubeSchemaDto.Response sampleResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        schemaId = UUID.randomUUID();
        datasourceId = UUID.randomUUID();
        sampleResponse = new CubeSchemaDto.Response(
                schemaId, datasourceId, "test-ds", "orders-schema", "설명",
                "{\"cube\":\"Orders\"}", 1, SchemaStatus.DRAFT,
                userId, "admin", Instant.now(), Instant.now()
        );
    }

    @Nested
    @DisplayName("GET /api/cube/schemas")
    class ListSchemas {

        @Test
        @DisplayName("필터 없이 전체 목록을 조회한다")
        void shouldListAllSchemas() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<CubeSchemaDto.Response> page = new PageImpl<>(
                    List.of(sampleResponse), pageable, 1);

            when(cubeSchemaService.findAll(any(Pageable.class))).thenReturn(page);

            ResponseEntity<PageResponse<CubeSchemaDto.Response>> response =
                    controller.listSchemas(null, null, pageable);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getContent()).hasSize(1);
        }

        @Test
        @DisplayName("상태 필터로 목록을 조회한다")
        void shouldListSchemasByStatus() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<CubeSchemaDto.Response> page = new PageImpl<>(List.of(), pageable, 0);

            when(cubeSchemaService.findByStatus(eq(SchemaStatus.ACTIVE), any(Pageable.class)))
                    .thenReturn(page);

            controller.listSchemas(SchemaStatus.ACTIVE, null, pageable);

            verify(cubeSchemaService).findByStatus(eq(SchemaStatus.ACTIVE), any());
            verify(cubeSchemaService, never()).findAll(any());
        }

        @Test
        @DisplayName("데이터소스 ID 필터로 목록을 조회한다")
        void shouldListSchemasByDatasourceId() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<CubeSchemaDto.Response> page = new PageImpl<>(List.of(), pageable, 0);

            when(cubeSchemaService.findByDatasourceId(eq(datasourceId), any(Pageable.class)))
                    .thenReturn(page);

            controller.listSchemas(null, datasourceId, pageable);

            verify(cubeSchemaService).findByDatasourceId(eq(datasourceId), any());
        }
    }

    @Nested
    @DisplayName("GET /api/cube/schemas/meta")
    class GetMeta {

        @Test
        @DisplayName("활성 스키마 메타 정보를 반환한다")
        void shouldReturnMeta() {
            List<Map<String, Object>> meta = List.of(
                    Map.of("cubeName", "Orders", "measures", List.of("count"))
            );

            when(cubeSchemaService.getMeta()).thenReturn(meta);

            ResponseEntity<List<Map<String, Object>>> response = controller.getMeta();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("POST /api/cube/schemas")
    class CreateSchema {

        @Test
        @DisplayName("스키마를 생성하면 201 Created를 반환한다")
        void shouldCreateAndReturn201() {
            CubeSchemaDto.CreateRequest request = new CubeSchemaDto.CreateRequest(
                    datasourceId, "new-schema", null, "{\"cube\":\"Test\"}"
            );

            when(securityUtils.requireCurrentUserId()).thenReturn(userId);
            when(cubeSchemaService.create(eq(request), eq(userId))).thenReturn(sampleResponse);

            ResponseEntity<CubeSchemaDto.Response> response = controller.createSchema(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }
    }

    @Nested
    @DisplayName("POST /api/cube/schemas/{id}/activate")
    class ActivateSchema {

        @Test
        @DisplayName("스키마를 활성화하면 200 OK를 반환한다")
        void shouldActivateAndReturn200() {
            when(cubeSchemaService.activate(schemaId)).thenReturn(sampleResponse);

            ResponseEntity<CubeSchemaDto.Response> response = controller.activateSchema(schemaId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(cubeSchemaService).activate(schemaId);
        }
    }

    @Nested
    @DisplayName("POST /api/cube/schemas/{id}/archive")
    class ArchiveSchema {

        @Test
        @DisplayName("스키마를 아카이브하면 200 OK를 반환한다")
        void shouldArchiveAndReturn200() {
            when(cubeSchemaService.archive(schemaId)).thenReturn(sampleResponse);

            ResponseEntity<CubeSchemaDto.Response> response = controller.archiveSchema(schemaId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(cubeSchemaService).archive(schemaId);
        }
    }

    @Nested
    @DisplayName("POST /api/cube/schemas/{id}/validate")
    class ValidateSchema {

        @Test
        @DisplayName("스키마 검증 결과를 반환한다")
        void shouldReturnValidationResult() {
            CubeSchemaDto.ValidationResult result = new CubeSchemaDto.ValidationResult(true, "검증 통과");

            when(cubeSchemaService.validate(schemaId)).thenReturn(result);

            ResponseEntity<CubeSchemaDto.ValidationResult> response = controller.validateSchema(schemaId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().valid()).isTrue();
        }
    }
}
