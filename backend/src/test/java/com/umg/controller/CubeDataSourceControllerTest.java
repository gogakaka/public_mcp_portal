package com.umg.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umg.domain.enums.CubeDbType;
import com.umg.domain.enums.DataSourceStatus;
import com.umg.dto.CubeDataSourceDto;
import com.umg.dto.PageResponse;
import com.umg.security.SecurityUtils;
import com.umg.service.CubeDataSourceService;
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
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CubeDataSourceController 단위 테스트")
class CubeDataSourceControllerTest {

    @Mock
    private CubeDataSourceService cubeDataSourceService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private CubeDataSourceController controller;

    private UUID userId;
    private UUID dataSourceId;
    private CubeDataSourceDto.Response sampleResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        dataSourceId = UUID.randomUUID();
        sampleResponse = new CubeDataSourceDto.Response(
                dataSourceId, "test-ds", "설명", CubeDbType.POSTGRESQL,
                DataSourceStatus.ACTIVE, null, userId, "admin",
                Instant.now(), Instant.now()
        );
    }

    @Nested
    @DisplayName("GET /api/cube/datasources")
    class ListDataSources {

        @Test
        @DisplayName("필터 없이 전체 목록을 조회한다")
        void shouldListAllDataSources() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<CubeDataSourceDto.Response> page = new PageImpl<>(
                    List.of(sampleResponse), pageable, 1);

            when(cubeDataSourceService.findAll(any(Pageable.class))).thenReturn(page);

            ResponseEntity<PageResponse<CubeDataSourceDto.Response>> response =
                    controller.listDataSources(null, pageable);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getContent()).hasSize(1);
        }

        @Test
        @DisplayName("상태 필터로 목록을 조회한다")
        void shouldListDataSourcesByStatus() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<CubeDataSourceDto.Response> page = new PageImpl<>(
                    List.of(sampleResponse), pageable, 1);

            when(cubeDataSourceService.findByStatus(eq(DataSourceStatus.ACTIVE), any(Pageable.class)))
                    .thenReturn(page);

            ResponseEntity<PageResponse<CubeDataSourceDto.Response>> response =
                    controller.listDataSources(DataSourceStatus.ACTIVE, pageable);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(cubeDataSourceService).findByStatus(eq(DataSourceStatus.ACTIVE), any());
        }
    }

    @Nested
    @DisplayName("GET /api/cube/datasources/{id}")
    class GetDataSource {

        @Test
        @DisplayName("ID로 데이터소스를 조회한다")
        void shouldGetDataSourceById() {
            when(cubeDataSourceService.findById(dataSourceId)).thenReturn(sampleResponse);

            ResponseEntity<CubeDataSourceDto.Response> response = controller.getDataSource(dataSourceId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().name()).isEqualTo("test-ds");
        }
    }

    @Nested
    @DisplayName("POST /api/cube/datasources")
    class CreateDataSource {

        @Test
        @DisplayName("데이터소스를 생성하면 201 Created를 반환한다")
        void shouldCreateAndReturn201() {
            CubeDataSourceDto.CreateRequest request = new CubeDataSourceDto.CreateRequest(
                    "new-ds", null, CubeDbType.POSTGRESQL, "localhost", 5432, "db", "user", "pass"
            );

            when(securityUtils.requireCurrentUserId()).thenReturn(userId);
            when(cubeDataSourceService.create(eq(request), eq(userId))).thenReturn(sampleResponse);

            ResponseEntity<CubeDataSourceDto.Response> response = controller.createDataSource(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
            assertThat(response.getBody()).isNotNull();
        }
    }

    @Nested
    @DisplayName("PUT /api/cube/datasources/{id}")
    class UpdateDataSource {

        @Test
        @DisplayName("데이터소스를 수정하면 200 OK를 반환한다")
        void shouldUpdateAndReturn200() {
            CubeDataSourceDto.UpdateRequest request = new CubeDataSourceDto.UpdateRequest(
                    "updated-name", null, null, null, null, null, null
            );

            when(cubeDataSourceService.update(eq(dataSourceId), eq(request))).thenReturn(sampleResponse);

            ResponseEntity<CubeDataSourceDto.Response> response =
                    controller.updateDataSource(dataSourceId, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Nested
    @DisplayName("DELETE /api/cube/datasources/{id}")
    class DeleteDataSource {

        @Test
        @DisplayName("데이터소스를 삭제하면 204 No Content를 반환한다")
        void shouldDeleteAndReturn204() {
            ResponseEntity<Void> response = controller.deleteDataSource(dataSourceId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(cubeDataSourceService).delete(dataSourceId);
        }
    }

    @Nested
    @DisplayName("POST /api/cube/datasources/{id}/test")
    class TestConnection {

        @Test
        @DisplayName("연결 테스트 결과를 반환한다")
        void shouldReturnTestResult() {
            CubeDataSourceDto.ConnectionTestResult testResult =
                    new CubeDataSourceDto.ConnectionTestResult(true, "연결 성공", 42L);

            when(cubeDataSourceService.testConnection(dataSourceId)).thenReturn(testResult);

            ResponseEntity<CubeDataSourceDto.ConnectionTestResult> response =
                    controller.testConnection(dataSourceId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().success()).isTrue();
            assertThat(response.getBody().responseTimeMs()).isEqualTo(42L);
        }
    }

    @Nested
    @DisplayName("GET /api/cube/datasources/{id}/tables")
    class IntrospectTables {

        @Test
        @DisplayName("테이블 인트로스펙션 결과를 반환한다")
        void shouldReturnTableInfo() {
            Map<String, List<Map<String, Object>>> tables = new LinkedHashMap<>();
            tables.put("orders", List.of(
                    Map.of("columnName", "id", "dataType", "uuid"),
                    Map.of("columnName", "amount", "dataType", "numeric")
            ));

            when(cubeDataSourceService.introspectTables(dataSourceId)).thenReturn(tables);

            ResponseEntity<Map<String, List<Map<String, Object>>>> response =
                    controller.introspectTables(dataSourceId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).containsKey("orders");
            assertThat(response.getBody().get("orders")).hasSize(2);
        }
    }
}
