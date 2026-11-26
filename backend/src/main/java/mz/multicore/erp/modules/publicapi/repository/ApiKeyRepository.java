package mz.multicore.erp.modules.publicapi.repository;

import mz.multicore.erp.modules.publicapi.model.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {

    Optional<ApiKey> findByKeyHash(String keyHash);

    List<ApiKey> findByCompanyIdOrderByCreatedAtDescIdDesc(Long companyId);
}
