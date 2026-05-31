package com.phcpro.modules.pos.repository;

import com.phcpro.modules.pos.model.TillMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TillMovementRepository extends JpaRepository<TillMovement, Long> {
    List<TillMovement> findByTillSessionId(Long tillSessionId);
}
