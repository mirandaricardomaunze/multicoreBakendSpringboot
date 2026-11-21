package mz.multicore.erp.modules.currency.repository;

import mz.multicore.erp.modules.currency.model.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    boolean existsByCompanyIdAndCurrencyAndRateDate(Long companyId, String currency, LocalDate rateDate);

    List<ExchangeRate> findByCompanyIdOrderByRateDateDescCurrencyAsc(Long companyId);
}
