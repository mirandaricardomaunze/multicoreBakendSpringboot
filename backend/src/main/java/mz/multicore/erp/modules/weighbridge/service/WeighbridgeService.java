package mz.multicore.erp.modules.weighbridge.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.format.DisplayNumbers;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.service.ProductLookup;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import mz.multicore.erp.modules.numbering.service.DocumentSeries;
import mz.multicore.erp.modules.weighbridge.dto.FirstWeighingRequest;
import mz.multicore.erp.modules.weighbridge.dto.SecondWeighingRequest;
import mz.multicore.erp.modules.weighbridge.dto.WeighingTicketDTO;
import mz.multicore.erp.modules.weighbridge.model.WeighingStatus;
import mz.multicore.erp.modules.weighbridge.model.WeighingTicket;
import mz.multicore.erp.modules.weighbridge.repository.WeighingTicketRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Truck scale (SA-C18, {@code docs/BASCULA_CAMIAO_SPEC.md}). The truck is weighed when it enters and when it
 * leaves; the difference is what was delivered or taken. A typed weight is accepted but flagged as manual and
 * audited, so the anti-fraud report can show it. One plate has at most one open ticket.
 */
@Service
public class WeighbridgeService {

    private final WeighingTicketRepository ticketRepository;
    private final ProductLookup productLookup;
    private final CompanyLookup companyLookup;
    private final DocumentNumberService documentNumberService;
    private final AuditLogService auditLogService;

    public WeighbridgeService(WeighingTicketRepository ticketRepository, ProductLookup productLookup,
                              CompanyLookup companyLookup, DocumentNumberService documentNumberService,
                              AuditLogService auditLogService) {
        this.ticketRepository = ticketRepository;
        this.productLookup = productLookup;
        this.companyLookup = companyLookup;
        this.documentNumberService = documentNumberService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<WeighingTicketDTO> tickets(Long companyId, int days) {
        CurrentUserContext.requireCompany(companyId);
        return ticketRepository.findOpenAndRecent(companyId, LocalDate.now().minusDays(Math.max(1, Math.min(days, 120))).atStartOfDay())
                .stream().map(WeighbridgeService::toDto).toList();
    }

    @Transactional
    public WeighingTicketDTO first(FirstWeighingRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        PermissionGuard.requireSeller("pesar camião");
        var existing = ticketRepository.findByCompanyIdAndRequestKey(request.companyId(), request.requestKey());
        if (existing.isPresent()) return toDto(existing.get());
        String plate = WeighingRules.normalizePlate(request.plate());
        if (plate.length() < 4) throw new BusinessRuleException("Indique a matrícula do camião.");
        if (ticketRepository.hasOpenTicket(request.companyId(), plate)) {
            throw new BusinessRuleException("O camião " + plate + " já está no recinto: faça a 2.ª pesagem do talão aberto.");
        }
        Product product = request.productId() == null ? null : productLookup.findForCompany(request.productId(), request.companyId())
                .orElseThrow(() -> new BusinessRuleException("Artigo não encontrado nesta empresa."));
        WeighingTicket t = new WeighingTicket();
        t.setCompany(companyLookup.require(request.companyId()));
        t.setNumber(documentNumberService.next(DocumentSeries.WEIGHING_TICKET));
        t.setDirection(request.direction());
        t.setPlate(plate);
        t.setDriver(blankToNull(request.driver()));
        t.setCounterparty(blankToNull(request.counterparty()));
        t.setProduct(product);
        t.setReference(blankToNull(request.reference()));
        t.setFirstWeight(request.weightKg());
        t.setFirstAt(LocalDateTime.now());
        t.setManual(!request.fromScale());
        t.setNotes(blankToNull(request.notes()));
        t.setRequestKey(request.requestKey());
        t.setCreatedBy(CurrentUserContext.getUsername());
        t = ticketRepository.save(t);
        auditLogService.logCurrent(request.fromScale() ? "WEIGHING_FIRST" : "WEIGHING_FIRST_MANUAL", "Talão " + t.getNumber() + ": "
                + plate + " entrou com " + DisplayNumbers.qty(request.weightKg()) + " kg" + (request.fromScale() ? "." : " (peso escrito à mão)."));
        return toDto(t);
    }

    @Transactional
    public WeighingTicketDTO second(Long id, SecondWeighingRequest request) {
        CurrentUserContext.requireCompany(request.companyId());
        PermissionGuard.requireSeller("pesar camião");
        WeighingTicket t = ticketRepository.lockByIdAndCompanyId(id, request.companyId())
                .orElseThrow(() -> new BusinessRuleException("Talão de pesagem não encontrado."));
        if (t.getStatus() == WeighingStatus.CLOSED) throw new BusinessRuleException("O talão " + t.getNumber() + " já está fechado.");
        t.setNetWeight(WeighingRules.net(t.getDirection(), t.getFirstWeight(), request.weightKg()));
        t.setSecondWeight(request.weightKg());
        t.setSecondAt(LocalDateTime.now());
        if (!request.fromScale()) t.setManual(true);
        if (request.reference() != null && !request.reference().isBlank()) t.setReference(request.reference().trim());
        t.setStatus(WeighingStatus.CLOSED);
        t.setClosedBy(CurrentUserContext.getUsername());
        t = ticketRepository.save(t);
        auditLogService.logCurrent(request.fromScale() ? "WEIGHING_SECOND" : "WEIGHING_SECOND_MANUAL", "Talão " + t.getNumber()
                + ": líquido " + DisplayNumbers.qty(t.getNetWeight()) + " kg" + (request.fromScale() ? "." : " (peso escrito à mão)."));
        return toDto(t);
    }

    /** Ticket for printing, loaded inside the transaction. */
    @Transactional(readOnly = true)
    public WeighingTicket forPrint(Long companyId, Long id) {
        CurrentUserContext.requireCompany(companyId);
        WeighingTicket t = ticketRepository.findForPrint(id, companyId)
                .orElseThrow(() -> new BusinessRuleException("Talão de pesagem não encontrado."));
        t.getCompany().getName();
        return t;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public static WeighingTicketDTO toDto(WeighingTicket t) {
        return new WeighingTicketDTO(t.getId(), t.getNumber(), t.getStatus(), t.getDirection(), t.getPlate(), t.getDriver(),
                t.getCounterparty(), t.getProduct() == null ? null : t.getProduct().getId(),
                t.getProduct() == null ? null : t.getProduct().getName(), t.getReference(), t.getFirstWeight(), t.getFirstAt(),
                t.getSecondWeight(), t.getSecondAt(), t.getNetWeight(), t.isManual(), t.getNotes(), t.getCreatedBy(), t.getClosedBy());
    }
}
