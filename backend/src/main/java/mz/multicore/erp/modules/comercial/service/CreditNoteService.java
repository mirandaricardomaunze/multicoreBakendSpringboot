package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.dto.CreateCreditNoteRequest;
import mz.multicore.erp.modules.comercial.dto.CreditNoteDTO;
import mz.multicore.erp.modules.comercial.model.CreditNote;
import mz.multicore.erp.modules.comercial.repository.CreditNoteRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.inventory.service.WarehouseLookup;
import mz.multicore.erp.modules.inventory.service.InventoryService;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Orquestrador transacional do ciclo de vida das notas de crédito.
 * Stock é devolvido ao armazém quando aprovada com motivo RETURN.
 * Delega emissão a {@link CreditNoteCreationService}, transições de estado a {@link CreditNoteLifecycleService}
 * e consultas a {@link CreditNoteQueryService}.
 */
@Service
public class CreditNoteService {

    private final CreditNoteCreationService creationService;
    private final CreditNoteLifecycleService lifecycleService;
    private final CreditNoteQueryService queryService;

    @Autowired
    public CreditNoteService(
            CreditNoteCreationService creationService,
            CreditNoteLifecycleService lifecycleService,
            CreditNoteQueryService queryService
    ) {
        this.creationService = creationService;
        this.lifecycleService = lifecycleService;
        this.queryService = queryService;
    }

    public CreditNoteService(
            CreditNoteRepository creditNoteRepository,
            InvoiceRepository invoiceRepository,
            WarehouseLookup warehouseLookup,
            InventoryService inventoryService,
            DocumentNumberService documentNumberService,
            AuditLogService auditLogService,
            ApplicationEventPublisher eventPublisher
    ) {
        CreditNoteQueryService qService = new CreditNoteQueryService(creditNoteRepository, invoiceRepository);
        CreditNoteCreationService cService = new CreditNoteCreationService(
                creditNoteRepository, invoiceRepository, warehouseLookup, documentNumberService, qService, java.util.List.of());
        CreditNoteLifecycleService lService = new CreditNoteLifecycleService(
                creditNoteRepository, inventoryService, auditLogService, eventPublisher, qService);
        this.creationService = cService;
        this.lifecycleService = lService;
        this.queryService = qService;
    }

    @Transactional
    public CreditNoteDTO create(CreateCreditNoteRequest request) {
        return creationService.create(request);
    }

    @Transactional
    public CreditNoteDTO approve(Long id) {
        return lifecycleService.approve(id);
    }

    @Transactional
    public CreditNoteDTO reject(Long id, String rejectionReason) {
        return lifecycleService.reject(id, rejectionReason);
    }

    @Transactional
    public CreditNoteDTO cancel(Long id) {
        return lifecycleService.cancel(id);
    }

    @Transactional(readOnly = true)
    public List<CreditNoteDTO> findByCompany(Long companyId) {
        return queryService.findByCompany(companyId);
    }

    @Transactional(readOnly = true)
    public CreditNoteDTO findById(Long id) {
        return queryService.findById(id);
    }

    @Transactional(readOnly = true)
    public Map<Long, BigDecimal> getReturnedQuantitiesByInvoiceLine(Long invoiceId) {
        return queryService.getReturnedQuantitiesByInvoiceLine(invoiceId);
    }

    @Transactional(readOnly = true)
    public CreditNote loadForPrint(Long id) {
        return queryService.loadForPrint(id);
    }
}
