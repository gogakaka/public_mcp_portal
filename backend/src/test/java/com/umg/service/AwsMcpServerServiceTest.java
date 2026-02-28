package com.umg.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umg.domain.entity.AwsMcpServer;
import com.umg.domain.entity.AwsMcpSyncHistory;
import com.umg.domain.entity.Tool;
import com.umg.domain.entity.User;
import com.umg.domain.enums.*;
import com.umg.dto.AwsMcpServerDto;
import com.umg.repository.AwsMcpServerRepository;
import com.umg.repository.AwsMcpSyncHistoryRepository;
import com.umg.repository.ToolRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AwsMcpServerService 단위 테스트")
class AwsMcpServerServiceTest {

    @Mock
    private AwsMcpServerRepository awsMcpServerRepository;

    @Mock
    private AwsMcpSyncHistoryRepository awsMcpSyncHistoryRepository;

    @Mock
    private ToolRepository toolRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AwsMcpServerService service;

    private UUID userId;
    private UUID serverId;
    private AwsMcpServer sampleServer;
    private User sampleUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        serverId = UUID.randomUUID();

        sampleUser = new User();
        sampleUser.setId(userId);
        sampleUser.setName("테스트 관리자");

        sampleServer = AwsMcpServer.builder()
                .id(serverId)
                .name("test-mcp-server")
                .description("테스트 MCP 서버")
                .endpointUrl("https://mcp.example.com/api")
                .region("us-east-1")
                .service("execute-api")
                .authType(AwsAuthType.IAM_KEY)
                .credentialsConfig("{\"accessKeyId\":\"AKID123\",\"secretAccessKey\":\"secret123\"}")
                .status(DataSourceStatus.ACTIVE)
                .syncedToolCount(0)
                .createdBy(userId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("유효한 요청으로 AWS MCP 서버를 생성할 수 있다")
        void shouldCreateServer() {
            AwsMcpServerDto.CreateRequest request = new AwsMcpServerDto.CreateRequest(
                    "new-server", "설명", "https://mcp.example.com", "us-west-2",
                    null, AwsAuthType.IAM_KEY, "AKID", "SECRET", null
            );

            when(awsMcpServerRepository.save(any(AwsMcpServer.class))).thenAnswer(inv -> {
                AwsMcpServer s = inv.getArgument(0);
                s.setId(serverId);
                s.setCreatedAt(Instant.now());
                s.setUpdatedAt(Instant.now());
                return s;
            });
            when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

            AwsMcpServerDto.Response result = service.create(request, userId);

            assertThat(result).isNotNull();
            assertThat(result.name()).isEqualTo("new-server");
            assertThat(result.region()).isEqualTo("us-west-2");
            assertThat(result.authType()).isEqualTo(AwsAuthType.IAM_KEY);
        }

        @Test
        @DisplayName("service를 지정하지 않으면 기본값 execute-api가 사용된다")
        void shouldUseDefaultServiceWhenNotSpecified() {
            AwsMcpServerDto.CreateRequest request = new AwsMcpServerDto.CreateRequest(
                    "server", null, "https://example.com", "us-east-1",
                    null, AwsAuthType.IAM_KEY, "AKID", "SECRET", null
            );

            when(awsMcpServerRepository.save(any(AwsMcpServer.class))).thenAnswer(inv -> {
                AwsMcpServer s = inv.getArgument(0);
                s.setId(serverId);
                s.setCreatedAt(Instant.now());
                s.setUpdatedAt(Instant.now());
                return s;
            });
            when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

            service.create(request, userId);

            ArgumentCaptor<AwsMcpServer> captor = ArgumentCaptor.forClass(AwsMcpServer.class);
            verify(awsMcpServerRepository).save(captor.capture());
            assertThat(captor.getValue().getService()).isEqualTo("execute-api");
        }

        @Test
        @DisplayName("credentialsConfig에 accessKeyId, secretAccessKey가 포함된다")
        void shouldBuildCredentialsConfigCorrectly() throws Exception {
            AwsMcpServerDto.CreateRequest request = new AwsMcpServerDto.CreateRequest(
                    "server", null, "https://example.com", "eu-west-1",
                    "lambda", AwsAuthType.IAM_KEY, "MY_KEY", "MY_SECRET", "arn:aws:iam::123:role/test"
            );

            when(awsMcpServerRepository.save(any(AwsMcpServer.class))).thenAnswer(inv -> {
                AwsMcpServer s = inv.getArgument(0);
                s.setId(serverId);
                s.setCreatedAt(Instant.now());
                s.setUpdatedAt(Instant.now());
                return s;
            });
            when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

            service.create(request, userId);

            ArgumentCaptor<AwsMcpServer> captor = ArgumentCaptor.forClass(AwsMcpServer.class);
            verify(awsMcpServerRepository).save(captor.capture());

            String config = captor.getValue().getCredentialsConfig();
            var configMap = objectMapper.readValue(config, java.util.Map.class);
            assertThat(configMap.get("accessKeyId")).isEqualTo("MY_KEY");
            assertThat(configMap.get("secretAccessKey")).isEqualTo("MY_SECRET");
            assertThat(configMap.get("roleArn")).isEqualTo("arn:aws:iam::123:role/test");
        }
    }

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("이름과 엔드포인트를 수정할 수 있다")
        void shouldUpdateNameAndEndpoint() {
            AwsMcpServerDto.UpdateRequest request = new AwsMcpServerDto.UpdateRequest(
                    "updated-name", null, "https://new-endpoint.com", null, null, null, null, null
            );

            when(awsMcpServerRepository.findById(serverId)).thenReturn(Optional.of(sampleServer));
            when(awsMcpServerRepository.save(any(AwsMcpServer.class))).thenReturn(sampleServer);
            when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

            service.update(serverId, request);

            verify(awsMcpServerRepository).save(argThat(s ->
                    s.getName().equals("updated-name") &&
                    s.getEndpointUrl().equals("https://new-endpoint.com")
            ));
        }

