package com.umg.service;

import com.umg.domain.entity.*;
import com.umg.domain.enums.*;
import com.umg.dto.GdprExportResponse;
import com.umg.exception.ResourceNotFoundException;
import com.umg.repository.*;
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

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GdprService 단위 테스트")
class GdprServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private ToolRepository toolRepository;
    @Mock private ApiKeyRepository apiKeyRepository;
    @Mock private PermissionRepository permissionRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private AdminAuditService adminAuditService;

    @InjectMocks
    private GdprService gdprService;

    private UUID userId;
    private User testUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .name("Test User")
                .department("Engineering")
                .role(UserRole.USER)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Nested
    @DisplayName("exportUserData() - 데이터 내보내기")
    class ExportTests {

        @Test
        @DisplayName("사용자의 전체 데이터를 내보낸다")
        void shouldExportAllUserData() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(toolRepository.findAll()).thenReturn(List.of(
                    Tool.builder().id(UUID.randomUUID()).name("tool1").description("desc")
                            .toolType(ToolType.N8N).status(ToolStatus.APPROVED)
                            .ownerId(userId).createdAt(Instant.now()).build()
            ));
            when(apiKeyRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
            when(permissionRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
            when(auditLogRepository.findByUserIdOrderByCreatedAtDesc(eq(userId), any()))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));

            GdprExportResponse result = gdprService.exportUserData(userId, userId);

            assertThat(result.getUser().getEmail()).isEqualTo("test@example.com");
            assertThat(result.getTools()).hasSize(1);
            assertThat(result.getExportedAt()).isNotNull();
            verify(adminAuditService).log(eq(userId), eq(AdminAction.USER_DATA_EXPORT), any(), any(), any(), any());
        }

        @Test
        @DisplayName("존재하지 않는 사용자는 예외를 발생시킨다")
        void shouldThrowForNonexistentUser() {
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> gdprService.exportUserData(userId, userId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("deleteUserData() - 데이터 삭제")
    class DeleteTests {

        @Test
        @DisplayName("사용자 데이터를 삭제한다")
        void shouldDeleteUserData() {
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(permissionRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
            when(apiKeyRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

            gdprService.deleteUserData(userId, userId);

            verify(userRepository).delete(testUser);
            verify(adminAuditService).log(eq(userId), eq(AdminAction.USER_DELETE), any(), any(), any(), any());
        }
    }
}
