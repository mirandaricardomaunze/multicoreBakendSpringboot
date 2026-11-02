package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.events.InvoiceCancelledEvent;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.audit.service.AuditLogService;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.DeliveryGuide;
import mz.multicore.erp.modules.comercial.model.DeliveryGuideLine;
import mz.multicore.erp.modules.comercial.model.DeliveryGuideStatus;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.repository.DeliveryGuideRepository;
import mz.multicore.erp.modules.comercial.repository.InvoiceRepository;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.numbering.service.DocumentNumberService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Ver docs/COMERCIAL_DOCUMENTOS_AUDITORIA_SPEC.md. */
class DeliveryGuideBillingHarnessTest {

    private final DeliveryGuideRepository guides = mock(DeliveryGuideRepository.class);
    private final OrderRepository orders = mock(OrderRepository.class);
    private final InvoiceRepository invoices = mock(InvoiceRepository.class);
    private final ReceivablesService receivables = mock(ReceivablesService.class);
    private final ComercialInvoiceLifecycleService lifecycle = mock(ComercialInvoiceLifecycleService.class);
    private final DeliveryGuideBillingService service = new DeliveryGuideBillingService(guides, orders, invoices, receivables,
            mock(ComercialClientAndCreditService.class), lifecycle, mock(DocumentNumberService.class), mock(AuditLogService.class));

    @BeforeEach
    void signIn() {
        CurrentUserContext.setCurrentUser("ana", "ADMIN");
        CurrentUserContext.setCurrentCompanyId(1L);
        when(invoices.save(any(Invoice.class))).thenAnswer(call -> {
            Invoice invoice = call.getArgument(0);
            invoice.setId(77L);
            return invoice;
        });
    }

    @AfterEach
    void signOut() {
        CurrentUserContext.clear();
    }

    @Test
    @DisplayName("GF-01: Guia aprovada gera fatura com desconto e fica ligada a ela e à encomenda")
    void gf01_billsApprovedGuide() {
        DeliveryGuide guide = guide(DeliveryGuideStatus.APPROVED);
        guide.setOrderId(5L);
        Order order = new Order();
        when(guides.findForUpdate(9L, 1L)).thenReturn(Optional.of(guide));
        when(orders.findForUpdate(5L, 1L)).thenReturn(Optional.of(order));

        service.bill(9L);

        ArgumentCaptor<Invoice> saved = ArgumentCaptor.forClass(Invoice.class);
        verify(invoices).save(saved.capture());
        assertThat(saved.getValue().getTotalAmount()).isEqualByComparingTo("900.00");
        assertThat(saved.getValue().getDeliveryGuideId()).isEqualTo(9L);
        assertThat(guide.getInvoiceId()).isEqualTo(77L);
        assertThat(order.getStatus()).isEqualTo("BILLED");
        verify(lifecycle).publishSale(any(), any(), eq(false));
    }

    @Test
    @DisplayName("GF-02: Guia pendente ou já faturada não gera fatura")
    void gf02_refusesPendingOrBilledGuide() {
        DeliveryGuide billed = guide(DeliveryGuideStatus.APPROVED);
        billed.setInvoiceId(1L);
        billed.setInvoiceNumber("FT-1");
        when(guides.findForUpdate(9L, 1L)).thenReturn(Optional.of(guide(DeliveryGuideStatus.PENDING_APPROVAL)), Optional.of(billed));

        assertThatThrownBy(() -> service.bill(9L)).isInstanceOf(BusinessRuleException.class).hasMessageContaining("aprovada");
        assertThatThrownBy(() -> service.bill(9L)).isInstanceOf(BusinessRuleException.class).hasMessageContaining("já foi faturada");
        verify(invoices, never()).save(any());
    }

    @Test
    @DisplayName("GF-03: Anular a fatura deixa a guia e a encomenda prontas a faturar de novo")
    void gf03_cancelledInvoiceReleasesGuide() {
        DeliveryGuide guide = guide(DeliveryGuideStatus.APPROVED);
        guide.setOrderId(5L);
        guide.setInvoiceId(77L);
        guide.setInvoiceNumber("FT-1");
        Order order = new Order();
        order.setStatus("BILLED");
        order.setInvoiceId(77L);
        when(guides.findByCompanyIdAndInvoiceId(1L, 77L)).thenReturn(Optional.of(guide));
        when(orders.findFirstByCompanyIdAndInvoiceId(1L, 77L)).thenReturn(Optional.of(order));

        service.onInvoiceCancelled(new InvoiceCancelledEvent(1L, 77L, "FT-1", LocalDate.of(2026, 11, 1)));

        assertThat(guide.getInvoiceId()).isNull();
        assertThat(order.getStatus()).isEqualTo("GUIDED");
        assertThat(order.getInvoiceId()).isNull();
    }

    @Test
    @DisplayName("GF-04: Anular a fatura de uma encomenda sem guia devolve-a a pendente")
    void gf04_cancelledInvoiceReleasesOrder() {
        Order order = new Order();
        order.setStatus("BILLED");
        order.setInvoiceId(77L);
        when(guides.findByCompanyIdAndInvoiceId(1L, 77L)).thenReturn(Optional.empty());
        when(orders.findFirstByCompanyIdAndInvoiceId(1L, 77L)).thenReturn(Optional.of(order));

        service.onInvoiceCancelled(new InvoiceCancelledEvent(1L, 77L, "FT-1", LocalDate.of(2026, 11, 1)));

        assertThat(order.getStatus()).isEqualTo("PENDING");
        assertThat(order.getInvoiceId()).isNull();
    }

    private static DeliveryGuide guide(DeliveryGuideStatus status) {
        Company company = new Company();
        company.setId(1L);
        DeliveryGuideLine line = new DeliveryGuideLine();
        line.setProduct(new Product());
        line.setQuantity(new BigDecimal("10"));
        line.setUnitPrice(new BigDecimal("100.00"));
        line.setTaxRate(BigDecimal.ZERO);
        line.setDiscountPercentage(new BigDecimal("10"));
        DeliveryGuide guide = new DeliveryGuide();
        guide.setId(9L);
        guide.setGuideNumber("GR-1");
        guide.setCompany(company);
        guide.setClient(new Client());
        guide.setStatus(status);
        guide.addLine(line);
        return guide;
    }
}