        @Test
        @DisplayName("자격 증명 변경 시 credentialsConfig가 재구성된다")
        void shouldRebuildCredentialsWhenChanged() {
            AwsMcpServerDto.UpdateRequest request = new AwsMcpServerDto.UpdateRequest(
                    null, null, null, null, null, "NEW_KEY", null, null
            );

            when(awsMcpServerRepository.findById(serverId)).thenReturn(Optional.of(sampleServer));
            when(awsMcpServerRepository.save(any(AwsMcpServer.class))).thenReturn(sampleServer);
            when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

            service.update(serverId, request);

            verify(awsMcpServerRepository).save(argThat(s ->
                    s.getCredentialsConfig().contains("NEW_KEY")
            ));
        }

        @Test
        @DisplayName("존재하지 않는 서버를 수정하면 예외가 발생한다")
        void shouldThrowExceptionForNonExistentServer() {
            UUID nonExistentId = UUID.randomUUID();
            AwsMcpServerDto.UpdateRequest request = new AwsMcpServerDto.UpdateRequest(
                    "name", null, null, null, null, null, null, null
            );

            when(awsMcpServerRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(nonExistentId, request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("AWS MCP 서버를 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("존재하는 서버를 삭제할 수 있다")
        void shouldDeleteExistingServer() {
            when(awsMcpServerRepository.findById(serverId)).thenReturn(Optional.of(sampleServer));

            service.delete(serverId);

            verify(awsMcpServerRepository).delete(sampleServer);
        }
    }

    @Nested
    @DisplayName("findById() / findAll() / findByStatus()")
    class Find {

        @Test
        @DisplayName("ID로 서버를 조회할 수 있다")
        void shouldFindById() {
            when(awsMcpServerRepository.findById(serverId)).thenReturn(Optional.of(sampleServer));
            when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

            AwsMcpServerDto.Response result = service.findById(serverId);

            assertThat(result.id()).isEqualTo(serverId);
            assertThat(result.name()).isEqualTo("test-mcp-server");
        }

        @Test
        @DisplayName("모든 서버를 페이징하여 조회할 수 있다")
        void shouldFindAllPaged() {
            Pageable pageable = PageRequest.of(0, 10);
            Page<AwsMcpServer> page = new PageImpl<>(List.of(sampleServer), pageable, 1);

            when(awsMcpServerRepository.findAll(pageable)).thenReturn(page);
            when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));

            Page<AwsMcpServerDto.Response> result = service.findAll(pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("testConnection()")
    class TestConnection {

        @Test
        @DisplayName("자격 증명이 없으면 연결 테스트가 실패한다")
        void shouldFailWithMissingCredentials() {
            sampleServer.setCredentialsConfig("{\"accessKeyId\":null,\"secretAccessKey\":null}");

            when(awsMcpServerRepository.findById(serverId)).thenReturn(Optional.of(sampleServer));
            when(awsMcpServerRepository.save(any(AwsMcpServer.class))).thenReturn(sampleServer);

            AwsMcpServerDto.ConnectionTestResult result = service.testConnection(serverId);

            assertThat(result.success()).isFalse();
            assertThat(result.message()).contains("자격 증명 누락");

            verify(awsMcpServerRepository).save(argThat(s ->
                    s.getStatus() == DataSourceStatus.ERROR
            ));
        }
    }

    @Nested
    @DisplayName("getSyncHistory()")
    class SyncHistory {

        @Test
        @DisplayName("동기화 이력을 페이징하여 조회할 수 있다")
        void shouldGetSyncHistory() {
            Pageable pageable = PageRequest.of(0, 10);
            AwsMcpSyncHistory history = AwsMcpSyncHistory.builder()
                    .id(UUID.randomUUID())
                    .serverId(serverId)
                    .status(SyncStatus.SUCCESS)
                    .toolsDiscovered(5)
                    .toolsCreated(3)
                    .toolsUpdated(2)
                    .createdAt(Instant.now())
                    .build();

            Page<AwsMcpSyncHistory> page = new PageImpl<>(List.of(history), pageable, 1);
            when(awsMcpSyncHistoryRepository.findByServerIdOrderByCreatedAtDesc(serverId, pageable))
                    .thenReturn(page);

            Page<AwsMcpServerDto.SyncHistoryResponse> result = service.getSyncHistory(serverId, pageable);

            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).toolsDiscovered()).isEqualTo(5);
            assertThat(result.getContent().get(0).toolsCreated()).isEqualTo(3);
            assertThat(result.getContent().get(0).status()).isEqualTo("SUCCESS");
        }
    }
}
