package com.phcpro.modules.inventory.repository;

import com.phcpro.modules.inventory.model.InventoryCount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryCountRepository extends JpaRepository<InventoryCount, Long> {

    List<InventoryCount> findByCompany_IdOrderByCreatedAtDesc(Long companyId);
}
