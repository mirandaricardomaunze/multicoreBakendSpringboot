package mz.multicore.erp.modules.audit.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.dto.ForensicAuditSummaryDTO;
import mz.multicore.erp.modules.audit.dto.ForensicCategory;
import mz.multicore.erp.modules.audit.dto.ForensicSeverity;
import mz.multicore.erp.modules.audit.repository.AuditLogRepository;
import mz.multicore.erp.modules.comercial.service.InvoiceLookup;
import mz.multicore.erp.modules.inventory.service.StockWasteLookup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/** Orquestrador tenant-scoped de auditoria forense e detecção de anomalias operacionais. */
@Service
public class ForensicAuditService {

    private final ForensicAnomalyCollector anomalyCollector;
    private final ForensicSummaryEvaluator summaryEvaluator;

    @Autowired
    public ForensicAuditService(ForensicAnomalyCollector anomalyCollector,
                                ForensicSummaryEvaluator summaryEvaluator) {
        this.anomalyCollector = anomalyCollector;
        this.summaryEvaluator = summaryEvaluator;
    }

    /** Construtor retrocompatível preservado para testes e integrações existentes. */
    public ForensicAuditService(InvoiceLookup invoiceLookup,
                                StockWasteLookup stockWasteLookup,
                                AuditLogRepository auditLogRepository) {
        this(new ForensicAnomalyCollector(invoiceLookup, stockWasteLookup, auditLogRepository),
                new ForensicSummaryEvaluator());
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
        return summaryEvaluator.evaluate(
                anomalyCollector.collectAll(companyId),
                startDate, endDate, severityFilter, categoryFilter, operatorFilter
        );
    }
}
