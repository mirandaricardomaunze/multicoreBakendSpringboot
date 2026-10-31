package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.CollectionRadarItemDTO;
import mz.multicore.erp.modules.comercial.dto.CollectionRadarSummaryDTO;
import mz.multicore.erp.modules.comercial.dto.CollectionReminderTemplateDTO;
import mz.multicore.erp.modules.comercial.model.CollectionRadarBucket;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Orquestrador do Radar Inteligente de Cobranças e Lembretes Preventivos.
 * Ver {@code docs/RADAR_COBRANCAS_PREVENTIVO_SPEC.md}.
 */
@Service
public class CollectionRadarService {

    private final InvoiceRepository invoiceRepository;
    private final CollectionRadarAnalyzer radarAnalyzer;
    private final CollectionReminderTemplateBuilder templateBuilder;

    @Autowired
    public CollectionRadarService(InvoiceRepository invoiceRepository,
                                  CollectionRadarAnalyzer radarAnalyzer,
                                  CollectionReminderTemplateBuilder templateBuilder) {
        this.invoiceRepository = Objects.requireNonNull(invoiceRepository, "invoiceRepository obrigatório");
        this.radarAnalyzer = radarAnalyzer;
        this.templateBuilder = templateBuilder;
    }

    /** Construtor retrocompatível preservado para testes e integrações existentes. */
    public CollectionRadarService(InvoiceRepository invoiceRepository) {
        this(Objects.requireNonNull(invoiceRepository, "invoiceRepository obrigatório"),
                new CollectionRadarAnalyzer(invoiceRepository),
                new CollectionReminderTemplateBuilder());
    }

    @Transactional(readOnly = true)
    public CollectionRadarSummaryDTO getRadarSummary(Long companyId, LocalDate referenceDate) {
        Long resolvedCompanyId = companyId != null ? companyId : CurrentUserContext.requireCurrentCompanyId();
        LocalDate ref = referenceDate != null ? referenceDate : LocalDate.now();
        return radarAnalyzer.buildSummary(resolvedCompanyId, ref);
    }

    @Transactional(readOnly = true)
    public List<CollectionRadarItemDTO> getRadarItems(Long companyId, CollectionRadarBucket filterBucket, LocalDate referenceDate) {
        Long resolvedCompanyId = companyId != null ? companyId : CurrentUserContext.requireCurrentCompanyId();
        LocalDate ref = referenceDate != null ? referenceDate : LocalDate.now();
        return radarAnalyzer.buildItems(resolvedCompanyId, filterBucket, ref);
    }

    @Transactional(readOnly = true)
    public CollectionReminderTemplateDTO generateReminderTemplate(Long invoiceId, String channel) {
        if (invoiceId == null) throw new BusinessRuleException("ID da fatura obrigatório.");
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new BusinessRuleException("Fatura não encontrada: " + invoiceId));
        return templateBuilder.build(invoice, channel);
    }
}
