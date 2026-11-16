package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.model.EmploymentContract;
import mz.multicore.erp.modules.hr.model.Payslip;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import mz.multicore.erp.modules.hr.repository.EmploymentContractRepository;
import mz.multicore.erp.modules.hr.repository.PayslipRepository;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** MD-R02 — porta de leitura de colaboradores do RH para os outros módulos (docs/MODULE_DECOUPLING_SPEC.md §2). */
class EmployeeLookupTest {

    private final EmployeeRepository employees = mock(EmployeeRepository.class);
    private final PayslipRepository payslips = mock(PayslipRepository.class);
    private final EmploymentContractRepository contracts = mock(EmploymentContractRepository.class);
    private final EmployeeLookup lookup = new EmployeeLookup(employees, payslips, contracts);

    private static Employee employee(long id) {
        Employee e = new Employee();
        e.setId(id);
        e.setName("Colaborador " + id);
        return e;
    }

    @Test
    void require_existente_devolveColaborador() {
        Employee e = employee(4L);
        when(employees.findById(4L)).thenReturn(Optional.of(e));

        assertThat(lookup.require(4L)).isSameAs(e);
        assertThat(lookup.find(4L)).containsSame(e);
    }

    @Test
    void require_inexistente_mensagemNormalizada() {
        when(employees.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lookup.require(9L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Colaborador não encontrado.");
    }

    @Test
    void requireForCompany_foraDaEmpresa_mensagemNormalizada() {
        Employee e = employee(5L);
        when(employees.findByIdAndCompanyId(5L, 1L)).thenReturn(Optional.of(e));
        when(employees.findByIdAndCompanyId(5L, 2L)).thenReturn(Optional.empty());

        assertThat(lookup.requireForCompany(5L, 1L)).isSameAs(e);
        assertThatThrownBy(() -> lookup.requireForCompany(5L, 2L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Colaborador não encontrado nesta empresa.");
    }

    @Test
    void nulos_recusamSemTocarNoRepository() {
        assertThat(lookup.find(null)).isEmpty();
        assertThat(lookup.findForCompany(null, 1L)).isEmpty();
        assertThatThrownBy(() -> lookup.require(null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Colaborador não encontrado.");
        assertThatThrownBy(() -> lookup.requireForCompany(null, 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Colaborador não encontrado nesta empresa.");
        verifyNoInteractions(employees, payslips, contracts);
    }

    @Test
    void listas_delegam() {
        List<Employee> all = List.of(employee(1L), employee(2L));
        List<Employee> active = List.of(employee(1L));
        when(employees.findByCompanyIdOrderByName(3L)).thenReturn(all);
        when(employees.findByCompanyIdAndStatus(3L, "ACTIVE")).thenReturn(active);

        assertThat(lookup.listForCompany(3L)).isSameAs(all);
        assertThat(lookup.listByStatusForCompany(3L, "ACTIVE")).isSameAs(active);
    }

    @Test
    void recibo_doPeriodo_delega() {
        Payslip p = new Payslip();
        when(payslips.findByEmployeeIdAndYearAndMonth(4L, 2026, 11)).thenReturn(Optional.of(p));

        assertThat(lookup.findPayslip(4L, 2026, 11)).containsSame(p);
        verify(payslips).findByEmployeeIdAndYearAndMonth(4L, 2026, 11);
    }

    @Test
    void contrato_daEmpresa_ouMensagemNormalizada() {
        EmploymentContract c = new EmploymentContract();
        when(contracts.findByIdWithEmployeeAndCompanyId(8L, 1L)).thenReturn(Optional.of(c));
        when(contracts.findByIdWithEmployeeAndCompanyId(8L, 2L)).thenReturn(Optional.empty());

        assertThat(lookup.requireContractForCompany(8L, 1L)).isSameAs(c);
        assertThatThrownBy(() -> lookup.requireContractForCompany(8L, 2L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Contrato não encontrado.");
    }

    @Test
    void ePortaDeLeituraSoDeLeitura() {
        assertThat(EmployeeLookup.class).hasAnnotation(Service.class);
        Transactional tx = EmployeeLookup.class.getAnnotation(Transactional.class);
        assertThat(tx).isNotNull();
        assertThat(tx.readOnly()).isTrue();
    }
}
