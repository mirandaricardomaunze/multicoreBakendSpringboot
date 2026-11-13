package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.security.AttachmentCrypto;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.hr.dto.*;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.OccupationalHealthExamRepository;
import mz.multicore.erp.modules.purchases.service.SupplierLookup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Orquestrador de Saúde Ocupacional: aptidão médica, prestador, custo e conformidade.
 * Decomposto na Fase 109 em colaboradores especializados para conformidade estrita com SOLID/SRP.
 * Ver docs/OCCUPATIONAL_HEALTH_SERVICE_DECOMPOSITION_SPEC.md.
 */
@Service
public class OccupationalHealthService {
    public static final int ALERT_DAYS = 60;

    private final OccupationalHealthRegistrationService registrationService;
    private final OccupationalHealthFinanceService financeService;
    private final OccupationalHealthQueryService queryService;

    @Autowired
    public OccupationalHealthService(OccupationalHealthRegistrationService registrationService,
                                     OccupationalHealthFinanceService financeService,
                                     OccupationalHealthQueryService queryService) {
        this.registrationService = registrationService;
        this.financeService = financeService;
        this.queryService = queryService;
    }

    /**
     * Construtor retrocompatível preservado para compatibilidade integral com testes legados.
     */
    public OccupationalHealthService(OccupationalHealthExamRepository repository,
                                     EmployeeRepository employeeRepository,
                                     CompanyLookup companyLookup,
                                     SupplierLookup supplierLookup,
                                     @Lazy FinanceService financeService,
                                     AttachmentCrypto attachmentCrypto,
                                     AuditLogService auditLogService) {
        this(new OccupationalHealthRegistrationService(repository, employeeRepository, companyLookup,
                        supplierLookup, attachmentCrypto, auditLogService),
                new OccupationalHealthFinanceService(repository, supplierLookup, financeService, auditLogService),
                new OccupationalHealthQueryService(repository, employeeRepository, attachmentCrypto, auditLogService));
    }

    @Transactional(readOnly = true)
    public OccupationalHealthSummaryDTO summary(Long employeeId) {
        return queryService.summary(employeeId);
    }

    @Transactional
    public List<OccupationalHealthExamDTO> history(Long employeeId) {
        return queryService.history(employeeId);
    }

    @Transactional
    public OccupationalHealthAttachmentDTO attachment(Long examId) {
        return queryService.attachment(examId);
    }

    @Transactional(readOnly = true)
    public List<OccupationalHealthExamDTO> expiring() {
        return queryService.expiring();
    }

    @Transactional(readOnly = true)
    public List<MissingHealthExamDTO> missingExams() {
        return queryService.missingExams();
    }

    @Transactional(readOnly = true)
    public List<HealthProviderDTO> providers() {
        return financeService.providers();
    }

    @Transactional
    public OccupationalHealthExamDTO register(SaveOccupationalHealthExamRequest request) {
        return registrationService.register(request);
    }

    @Transactional
    public OccupationalHealthExamDTO payExam(Long examId) {
        return financeService.payExam(examId);
    }

    @Transactional(readOnly = true)
    public OccupationalHealthCostDTO costReport(LocalDate from, LocalDate to) {
        return financeService.costReport(from, to);
    }
}
