package mz.multicore.erp.modules.hr.repository;

import mz.multicore.erp.modules.hr.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByEmail(String email);
    List<Employee> findByCompanyIdOrderByName(Long companyId);
    Optional<Employee> findByIdAndCompanyId(Long id, Long companyId);
    boolean existsByCompanyIdAndEmployeeNumberIgnoreCase(Long companyId, String employeeNumber);
    boolean existsByCompanyIdAndEmployeeNumberIgnoreCaseAndIdNot(Long companyId, String employeeNumber, Long id);
    boolean existsByCompanyIdAndEmailIgnoreCase(Long companyId, String email);
    boolean existsByCompanyIdAndEmailIgnoreCaseAndIdNot(Long companyId, String email, Long id);

    /** O colaborador por trás de uma conta de utilizador, na empresa activa — resolve "o próprio". */
    Optional<Employee> findByCompanyIdAndAppUserUsername(Long companyId, String username);

    /** Uma conta não pode servir dois colaboradores da mesma empresa (espelha o índice da V48). */
    boolean existsByCompanyIdAndAppUserUsername(Long companyId, String username);
    boolean existsByCompanyIdAndAppUserUsernameAndIdNot(Long companyId, String username, Long id);
}
