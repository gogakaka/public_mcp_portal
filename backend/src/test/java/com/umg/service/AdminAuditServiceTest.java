package com.umg.service;

import com.umg.domain.entity.AdminAuditLog;
import com.umg.domain.entity.User;
import com.umg.domain.enums.AdminAction;
import com.umg.dto.AdminAuditLogResponse;
import com.umg.dto.PageResponse;
import com.umg.repository.AdminAuditLogRepository;
import com.umg.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminAuditService 단위 테스트")
class AdminAuditServiceTest {

    @Mock
    private AdminAuditLogRepository repository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminAuditService adminAuditService;

    private UUID actorId;
    private UUID targetId;

    @BeforeEach
    void setUp() {
        actorId = UUID.randomUUID();
        targetId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("log() - 관리자 감사 로그 기록")
    class LogTests {

        @Test
        @DisplayName("감사 로그를 정상적으로 저장한다")
        void shouldSaveAuditLog() {
            when(repository.save(any(AdminAuditLog.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            adminAuditService.log(actorId, AdminAction.TOOL_APPROVE, "Tool",
                    targetId, "test-tool", Map.of("previousStatus", "PENDING"));

            ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
            verify(repository).save(captor.capture());

            AdminAuditLog saved = captor.getValue();
            assertThat(saved.getActorId()).isEqualTo(actorId);
            assertThat(saved.getAction()).isEqualTo(AdminAction.TOOL_APPROVE);
            assertThat(saved.getTargetType()).isEqualTo("Tool");
            assertThat(saved.getTargetId()).isEqualTo(targetId);
            assertThat(saved.getTargetName()).isEqualTo("test-tool");
        }

        @Test
        @DisplayName("저장 실패 시 예외를 전파하지 않는다")
        void shouldNotPropagateException() {
            when(repository.save(any())).thenThrow(new RuntimeException("DB error"));

            // Should not throw
            adminAuditService.log(actorId, AdminAction.TOOL_REJECT, "Tool",
                    targetId, "test-tool", null);
        }
    }

    @Nested
    @DisplayName("query() - 감사 로그 조회")
    class QueryTests {

        @Test
        @DisplayName("필터 조건으로 감사 로그를 조회한다")
        void shouldQueryWithFilters() {
            AdminAuditLog log = AdminAuditLog.builder()
                    .id(UUID.randomUUID())
                    .actorId(actorId)
                    .action(AdminAction.TOOL_APPROVE)
                    .targetType("Tool")
                    .targetId(targetId)
                    .targetName("test-tool")
                    .createdAt(Instant.now())
                    .build();

            Pageable pageable = PageRequest.of(0, 50);
            Page<AdminAuditLog> page = new PageImpl<>(List.of(log), pageable, 1);
            when(repository.findWithFilters(eq(actorId), eq(AdminAction.TOOL_APPROVE), any(), any(), eq(pageable)))
                    .thenReturn(page);
            when(userRepository.findById(actorId))
                    .thenReturn(Optional.of(User.builder().name("Admin User").build()));

            PageResponse<AdminAuditLogResponse> result = adminAuditService.query(
                    actorId, AdminAction.TOOL_APPROVE, null, null, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getAction()).isEqualTo(AdminAction.TOOL_APPROVE);
            assertThat(result.getContent().get(0).getActorName()).isEqualTo("Admin User");
        }
    }
}
