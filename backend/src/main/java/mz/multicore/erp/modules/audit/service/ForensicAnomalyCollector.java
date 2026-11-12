package mz.multicore.erp.modules.audit.service;

import mz.multicore.erp.architecture.format.DisplayLocale;
import mz.multicore.erp.modules.audit.dto.ForensicAnomalyDTO;
import mz.multicore.erp.modules.audit.dto.ForensicCategory;
import mz.multicore.erp.modules.audit.dto.ForensicSeverity;
import mz.multicore.erp.modules.audit.model.AuditLog;
import mz.multicore.erp.modules.audit.repository.AuditLogRepository;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.service.InvoiceLookup;
import mz.multicore.erp.modules.inventory.model.StockWaste;
import mz.multicore.erp.modules.inventory.repository.StockWasteRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Recolhe anomalias forenses a partir de facturas, quebras de stock e logs de auditoria. */
@Component
public class ForensicAnomalyCollector {

    private final InvoiceLookup invoiceLookup;
    private final StockWasteRepository stockWasteRepository;
    private final AuditLogRepository auditLogRepository;

    public ForensicAnomalyCollector(InvoiceLookup invoiceLookup,
                                    StockWasteRepository stockWasteRepository,
                                    AuditLogRepository auditLogRepository) {
        this.invoiceLookup = invoiceLookup;
        this.stockWasteRepository = stockWasteRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public List<ForensicAnomalyDTO> collectAll(Long companyId) {
        List<ForensicAnomalyDTO> anomalies = new ArrayList<>();
        collectInvoiceAnomalies(companyId, anomalies);
        collectStockWasteAnomalies(companyId, anomalies);
        collectAuditSecurityAnomalies(companyId, anomalies);
        return anomalies;
    }

    private void collectInvoiceAnomalies(Long companyId, List<ForensicAnomalyDTO> out) {
        List<Invoice> invoices = companyId != null
                ? invoiceLookup.listForCompany(companyId)
                : invoiceLookup.listAll();

        for (Invoice inv : invoices) {
            LocalDateTime eventTime = inv.getUpdatedAt() != null
                    ? inv.getUpdatedAt()
                    : (inv.getCreatedAt() != null ? inv.getCreatedAt() : LocalDateTime.now());
            String docRef = inv.getInvoiceNumber() != null ? inv.getInvoiceNumber() : "FT-" + inv.getId();

            if (inv.getStatus() == InvoiceStatus.CANCELLED) {
                BigDecimal amount = inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO;
                String reason = inv.getCancellationReason();
                boolean isReasonMissing = (reason == null || reason.trim().isEmpty());
                ForensicSeverity sev = (amount.compareTo(new BigDecimal("5000.00")) > 0 || isReasonMissing)
                        ? ForensicSeverity.CRITICAL
                        : ForensicSeverity.SUSPICIOUS;
                String details = String.format(DisplayLocale.NUMBERS, "Fatura fiscal cancelada no valor de %,.2f MT. Motivo registado: %s.",
                        amount, isReasonMissing ? "[NÃO ESPECIFICADO]" : reason);
                String rec = isReasonMissing
                        ? "Exigir abertura de inquérito imediato para justificar cancelamento sem motivo e validar caixa físico."
                        : "Reconciliar duplicado do documento anulado e confirmar estorno no sistema de faturação e caixa.";

                out.add(new ForensicAnomalyDTO(inv.getId(), eventTime, "Operador",
                        ForensicCategory.DOC_CANCELLATION, ForensicCategory.DOC_CANCELLATION.getLabel(),
                        sev, docRef, amount, details, reason != null ? reason : "Sem justificação", rec));
            } else if (inv.getStatus() == InvoiceStatus.APPROVED && inv.getLines() != null) {
                for (InvoiceLine line : inv.getLines()) {
                    BigDecimal discPct = line.getDiscountPercentage() != null ? line.getDiscountPercentage() : BigDecimal.ZERO;
                    if (discPct.compareTo(new BigDecimal("10.00")) > 0) {
                        BigDecimal discVal = line.getUnitPrice().multiply(line.getQuantity())
                                .multiply(discPct).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                        ForensicSeverity sev = discVal.compareTo(new BigDecimal("3000.00")) > 0
                                ? ForensicSeverity.CRITICAL
                                : ForensicSeverity.SUSPICIOUS;
                        String prodName = line.getProduct() != null ? line.getProduct().getName() : "Item";
                        String details = String.format(DisplayLocale.NUMBERS, "Desconto manual de %.1f%% concedido no item '%s' (Total desconto: %,.2f MT).",
                                discPct.doubleValue(), prodName, discVal);

                        out.add(new ForensicAnomalyDTO(inv.getId(), eventTime, "Comercial",
                                ForensicCategory.EXCESSIVE_DISCOUNT, ForensicCategory.EXCESSIVE_DISCOUNT.getLabel(),
                                sev, docRef, discVal, details, "Desconto manual em linha de venda",
                                "Confrontar autorização de desconto com a tabela de alçadas da direção comercial."));
                    }
                }
            }
        }
    }

    private void collectStockWasteAnomalies(Long companyId, List<ForensicAnomalyDTO> out) {
        List<StockWaste> wasteRecords = companyId != null
                ? stockWasteRepository.findByCompanyIdOrderByCreatedAtDesc(companyId)
                : stockWasteRepository.findAll();

        for (StockWaste waste : wasteRecords) {
            LocalDateTime eventTime = waste.getCreatedAt() != null ? waste.getCreatedAt() : LocalDateTime.now();
            BigDecimal cost = waste.getTotalCost() != null ? waste.getTotalCost() : BigDecimal.ZERO;
            ForensicSeverity sev = cost.compareTo(new BigDecimal("10000.00")) > 0
                    ? ForensicSeverity.CRITICAL
                    : (cost.compareTo(new BigDecimal("3000.00")) > 0 ? ForensicSeverity.SUSPICIOUS : ForensicSeverity.INFO);
            String prodName = waste.getProduct() != null ? waste.getProduct().getName() : "Artigo";
            String reasonDesc = waste.getReason() != null ? waste.getReason().name() : "N/D";
            String details = String.format(DisplayLocale.NUMBERS, "Perda/Quebra de stock no montante de %,.2f MT no artigo '%s' (Motivo: %s).",
                    cost, prodName, reasonDesc);

            out.add(new ForensicAnomalyDTO(waste.getId(), eventTime,
                    waste.getRegisteredBy() != null ? waste.getRegisteredBy() : "Armazém",
                    ForensicCategory.STOCK_SHRINKAGE, ForensicCategory.STOCK_SHRINKAGE.getLabel(),
                    sev, "QB-" + waste.getId(), cost, details,
                    waste.getNotes() != null ? waste.getNotes() : "Registo de quebra de inventário",
                    "Realizar contagem física cega de controlo e apurar termo de avaria/desvio no armazém."));
        }
    }

    private void collectAuditSecurityAnomalies(Long companyId, List<ForensicAnomalyDTO> out) {
        List<AuditLog> logs = companyId != null
                ? auditLogRepository.findByCompanyIdOrderByEventTimeDesc(companyId)
                : auditLogRepository.findByOrderByEventTimeDesc();

        for (AuditLog log : logs) {
            String act = log.getAction() != null ? log.getAction().toUpperCase() : "";
            if (act.contains("CANCEL") || act.contains("VOID") || act.contains("DELETE") || act.contains("OVERRIDE")) {
                out.add(new ForensicAnomalyDTO(log.getId(),
                        log.getEventTime() != null ? log.getEventTime() : LocalDateTime.now(),
                        log.getUsername() != null ? log.getUsername() : "SYSTEM",
                        ForensicCategory.AUDIT_SECURITY, ForensicCategory.AUDIT_SECURITY.getLabel(),
                        ForensicSeverity.INFO, "LOG-" + log.getId(), BigDecimal.ZERO,
                        "Acção sensível auditada: " + log.getAction() + " — " + (log.getDetails() != null ? log.getDetails() : ""),
                        "Registo de auditoria geral",
                        "Verificar conformidade com política de acessos privilegiados."));
            }
        }
    }
}
