package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.architecture.security.PermissionGuard;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.financeira.service.FinanceService;
import mz.multicore.erp.modules.hr.dto.*;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.OccupationalHealthExam;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.OccupationalHealthExamRepository;
import mz.multicore.erp.modules.purchases.model.Supplier;
import mz.multicore.erp.modules.purchases.repository.SupplierRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Saúde ocupacional: aptidão médica, prestador, custo e conformidade.
 * Ver docs/CONFORMIDADE_LEGAL_MZ_SPEC.md.
 *
 * <p><b>Este serviço guarda dados de saúde de pessoas identificadas</b>, e é essa a razão de quase
 * todas as regras que aqui estão. A Lei n.º 13/2023 permite ao empregador exigir exames de aptidão,
 * mas fecha o que ele pode <i>saber</i> do resultado: o médico não pode comunicar-lhe nada além da
 * capacidade ou incapacidade para o trabalho. O sistema é onde essa fronteira ou se cumpre ou se
 * perde — um campo de observações sem regra é um campo de diagnóstico à espera de acontecer.
 */
@Service
public class OccupationalHealthService {
    public static final int ALERT_DAYS = 60;
    private static final Set<String> RESULTS = Set.of("FIT", "FIT_WITH_RESTRICTIONS", "UNFIT");

    /**
     * O que <b>nunca</b> pode entrar num registo de aptidão. A Lei n.º 19/2014 e a Lei n.º 13/2023
     * proíbem expressamente apurar o estado de HIV/SIDA do trabalhador ou do candidato, e o dever
     * de sigilo do médico impede que um resultado serológico chegue sequer ao empregador.
     *
     * <p>A lista é curta de propósito: não tenta detectar "diagnóstico" — coisa que nenhuma
     * expressão regular sabe fazer — apura só o caso que a lei nomeia. Um falso positivo custa uma
     * reformulação da frase; o falso negativo custa dados que não podiam ter sido escritos.
     */
    private static final Pattern PROHIBITED_HEALTH_DATA = Pattern.compile(
            "\\b(hiv|sida|vih|aids|cd4)\\b|seropositiv|soropositiv|serolog|carga\\s*viral",
            Pattern.CASE_INSENSITIVE);

    private final OccupationalHealthExamRepository repository;
    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;
    private final SupplierRepository supplierRepository;
    private final FinanceService financeService;
    private final AuditLogService auditLogService;

    public OccupationalHealthService(OccupationalHealthExamRepository repository,
                                     EmployeeRepository employeeRepository,
                                     CompanyRepository companyRepository,
                                     SupplierRepository supplierRepository,
                                     @Lazy FinanceService financeService,
                                     AuditLogService auditLogService) {
        this.repository = repository;
        this.employeeRepository = employeeRepository;
        this.companyRepository = companyRepository;
        this.supplierRepository = supplierRepository;
        this.financeService = financeService;
        this.auditLogService = auditLogService;
    }

    // ─── Consulta ─────────────────────────────────────────────────────────────

    /**
     * Resumo de aptidão para a ficha do trabalhador.
     *
     * <p><b>Um resumo continua a ser um dado de saúde.</b> "Inapto" diz sobre a pessoa aquilo que a
     * lei quis que só o médico soubesse em detalhe — pelo que este resumo é do gestor e do próprio,
     * de mais ninguém. Sem esta guarda, qualquer conta autenticada da empresa lia o estado de
     * aptidão de qualquer colega trocando um número no endereço.
     */
    @Transactional(readOnly = true)
    public OccupationalHealthSummaryDTO summary(Long employeeId) {
        Employee employee = verifyEmployee(employeeId);
        ensureManagerOrSelf(employee, "consultar a aptidão médica de outro trabalhador");
        return repository.findFirstByCompanyIdAndEmployeeIdOrderByExamDateDescIdDesc(companyId(), employeeId)
                .map(this::toSummary)
                .orElse(new OccupationalHealthSummaryDTO(employeeId, false, null, null, null, null, "NOT_REGISTERED"));
    }

