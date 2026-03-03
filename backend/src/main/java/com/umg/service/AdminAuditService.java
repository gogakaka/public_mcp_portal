package com.umg.service;

import com.umg.domain.entity.AdminAuditLog;
import com.umg.domain.enums.AdminAction;
import com.umg.dto.AdminAuditLogResponse;
import com.umg.dto.PageResponse;
import com.umg.repository.AdminAuditLogRepository;
import com.umg.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * 관리자 행위 감사 로그 서비스.
 *
 * <p>도구 승인/거부, 권한 변경, 사용자 삭제 등 관리자 행위를
 * 비동기적으로 기록하고 조회 기능을 제공합니다.</p>
 */
@Service
public class AdminAuditService {

    private static final Logger log = LoggerFactory.getLogger(AdminAuditService.class);

    private final AdminAuditLogRepository repository;
    private final UserRepository userRepository;

    public AdminAuditService(AdminAuditLogRepository repository, UserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    @Async("asyncExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(UUID actorId, AdminAction action, String targetType,
                    UUID targetId, String targetName, Map<String, Object> details) {
        try {
            AdminAuditLog auditLog = AdminAuditLog.builder()
                    .actorId(actorId)
                    .action(action)
                    .targetType(targetType)
                    .targetId(targetId)
                    .targetName(targetName)
                    .details(details)
                    .build();

            repository.save(auditLog);
            log.debug("Admin audit logged: actor={}, action={}, target={}/{}",
                    actorId, action, targetType, targetName);
        } catch (Exception e) {
            log.error("Failed to write admin audit log: {}", e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminAuditLogResponse> query(UUID actorId, AdminAction action,
                                                      Instant start, Instant end,
                                                      Pageable pageable) {
        Page<AdminAuditLog> page = repository.findWithFilters(actorId, action, start, end, pageable);
        return PageResponse.from(page, page.getContent().stream().map(this::toResponse).toList());
    }

    private AdminAuditLogResponse toResponse(AdminAuditLog entity) {
        String actorName = userRepository.findById(entity.getActorId())
                .map(u -> u.getName())
                .orElse("Unknown");

        return AdminAuditLogResponse.builder()
                .id(entity.getId())
                .actorId(entity.getActorId())
                .actorName(actorName)
                .action(entity.getAction())
                .targetType(entity.getTargetType())
                .targetId(entity.getTargetId())
                .targetName(entity.getTargetName())
                .details(entity.getDetails())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
