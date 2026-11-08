package mz.multicore.erp.modules.pricing.repository;

import mz.multicore.erp.modules.pricing.model.PriceList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PriceListRepository extends JpaRepository<PriceList, Long> {

    List<PriceList> findByCompanyIdOrderByName(Long companyId);

    Optional<PriceList> findByIdAndCompanyId(Long id, Long companyId);

    Optional<PriceList> findByCompanyIdAndCode(Long companyId, String code);
}
