package mz.multicore.erp.modules.comercial.model;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Data de entrega prometida e rótulos de estado da encomenda — domínio puro.
 * Ver docs/ENCOMENDA_PROFISSIONAL_HARNESS.md (EP-01..06, EP-15..17).
 */
class OrderTest {

    private static final LocalDate CONFIRMED = LocalDate.of(2026, 8, 20);

    @Test
    void assignExpectedDelivery_comDias_somaAoDiaDaConfirmacao() { // EP-01
        Order order = new Order();
        order.assignExpectedDelivery(CONFIRMED, 7);
        assertEquals(LocalDate.of(2026, 8, 27), order.getExpectedDeliveryDate());
    }

    @Test
    void assignExpectedDelivery_semDias_deixaSemDataPrometida() { // EP-02
        Order order = new Order();
        order.assignExpectedDelivery(CONFIRMED, null);
        // Encomenda sem promessa de entrega — o comportamento de toda a base anterior à V45.
        assertNull(order.getExpectedDeliveryDate());
    }

    @Test
    void assignExpectedDelivery_diasZeroOuNegativos_lancaBusinessRuleException() { // EP-03
        Order order = new Order();
        assertThrows(BusinessRuleException.class, () -> order.assignExpectedDelivery(CONFIRMED, 0));
        assertThrows(BusinessRuleException.class, () -> order.assignExpectedDelivery(CONFIRMED, -2));
    }

    @Test
    void assignExpectedDelivery_semDataDeConfirmacao_lancaBusinessRuleException() { // EP-04
        Order order = new Order();
        assertThrows(BusinessRuleException.class, () -> order.assignExpectedDelivery(null, 7));
    }

    @Test
    void isDeliveryOverdue_noProprioDiaNaoEstaEmAtraso_noSeguinteEsta() { // EP-05
        Order order = new Order();
        order.setStatus("PENDING");
        order.assignExpectedDelivery(CONFIRMED, 7);

        assertFalse(order.isDeliveryOverdue(LocalDate.of(2026, 8, 27)));
        assertTrue(order.isDeliveryOverdue(LocalDate.of(2026, 8, 28)));
    }

    @Test
    void isDeliveryOverdue_semDataPrometida_nuncaEstaEmAtraso() {
        Order order = new Order();
        order.setStatus("PENDING");
        assertFalse(order.isDeliveryOverdue(LocalDate.of(2027, 1, 1)));
    }

    @Test
    void isDeliveryOverdue_encomendaFechada_naoEstaEmAtraso() { // EP-06
        LocalDate muitoDepois = LocalDate.of(2027, 1, 1);
        for (String fechada : new String[]{"BILLED", "GUIDED", "CANCELLED"}) {
            Order order = new Order();
            order.setStatus(fechada);
            order.assignExpectedDelivery(CONFIRMED, 7);
            // O que já foi facturado, expedido ou cancelado não está em atraso — está feito.
            assertFalse(order.isDeliveryOverdue(muitoDepois), "estado " + fechada);
        }
    }

    @Test
    void statusLabel_traduzTodosOsEstadosDocumentados() { // EP-15, EP-16
        assertEquals("Pendente de aprovação", OrderStatusLabel.of("PENDING_APPROVAL"));
        assertEquals("Por facturar", OrderStatusLabel.of("PENDING"));
        assertEquals("Aguarda separação", OrderStatusLabel.of("AWAITING_SEPARATION"));
        assertEquals("Em separação", OrderStatusLabel.of("IN_SEPARATION"));
        assertEquals("Separado", OrderStatusLabel.of("SEPARATED"));
        assertEquals("Guia por aprovar", OrderStatusLabel.of("GUIDE_PENDING"));
        assertEquals("Expedido por guia", OrderStatusLabel.of("GUIDED"));
        assertEquals("Facturado", OrderStatusLabel.of("BILLED"));
        assertEquals("Cancelado", OrderStatusLabel.of("CANCELLED"));
    }

    @Test
    void statusLabel_nuncaDevolveAConstanteDosEstadosConhecidos() { // EP-15
        for (String status : new String[]{"PENDING_APPROVAL", "PENDING", "AWAITING_SEPARATION",
                "IN_SEPARATION", "SEPARATED", "GUIDE_PENDING", "GUIDED", "BILLED", "CANCELLED"}) {
            assertNotEquals(status, OrderStatusLabel.of(status));
        }
    }

    @Test
    void statusLabel_nuloOuDesconhecido_naoRebenta() { // EP-17
        assertEquals("—", OrderStatusLabel.of(null));
        assertEquals("—", OrderStatusLabel.of("   "));
        assertEquals("ESTADO_NOVO", OrderStatusLabel.of("ESTADO_NOVO"));
    }

    @Test
    void orderTerms_noneNaoTemOrigemNemCondicoes() { // EP-14
        OrderTerms none = OrderTerms.none();
        assertNull(none.quotationId());
        assertNull(none.quotationNumber());
        assertNull(none.paymentTerms());
        assertNull(none.deliveryTerms());
        assertNull(none.deliveryDays());
        assertSame(none, OrderTerms.orNone(null));
    }

    @Test
    void agreedTerms_devolveOQueACotacaoPrometeu() { // EP-12
        Quotation quotation = new Quotation();
        quotation.setId(9L);
        quotation.setQuotationNumber("CT-2026/9");
        quotation.setPaymentTerms("30 dias");
        quotation.setDeliveryTerms("7 dias úteis");
        quotation.setDeliveryDays(7);

        OrderTerms terms = quotation.agreedTerms();

        assertEquals(9L, terms.quotationId());
        assertEquals("CT-2026/9", terms.quotationNumber());
        assertEquals("30 dias", terms.paymentTerms());
        assertEquals("7 dias úteis", terms.deliveryTerms());
        assertEquals(7, terms.deliveryDays());
    }
}