    /**
     * Histórico clínico ocupacional do trabalhador.
     *
     * <p><b>Não é {@code readOnly} de propósito:</b> ler dados de saúde deixa rasto. Numa transacção
     * só de leitura o registo de acesso seria criado e nunca escrito — o pior dos dois mundos, um
     * rasto que parece existir e não existe.
     */
    @Transactional
    public List<OccupationalHealthExamDTO> history(Long employeeId) {
        PermissionGuard.requireManagerOrAdmin("consultar dados de saúde ocupacional");
        Employee employee = verifyEmployee(employeeId);
        List<OccupationalHealthExamDTO> history =
                repository.findHistory(companyId(), employeeId).stream().map(this::toDTO).toList();
        auditLogService.logCurrent("OCCUPATIONAL_HEALTH_ACCESS", String.format(
                "%s consultou o historial de saúde ocupacional de %s (%d registo(s))",
                CurrentUserContext.getUsername(), employee.getName(), history.size()));
        return history;
    }

    @Transactional(readOnly = true)
    public List<OccupationalHealthExamDTO> expiring() {
        PermissionGuard.requireManagerOrAdmin("consultar alertas de saúde ocupacional");
        return repository.findLatestExpiring(companyId(), LocalDate.now().plusDays(ALERT_DAYS))
                .stream().map(this::toDTO).toList();
    }

