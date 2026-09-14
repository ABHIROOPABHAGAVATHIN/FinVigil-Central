package com.finvigil.customer.service;

import com.finvigil.customer.entity.AuditLog;
import com.finvigil.customer.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);
    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void logEvent(String eventType, String entityType, String entityId, String action) {
        try {
            AuditLog auditLog = new AuditLog(eventType, entityType, entityId, action);
            auditLogRepository.save(auditLog);
            log.info("AUDIT: eventType={}, entityType={}, entityId={}, action={}",
                    eventType, entityType, entityId, action);
        } catch (Exception e) {
            log.error("Failed to write audit log for eventType={}, entityId={}: {}",
                    eventType, entityId, e.getMessage());
        }
    }
}
