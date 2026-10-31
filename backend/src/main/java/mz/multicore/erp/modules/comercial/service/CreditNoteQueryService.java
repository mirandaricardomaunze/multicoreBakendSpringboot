package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.CreditNoteDTO;
import mz.multicore.erp.modules.comercial.dto.CreditNoteLineDTO;
import mz.multicore.erp.modules.comercial.model.CreditNote;
import mz.multicore.erp.modules.comercial.model.CreditNoteLine;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;
import mz.multicore.erp.modules.comercial.repository.CreditNoteRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

/**
 * Gestor de consultas analíticas e projeção DTO de notas de crédito.
 */
@Service
public class CreditNoteQueryService {

    private final CreditNoteRepository creditNoteRepository;
    private final InvoiceRepository invoiceRepository;

    @Autowired
    public CreditNoteQueryService(CreditNoteRepository creditNoteRepository, InvoiceRepository invoiceRepository) {
        this.creditNoteRepository = creditNoteRepository;
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional(readOnly = true)
    public List<CreditNoteDTO> findByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return creditNoteRepository.findByCompanyId(companyId).stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public CreditNoteDTO findById(Long id) {
        return creditNoteRepository.findByIdWithLinesAndCompanyId(id, CurrentUserContext.getCurrentCompanyId())
                .map(this::toDTO)
                .orElseThrow(() -> new BusinessRuleException("Nota de crédito não encontrada."));
    }

    @Transactional(readOnly = true)
    public CreditNote loadForPrint(Long id) {
        return creditNoteRepository.findByIdWithLinesAndCompanyId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Nota de crédito não encontrada."));
    }

    @Transactional(readOnly = true)
    public Map<Long, BigDecimal> getReturnedQuantitiesByInvoiceLine(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessRuleException("Fatura não encontrada."));
        CurrentUserContext.requireCompany(invoice.getCompany().getId());
        Map<Long, BigDecimal> result = new HashMap<>();
        for (InvoiceLine il : invoice.getLines()) {
            BigDecimal qty = creditNoteRepository.sumNonVoidedReturnedByInvoiceLineId(il.getId());
            result.put(il.getId(), qty == null ? BigDecimal.ZERO : qty);
        }
        return result;
    }

    public CreditNoteDTO toDTO(CreditNote n) {
        if (n == null) return null;
        List<CreditNoteLineDTO> lineDTOs = n.getLines().stream()
                .map(l -> new CreditNoteLineDTO(
                        l.getId(),
                        l.getProduct().getId(),
                        l.getProduct().getSku(),
                        l.getProduct().getName(),
                        l.getQuantity(),
                        l.getUnitPrice(),
                        l.getTaxRate(),
                        l.getLineTotal(),
                        l.getBatchNumber()
                )).toList();
        Long buId = n.getInvoice() != null && n.getInvoice().getBusinessUnit() != null ? n.getInvoice().getBusinessUnit().getId() : null;
        String buName = n.getInvoice() != null && n.getInvoice().getBusinessUnit() != null ? n.getInvoice().getBusinessUnit().getName() : null;
        if (buId == null && n.getLines() != null && !n.getLines().isEmpty()) {
            Set<String> bus = new HashSet<>();
            for (CreditNoteLine l : n.getLines()) {
                if (l.getProduct() != null && l.getProduct().getBusinessUnit() != null) {
                    bus.add(l.getProduct().getBusinessUnit().getName());
                    if (buId == null) {
                        buId = l.getProduct().getBusinessUnit().getId();
                        buName = l.getProduct().getBusinessUnit().getName();
                    }
                }
            }
            if (bus.size() > 1) {
                buId = null;
                buName = "Misto";
            }
        }
        return new CreditNoteDTO(
                n.getId(),
                n.getNoteNumber(),
                n.getIssueDate(),
                n.getCompany() != null ? n.getCompany().getId() : null,
                n.getClient().getId(),
                n.getClient().getName(),
                n.getInvoice().getId(),
                n.getInvoice().getInvoiceNumber(),
                n.getWarehouse() != null ? n.getWarehouse().getId() : null,
                n.getWarehouse() != null ? n.getWarehouse().getName() : null,
                n.getReason().name(),
                n.getStatus().name(),
                n.getTotalBeforeTax(),
                n.getTaxAmount(),
                n.getTotalAmount(),
                n.getDescription(),
                n.getApprovedBy(),
                n.getApprovedAt(),
                n.getRejectionReason(),
                lineDTOs,
                buId,
                buName
        );
    }
}
