package mz.multicore.erp.modules.currency.repository;

import mz.multicore.erp.modules.currency.model.ExchangeRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    boolean existsByCompanyIdAndCurrencyAndRateDate(Long companyId, String currency, LocalDate rateDate);

    List<ExchangeRate> findByCompanyIdOrderByRateDateDescCurrencyAsc(Long companyId);

    Optional<ExchangeRate> findByIdAndCompanyId(Long id, Long companyId);

    /** Rate of the given date or, failing that, of the most recent earlier date. */
    Optional<ExchangeRate> findFirstByCompanyIdAndCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(
            Long companyId, String currency, LocalDate rateDate);
}
