package mz.multicore.erp.modules.pos.repository;

import mz.multicore.erp.modules.pos.model.StoreVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreVoucherRepository extends JpaRepository<StoreVoucher, Long> {
    Optional<StoreVoucher> findByCodeAndCompanyId(String code, Long companyId);
    Optional<StoreVoucher> findByCode(String code);
    List<StoreVoucher> findByCompanyIdOrderByIssuedAtDesc(Long companyId);
    List<StoreVoucher> findByClientIdAndCompanyIdOrderByIssuedAtDesc(Long clientId, Long companyId);
    boolean existsByCode(String code);
}
