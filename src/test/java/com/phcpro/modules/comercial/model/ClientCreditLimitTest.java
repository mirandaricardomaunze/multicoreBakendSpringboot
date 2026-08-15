package com.phcpro.modules.comercial.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Aritmética do limite de crédito — regra de domínio pura (LC-01..LC-07).
 * Ver docs/LIMITE_CREDITO_SPEC.md.
 */
class ClientCreditLimitTest {

    private Client comLimite(String limite) {
        Client client = new Client();
        client.setName("Cliente Teste");
        client.setCreditLimit(new BigDecimal(limite));
        return client;
    }

    @Test // LC-01
    void semLimiteDefinido_oCreditoELivre() {
        Client client = new Client();

        assertFalse(client.hasCreditLimit());
        assertNull(client.creditAvailable(new BigDecimal("999999.00")), "sem limite não há disponível a calcular");
        assertFalse(client.exceedsCreditLimit(new BigDecimal("999999.00"), new BigDecimal("500000.00")));
    }

    @Test // LC-02
    void dentroDoLimitePassa() {
        Client client = comLimite("10000.00");

        assertFalse(client.exceedsCreditLimit(new BigDecimal("3000.00"), new BigDecimal("2000.00")));
        assertEquals(new BigDecimal("7000.00"), client.creditAvailable(new BigDecimal("3000.00")));
    }

    @Test // LC-03
    void bateCertoNoLimite_aindaPassa() {
        Client client = comLimite("10000.00");

        assertFalse(client.exceedsCreditLimit(new BigDecimal("6000.00"), new BigDecimal("4000.00")),
                "o limite é o tecto inclusivo — 10.000 de 10.000 cabe");
        assertEquals(BigDecimal.ZERO.setScale(2), client.creditAvailable(new BigDecimal("10000.00")));
    }

    @Test // LC-04
    void umCenticoAcimaDoLimiteJaRecusa() {
        Client client = comLimite("10000.00");

        assertTrue(client.exceedsCreditLimit(new BigDecimal("6000.00"), new BigDecimal("4000.01")));
    }

    @Test // LC-05
    void limiteZeroSignificaNaoVendeFiado() {
        Client client = comLimite("0.00");

        assertTrue(client.hasCreditLimit(), "zero é um limite definido, não é ausência de limite");
        assertTrue(client.exceedsCreditLimit(BigDecimal.ZERO, new BigDecimal("0.01")));
    }

    @Test // LC-06
    void vendaPagaNaHoraNaoConsomeCredito() {
        Client client = comLimite("1000.00");

        assertFalse(client.exceedsCreditLimit(new BigDecimal("1000.00"), BigDecimal.ZERO),
                "já estourado, mas esta venda não acrescenta dívida nenhuma");
        assertFalse(client.exceedsCreditLimit(new BigDecimal("1000.00"), null));
    }

    @Test // LC-07
    void quemJaEstourouTemZeroDisponivel_nuncaNegativo() {
        Client client = comLimite("1000.00");

        assertEquals(BigDecimal.ZERO, client.creditAvailable(new BigDecimal("2500.00")));
    }
}