    /**
     * Trabalhadores no activo que <b>nunca</b> fizeram exame de aptidão.
     *
     * <p>A lista de exames a caducar só conhece quem já fez exame. Quem nunca fez não caduca nunca,
     * logo nunca aparecia em aviso nenhum — e é o caso que a inspecção do trabalho encontra
     * primeiro. Mesma lição das obrigações sem prazo do §B5: a ausência tem de ter linha própria,
     * senão é a única coisa que desaparece.
     */
    @Transactional(readOnly = true)
    public List<MissingHealthExamDTO> missingExams() {
        PermissionGuard.requireManagerOrAdmin("consultar a conformidade de saúde ocupacional");
        Long companyId = companyId();
        Set<Long> withExam = new HashSet<>(repository.findEmployeeIdsWithExam(companyId));
        LocalDate today = LocalDate.now();
        return employeeRepository.findByCompanyIdOrderByName(companyId).stream()
                .filter(employee -> "ACTIVE".equalsIgnoreCase(employee.getStatus()))
                .filter(employee -> !withExam.contains(employee.getId()))
                .map(employee -> new MissingHealthExamDTO(employee.getId(), employee.getName(),
                        employee.getRole(), employee.getDepartment(), employee.getHireDate(),
                        employee.getHireDate() == null ? null
                                : ChronoUnit.DAYS.between(employee.getHireDate(), today)))
                .sorted(Comparator.comparing(MissingHealthExamDTO::daysSinceHire,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    /** Prestadores activos do cadastro de fornecedores, para escolher no registo do exame. */
    @Transactional(readOnly = true)
    public List<HealthProviderDTO> providers() {
        PermissionGuard.requireManagerOrAdmin("consultar prestadores de saúde ocupacional");
        return supplierRepository.findByCompanyIdAndActiveTrue(companyId()).stream()
                .sorted(Comparator.comparing(Supplier::getName, String.CASE_INSENSITIVE_ORDER))
                .map(supplier -> new HealthProviderDTO(supplier.getId(), supplier.getName(), supplier.getTaxId()))
                .toList();
    }

    // ─── Registo ──────────────────────────────────────────────────────────────

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
        exam.setCompany(companyRepository.findById(companyId())
                .orElseThrow(() -> new BusinessRuleException("Empresa activa não encontrada.")));
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
        exam.setAttachmentData(request.attachmentData());
        OccupationalHealthExam saved = repository.save(exam);
        auditLogService.logCurrent("OCCUPATIONAL_HEALTH_EXAM_REGISTER", String.format(
                "Exame ocupacional de %s registado: %s, válido até %s%s",
                employee.getName(), result, saved.getExpiryDate(),
                cost == null ? "" : ", custo " + cost + " em " + describeProvider(saved)));
        return toDTO(saved);
    }

    // ─── Custo ────────────────────────────────────────────────────────────────

    /**
     * Paga o exame à clínica: saída de tesouraria pela mesma porta do recibo e da retenção.
     *
     * <p>O encargo é do empregador — nunca do trabalhador — pelo que este pagamento sai da
     * tesouraria e <b>não</b> tem contrapartida em desconto na folha.
     */
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
                "Exame de saúde ocupacional de %s — %s", exam.getEmployee().getName(), describeProvider(exam)));
        exam.setPaidAt(LocalDate.now());
        OccupationalHealthExam saved = repository.save(exam);
        auditLogService.logCurrent("OCCUPATIONAL_HEALTH_EXAM_PAID", String.format(
                "Exame ocupacional de %s pago: %s a %s%s", saved.getEmployee().getName(),
                saved.getCost(), describeProvider(saved),
                saved.getInvoiceNumber() == null ? "" : " (factura " + saved.getInvoiceNumber() + ")"));
        return toDTO(saved);
    }

    /** Custo da saúde ocupacional num intervalo, total e por prestador. */
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

    // ─── Regras ───────────────────────────────────────────────────────────────

    /**
     * Recusa escrever no ERP aquilo que a lei não deixa o empregador saber.
     * Ver docs/CONFORMIDADE_LEGAL_MZ_SPEC.md §2.
     */
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

    /**
     * Um gestor vê por qualquer trabalhador; toda a gente vê por si e por mais ninguém.
     * Mesma regra do {@code ensureCanActFor} do {@code HRService} — aqui aplicada à leitura.
     */
    private void ensureManagerOrSelf(Employee employee, String operation) {
        if (PermissionGuard.isManagerOrAdmin()) return;
        String username = CurrentUserContext.getUsername();
        Employee self = username == null ? null
                : employeeRepository.findByCompanyIdAndAppUserUsername(companyId(), username).orElse(null);
        if (self == null || !self.getId().equals(employee.getId())) {
            throw new BusinessRuleException(String.format(
                    "Sem permissão para %s. A aptidão médica é dado de saúde: só o próprio "
                            + "trabalhador e os gestores de RH lhe podem aceder.", operation));
        }
    }

    private Employee verifyEmployee(Long employeeId) {
        return employeeRepository.findByIdAndCompanyId(employeeId, companyId())
                .orElseThrow(() -> new BusinessRuleException("Trabalhador não encontrado na empresa activa."));
    }

    private Long companyId() { return CurrentUserContext.requireCurrentCompanyId(); }

    private String blank(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private String describeProvider(OccupationalHealthExam exam) {
        String label = exam.providerLabel();
        return label == null || label.isBlank() ? "prestador não identificado" : label;
    }

    private String status(OccupationalHealthExam exam) {
        long days = exam.daysUntilExpiry(LocalDate.now());
        return days < 0 ? "EXPIRED" : days <= ALERT_DAYS ? "EXPIRING" : "VALID";
    }

    private OccupationalHealthSummaryDTO toSummary(OccupationalHealthExam exam) {
        long days = exam.daysUntilExpiry(LocalDate.now());
        return new OccupationalHealthSummaryDTO(exam.getEmployee().getId(), true,
                exam.getFitnessResult(), exam.getExamDate(), exam.getExpiryDate(), days, status(exam));
    }

    private OccupationalHealthExamDTO toDTO(OccupationalHealthExam exam) {
        return new OccupationalHealthExamDTO(exam.getId(), exam.getEmployee().getId(),
                exam.getEmployee().getName(), exam.getCardNumber(), exam.getExamDate(), exam.getExpiryDate(),
                exam.getFitnessResult(),
                exam.getProvider() == null ? null : exam.getProvider().getId(),
                exam.providerLabel(), exam.getClinic(), exam.getDoctorName(), exam.getRestrictions(),
                exam.getNotes(), exam.getCost(), exam.getInvoiceNumber(), exam.isPaid(), exam.getPaidAt(),
                exam.getAttachmentData() != null && exam.getAttachmentData().length > 0,
                exam.getAttachmentName(), exam.daysUntilExpiry(LocalDate.now()), status(exam));
    }
}
