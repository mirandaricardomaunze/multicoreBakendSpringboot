package mz.multicore.erp.modules.hr.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import mz.multicore.erp.modules.hr.model.Employee;
import mz.multicore.erp.modules.hr.repository.EmployeeRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Quem pode agir, sobre quem, e em que empresa — as perguntas que <b>todas</b> as áreas do RH fazem
 * antes de fazer seja o que for.
 *
 * <p><b>Porque existe.</b> Estas guardas viviam privadas no {@code HRService}. Enquanto o RH era só
 * a folha de salários isso chegava; deixou de chegar quando o módulo passou a ter contratos, ponto,
 * cessação, retenções e descontos, cada um a precisar exactamente das mesmas respostas. A alternativa
 * era copiá-las para cada serviço novo — e regra copiada é a forma exacta dos bugs que este projecto
 * já apanhou no IVA (a taxa em duas portas) e no saldo em dívida (o valor por cobrar em três).
 *
 * <p>Não substitui o {@link mz.multicore.erp.architecture.security.PermissionGuard}, que é estático e
 * só sabe de papéis. Esta guarda precisa de ir à base de dados — para saber <b>qual colaborador</b> é
 * o utilizador autenticado — e por isso é um bean.
 */
@Component
public class HrAccessGuard {

    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;

    public HrAccessGuard(EmployeeRepository employeeRepository, CompanyRepository companyRepository) {
        this.employeeRepository = employeeRepository;
        this.companyRepository = companyRepository;
    }

    public Long currentCompanyId() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        if (companyId == null) {
            throw new BusinessRuleException("Selecione uma empresa ativa.");
        }
        return companyId;
    }

    public Company currentCompany() {
        return companyRepository.findById(currentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Empresa ativa não encontrada."));
    }

    public void ensureHrManager() {
        if (!isHrManager()) {
            throw new BusinessRuleException("Apenas gestores ou administradores podem executar esta operação de RH.");
        }
    }

    public boolean isHrManager() {
        String role = CurrentUserContext.getRole();
        return "ADMIN".equalsIgnoreCase(role) || "MANAGER".equalsIgnoreCase(role);
    }

    public Employee findEmployee(Long id) {
        return employeeRepository.findByIdAndCompanyId(id, currentCompanyId())
                .orElseThrow(() -> new BusinessRuleException("Colaborador não encontrado na empresa ativa."));
    }

    public Employee findActiveEmployee(Long id) {
        Employee employee = findEmployee(id);
        if (!"ACTIVE".equals(employee.getStatus())) {
            throw new BusinessRuleException("O colaborador não está ativo.");
        }
        return employee;
    }

    /** O colaborador do utilizador autenticado, quando a conta está ligada a um (V48). */
    public Optional<Employee> findSelfEmployee() {
        String username = CurrentUserContext.getUsername();
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return employeeRepository.findByCompanyIdAndAppUserUsername(currentCompanyId(), username);
    }

    /**
     * Um gestor age por qualquer colaborador; toda a gente age <b>por si própria</b> e por mais
     * ninguém. É esta regra que devolve o self-service sem reabrir o furo: até à V48, "o próprio" não
     * era identificável, pelo que agir por outro era indistinguível de agir por si.
     */
    public void ensureCanActFor(Employee employee) {
        if (isHrManager()) {
            return;
        }
        Employee self = findSelfEmployee().orElseThrow(() -> new BusinessRuleException(
                "A sua conta não está associada a nenhum colaborador. Peça ao RH para fazer a associação."));
        if (!self.getId().equals(employee.getId())) {
            throw new BusinessRuleException(
                    "Só pode submeter pedidos em seu próprio nome. Para o fazer por outro colaborador é "
                            + "preciso perfil de gestor ou administrador.");
        }
    }
}
