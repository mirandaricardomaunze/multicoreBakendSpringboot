package mz.multicore.erp.modules.comercial.model;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Vencimento e antiguidade — regra de domínio pura (VA-01..VA-09).
 * Ver docs/VENCIMENTO_ANTIGUIDADE_SPEC.md.
 */
class InvoiceAgingTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 8, 14);

    private Invoice fatura(BigDecimal total, BigDecimal pago, InvoiceStatus status, LocalDate vencimento) {
        Invoice invoice = new Invoice();
        invoice.setTotalAmount(total);
        invoice.setAmountPaid(pago);
        invoice.setStatus(status);
        invoice.setDueDate(vencimento);
        invoice.setCreatedAt(LocalDateTime.of(2026, 1, 1, 9, 0));
        return invoice;
    }

    private Client cliente(int prazoDias) {
        Client client = new Client();
        client.setName("Cliente Teste");
        client.setPaymentTermsDays(prazoDias);
        return client;
    }

    @Test // VA-01
    void vencimentoSaiDoPrazoDoCliente() {
        Invoice invoice = new Invoice();
        invoice.setClient(cliente(30));

        invoice.assignDueDate(HOJE, null);

        assertEquals(HOJE.plusDays(30), invoice.getDueDate());
    }

    @Test // VA-02
    void semPrazoOClienteEPromptPagamento() {
        Invoice invoice = new Invoice();
        invoice.setClient(cliente(0));

        invoice.assignDueDate(HOJE, null);

        assertEquals(HOJE, invoice.getDueDate(), "prazo zero vence no próprio dia");
    }

    @Test // VA-03
    void vencimentoExplicitoGanhaAoPrazoDoCliente() {
        Invoice invoice = new Invoice();
        invoice.setClient(cliente(30));

        invoice.assignDueDate(HOJE, HOJE.plusDays(7));

        assertEquals(HOJE.plusDays(7), invoice.getDueDate());
    }

    @Test // VA-04
    void vencimentoAnteriorAEmissaoERecusado() {
        Invoice invoice = new Invoice();
        invoice.setClient(cliente(0));

        BusinessRuleException error = assertThrows(BusinessRuleException.class,
                () -> invoice.assignDueDate(HOJE, HOJE.minusDays(1)));

        assertTrue(error.getMessage().contains("anterior à data de emissão"));
    }

    @Test // VA-05
    void dentroDoPrazoNaoTemAtraso() {
        Invoice invoice = fatura(new BigDecimal("1000.00"), BigDecimal.ZERO,
                InvoiceStatus.APPROVED, HOJE.plusDays(5));

        assertEquals(0, invoice.daysOverdue(HOJE));
        assertEquals(AgingBucket.CORRENTE, invoice.agingBucket(HOJE));
        assertFalse(invoice.agingBucket(HOJE).isOverdue());
    }

    @Test // VA-06
    void oDiaDoVencimentoAindaNaoEAtraso() {
        Invoice invoice = fatura(new BigDecimal("1000.00"), BigDecimal.ZERO,
                InvoiceStatus.APPROVED, HOJE);

        assertEquals(0, invoice.daysOverdue(HOJE), "vence hoje: ainda há o dia todo para pagar");
        assertEquals(AgingBucket.CORRENTE, invoice.agingBucket(HOJE));
    }

    @Test // VA-07
    void atrasoContaDoVencimentoEEscalona() {
        assertEquals(AgingBucket.ATE_30,
                fatura(BigDecimal.TEN, BigDecimal.ZERO, InvoiceStatus.APPROVED, HOJE.minusDays(1)).agingBucket(HOJE));
        assertEquals(AgingBucket.ATE_30,
                fatura(BigDecimal.TEN, BigDecimal.ZERO, InvoiceStatus.APPROVED, HOJE.minusDays(30)).agingBucket(HOJE));
        assertEquals(AgingBucket.DE_31_A_60,
                fatura(BigDecimal.TEN, BigDecimal.ZERO, InvoiceStatus.APPROVED, HOJE.minusDays(31)).agingBucket(HOJE));
        assertEquals(AgingBucket.DE_61_A_90,
                fatura(BigDecimal.TEN, BigDecimal.ZERO, InvoiceStatus.APPROVED, HOJE.minusDays(61)).agingBucket(HOJE));
        assertEquals(AgingBucket.MAIS_DE_90,
                fatura(BigDecimal.TEN, BigDecimal.ZERO, InvoiceStatus.APPROVED, HOJE.minusDays(91)).agingBucket(HOJE));

        Invoice vencidaHa45 = fatura(BigDecimal.TEN, BigDecimal.ZERO, InvoiceStatus.APPROVED, HOJE.minusDays(45));
        assertEquals(45, vencidaHa45.daysOverdue(HOJE));
    }

    @Test // VA-08
    void faturaSemSaldoNaoEstaEmAtrasoAindaQueVencida() {
        Invoice paga = fatura(new BigDecimal("500.00"), new BigDecimal("500.00"),
                InvoiceStatus.PAID, HOJE.minusDays(120));
        Invoice anulada = fatura(new BigDecimal("500.00"), BigDecimal.ZERO,
                InvoiceStatus.CANCELLED, HOJE.minusDays(120));

        assertEquals(0, paga.daysOverdue(HOJE), "quem já pagou não deve nada");
        assertEquals(0, anulada.daysOverdue(HOJE), "documento anulado não é cobrável");
        assertEquals(AgingBucket.CORRENTE, paga.agingBucket(HOJE));
    }

    @Test // VA-09
    void pagamentoParcialContinuaAEnvelhecerPeloSaldo() {
        Invoice invoice = fatura(new BigDecimal("1000.00"), new BigDecimal("400.00"),
                InvoiceStatus.PARTIALLY_PAID, HOJE.minusDays(65));

        assertEquals(new BigDecimal("600.00"), invoice.outstandingAmount());
        assertEquals(65, invoice.daysOverdue(HOJE));
        assertEquals(AgingBucket.DE_61_A_90, invoice.agingBucket(HOJE));
    }

    @Test // VA-10
    void documentoAntigoSemVencimentoGravadoUsaADataDeEmissao() {
        Invoice legado = fatura(new BigDecimal("100.00"), BigDecimal.ZERO, InvoiceStatus.APPROVED, null);

        assertEquals(LocalDate.of(2026, 1, 1), legado.effectiveDueDate());
        assertTrue(legado.daysOverdue(HOJE) > 0, "sem vencimento gravado conta desde a emissão");
    }
}
