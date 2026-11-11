package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.AttachmentCrypto;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.service.CompanyLookup;
import mz.multicore.erp.modules.hr.dto.OccupationalHealthExamDTO;
import mz.multicore.erp.modules.hr.dto.SaveOccupationalHealthExamRequest;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.OccupationalHealthExam;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.OccupationalHealthExamRepository;
import mz.multicore.erp.modules.purchases.model.Supplier;
import mz.multicore.erp.modules.purchases.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import static mz.multicore.erp.modules.hr.service.OccupationalHealthMapper.blank;
import static mz.multicore.erp.modules.hr.service.OccupationalHealthMapper.describeProvider;
import static mz.multicore.erp.modules.hr.service.OccupationalHealthMapper.toDTO;

@Service
public class OccupationalHealthRegistrationService {
    private static final Set<String> RESULTS = Set.of("FIT", "FIT_WITH_RESTRICTIONS", "UNFIT");

    private static final Pattern PROHIBITED_HEALTH_DATA = Pattern.compile(
            "\\b(hiv|sida|vih|aids|cd4)\\b|seropositiv|soropositiv|serolog|carga\\s*viral",
            Pattern.CASE_INSENSITIVE);

    private final OccupationalHealthExamRepository repository;
    private final EmployeeRepository employeeRepository;
    private final CompanyLookup companyLookup;
    private final SupplierRepository supplierRepository;
    private final AttachmentCrypto attachmentCrypto;
    private final AuditLogService auditLogService;

    public OccupationalHealthRegistrationService(OccupationalHealthExamRepository repository,
                                                 EmployeeRepository employeeRepository,
                                                 CompanyLookup companyLookup,
                                                 SupplierRepository supplierRepository,
                                                 AttachmentCrypto attachmentCrypto,
                                                 AuditLogService auditLogService) {
        this.repository = repository;
        this.employeeRepository = employeeRepository;
        this.companyLookup = companyLookup;
        this.supplierRepository = supplierRepository;
        this.attachmentCrypto = attachmentCrypto;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public OccupationalHealthExamDTO register(SaveOccupationalHealthExamRequest request) {
        PermissionGuard.requireManagerOrAdmin("registar exames de saúde ocupacional");
        String result = request.fitnessResult().trim().toUpperCase();
        if (!RESULTS.contains(result)) throw new BusinessRuleException("Resultado de aptidão inválido.");
        if (request.expiryDate().isBefore(request.examDate())) {
            throw new BusinessRuleException("A validade não pode ser anterior à data do exame.");
        }
        if ("FIT_WITH_RESTRICTIONS".equals(result)
                && (request.restrictions() == null || request.restrictions().isBlank())) {
            throw new BusinessRuleException("Indique as restrições aplicáveis ao trabalhador.");
        }
        rejectProhibitedHealthData(request.restrictions(), request.notes(), request.cardNumber());
        BigDecimal cost = validateCost(request);

        Employee employee = verifyEmployee(request.employeeId());
        OccupationalHealthExam exam = new OccupationalHealthExam();
        exam.setCompany(companyLookup.requireActive(companyId()));
        exam.setEmployee(employee);
        exam.setCardNumber(blank(request.cardNumber()));
        exam.setExamDate(request.examDate());
        exam.setExpiryDate(request.expiryDate());
        exam.setFitnessResult(result);
        exam.setProvider(resolveProvider(request.providerId()));
        exam.setClinic(blank(request.clinic()));
        exam.setDoctorName(blank(request.doctorName()));
        exam.setRestrictions(blank(request.restrictions()));
        exam.setNotes(blank(request.notes()));
        exam.setCost(cost);
        exam.setInvoiceNumber(blank(request.invoiceNumber()));
        exam.setAttachmentName(blank(request.attachmentName()));
        exam.setAttachmentData(attachmentCrypto.protect(request.attachmentData()));
        OccupationalHealthExam saved = repository.save(exam);
        auditLogService.logCurrent("OCCUPATIONAL_HEALTH_EXAM_REGISTER", String.format(
                "Exame ocupacional de %s registado: %s, válido até %s%s",
                employee.getName(), result, saved.getExpiryDate(),
                cost == null ? "" : ", custo " + cost + " em " + describeProvider(saved)));
        return toDTO(saved);
    }

    private void rejectProhibitedHealthData(String... fields) {
        for (String field : fields) {
            if (field == null || field.isBlank()) continue;
            String plain = Normalizer.normalize(field, Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
            if (PROHIBITED_HEALTH_DATA.matcher(plain).find()) {
                throw new BusinessRuleException(
                        "A lei proíbe apurar e registar o estado serológico do trabalhador (HIV/SIDA). "
                                + "O exame de aptidão só regista se a pessoa está apta, com que restrições "
                                + "de função, e nada mais. Reformule sem referência a diagnóstico.");
            }
        }
    }

    private BigDecimal validateCost(SaveOccupationalHealthExamRequest request) {
        BigDecimal cost = request.cost();
        if (cost != null && cost.signum() < 0) {
            throw new BusinessRuleException("O custo do exame não pode ser negativo.");
        }
        if (cost != null && cost.signum() == 0) cost = null;
        if (cost == null && blank(request.invoiceNumber()) != null) {
            throw new BusinessRuleException(
                    "Indicou o número da factura mas não o valor do exame. Registe o custo da factura.");
        }
        return cost;
    }

    private Supplier resolveProvider(Long providerId) {
        if (providerId == null) return null;
        Supplier provider = supplierRepository.findById(providerId)
                .filter(candidate -> candidate.getCompany() != null
                        && companyId().equals(candidate.getCompany().getId()))
                .orElseThrow(() -> new BusinessRuleException(
                        "Prestador não encontrado no cadastro de fornecedores da empresa activa."));
        if (!provider.isActive()) {
            throw new BusinessRuleException(String.format(
                    "O prestador %s está inactivo no cadastro. Reactive-o antes de lhe atribuir exames.",
                    provider.getName()));
        }
        return provider;
    }

    private Employee verifyEmployee(Long employeeId) {
        return employeeRepository.findByIdAndCompanyId(employeeId, companyId())
                .orElseThrow(() -> new BusinessRuleException("Trabalhador não encontrado na empresa activa."));
    }

    private Long companyId() {
        return CurrentUserContext.requireCurrentCompanyId();
    }
}
