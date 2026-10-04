package mz.multicore.erp.modules.audit.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.dto.ForensicAnomalyDTO;
import mz.multicore.erp.modules.audit.dto.ForensicAuditSummaryDTO;
import mz.multicore.erp.modules.audit.dto.ForensicCategory;
import mz.multicore.erp.modules.audit.dto.ForensicSeverity;
import mz.multicore.erp.modules.audit.model.AuditLog;
import mz.multicore.erp.modules.audit.repository.AuditLogRepository;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.InvoiceLine;
import mz.multicore.erp.modules.comercial.model.InvoiceStatus;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.inventory.model.StockWaste;
import mz.multicore.erp.modules.inventory.repository.StockWasteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ForensicAuditService {

    private final InvoiceRepository invoiceRepository;
    private final StockWasteRepository stockWasteRepository;
    private final AuditLogRepository auditLogRepository;

    public ForensicAuditService(
            InvoiceRepository invoiceRepository,
            StockWasteRepository stockWasteRepository,
            AuditLogRepository auditLogRepository
    ) {
        this.invoiceRepository = invoiceRepository;
        this.stockWasteRepository = stockWasteRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public ForensicAuditSummaryDTO getForensicSummary(
            LocalDate startDate,
            LocalDate endDate,
            ForensicSeverity severityFilter,
            ForensicCategory categoryFilter,
            String operatorFilter
    ) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        return getForensicSummaryForCompany(companyId, startDate, endDate, severityFilter, categoryFilter, operatorFilter);
    }

    @Transactional(readOnly = true)
    public ForensicAuditSummaryDTO getForensicSummaryForCompany(
            Long companyId,
            LocalDate startDate,
            LocalDate endDate,
            ForensicSeverity severityFilter,
            ForensicCategory categoryFilter,
            String operatorFilter
    ) {
        if (PermissionGuard.SUPERADMIN_ROLE.equalsIgnoreCase(CurrentUserContext.getRole())) {
            if (companyId == null) {
                throw new BusinessRuleException("Seleccione uma empresa para consultar a auditoria forense.");
            }
        } else {
            PermissionGuard.requireManagerOrAdmin("consultar auditoria forense e controlo de fraude");
            CurrentUserContext.requireCompany(companyId);
        }
        List<ForensicAnomalyDTO> allAnomalies = new ArrayList<>();

        // 1. Inspecção de Cancelamentos de Faturas e Recibos
        List<Invoice> invoices = companyId != null
                ? invoiceRepository.findByCompanyId(companyId)
                : invoiceRepository.findAll();

        for (Invoice inv : invoices) {
            LocalDateTime eventTime = inv.getUpdatedAt() != null ? inv.getUpdatedAt() : (inv.getCreatedAt() != null ? inv.getCreatedAt() : LocalDateTime.now());

            if (inv.getStatus() == InvoiceStatus.CANCELLED) {
                BigDecimal amount = inv.getTotalAmount() != null ? inv.getTotalAmount() : BigDecimal.ZERO;
                String reason = inv.getCancellationReason();
                boolean isReasonMissing = (reason == null || reason.trim().isEmpty());

                // AFF-01: Crítico se > 5.000 MT ou sem motivo expresso
                ForensicSeverity sev = (amount.compareTo(new BigDecimal("5000.00")) > 0 || isReasonMissing)
                        ? ForensicSeverity.CRITICAL
                        : ForensicSeverity.SUSPICIOUS;

                String details = String.format("Fatura fiscal cancelada no valor de %,.2f MT. Motivo registado: %s.",
                        amount, isReasonMissing ? "[NÃO ESPECIFICADO]" : reason);
                String rec = isReasonMissing
                        ? "Exigir abertura de inquérito imediato para justificar cancelamento sem motivo e validar caixa físico."
                        : "Reconciliar duplicado do documento anulado e confirmar estorno no sistema de faturação e caixa.";

                allAnomalies.add(new ForensicAnomalyDTO(
                        inv.getId(),
                        eventTime,
                        "Operador",
                        ForensicCategory.DOC_CANCELLATION,
                        ForensicCategory.DOC_CANCELLATION.getLabel(),
                        sev,
                        inv.getInvoiceNumber() != null ? inv.getInvoiceNumber() : "FT-" + inv.getId(),
                        amount,
                        details,
                        reason != null ? reason : "Sem justificação",
                        rec
                ));
            } else if (inv.getStatus() == InvoiceStatus.APPROVED && inv.getLines() != null) {
                // AFF-02: Deteção de Descontos Excessivos (>10%)
                for (InvoiceLine line : inv.getLines()) {
                    BigDecimal discPct = line.getDiscountPercentage() != null ? line.getDiscountPercentage() : BigDecimal.ZERO;
                    if (discPct.compareTo(new BigDecimal("10.00")) > 0) {
                        BigDecimal discVal = line.getUnitPrice().multiply(line.getQuantity()).multiply(discPct).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                        ForensicSeverity sev = discVal.compareTo(new BigDecimal("3000.00")) > 0
                                ? ForensicSeverity.CRITICAL
                                : ForensicSeverity.SUSPICIOUS;

                        String prodName = line.getProduct() != null ? line.getProduct().getName() : "Item";
                        String details = String.format("Desconto manual de %.1f%% concedido no item '%s' (Total desconto: %,.2f MT).",
                                discPct.doubleValue(), prodName, discVal);

                        allAnomalies.add(new ForensicAnomalyDTO(
                                inv.getId(),
                                eventTime,
                                "Comercial",
                                ForensicCategory.EXCESSIVE_DISCOUNT,
                                ForensicCategory.EXCESSIVE_DISCOUNT.getLabel(),
                                sev,
                                inv.getInvoiceNumber() != null ? inv.getInvoiceNumber() : "FT-" + inv.getId(),
                                discVal,
                                details,
                                "Desconto manual em linha de venda",
                                "Confrontar autorização de desconto com a tabela de alçadas da direção comercial."
                        ));
                    }
                }
            }
        }

        // 2. Inspecção de Quebras de Stock Anormais
        List<StockWaste> wasteRecords = companyId != null
                ? stockWasteRepository.findByCompanyIdOrderByCreatedAtDesc(companyId)
                : stockWasteRepository.findAll();

        for (StockWaste waste : wasteRecords) {
            LocalDateTime eventTime = waste.getCreatedAt() != null ? waste.getCreatedAt() : LocalDateTime.now();
            BigDecimal cost = waste.getTotalCost() != null ? waste.getTotalCost() : BigDecimal.ZERO;

            // AFF-03: > 10.000 MT = CRITICAL; 3.000 a 10.000 MT = SUSPICIOUS; <= 3.000 = INFO
            ForensicSeverity sev;
            if (cost.compareTo(new BigDecimal("10000.00")) > 0) {
                sev = ForensicSeverity.CRITICAL;
            } else if (cost.compareTo(new BigDecimal("3000.00")) > 0) {
                sev = ForensicSeverity.SUSPICIOUS;
            } else {
                sev = ForensicSeverity.INFO;
            }

            String prodName = waste.getProduct() != null ? waste.getProduct().getName() : "Artigo";
            String reasonDesc = waste.getReason() != null ? waste.getReason().name() : "N/D";
            String details = String.format("Perda/Quebra de stock no montante de %,.2f MT no artigo '%s' (Motivo: %s).",
                    cost, prodName, reasonDesc);

            allAnomalies.add(new ForensicAnomalyDTO(
                    waste.getId(),
                    eventTime,
                    waste.getRegisteredBy() != null ? waste.getRegisteredBy() : "Armazém",
                    ForensicCategory.STOCK_SHRINKAGE,
                    ForensicCategory.STOCK_SHRINKAGE.getLabel(),
                    sev,
                    "QB-" + waste.getId(),
                    cost,
                    details,
                    waste.getNotes() != null ? waste.getNotes() : "Registo de quebra de inventário",
                    "Realizar contagem física cega de controlo e apurar termo de avaria/desvio no armazém."
            ));
        }

        // 3. Inspecção de Logs de Auditoria do Sistema
        List<AuditLog> logs = companyId != null
                ? auditLogRepository.findByCompanyIdOrderByEventTimeDesc(companyId)
                : auditLogRepository.findByOrderByEventTimeDesc();

        for (AuditLog log : logs) {
            String act = log.getAction() != null ? log.getAction().toUpperCase() : "";
            if (act.contains("CANCEL") || act.contains("VOID") || act.contains("DELETE") || act.contains("OVERRIDE")) {
                allAnomalies.add(new ForensicAnomalyDTO(
                        log.getId(),
                        log.getEventTime() != null ? log.getEventTime() : LocalDateTime.now(),
                        log.getUsername() != null ? log.getUsername() : "SYSTEM",
                        ForensicCategory.AUDIT_SECURITY,
                        ForensicCategory.AUDIT_SECURITY.getLabel(),
                        ForensicSeverity.INFO,
                        "LOG-" + log.getId(),
                        BigDecimal.ZERO,
                        "Acção sensível auditada: " + log.getAction() + " — " + (log.getDetails() != null ? log.getDetails() : ""),
                        "Registo de auditoria geral",
                        "Verificar conformidade com política de acessos privilegiados."
                ));
            }
        }

        // 4. Aplicação de Filtros
        List<ForensicAnomalyDTO> filtered = allAnomalies.stream()
                .filter(a -> {
                    if (startDate != null && a.timestamp().toLocalDate().isBefore(startDate)) return false;
                    if (endDate != null && a.timestamp().toLocalDate().isAfter(endDate)) return false;
                    if (severityFilter != null && a.severity() != severityFilter) return false;
                    if (categoryFilter != null && a.category() != categoryFilter) return false;
                    if (operatorFilter != null && !operatorFilter.isBlank() &&
                            !a.operator().toLowerCase().contains(operatorFilter.toLowerCase())) return false;
                    return true;
                })
                .sorted(Comparator.comparing(ForensicAnomalyDTO::timestamp).reversed())
                .collect(Collectors.toList());

        // 5. Agregações e Métricas
        int criticalCount = 0;
        int suspiciousCount = 0;
        int infoCount = 0;
        BigDecimal totalFinancialRisk = BigDecimal.ZERO;

        for (ForensicAnomalyDTO a : filtered) {
            if (a.severity() == ForensicSeverity.CRITICAL) {
                criticalCount++;
                totalFinancialRisk = totalFinancialRisk.add(a.financialImpact());
            } else if (a.severity() == ForensicSeverity.SUSPICIOUS) {
                suspiciousCount++;
                totalFinancialRisk = totalFinancialRisk.add(a.financialImpact());
            } else {
                infoCount++;
            }
        }

        // 6. Índice de Conformidade (AFF-05)
        String complianceScore;
        if (totalFinancialRisk.compareTo(BigDecimal.ZERO) == 0) {
            complianceScore = "100% — Excelente / Sem Desvios";
        } else if (totalFinancialRisk.compareTo(new BigDecimal("10000.00")) <= 0) {
            complianceScore = "88% — Bom / Baixo Risco";
        } else if (totalFinancialRisk.compareTo(new BigDecimal("50000.00")) <= 0) {
            complianceScore = "68% — Atenção / Risco Moderado";
        } else {
            complianceScore = "42% — Crítico / Elevada Exposição";
        }

        return new ForensicAuditSummaryDTO(
                filtered.size(),
                criticalCount,
                suspiciousCount,
                infoCount,
                totalFinancialRisk,
                complianceScore,
                filtered
        );
    }
}
