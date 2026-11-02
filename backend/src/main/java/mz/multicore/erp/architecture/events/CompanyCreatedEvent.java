package mz.multicore.erp.architecture.events;

/** Facto emitido quando uma empresa nova entra na plataforma. */
public record CompanyCreatedEvent(Long companyId) {}
