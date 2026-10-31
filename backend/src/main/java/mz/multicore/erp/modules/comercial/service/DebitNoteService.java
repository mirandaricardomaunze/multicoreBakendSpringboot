package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.dto.CreateDebitNoteRequest;
import mz.multicore.erp.modules.comercial.dto.DebitNoteDTO;
import mz.multicore.erp.modules.comercial.model.DebitNote;
import mz.multicore.erp.modules.comercial.repository.DebitNoteRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Orchestrates debit-note lifecycle. Debit notes are purely financial — they
 * never touch inventory.
 */
@Service
public class DebitNoteService {

    private final DebitNoteLifecycleService lifecycleService;
    private final DebitNoteQueryService queryService;

    @Autowired
    public DebitNoteService(DebitNoteLifecycleService lifecycleService,
                            DebitNoteQueryService queryService) {
        this.lifecycleService = lifecycleService;
        this.queryService = queryService;
    }

    public DebitNoteService(
            DebitNoteRepository debitNoteRepository,
            InvoiceRepository invoiceRepository,
            DocumentNumberService documentNumberService,
            AuditLogService auditLogService,
            ApplicationEventPublisher eventPublisher
    ) {
        DebitNoteQueryService query = new DebitNoteQueryService(debitNoteRepository);
        this.queryService = query;
        this.lifecycleService = new DebitNoteLifecycleService(
                debitNoteRepository, invoiceRepository, documentNumberService,
                auditLogService, eventPublisher, query);
    }

    @Transactional
    public DebitNoteDTO create(CreateDebitNoteRequest request) {
        return lifecycleService.create(request);
    }

    @Transactional
    public DebitNoteDTO approve(Long id) {
        PermissionGuard.requireManagerOrAdmin("aprovar nota de débito");
        return lifecycleService.approve(id);
    }

    @Transactional
    public DebitNoteDTO reject(Long id, String rejectionReason) {
        PermissionGuard.requireManagerOrAdmin("rejeitar nota de débito");
        return lifecycleService.reject(id, rejectionReason);
    }

    @Transactional
    public DebitNoteDTO cancel(Long id) {
        PermissionGuard.requireManagerOrAdmin("cancelar nota de débito");
        return lifecycleService.cancel(id);
    }

    @Transactional(readOnly = true)
    public List<DebitNoteDTO> findByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return queryService.findByCompany(companyId);
    }

    @Transactional(readOnly = true)
    public DebitNoteDTO findById(Long id) {
        return queryService.findById(id);
    }

    @Transactional(readOnly = true)
    public DebitNote loadForPrint(Long id) {
        return queryService.loadForPrint(id);
    }
}
