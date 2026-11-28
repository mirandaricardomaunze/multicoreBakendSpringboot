package mz.multicore.erp.modules.onlineshop.repository;

import mz.multicore.erp.modules.onlineshop.model.OnlineShopSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OnlineShopSettingsRepository extends JpaRepository<OnlineShopSettings, Long> {

    Optional<OnlineShopSettings> findByCompanyId(Long companyId);

    /** The shop behind {@code /loja/{code}} (docs/LOJA_ONLINE_SPEC.md §3.1). */
    Optional<OnlineShopSettings> findByPublicCode(String publicCode);

    /** The public code is unique in the whole system (docs/LOJA_ONLINE_SPEC.md §3.1). */
    boolean existsByPublicCodeAndCompanyIdNot(String publicCode, Long companyId);
}
