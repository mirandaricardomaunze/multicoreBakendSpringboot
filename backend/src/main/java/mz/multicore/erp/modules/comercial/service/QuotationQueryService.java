package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.QuotationDTO;
import mz.multicore.erp.modules.comercial.dto.QuotationLineDTO;
import mz.multicore.erp.modules.comercial.model.Quotation;
import mz.multicore.erp.modules.comercial.repository.QuotationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Consultas analíticas e projecção de cotações comerciais para DTO.
 */
@Service
public class QuotationQueryService {

    private final QuotationRepository quotationRepository;

    public QuotationQueryService(QuotationRepository quotationRepository) {
        this.quotationRepository = quotationRepository;
    }

    @Transactional(readOnly = true)
    public List<QuotationDTO> findByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return quotationRepository.findByCompanyIdOrderByQuotationDateDesc(companyId)
                .stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public List<QuotationDTO> findOpenByCompany(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        LocalDate today = LocalDate.now();
        return quotationRepository.findOpenByCompanyIdWithLines(companyId).stream()
                .filter(q -> !q.isExpired(today))
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public QuotationDTO findById(Long id) {
        return toDTO(load(id));
    }

    public Quotation load(Long id) {
        return quotationRepository
                .findByIdWithLinesAndCompanyId(id, CurrentUserContext.getCurrentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Cotação não encontrada."));
    }

    public QuotationDTO toDTO(Quotation q) {
        LocalDate today = LocalDate.now();
        List<QuotationLineDTO> lines = q.getLines().stream()
                .map(l -> new QuotationLineDTO(
                        l.getId(),
                        l.getProduct().getId(),
                        l.getProduct().getSku(),
                        l.getProduct().getName(),
                        l.getQuantity(),
                        l.getUnitPrice(),
                        l.getTaxRate(),
                        l.getDiscountPercentage(),
                        l.getLineTotal(),
                        Math.max(1, l.getProduct().getUnitsPerBox())))
                .toList();

        Set<String> bus = q.getLines().stream()
                .map(l -> l.getProduct() != null && l.getProduct().getBusinessUnit() != null
                        ? l.getProduct().getBusinessUnit().getName() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        String buName = bus.isEmpty() ? null : (bus.size() == 1 ? bus.iterator().next() : "Misto");
        Long buId = bus.size() == 1 ? q.getLines().stream()
                .map(l -> l.getProduct() != null && l.getProduct().getBusinessUnit() != null
                        ? l.getProduct().getBusinessUnit().getId() : null)
                .filter(Objects::nonNull).findFirst().orElse(null) : null;

        return new QuotationDTO(
                q.getId(),
                q.getQuotationNumber(),
                q.getQuotationDate(),
                q.getValidUntil(),
                q.isExpired(today),
                q.daysUntilExpiry(today),
                q.getCompany() != null ? q.getCompany().getId() : null,
                q.getClient() != null ? q.getClient().getId() : null,
                q.clientLabel(),
                q.getClient() != null ? q.getClient().getTaxId() : null,
                q.getWalkInName(),
                q.getWarehouse() != null ? q.getWarehouse().getId() : null,
                q.getWarehouse() != null ? q.getWarehouse().getName() : null,
                q.getTotalBeforeTax(),
                q.getTaxAmount(),
                q.getTotalAmount(),
                q.getStatus().name(),
                q.getStatus().getLabel(),
                q.getPaymentTerms(),
                q.getDeliveryTerms(),
                q.getDeliveryDays(),
                q.getNotes(),
                q.getSentAt(),
                q.getDecidedAt(),
                q.getDecidedBy(),
                q.getRejectionReason(),
                q.getOrderId(),
                q.getOrderNumber(),
                q.getCreatedBy(),
                lines,
                q.getInvoiceId(),
                q.getInvoiceNumber(),
                q.getVersion(),
                buId,
                buName);
    }
}
