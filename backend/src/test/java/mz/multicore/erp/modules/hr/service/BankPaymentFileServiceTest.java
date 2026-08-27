package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.hr.dto.BankPaymentFileDTO;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.Payslip;
import mz.multicore.erp.modules.hr.repository.PayslipRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** B8.7 do harness do RH (RHC-75): numa folha de 30 pessoas, pagava-se uma a uma. */
class BankPaymentFileServiceTest {

    private static final Long COMPANY = 7L;

    private PayslipRepository payslipRepository;
    private AuditLogService auditLogService;
    private BankPaymentFileService service;

    @BeforeEach
    void setUp() {
        payslipRepository = mock(PayslipRepository.class);
        auditLogService = mock(AuditLogService.class);
        service = new BankPaymentFileService(payslipRepository, auditLogService);
        CurrentUserContext.setCurrentCompanyId(COMPANY);
        CurrentUserContext.setCurrentUser("gestor", "MANAGER");
    }

    @AfterEach
    void tearDown() {
        CurrentUserContext.clear();
    }

    @Test
    void generatesOneLinePerApprovedPayslip() { // RHC-75
        when(payslipRepository.findByCompanyIdAndYearAndMonth(COMPANY, 2026, 8)).thenReturn(List.of(
                payslip("APPROVED", "Maria", "BIM", "1234567", "26100.00"),
                payslip("APPROVED", "João", "Millennium", "7654321", "18500.00")));

        BankPaymentFileDTO file = service.generate(2026, 8);

        assertEquals(2, file.paymentCount());
        assertEquals(0, new BigDecimal("44600.00").compareTo(file.totalAmount()));
        assertTrue(file.csv().startsWith("numero_colaborador;nome;banco;conta;recibo;referencia;valor"));
        assertTrue(file.csv().contains("SALARIO 08/2026"));
        verify(auditLogService).logCurrent(eq("PAYROLL_BANK_FILE"), any());
    }

    @Test
    void alreadyPaidPayslips_areNotInTheFile() {
        // Incluí-los pagaria a mesma pessoa duas vezes — o erro que um pagamento em lote torna
        // fácil e caro. E um rascunho ainda pode mudar de valor.
        when(payslipRepository.findByCompanyIdAndYearAndMonth(COMPANY, 2026, 8)).thenReturn(List.of(
                payslip("APPROVED", "Maria", "BIM", "1234567", "26100.00"),
                payslip("PAID", "João", "Millennium", "7654321", "18500.00"),
                payslip("DRAFT", "Rita", "BCI", "5555555", "12000.00")));

        BankPaymentFileDTO file = service.generate(2026, 8);

        assertEquals(1, file.paymentCount());
        assertEquals(0, new BigDecimal("26100.00").compareTo(file.totalAmount()));
    }

    @Test
    void employeesWithoutAnAccount_areListedInsteadOfDisappearing() { // RHC-75
        // Um pagamento que falta é a coisa que menos pode desaparecer sem aviso.
        when(payslipRepository.findByCompanyIdAndYearAndMonth(COMPANY, 2026, 8)).thenReturn(List.of(
                payslip("APPROVED", "Maria", "BIM", "1234567", "26100.00"),
                payslip("APPROVED", "Carlos", null, null, "9000.00")));

        BankPaymentFileDTO file = service.generate(2026, 8);

        assertEquals(1, file.paymentCount());
        assertEquals(List.of("Carlos"), file.missingAccount());
    }

    @Test
    void nameWithASemicolon_doesNotBreakTheLine() {
        // "Silva; Jr." partia a linha em duas e o banco lia o valor errado na coluna errada.
        when(payslipRepository.findByCompanyIdAndYearAndMonth(COMPANY, 2026, 8)).thenReturn(List.of(
                payslip("APPROVED", "Silva; Jr.", "BIM", "1234567", "26100.00")));

        BankPaymentFileDTO file = service.generate(2026, 8);

        assertTrue(file.csv().contains("\"Silva; Jr.\""));
        assertEquals(2, file.csv().strip().split("\n").length, "cabeçalho + uma linha");
    }

    @Test
    void withoutApprovedPayslips_saysWhatToDoFirst() {
        when(payslipRepository.findByCompanyIdAndYearAndMonth(COMPANY, 2026, 8)).thenReturn(List.of());

        var ex = assertThrows(BusinessRuleException.class, () -> service.generate(2026, 8));

        assertTrue(ex.getMessage().contains("Aprove a folha"));
    }

    @Test
    void employeeRole_cannotGenerateTheFile() {
        CurrentUserContext.setCurrentUser("operador", "EMPLOYEE");

        assertThrows(BusinessRuleException.class, () -> service.generate(2026, 8));
    }

    private Payslip payslip(String status, String name, String bank, String account, String net) {
        Employee employee = new Employee();
        employee.setId(5L);
        employee.setEmployeeNumber("EMP-" + name.charAt(0));
        employee.setName(name);
        employee.setBankName(bank);
        employee.setBankAccount(account);

        Payslip p = new Payslip();
        p.setEmployee(employee);
        p.setPayslipNumber("REC-2026/1");
        p.setYear(2026);
        p.setMonth(8);
        p.setStatus(status);
        p.setNetPay(new BigDecimal(net));
        return p;
    }
}
