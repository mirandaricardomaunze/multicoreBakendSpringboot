package mz.multicore.erp.modules.pos.repository;

import mz.multicore.erp.modules.pos.model.TillMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDateTime;

@Repository
public interface TillMovementRepository extends JpaRepository<TillMovement, Long> {
    List<TillMovement> findByTillSessionId(Long tillSessionId);

    List<TillMovement> findByTillSessionCompanyIdAndMovementDateBetween(
            Long companyId, LocalDateTime from, LocalDateTime to);
}
