package mz.multicore.erp.modules.hr;

import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.Payslip;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.PayslipRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Regressão do bug multi-tenant na numeração de recibos de salário (payslips) — irmão de
 * {@code InvoiceNumberUniquenessPerCompanyTest}. O {@code payslip_number} é gerado por empresa
 * (via {@code DocumentNumberService}), mas a coluna tinha {@code UNIQUE} global e a tabela nem tinha
 * {@code company_id}. O fix adicionou {@code company_id} (= empresa do colaborador) e a restrição
 * {@code UNIQUE(company_id, payslip_number)} (migração {@code V32}).
 */
@DataJpaTest
class PayslipNumberUniquenessPerCompanyTest {

    @Autowired private CompanyRepository companyRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private PayslipRepository payslipRepository;

    @Test
    void mesmoNumeroPermitidoEmEmpresasDiferentes() {
        Company a = company("Empresa A", "100000011");
        Company b = company("Empresa B", "100000012");
        Employee ea = employee(a, "E-A", "ea@example.co.mz");
        Employee eb = employee(b, "E-B", "eb@example.co.mz");

        payslipRepository.saveAndFlush(payslip("RS-2026/1", a, ea, 2026, 1));

        // Antes do fix (UNIQUE global) rebentava aqui; agora as duas empresas coexistem.
        assertDoesNotThrow(() -> payslipRepository.saveAndFlush(payslip("RS-2026/1", b, eb, 2026, 1)));
        assertEquals(2, payslipRepository.count());
    }

    @Test
    void mesmoNumeroRejeitadoNaMesmaEmpresa() {
        Company a = company("Empresa A", "100000013");
        Employee e1 = employee(a, "E-1", "e1@example.co.mz");
        Employee e2 = employee(a, "E-2", "e2@example.co.mz");

        payslipRepository.saveAndFlush(payslip("RS-2026/1", a, e1, 2026, 1));

        // Colaborador/período diferentes para isolar a UNIQUE(company, numero) da
        // UNIQUE(employee, ano, mes) — o que dispara é a repetição do número na mesma empresa.
        assertThrows(DataIntegrityViolationException.class,
                () -> payslipRepository.saveAndFlush(payslip("RS-2026/1", a, e2, 2026, 2)));
    }

    private Company company(String name, String taxId) {
        Company c = new Company();
        c.setName(name);
        c.setTaxId(taxId);
        return companyRepository.saveAndFlush(c);
    }

    private Employee employee(Company company, String number, String email) {
        Employee e = new Employee();
        e.setCompany(company);
        e.setEmployeeNumber(number);
        e.setName(number);
        e.setEmail(email);
        e.setDepartment("Geral");
        e.setBaseSalary(new BigDecimal("1000.00"));
        e.setRole("EMPLOYEE");
        return employeeRepository.saveAndFlush(e);
    }

    private Payslip payslip(String number, Company company, Employee employee, int year, int month) {
        Payslip p = new Payslip();
        p.setPayslipNumber(number);
        p.setCompany(company);
        p.setEmployee(employee);
        p.setYear(year);
        p.setMonth(month);
        return p;
    }
}
