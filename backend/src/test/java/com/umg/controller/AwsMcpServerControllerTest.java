package com.umg.controller;

import com.umg.domain.enums.AwsAuthType;
import com.umg.domain.enums.DataSourceStatus;
import com.umg.dto.AwsMcpServerDto;
import com.umg.dto.PageResponse;
import com.umg.security.SecurityUtils;
import com.umg.service.AwsMcpServerService;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AwsMcpServerController 단위 테스트")
class AwsMcpServerControllerTest {

    @Mock
    private AwsMcpServerService awsMcpServerService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private AwsMcpServerController controller;

    private UUID userId;
    private UUID serverId;
    private AwsMcpServerDto.Response sampleResponse;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        serverId = UUID.randomUUID();
        sampleResponse = new AwsMcpServerDto.Response(
                serverId, "test-server", "설명", "https://mcp.example.com",
                "us-east-1", "execute-api", AwsAuthType.IAM_KEY,
                DataSourceStatus.ACTIVE, 5, null, null,
                userId, "admin", Instant.now(), Instant.now()
        );
    }

    @Nested
    @DisplayName("GET /api/aws-mcp/servers")
    class ListServers {

        @Test
        @DisplayName("필터 없이 전체 목록을 조회한다")
        void shouldListAllServers() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<AwsMcpServerDto.Response> page = new PageImpl<>(
                    List.of(sampleResponse), pageable, 1);

            when(awsMcpServerService.findAll(any(Pageable.class))).thenReturn(page);

            ResponseEntity<PageResponse<AwsMcpServerDto.Response>> response =
                    controller.listServers(null, pageable);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getContent()).hasSize(1);
        }

        @Test
        @DisplayName("상태 필터로 목록을 조회한다")
        void shouldListServersByStatus() {
            Pageable pageable = PageRequest.of(0, 20);
            Page<AwsMcpServerDto.Response> page = new PageImpl<>(List.of(), pageable, 0);

            when(awsMcpServerService.findByStatus(eq(DataSourceStatus.ACTIVE), any(Pageable.class)))
                    .thenReturn(page);

            controller.listServers(DataSourceStatus.ACTIVE, pageable);

            verify(awsMcpServerService).findByStatus(eq(DataSourceStatus.ACTIVE), any());
        }
    }

    @Nested
    @DisplayName("GET /api/aws-mcp/servers/{id}")
    class GetServer {

        @Test
        @DisplayName("ID로 서버를 조회한다")
        void shouldGetServerById() {
            when(awsMcpServerService.findById(serverId)).thenReturn(sampleResponse);

            ResponseEntity<AwsMcpServerDto.Response> response = controller.getServer(serverId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().name()).isEqualTo("test-server");
        }
    }

    @Nested
    @DisplayName("POST /api/aws-mcp/servers")
    class CreateServer {

        @Test
        @DisplayName("서버를 등록하면 201 Created를 반환한다")
        void shouldCreateAndReturn201() {
            AwsMcpServerDto.CreateRequest request = new AwsMcpServerDto.CreateRequest(
                    "new-server", null, "https://example.com", "us-east-1",
                    null, AwsAuthType.IAM_KEY, "AKID", "SECRET", null
            );

            when(securityUtils.requireCurrentUserId()).thenReturn(userId);
            when(awsMcpServerService.create(eq(request), eq(userId))).thenReturn(sampleResponse);

            ResponseEntity<AwsMcpServerDto.Response> response = controller.createServer(request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        }
    }

    @Nested
    @DisplayName("PUT /api/aws-mcp/servers/{id}")
    class UpdateServer {

        @Test
        @DisplayName("서버를 수정하면 200 OK를 반환한다")
        void shouldUpdateAndReturn200() {
            AwsMcpServerDto.UpdateRequest request = new AwsMcpServerDto.UpdateRequest(
                    "updated-name", null, null, null, null, null, null, null
            );

            when(awsMcpServerService.update(eq(serverId), eq(request))).thenReturn(sampleResponse);

            ResponseEntity<AwsMcpServerDto.Response> response =
                    controller.updateServer(serverId, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    @Nested
    @DisplayName("DELETE /api/aws-mcp/servers/{id}")
    class DeleteServer {

        @Test
        @DisplayName("서버를 삭제하면 204 No Content를 반환한다")
        void shouldDeleteAndReturn204() {
            ResponseEntity<Void> response = controller.deleteServer(serverId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            verify(awsMcpServerService).delete(serverId);
        }
    }

    @Nested
    @DisplayName("POST /api/aws-mcp/servers/{id}/test")
    class TestConnection {

        @Test
        @DisplayName("연결 테스트 결과를 반환한다")
        void shouldReturnTestResult() {
            AwsMcpServerDto.ConnectionTestResult testResult = new AwsMcpServerDto.ConnectionTestResult(
                    true, "연결 성공", "2024-11-05", "test-server", 150L
            );

            when(awsMcpServerService.testConnection(serverId)).thenReturn(testResult);

            ResponseEntity<AwsMcpServerDto.ConnectionTestResult> response =
                    controller.testConnection(serverId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().success()).isTrue();
            assertThat(response.getBody().protocolVersion()).isEqualTo("2024-11-05");
        }
    }

    @Nested
    @DisplayName("POST /api/aws-mcp/servers/{id}/sync")
    class SyncTools {

        @Test
        @DisplayName("도구 동기화 결과를 반환한다")
        void shouldReturnSyncResult() {
            AwsMcpServerDto.SyncResult syncResult = new AwsMcpServerDto.SyncResult(
                    true, "동기화 성공", 10, 7, 3
            );

            when(securityUtils.requireCurrentUserId()).thenReturn(userId);
            when(awsMcpServerService.syncTools(eq(serverId), eq(userId))).thenReturn(syncResult);

            ResponseEntity<AwsMcpServerDto.SyncResult> response = controller.syncTools(serverId);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().success()).isTrue();
            assertThat(response.getBody().toolsDiscovered()).isEqualTo(10);
            assertThat(response.getBody().toolsCreated()).isEqualTo(7);
        }
    }

    @Nested
    @DisplayName("GET /api/aws-mcp/servers/{id}/sync-history")
    class GetSyncHistory {

        @Test
        @DisplayName("동기화 이력을 페이징하여 반환한다")
        void shouldReturnSyncHistory() {
            Pageable pageable = PageRequest.of(0, 20);
            AwsMcpServerDto.SyncHistoryResponse historyItem = new AwsMcpServerDto.SyncHistoryResponse(
                    UUID.randomUUID(), serverId, "SUCCESS", 5, 3, 2, null, Instant.now()
            );
            Page<AwsMcpServerDto.SyncHistoryResponse> page = new PageImpl<>(
                    List.of(historyItem), pageable, 1);

            when(awsMcpServerService.getSyncHistory(eq(serverId), any(Pageable.class)))
                    .thenReturn(page);

            ResponseEntity<PageResponse<AwsMcpServerDto.SyncHistoryResponse>> response =
                    controller.getSyncHistory(serverId, pageable);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().getContent()).hasSize(1);
            assertThat(response.getBody().getContent().get(0).status()).isEqualTo("SUCCESS");
        }
    }
}
