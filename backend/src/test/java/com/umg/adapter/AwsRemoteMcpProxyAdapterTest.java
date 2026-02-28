package com.umg.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.umg.domain.entity.AwsMcpServer;
import com.umg.domain.entity.Tool;
import com.umg.domain.enums.AwsAuthType;
import com.umg.domain.enums.DataSourceStatus;
import com.umg.domain.enums.ToolType;
import com.umg.exception.ToolExecutionException;
import com.umg.repository.AwsMcpServerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AwsRemoteMcpProxyAdapter 단위 테스트")
class AwsRemoteMcpProxyAdapterTest {

    @Mock
    private AwsMcpServerRepository awsMcpServerRepository;

    private AwsRemoteMcpProxyAdapter adapter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        adapter = new AwsRemoteMcpProxyAdapter(objectMapper, awsMcpServerRepository);
    }

    @Test
    @DisplayName("AWS_REMOTE 타입을 지원한다")
    void shouldSupportAwsRemoteToolType() {
        assertThat(adapter.supports(ToolType.AWS_REMOTE)).isTrue();
    }

    @Test
    @DisplayName("다른 타입은 지원하지 않는다")
    void shouldNotSupportOtherToolTypes() {
        assertThat(adapter.supports(ToolType.HTTP_API)).isFalse();
        assertThat(adapter.supports(ToolType.CUBE_JS)).isFalse();
    }

    @Nested
    @DisplayName("resolveConnectionInfo - 내부 서버 레지스트리")
    class ResolveConnectionInfoInternal {

        @Test
        @DisplayName("존재하지 않는 서버 ID면 ToolExecutionException이 발생한다")
        void shouldThrowExceptionForNonExistentServer() {
            UUID serverId = UUID.randomUUID();
            Tool tool = Tool.builder()
                    .name("test-tool")
                    .toolType(ToolType.AWS_REMOTE)
                    .awsServerId(serverId)
                    .build();

            when(awsMcpServerRepository.findById(serverId)).thenReturn(Optional.empty());

            var future = adapter.execute(tool, Map.of(), null);

            assertThatThrownBy(future::join)
                    .hasCauseInstanceOf(ToolExecutionException.class)
                    .hasMessageContaining("연결된 AWS MCP 서버를 찾을 수 없습니다");
        }

        @Test
        @DisplayName("내부 서버 자격 증명이 잘못된 JSON이면 ToolExecutionException이 발생한다")
        void shouldThrowExceptionForInvalidCredentials() {
            UUID serverId = UUID.randomUUID();
            AwsMcpServer server = AwsMcpServer.builder()
                    .id(serverId)
                    .name("bad-server")
                    .endpointUrl("https://example.com")
                    .region("us-east-1")
                    .service("execute-api")
                    .authType(AwsAuthType.IAM_KEY)
                    .credentialsConfig("not json")
                    .status(DataSourceStatus.ACTIVE)
                    .build();

            Tool tool = Tool.builder()
                    .name("test-tool")
                    .toolType(ToolType.AWS_REMOTE)
                    .awsServerId(serverId)
                    .build();

            when(awsMcpServerRepository.findById(serverId)).thenReturn(Optional.of(server));

            var future = adapter.execute(tool, Map.of(), null);

            assertThatThrownBy(future::join)
                    .hasCauseInstanceOf(ToolExecutionException.class)
                    .hasMessageContaining("자격 증명 파싱 실패");
        }
    }

    @Nested
    @DisplayName("resolveConnectionInfo - 외부 connectionConfig")
    class ResolveConnectionInfoExternal {

        @Test
        @DisplayName("awsServerId가 null이면 connectionConfig에서 직접 읽는다")
        void shouldResolveFromConnectionConfig() {
            Tool tool = Tool.builder()
                    .name("test-tool")
                    .toolType(ToolType.AWS_REMOTE)
                    .awsServerId(null)
                    .connectionConfig("{\"endpointUrl\":\"https://mcp.example.com\",\"accessKeyId\":\"KEY\",\"secretAccessKey\":\"SECRET\",\"region\":\"us-east-1\"}")
                    .build();

            // Execute - will fail due to no actual HTTP server, but verifies no repo lookup
            var future = adapter.execute(tool, Map.of("arg1", "val1"), null);

            verify(awsMcpServerRepository, never()).findById(any());
        }

        @Test
        @DisplayName("connectionConfig가 잘못된 JSON이면 ToolExecutionException이 발생한다")
        void shouldThrowExceptionForInvalidConnectionConfig() {
            Tool tool = Tool.builder()
                    .name("test-tool")
                    .toolType(ToolType.AWS_REMOTE)
                    .awsServerId(null)
                    .connectionConfig("not json {{")
                    .build();

            var future = adapter.execute(tool, Map.of(), null);

            assertThatThrownBy(future::join)
                    .hasCauseInstanceOf(ToolExecutionException.class);
        }

        @Test
        @DisplayName("필수 필드가 없으면 예외가 발생한다")
        void shouldThrowExceptionForMissingRequiredFields() {
            Tool tool = Tool.builder()
                    .name("test-tool")
                    .toolType(ToolType.AWS_REMOTE)
                    .awsServerId(null)
                    .connectionConfig("{\"region\":\"us-east-1\"}")
                    .build();

            var future = adapter.execute(tool, Map.of(), null);

            assertThatThrownBy(future::join)
                    .hasCauseInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("필수 AWS 설정 누락");
        }
    }
}
