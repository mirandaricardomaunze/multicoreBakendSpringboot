package mz.multicore.erp.modules.currency.controller;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.company.service.CompanyBaseCurrencyService;
import mz.multicore.erp.modules.currency.dto.BaseCurrencyDTO;
import mz.multicore.erp.modules.currency.dto.ChangeBaseCurrencyRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Base currency of the active company for the settings screen (SPEC §3.1, §4.5). It lives in the exchange-rate
 * area so {@code ApiRolePolicy} applies the same decision: consulting for MANAGER, changing ADMIN only.
 */
@RestController
@RequestMapping("/api/exchange-rates/base-currency")
public class BaseCurrencyController {

    private final CompanyBaseCurrencyService baseCurrencyService;

    public BaseCurrencyController(CompanyBaseCurrencyService baseCurrencyService) {
        this.baseCurrencyService = baseCurrencyService;
    }

    @GetMapping
    public ResponseEntity<BaseCurrencyDTO> get() {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        return ResponseEntity.ok(current(companyId));
    }

    @PutMapping
    public ResponseEntity<BaseCurrencyDTO> change(@RequestBody ChangeBaseCurrencyRequest request) {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        baseCurrencyService.changeBaseCurrency(companyId, request == null ? null : request.currency());
        return ResponseEntity.ok(current(companyId));
    }

    private BaseCurrencyDTO current(Long companyId) {
        return new BaseCurrencyDTO(baseCurrencyService.baseCurrency(companyId).name(),
                baseCurrencyService.isBaseCurrencyEditable(companyId));
    }
}
