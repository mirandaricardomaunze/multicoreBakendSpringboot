package mz.multicore.erp.modules.printing;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.hr.model.ContractStatus;
import mz.multicore.erp.modules.hr.model.ContractType;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.EmploymentContract;
import mz.multicore.erp.modules.hr.repository.EmploymentContractRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * RHC-18 do harness do RH. Estes casos lêem o <b>texto do PDF</b>, não o código que o gera: um
 * contrato impresso que diga coisa diferente do que está gravado é pior do que não haver contrato.
 */
class EmploymentContractPrintServiceTest {

    private static final Long COMPANY = 7L;

    private EmploymentContractRepository repository;
    private EmploymentContractPrintService service;

    @BeforeEach
    void setUp() {
        repository = mock(EmploymentContractRepository.class);
        service = new EmploymentContractPrintService(repository);
        CurrentUserContext.setCurrentCompanyId(COMPANY);
        CurrentUserContext.setCurrentUser("gestor", "MANAGER");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void fixedTermContract_printsPartiesTermsAndClauses() {
        EmploymentContract contract = contract(ContractType.TERMO_CERTO,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
        contract.setTermReason("Acréscimo excepcional de actividade");
        contract.setWorkLocation("Loja da Baixa");
        when(repository.findByIdWithEmployeeAndCompanyId(3L, COMPANY)).thenReturn(Optional.of(contract));

        String text = textOf(service.render(3L));

        assertTrue(text.contains("Multicore Lda"), "cabeçalho da empresa");
        assertTrue(text.contains("CTR-2026/3"), "número do contrato");
        assertTrue(text.contains("Maria Santos"), "nome do trabalhador");
        assertTrue(text.contains("A termo certo"), "tipo em PT-MZ, não a constante");
        assertTrue(text.contains("Operadora de Caixa"), "função acordada");
        assertTrue(text.contains("Loja da Baixa"), "local de trabalho");
        // A justificação do termo é exigência legal: tem de sair no papel que se assina.
        assertTrue(text.contains("Acréscimo excepcional de actividade"), "motivo do termo");
        assertTrue(text.contains("Justificação do termo"), "cláusula do motivo");
        assertTrue(text.contains("31/12/2026"), "data de fim");
        assertTrue(text.contains("O(A) trabalhador(a)"), "bloco de assinaturas");
    }

    @Test
    void openEndedContract_saysSemTermo_andHasNoTermClause() {
        EmploymentContract contract = contract(ContractType.SEM_TERMO, LocalDate.of(2024, 1, 15), null);
        when(repository.findByIdWithEmployeeAndCompanyId(3L, COMPANY)).thenReturn(Optional.of(contract));

        String text = textOf(service.render(3L));

        assertTrue(text.contains("Sem termo"));
        assertTrue(text.contains("celebrado sem termo"), "cláusula de duração sem prazo");
        // Sem termo não tem motivo do termo — a cláusula não pode aparecer vazia.
        assertFalse(text.contains("Justificação do termo"));
    }

    @Test
    void salaryOnPaper_isTheAgreedOne_notTheEmployeeCardOne() {
        // A ficha e o contrato podem divergir por engano; o papel assinado segue o contrato.
        EmploymentContract contract = contract(ContractType.SEM_TERMO, LocalDate.of(2024, 1, 15), null);
        contract.setAgreedSalary(new BigDecimal("42500.00"));
        contract.getEmployee().setBaseSalary(new BigDecimal("11111.00"));
        when(repository.findByIdWithEmployeeAndCompanyId(3L, COMPANY)).thenReturn(Optional.of(contract));

        String text = textOf(service.render(3L));

        assertTrue(text.contains("42.500,00") || text.contains("42,500.00") || text.contains("42500"),
                "o PDF mostra o salário acordado: " + text);
        assertFalse(text.contains("11.111,00"), "não pode mostrar o da ficha");
    }

    @Test
    void probationPeriod_appearsWhenAgreed() {
        EmploymentContract contract = contract(ContractType.SEM_TERMO, LocalDate.of(2026, 1, 1), null);
        contract.setProbationEndDate(LocalDate.of(2026, 3, 31));
        when(repository.findByIdWithEmployeeAndCompanyId(3L, COMPANY)).thenReturn(Optional.of(contract));

        String text = textOf(service.render(3L));

        assertTrue(text.contains("Período experimental"));
        assertTrue(text.contains("31/03/2026"));
    }

    @Test
    void contractFromAnotherCompany_isNotPrinted() {
        when(repository.findByIdWithEmployeeAndCompanyId(3L, COMPANY)).thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class, () -> service.render(3L));
    }

    private static String textOf(byte[] pdf) {
        try {
            PdfReader reader = new PdfReader(pdf);
            StringBuilder text = new StringBuilder();
            for (int page = 1; page <= reader.getNumberOfPages(); page++) {
                text.append(new PdfTextExtractor(reader).getTextFromPage(page));
            }
            reader.close();
            return text.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("PDF ilegível", ex);
        }
    }

    private static EmploymentContract contract(ContractType type, LocalDate start, LocalDate end) {
        Company company = new Company();
        company.setId(COMPANY);
        company.setName("Multicore Lda");
        company.setTaxId("400123456");

        Employee employee = new Employee();
        employee.setId(5L);
        employee.setEmployeeNumber("PT-001");
        employee.setName("Maria Santos");
        employee.setTaxId("100200300");
        employee.setBaseSalary(new BigDecimal("30000.00"));
        employee.setCompany(company);

        EmploymentContract contract = new EmploymentContract();
        contract.setId(3L);
        contract.setContractNumber("CTR-2026/3");
        contract.setEmployee(employee);
        contract.setCompany(company);
        contract.setContractType(type);
        contract.setStatus(ContractStatus.VIGENTE);
        contract.setStartDate(start);
        contract.setEndDate(end);
        contract.setAgreedSalary(new BigDecimal("30000.00"));
        contract.setWeeklyHours(40);
        contract.setJobTitle("Operadora de Caixa");
        return contract;
    }
}
