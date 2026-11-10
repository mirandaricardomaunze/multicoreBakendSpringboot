package mz.multicore.erp.modules.pos.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.repository.ProductRepository;
import mz.multicore.erp.modules.comercial.repository.QuotationRepository;
import mz.multicore.erp.modules.pos.dto.POSCheckoutLineRequest;
import mz.multicore.erp.modules.pos.dto.POSCheckoutRequest;
import mz.multicore.erp.modules.promotions.service.PromotionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.*;

/** A weight typed in the POS instead of read from the scale is audited, once per line (SA-C21). */
class PosTypedWeightAuditTest {

    @AfterEach
    void clear() {
        CurrentUserContext.clear();
    }

    @Test
    void onlyTypedWeightsAreAudited() {
        AuditLogService audit = mock(AuditLogService.class);
        PosPriceGuard guard = new PosPriceGuard(mock(ProductRepository.class), mock(QuotationRepository.class),
                mock(PromotionService.class), mock(PosDiscountAuthorizationService.class), audit);
        CurrentUserContext.setCurrentUser("gerente", "MANAGER");
        CurrentUserContext.setCurrentCompanyId(1L);
        List<POSCheckoutLineRequest> lines = List.of(
                new POSCheckoutLineRequest(1L, new BigDecimal("1.250"), BigDecimal.ZERO, null, null, null, POSCheckoutLineRequest.WEIGHT_TYPED),
                new POSCheckoutLineRequest(2L, new BigDecimal("0.800"), BigDecimal.ZERO, null, null, null, POSCheckoutLineRequest.WEIGHT_FROM_SCALE),
                new POSCheckoutLineRequest(3L, BigDecimal.ONE, BigDecimal.ZERO, null, null, null, null));
        guard.requireAuthorized(new POSCheckoutRequest("caixa", 1L, null, "Cliente", 1L, null, lines, List.of(), (String) null));
        verify(audit, times(1)).logCurrent(eq("POS_WEIGHT_MANUAL"), startsWith("Venda ao peso com peso escrito à mão: 1.250"));
        verifyNoMoreInteractions(audit);
    }
}
