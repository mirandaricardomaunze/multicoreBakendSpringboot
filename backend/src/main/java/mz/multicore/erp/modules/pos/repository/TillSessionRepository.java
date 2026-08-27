package mz.multicore.erp.modules.pos.repository;

import mz.multicore.erp.modules.pos.model.TillSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TillSessionRepository extends JpaRepository<TillSession, Long> {
    Optional<TillSession> findByOperatorAndStatusAndCompanyId(String operator, String status, Long companyId);
    List<TillSession> findByCompanyId(Long companyId);
}
