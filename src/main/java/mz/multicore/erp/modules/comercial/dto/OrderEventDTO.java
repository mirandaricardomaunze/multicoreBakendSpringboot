package mz.multicore.erp.modules.comercial.dto;
import java.time.LocalDateTime;
public record OrderEventDTO(Long id, String eventType, String previousStatus, String newStatus,
 String actor, String actorRole, String terminalName, String details, LocalDateTime occurredAt) {}
