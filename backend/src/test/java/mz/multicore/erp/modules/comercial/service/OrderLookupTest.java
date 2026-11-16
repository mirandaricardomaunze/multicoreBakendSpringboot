package mz.multicore.erp.modules.comercial.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.comercial.model.DeliveryGuide;
import mz.multicore.erp.modules.comercial.model.Invoice;
import mz.multicore.erp.modules.comercial.model.Order;
import mz.multicore.erp.modules.comercial.model.Quotation;
import mz.multicore.erp.modules.comercial.model.QuotationStatus;
import mz.multicore.erp.modules.comercial.repository.DeliveryGuideRepository;
import mz.multicore.erp.modules.comercial.repository.OrderRepository;
import mz.multicore.erp.modules.comercial.repository.QuotationRepository;
import mz.multicore.erp.modules.company.model.Company;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** MD-V04 — porta de leitura de encomendas, cotações e guias de entrega (docs/MODULE_DECOUPLING_SPEC.md §2). */
class OrderLookupTest {

    private final OrderRepository orders = mock(OrderRepository.class);
    private final QuotationRepository quotations = mock(QuotationRepository.class);
    private final DeliveryGuideRepository guides = mock(DeliveryGuideRepository.class);
    private final OrderLookup lookup = new OrderLookup(orders, quotations, guides);

    private static Company company(long id) {
        Company c = new Company();
        c.setId(id);
        return c;
    }

    private static Order order(long id, Company company) {
        Order o = new Order();
        o.setId(id);
        o.setCompany(company);
        return o;
    }

    @Test
    void requireOrder_existente_devolveEncomenda() {
        Order o = order(10L, company(5L));
        when(orders.findById(10L)).thenReturn(Optional.of(o));

        assertThat(lookup.requireOrder(10L)).isSameAs(o);
    }

    @Test
    void requireOrder_inexistente_lancaMensagemNormalizada() {
        when(orders.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lookup.requireOrder(99L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Encomenda não encontrada.");
    }

    @Test
    void findOrder_idNulo_devolveVazioSemTocarNoRepositorio() {
        assertThat(lookup.findOrder(null)).isEmpty();
        verifyNoInteractions(orders);
    }

    @Test
    void findOrderForCompany_filtraPelaEmpresa() {
        Order o = order(10L, company(5L));
        when(orders.findById(10L)).thenReturn(Optional.of(o));

        assertThat(lookup.findOrderForCompany(10L, 5L)).containsSame(o);
        assertThat(lookup.findOrderForCompany(10L, 6L)).isEmpty();
    }

    @Test
    void findOrderForCompany_semEmpresa_devolveVazio() {
        when(orders.findById(10L)).thenReturn(Optional.of(order(10L, null)));

        assertThat(lookup.findOrderForCompany(10L, 5L)).isEmpty();
    }

    @Test
    void listOrders_e_findOrderByInvoice_delegamNoRepositorio() {
        Order o = order(1L, company(5L));
        when(orders.findByCompanyId(5L)).thenReturn(List.of(o));
        when(orders.findFirstByCompanyIdAndInvoiceId(5L, 77L)).thenReturn(Optional.of(o));

        assertThat(lookup.listOrdersForCompany(5L)).containsExactly(o);
        assertThat(lookup.findOrderByInvoice(5L, 77L)).containsSame(o);
    }

    @Test
    void cotacoes_delegamComEmpresa() {
        Quotation q = new Quotation();
        when(quotations.findByCompanyIdOrderByQuotationDateDesc(5L)).thenReturn(List.of(q));
        when(quotations.findByIdAndCompanyId(3L, 5L)).thenReturn(Optional.of(q));
        when(quotations.findByIdWithLinesAndCompanyId(3L, 5L)).thenReturn(Optional.of(q));

        assertThat(lookup.listQuotationsForCompany(5L)).containsExactly(q);
        assertThat(lookup.findQuotationForCompany(3L, 5L)).containsSame(q);
        assertThat(lookup.findQuotationWithLines(3L, 5L)).containsSame(q);
    }

    @Test
    void guias_delegamComEmpresa() {
        DeliveryGuide g = new DeliveryGuide();
        when(guides.findByCompanyId(5L)).thenReturn(List.of(g));
        when(guides.findByIdWithLinesAndCompanyId(4L, 5L)).thenReturn(Optional.of(g));

        assertThat(lookup.listDeliveryGuidesForCompany(5L)).containsExactly(g);
        assertThat(lookup.findDeliveryGuideWithLines(4L, 5L)).containsSame(g);
        verify(guides).findByIdWithLinesAndCompanyId(4L, 5L);
    }

    @Test
    void ehServicoSoDeLeitura() {
        assertThat(OrderLookup.class.isAnnotationPresent(Service.class)).isTrue();
        Transactional tx = OrderLookup.class.getAnnotation(Transactional.class);
        assertThat(tx).isNotNull();
        assertThat(tx.readOnly()).isTrue();
    }

    @Test
    void saveOrder_gravaPeloRepositorioDono() {
        OrderCommandService commands = new OrderCommandService(orders, quotations);
        Order o = order(1L, company(5L));
        when(orders.save(o)).thenReturn(o);

        assertThat(commands.saveOrder(o)).isSameAs(o);
        assertThat(OrderCommandService.class.isAnnotationPresent(Service.class)).isTrue();
    }

    @Test
    void markQuotationConvertedByPos_cotacaoAberta_ficaConvertidaComAFactura() {
        OrderCommandService commands = new OrderCommandService(orders, quotations);
        Quotation q = new Quotation();
        q.setStatus(QuotationStatus.SENT);
        Invoice invoice = new Invoice();
        invoice.setId(77L);
        invoice.setInvoiceNumber("FT 1/2026");
        invoice.setCreatedBy("caixa");
        when(quotations.findByIdAndCompanyId(3L, 5L)).thenReturn(Optional.of(q));

        assertThat(commands.markQuotationConvertedByPos(3L, 5L, invoice)).containsSame(q);
        assertThat(q.getStatus()).isEqualTo(QuotationStatus.CONVERTED);
        assertThat(q.getInvoiceId()).isEqualTo(77L);
        assertThat(q.getInvoiceNumber()).isEqualTo("FT 1/2026");
        assertThat(q.getDecidedAt()).isNotNull();
        assertThat(q.getDecidedBy()).isEqualTo("caixa");
        verify(quotations).save(q);
    }

    @Test
    void markQuotationConvertedByPos_cotacaoJaConvertida_naoGrava() {
        OrderCommandService commands = new OrderCommandService(orders, quotations);
        Quotation q = new Quotation();
        q.setStatus(QuotationStatus.CONVERTED);
        when(quotations.findByIdAndCompanyId(3L, 5L)).thenReturn(Optional.of(q));

        assertThat(commands.markQuotationConvertedByPos(3L, 5L, new Invoice())).isEmpty();
        verify(quotations, never()).save(q);
    }
}
