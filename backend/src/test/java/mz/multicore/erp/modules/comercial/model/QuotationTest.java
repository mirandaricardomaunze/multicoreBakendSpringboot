package mz.multicore.erp.modules.comercial.model;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regras de validade da cotação — domínio puro, sem Spring nem base de dados.
 * Ver docs/COTACAO_HARNESS.md (CT-01..CT-07).
 */
class QuotationTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 19);

    @Test
    void assignValidity_semDiasIndicados_aplicaOPadrao() { // CT-01
        Quotation q = new Quotation();
        q.assignValidity(TODAY, null);
        assertEquals(TODAY.plusDays(QuotationValidity.DEFAULT_DAYS), q.getValidUntil());
    }

    @Test
    void assignValidity_comDiasIndicados_usaEsses() { // CT-02
        Quotation q = new Quotation();
        q.assignValidity(TODAY, 7);
        assertEquals(TODAY.plusDays(7), q.getValidUntil());
    }

    @Test
    void assignValidity_diasZeroOuNegativos_lancaBusinessRuleException() { // CT-03
        Quotation q = new Quotation();
        assertThrows(BusinessRuleException.class, () -> q.assignValidity(TODAY, 0));
        assertThrows(BusinessRuleException.class, () -> q.assignValidity(TODAY, -5));
    }

    @Test
    void assignValidity_semDataDeEmissao_lancaBusinessRuleException() {
        Quotation q = new Quotation();
        assertThrows(BusinessRuleException.class, () -> q.assignValidity(null, 30));
    }

    @Test
    void isExpired_noUltimoDiaDeValidade_aindaEValida() { // CT-04
        Quotation q = new Quotation();
        q.assignValidity(TODAY, 10);
        // Quem recebe uma proposta "válida até dia 29" espera poder aceitá-la no dia 29.
        assertFalse(q.isExpired(TODAY.plusDays(10)));
    }

    @Test
    void isExpired_noDiaSeguinteAoLimite_estaCaducada() { // CT-05
        Quotation q = new Quotation();
        q.assignValidity(TODAY, 10);
        assertTrue(q.isExpired(TODAY.plusDays(11)));
    }

    @Test
    void daysUntilExpiry_antesNoDiaEDepois() { // CT-06
        Quotation q = new Quotation();
        q.assignValidity(TODAY, 10);
        assertEquals(10, q.daysUntilExpiry(TODAY));
        assertEquals(0, q.daysUntilExpiry(TODAY.plusDays(10)));
        assertEquals(-3, q.daysUntilExpiry(TODAY.plusDays(13)));
    }

    @Test
    void isConvertible_exigeEstadoAbertoEValidade() {
        Quotation q = new Quotation();
        q.assignValidity(TODAY, 10);

        q.setStatus(QuotationStatus.DRAFT);
        assertTrue(q.isConvertible(TODAY));
        q.setStatus(QuotationStatus.SENT);
        assertTrue(q.isConvertible(TODAY));
        q.setStatus(QuotationStatus.ACCEPTED);
        assertTrue(q.isConvertible(TODAY));

        // Caducada: aberta mas fora de prazo.
        assertFalse(q.isConvertible(TODAY.plusDays(11)));

        // Fechada: dentro do prazo mas sem nada a converter.
        q.setStatus(QuotationStatus.CONVERTED);
        assertFalse(q.isConvertible(TODAY));
        q.setStatus(QuotationStatus.REJECTED);
        assertFalse(q.isConvertible(TODAY));
        q.setStatus(QuotationStatus.CANCELLED);
        assertFalse(q.isConvertible(TODAY));
    }

    @Test
    void estados_abertoVersusTerminal() {
        assertTrue(QuotationStatus.DRAFT.isOpen());
        assertTrue(QuotationStatus.SENT.isOpen());
        assertTrue(QuotationStatus.ACCEPTED.isOpen());
        assertTrue(QuotationStatus.REJECTED.isTerminal());
        assertTrue(QuotationStatus.CONVERTED.isTerminal());
        assertTrue(QuotationStatus.CANCELLED.isTerminal());
    }

    @Test
    void rotulos_saoEmPortugues_semCodigosInternos() {
        for (QuotationStatus status : QuotationStatus.values()) {
            assertNotNull(status.getLabel());
            assertNotEquals(status.name(), status.getLabel());
        }
    }

    @Test
    void clientLabel_preferOWalkInName_quandoNaoHaClienteRegistado() {
        Quotation q = new Quotation();
        Client client = new Client();
        client.setName("Consumidor Final");
        q.setClient(client);
        assertEquals("Consumidor Final", q.clientLabel());

        q.setWalkInName("Sr. Alberto");
        assertEquals("Sr. Alberto", q.clientLabel());
    }
}
