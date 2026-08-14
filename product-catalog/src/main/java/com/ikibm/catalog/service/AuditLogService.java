package com.ikibm.catalog.service;

import com.ikibm.catalog.entity.AuditLog;
import com.ikibm.catalog.repository.AuditLogRepository;
import com.ikibm.catalog.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    public AuditLogService(AuditLogRepository auditLogRepository, UserRepository userRepository) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
    }

    public void record(Integer actorId, String action, String entityType, String entityId, String ip) {
        AuditLog log = new AuditLog();
        if (actorId != null) log.setActor(userRepository.getReferenceById(actorId));
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setIpAddress(ip);
        auditLogRepository.save(log);
    }

    public Page<AuditLog> list(int page) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(Math.max(0, page - 1), 50));
    }
}
