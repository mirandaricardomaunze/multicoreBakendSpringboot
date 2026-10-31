package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.DebitNoteDTO;
import mz.multicore.erp.modules.comercial.dto.DebitNoteLineDTO;
import mz.multicore.erp.modules.comercial.model.DebitNote;
import mz.multicore.erp.modules.comercial.repository.DebitNoteRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DebitNoteQueryService {

    private final DebitNoteRepository debitNoteRepository;

    public DebitNoteQueryService(DebitNoteRepository debitNoteRepository) {
        this.debitNoteRepository = debitNoteRepository;
    }

    public List<DebitNoteDTO> findByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return debitNoteRepository.findByCompanyId(companyId).stream().map(this::toDTO).toList();
    }

    public DebitNoteDTO findById(Long id) {
        return debitNoteRepository.findByIdWithLinesAndCompanyId(id, CurrentUserContext.getCurrentCompanyId())
                .map(this::toDTO)
                .orElseThrow(() -> new BusinessRuleException("Nota de débito não encontrada."));
    }

    public DebitNote loadForPrint(Long id) {
        return debitNoteRepository.findByIdWithLinesAndCompanyId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Nota de débito não encontrada."));
    }

    public DebitNoteDTO toDTO(DebitNote n) {
        List<DebitNoteLineDTO> lineDTOs = n.getLines().stream()
                .map(l -> new DebitNoteLineDTO(
                        l.getId(),
                        l.getDescription(),
                        l.getAmount(),
                        l.getTaxRate(),
                        l.getLineTotal()
                )).toList();
        Long buId = n.getInvoice() != null && n.getInvoice().getBusinessUnit() != null ? n.getInvoice().getBusinessUnit().getId() : null;
        String buName = n.getInvoice() != null && n.getInvoice().getBusinessUnit() != null ? n.getInvoice().getBusinessUnit().getName() : null;
        return new DebitNoteDTO(
                n.getId(),
                n.getNoteNumber(),
                n.getIssueDate(),
                n.getCompany() != null ? n.getCompany().getId() : null,
                n.getClient().getId(),
                n.getClient().getName(),
                n.getInvoice().getId(),
                n.getInvoice().getInvoiceNumber(),
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
