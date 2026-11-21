package mz.multicore.erp.modules.currency.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.company.model.CurrencyCode;
import mz.multicore.erp.modules.company.service.CompanyBaseCurrencyService;
import mz.multicore.erp.modules.currency.dto.CreateExchangeRateRequest;
import mz.multicore.erp.modules.currency.dto.ExchangeRateDTO;
import mz.multicore.erp.modules.currency.model.ExchangeRate;
import mz.multicore.erp.modules.currency.repository.ExchangeRateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Daily exchange-rate table of the active company (docs/MOEDA_ESTRANGEIRA_IMPORTACAO_SPEC.md §3.2):
 * closed currency list, rate &gt; 0 with 4 decimals, one rate per company, currency and date.
 */
@Service
public class ExchangeRateService {

    private static final DateTimeFormatter DATE_PT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ExchangeRateRepository repository;
    private final CompanyBaseCurrencyService baseCurrencyService;

    public ExchangeRateService(ExchangeRateRepository repository, CompanyBaseCurrencyService baseCurrencyService) {
        this.repository = repository;
        this.baseCurrencyService = baseCurrencyService;
    }

    @Transactional(readOnly = true)
    public List<ExchangeRateDTO> list(Long companyId) {
        CurrentUserContext.requireCompany(companyId);
        return repository.findByCompanyIdOrderByRateDateDescCurrencyAsc(companyId).stream()
                .map(ExchangeRateService::toDto)
                .toList();
    }

    @Transactional
    public ExchangeRateDTO create(Long companyId, CreateExchangeRateRequest request) {
        CurrentUserContext.requireCompany(companyId);
        CurrencyCode currency = foreignCurrency(companyId, request.currency());
        LocalDate rateDate = requireDate(request.rateDate());
        BigDecimal rate = normalizeRate(request.rate());
        if (repository.existsByCompanyIdAndCurrencyAndRateDate(companyId, currency.name(), rateDate)) {
            throw new BusinessRuleException("Já existe um câmbio de " + currency.name() + " para "
                    + DATE_PT.format(rateDate) + ". Corrija o câmbio existente em vez de criar outro.");
        }
        ExchangeRate entity = new ExchangeRate();
        entity.setCompanyId(companyId);
        entity.setCurrency(currency.name());
        entity.setRateDate(rateDate);
        entity.setRate(rate);
        entity.setCreatedBy(CurrentUserContext.getUsername());
        return toDto(repository.save(entity));
    }

    private CurrencyCode foreignCurrency(Long companyId, String code) {
        CurrencyCode currency = CurrencyCode.parse(code);
        if (currency == baseCurrencyService.baseCurrency(companyId)) {
            throw new BusinessRuleException(currency.name() + " é a moeda base da empresa: o câmbio é sempre 1 "
                    + "e não entra na tabela de câmbios.");
        }
        return currency;
    }

    private static LocalDate requireDate(LocalDate rateDate) {
        if (rateDate == null) {
            throw new BusinessRuleException("Indique a data do câmbio.");
        }
        return rateDate;
    }

    /** Rate &gt; 0 with at most 4 significant decimals, stored with exactly 4. */
    static BigDecimal normalizeRate(BigDecimal rate) {
        if (rate == null || rate.signum() <= 0) {
            throw new BusinessRuleException("O câmbio tem de ser maior que zero.");
        }
        if (rate.stripTrailingZeros().scale() > ExchangeRate.RATE_SCALE) {
            throw new BusinessRuleException("O câmbio aceita no máximo " + ExchangeRate.RATE_SCALE
                    + " casas decimais.");
        }
        return rate.setScale(ExchangeRate.RATE_SCALE, RoundingMode.UNNECESSARY);
    }

    private static ExchangeRateDTO toDto(ExchangeRate entity) {
        return new ExchangeRateDTO(entity.getId(), entity.getCurrency(), entity.getRateDate(), entity.getRate());
    }
}
