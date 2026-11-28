package mz.multicore.erp.modules.onlineshop.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.model.OrderOrigin;
import mz.multicore.erp.modules.comercial.model.OrderStatus;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import mz.multicore.erp.modules.comercial.service.ComercialService;
import mz.multicore.erp.modules.comercial.service.OrderLookup;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.onlineshop.model.OnlineShopSettings;
import mz.multicore.erp.modules.onlineshop.repository.OnlineShopSettingsRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Harness LO-10 (docs/LOJA_ONLINE_SPEC.md §3.4): an online order not billed by the end of the shop's cancellation
 * period is cancelled automatically, through the same cancellation as the desktop (which releases the reservation and
 * writes the {@code ORDER_CANCEL} audit with the reason), acting as {@code SYSTEM} for the shop's company.
 */
class OnlineShopExpiryTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 28, 10, 0);

    private final OnlineShopSettingsRepository settingsRepository = mock(OnlineShopSettingsRepository.class);
    private final OrderLookup orderLookup = mock(OrderLookup.class);
    private final ComercialService comercialService = mock(ComercialService.class);
    private final OnlineShopExpiryService service =
            new OnlineShopExpiryService(settingsRepository, orderLookup, comercialService);

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    @Test
    void lo10_cancelaAsEncomendasOnlineForaDoPrazoComMotivoComoSistemaDaEmpresa() {
        when(settingsRepository.findAll()).thenReturn(List.of(shop(1L, 7, true)));
        when(orderLookup.listCancellableByOriginCreatedBefore(1L, OrderOrigin.LOJA_ONLINE, NOW.minusDays(7)))
                .thenReturn(List.of(order(100L, "ENC-1"), order(101L, "ENC-2")));
        List<String> contexts = new ArrayList<>();
        doAnswer(inv -> {
            contexts.add(CurrentUserContext.getUsername() + "|" + CurrentUserContext.getRole() + "|"
                    + CurrentUserContext.requireCurrentCompanyId());
            return null;
        }).when(comercialService).cancelOrder(anyLong(), anyString());

        int cancelled = service.cancelExpired(NOW);

        assertEquals(2, cancelled);
        ArgumentCaptor<String> reason = ArgumentCaptor.forClass(String.class);
        verify(comercialService).cancelOrder(eq(100L), reason.capture());
        verify(comercialService).cancelOrder(eq(101L), any());
        assertEquals("Cancelada automaticamente: encomenda da loja online não facturada em 7 dia(s).",
                reason.getValue());
        assertEquals(List.of("SYSTEM|ADMIN|1", "SYSTEM|ADMIN|1"), contexts);
    }

    @Test
    void lo10_prazoDeCadaLojaContaDesdeACriacao() {
        when(settingsRepository.findAll()).thenReturn(List.of(shop(1L, 7, true), shop(2L, 3, true)));
        when(orderLookup.listCancellableByOriginCreatedBefore(anyLong(), any(), any())).thenReturn(List.of());

        service.cancelExpired(NOW);

        verify(orderLookup).listCancellableByOriginCreatedBefore(1L, OrderOrigin.LOJA_ONLINE, NOW.minusDays(7));
        verify(orderLookup).listCancellableByOriginCreatedBefore(2L, OrderOrigin.LOJA_ONLINE, NOW.minusDays(3));
        verify(comercialService, never()).cancelOrder(anyLong(), anyString());
    }

    @Test
    void lo10_lojaDesactivadaContinuaACancelarAsEncomendasJaFeitas() {
        when(settingsRepository.findAll()).thenReturn(List.of(shop(1L, 5, false)));
        when(orderLookup.listCancellableByOriginCreatedBefore(1L, OrderOrigin.LOJA_ONLINE, NOW.minusDays(5)))
                .thenReturn(List.of(order(100L, "ENC-1")));

        assertEquals(1, service.cancelExpired(NOW));
        verify(comercialService).cancelOrder(eq(100L), any());
    }

    @Test
    void lo10_umaEncomendaRecusadaNaoImpedeAsOutras() {
        when(settingsRepository.findAll()).thenReturn(List.of(shop(1L, 7, true)));
        when(orderLookup.listCancellableByOriginCreatedBefore(1L, OrderOrigin.LOJA_ONLINE, NOW.minusDays(7)))
                .thenReturn(List.of(order(100L, "ENC-1"), order(101L, "ENC-2")));
        doThrow(new BusinessRuleException("Encomenda já faturada — anule a fatura associada."))
                .when(comercialService).cancelOrder(eq(100L), anyString());

        assertEquals(1, service.cancelExpired(NOW));
        verify(comercialService).cancelOrder(eq(101L), anyString());
    }

    @Test
    void lo10_contextoAnteriorERepostoNoFim() {
        when(settingsRepository.findAll()).thenReturn(List.of(shop(1L, 7, true)));
        when(orderLookup.listCancellableByOriginCreatedBefore(anyLong(), any(), any()))
                .thenReturn(List.of(order(100L, "ENC-1")));

        service.cancelExpired(NOW);

        assertNull(CurrentUserContext.findCurrentUser(), "o job não deixa o SYSTEM no fio de execução");
    }

    @SuppressWarnings("unchecked")
    @Test
    void lo10_soEncomendasOnlineAindaCanceláveisEntramNoCancelamento() {
        OrderRepository orderRepository = mock(OrderRepository.class);
        OrderLookup lookup = new OrderLookup(orderRepository, null, null);
        LocalDateTime cutoff = NOW.minusDays(7);

        lookup.listCancellableByOriginCreatedBefore(1L, OrderOrigin.LOJA_ONLINE, cutoff);

        ArgumentCaptor<Collection<String>> statuses = ArgumentCaptor.forClass(Collection.class);
        verify(orderRepository).findByCompanyIdAndOriginAndStatusInAndCreatedAtBefore(eq(1L),
                eq(OrderOrigin.LOJA_ONLINE), statuses.capture(), eq(cutoff));
        for (OrderStatus status : OrderStatus.values()) {
            assertEquals(OrderStatus.isCancellable(status.name()), statuses.getValue().contains(status.name()),
                    status.name());
        }
        assertFalse(statuses.getValue().contains(OrderStatus.BILLED.name()));
        assertFalse(statuses.getValue().contains(OrderStatus.INVOICED.name()));
        assertTrue(statuses.getValue().contains(OrderStatus.PENDING_APPROVAL.name()));
    }

    private static OnlineShopSettings shop(Long companyId, int days, boolean enabled) {
        Company company = new Company();
        company.setId(companyId);
        OnlineShopSettings shop = new OnlineShopSettings();
        shop.setCompany(company);
        shop.setEnabled(enabled);
        shop.setPublicCode("loja-" + companyId);
        shop.setAutoCancelDays(days);
        return shop;
    }

    private static Order order(Long id, String number) {
        Order order = new Order();
        order.setId(id);
        order.setOrderNumber(number);
        return order;
    }
}
