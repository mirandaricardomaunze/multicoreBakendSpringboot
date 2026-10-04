package mz.multicore.erp.modules.pos.repository;

import mz.multicore.erp.modules.pos.model.TillSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

@Repository
public interface TillSessionRepository extends JpaRepository<TillSession, Long> {
    Optional<TillSession> findByOperatorAndStatusAndCompanyId(String operator, String status, Long companyId);
    Optional<TillSession> findByCurrentOperatorAndStatusAndCompanyId(String currentOperator, String status, Long companyId);

    @org.springframework.data.jpa.repository.Query("SELECT s FROM TillSession s WHERE s.status = :status AND s.company.id = :companyId " +
            "AND ((s.currentOperator IS NOT NULL AND s.currentOperator = :operator) " +
            "  OR (s.currentOperator IS NULL AND s.operator = :operator))")
    Optional<TillSession> findActiveSessionForOperator(
            @org.springframework.data.repository.query.Param("operator") String operator,
            @org.springframework.data.repository.query.Param("status") String status,
            @org.springframework.data.repository.query.Param("companyId") Long companyId);

    List<TillSession> findByCompanyId(Long companyId);
    List<TillSession> findByCompanyIdOrderByOpenDateDesc(Long companyId);
    List<TillSession> findByCompanyIdAndOpenDateBetweenOrderByOpenDateDesc(Long companyId, LocalDateTime from, LocalDateTime to);
}
