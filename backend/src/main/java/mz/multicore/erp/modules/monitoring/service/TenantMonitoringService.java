package mz.multicore.erp.modules.monitoring.service;

import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.dto.ForensicAuditSummaryDTO;
import mz.multicore.erp.modules.audit.service.ForensicAuditService;
import mz.multicore.erp.modules.backup.service.ScheduledBackupService;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.monitoring.dto.TenantHealthDTO;
import mz.multicore.erp.modules.users.repository.AppUserCompanyAccessRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Serviço de observabilidade e monitoramento multi-tenant do Multicore ERP.
 * Consolida o estado de cada empresa/inquilino (backups, anomalias forenses e utilizadores ativos).
 */
@Service
public class TenantMonitoringService {

    private static final Logger log = LoggerFactory.getLogger(TenantMonitoringService.class);

    private final CompanyRepository companyRepository;
    private final AppUserCompanyAccessRepository companyAccessRepository;
    private final ForensicAuditService forensicAuditService;
    private final ScheduledBackupService scheduledBackupService;
    private final SystemIncidentManager systemIncidentManager;
    private final SystemAlertEmailService systemAlertEmailService;

    public TenantMonitoringService(
            CompanyRepository companyRepository,
            AppUserCompanyAccessRepository companyAccessRepository,
            ForensicAuditService forensicAuditService,
            ScheduledBackupService scheduledBackupService,
            SystemIncidentManager systemIncidentManager,
            SystemAlertEmailService systemAlertEmailService
    ) {
        this.companyRepository = companyRepository;
        this.companyAccessRepository = companyAccessRepository;
        this.forensicAuditService = forensicAuditService;
        this.scheduledBackupService = scheduledBackupService;
        this.systemIncidentManager = systemIncidentManager;
        this.systemAlertEmailService = systemAlertEmailService;
    }

    @Transactional(readOnly = true)
    public List<TenantHealthDTO> getTenantsHealth() {
        PermissionGuard.requireMonitoringAdmin("consultar estado operacional multi-tenant");

        List<Company> companies = PermissionGuard.SUPERADMIN_ROLE.equalsIgnoreCase(
                mz.multicore.erp.architecture.security.CurrentUserContext.getRole())
                ? companyRepository.findAll()
                : companyRepository.findById(
                        mz.multicore.erp.architecture.security.CurrentUserContext.requireCurrentCompanyId())
                        .map(List::of).orElseGet(List::of);
        List<TenantHealthDTO> result = new ArrayList<>();
        LocalDate now = LocalDate.now();
        LocalDate thirtyDaysAgo = now.minusDays(30);

        String globalBackupStatus = "NONE";
        ScheduledBackupService.LastRun lastRun = scheduledBackupService.getLastRun();
        if (lastRun != null) {
            globalBackupStatus = lastRun.success() ? "UP_TO_DATE" : "FAILED";
        }

        for (Company company : companies) {
            long userCount = companyAccessRepository.countByCompanyId(company.getId());

            int criticalAnomalies = 0;
            int totalAnomalies = 0;
            try {
                ForensicAuditSummaryDTO summary = forensicAuditService.getForensicSummaryForCompany(
                        company.getId(), thirtyDaysAgo, now, null, null, null
                );
                if (summary != null) {
                    criticalAnomalies = summary.criticalCount();
                    totalAnomalies = summary.totalAnomalies();
                }
            } catch (Exception ex) {
                log.debug("Erro ao recolher auditoria forense para empresa {}: {}", company.getId(), ex.getMessage());
            }

            String tenantBackupStatus = globalBackupStatus;
            String status;

            if (!company.isActive()) {
                status = "WARNING";
            } else if (criticalAnomalies > 0 || "FAILED".equals(tenantBackupStatus)) {
                status = "CRITICAL";
                String alertMsg = String.format("Empresa '%s' com %d anomalias críticas e estado de backup '%s'.",
                        company.getName(), criticalAnomalies, tenantBackupStatus);
                systemIncidentManager.recordIncident("MultiTenant", company.getName(), "CRITICAL", alertMsg);
                systemAlertEmailService.sendCriticalAlert("MultiTenant", company.getName(), alertMsg, null);
            } else if (totalAnomalies > 0) {
                status = "WARNING";
            } else {
                status = "HEALTHY";
            }

            result.add(new TenantHealthDTO(
                    company.getId(),
                    company.getName(),
                    company.getTaxId(),
                    status,
                    userCount,
                    tenantBackupStatus,
                    totalAnomalies,
                    Instant.now()
            ));
        }

        result.sort(Comparator.comparing(TenantHealthDTO::status)
                .thenComparing(TenantHealthDTO::companyName, String.CASE_INSENSITIVE_ORDER));
        return result;
    }
}
