package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.hr.dto.HealthProviderDTO;
import mz.multicore.erp.modules.hr.dto.OccupationalHealthCostDTO;
import mz.multicore.erp.modules.hr.dto.OccupationalHealthExamDTO;
import mz.multicore.erp.modules.hr.dto.OccupationalHealthProviderCostDTO;
import mz.multicore.erp.modules.hr.model.OccupationalHealthExam;
import mz.multicore.erp.modules.hr.repository.OccupationalHealthExamRepository;
import mz.multicore.erp.modules.purchases.model.Supplier;
import mz.multicore.erp.modules.purchases.service.SupplierLookup;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static mz.multicore.erp.modules.hr.service.OccupationalHealthMapper.describeProvider;
import static mz.multicore.erp.modules.hr.service.OccupationalHealthMapper.toDTO;

@Service
public class OccupationalHealthFinanceService {
    private final OccupationalHealthExamRepository repository;
    private final SupplierLookup supplierLookup;
    private final FinanceService financeService;
    private final AuditLogService auditLogService;

    public OccupationalHealthFinanceService(OccupationalHealthExamRepository repository,
                                            SupplierLookup supplierLookup,
                                            @Lazy FinanceService financeService,
                                            AuditLogService auditLogService) {
        this.repository = repository;
        this.supplierLookup = supplierLookup;
        this.financeService = financeService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<HealthProviderDTO> providers() {
        PermissionGuard.requireManagerOrAdmin("consultar prestadores de saúde ocupacional");
        return supplierLookup.listActiveForCompany(companyId()).stream()
                .sorted(Comparator.comparing(Supplier::getName, String.CASE_INSENSITIVE_ORDER))
                .map(supplier -> new HealthProviderDTO(supplier.getId(), supplier.getName(), supplier.getTaxId()))
                .toList();
    }

    @Transactional
    public OccupationalHealthExamDTO payExam(Long examId) {
        PermissionGuard.requireManagerOrAdmin("pagar exames de saúde ocupacional");
        OccupationalHealthExam exam = repository.findByIdAndCompanyId(examId, companyId())
                .orElseThrow(() -> new BusinessRuleException("Exame não encontrado na empresa activa."));
        if (!exam.isPayable()) {
            throw new BusinessRuleException(
                    "Este exame não tem custo registado. Indique o valor da factura da clínica antes de pagar.");
        }
        if (exam.isPaid()) {
            throw new BusinessRuleException(String.format(
                    "Este exame já foi pago em %s.", exam.getPaidAt()));
        }
        financeService.registerAutoPayout(exam.getCost(), String.format(
                "Exame de saúde ocupacional de %s — %s", exam.getEmployee().getName(), describeProvider(exam)), mz.multicore.erp.architecture.events.PayoutPurpose.EXTERNAL_SERVICE);
        exam.setPaidAt(LocalDate.now());
        OccupationalHealthExam saved = repository.save(exam);
        auditLogService.logCurrent("OCCUPATIONAL_HEALTH_EXAM_PAID", String.format(
                "Exame ocupacional de %s pago: %s a %s%s", saved.getEmployee().getName(),
                saved.getCost(), describeProvider(saved),
                saved.getInvoiceNumber() == null ? "" : " (factura " + saved.getInvoiceNumber() + ")"));
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public OccupationalHealthCostDTO costReport(LocalDate from, LocalDate to) {
        PermissionGuard.requireManagerOrAdmin("consultar custos de saúde ocupacional");
        LocalDate start = from != null ? from : LocalDate.now().withDayOfYear(1);
        LocalDate end = to != null ? to : LocalDate.now();
        if (end.isBefore(start)) {
            throw new BusinessRuleException("A data final não pode ser anterior à data inicial.");
        }
        List<OccupationalHealthExam> exams = repository.findByPeriod(companyId(), start, end).stream()
                .filter(OccupationalHealthExam::isPayable).toList();

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal paid = BigDecimal.ZERO;
        Map<String, OccupationalHealthProviderCostDTO> byProvider = new LinkedHashMap<>();
        for (OccupationalHealthExam exam : exams) {
            total = total.add(exam.getCost());
            if (exam.isPaid()) paid = paid.add(exam.getCost());
            String label = describeProvider(exam);
            Long providerId = exam.getProvider() == null ? null : exam.getProvider().getId();
            OccupationalHealthProviderCostDTO line = byProvider.get(label);
            BigDecimal pendingHere = exam.isPaid() ? BigDecimal.ZERO : exam.getCost();
            byProvider.put(label, line == null
                    ? new OccupationalHealthProviderCostDTO(providerId, label, 1, exam.getCost(), pendingHere)
                    : new OccupationalHealthProviderCostDTO(line.providerId(), label, line.examCount() + 1,
                            line.total().add(exam.getCost()), line.pending().add(pendingHere)));
        }
        List<OccupationalHealthProviderCostDTO> lines = new ArrayList<>(byProvider.values());
        lines.sort(Comparator.comparing(OccupationalHealthProviderCostDTO::total).reversed());
        return new OccupationalHealthCostDTO(start, end, exams.size(), total, paid, total.subtract(paid), lines);
    }

    private Long companyId() {
        return CurrentUserContext.requireCurrentCompanyId();
    }
}
