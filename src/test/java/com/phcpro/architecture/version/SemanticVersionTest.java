package com.phcpro.architecture.version;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comparação de versões (AC-01..AC-08). Ver docs/ACTUALIZACOES_CLIENTE_SPEC.md.
 */
class SemanticVersionTest {

    @Test // AC-01
    void comparaNumeroANumero_naoComoTexto() {
        // O erro clássico: em ordem alfabética "1.10.0" < "1.9.0" e um cliente NOVO seria
        // bloqueado como se fosse velho.
        assertTrue(SemanticVersion.isOlderThan("1.9.0", "1.10.0"));
        assertFalse(SemanticVersion.isOlderThan("1.10.0", "1.9.0"));
    }

    @Test // AC-02
    void versoesIguaisNaoSaoMaisAntigas() {
        assertFalse(SemanticVersion.isOlderThan("1.4.2", "1.4.2"));
        assertEquals(0, SemanticVersion.compare("1.4.2", "1.4.2"));
    }

    @Test // AC-03
    void compararPelaOrdemMaiorMenorCorreccao() {
        assertTrue(SemanticVersion.isOlderThan("1.0.0", "2.0.0"));
        assertTrue(SemanticVersion.isOlderThan("1.2.9", "1.3.0"));
        assertTrue(SemanticVersion.isOlderThan("1.2.3", "1.2.4"));
        assertFalse(SemanticVersion.isOlderThan("2.0.0", "1.99.99"));
    }

    @Test // AC-04
    void partesEmFaltaValemZero() {
        assertEquals(0, SemanticVersion.compare("1.2", "1.2.0"));
        assertEquals(0, SemanticVersion.compare("1", "1.0.0"));
        assertTrue(SemanticVersion.isOlderThan("1.2", "1.2.1"));
    }

    @Test // AC-05
    void sufixosSaoIgnorados() {
        assertEquals(0, SemanticVersion.compare("1.2.0-SNAPSHOT", "1.2.0"));
        assertTrue(SemanticVersion.isOlderThan("1.1.0-rc1", "1.2.0"));
    }

    @Test // AC-06
    void versaoAusenteOuIlegivelContaComoAMaisAntiga() {
        // Conservador de propósito: quem não se identifica não pode ser tratado como o mais
        // recente — seria a forma mais fácil de contornar a política.
        assertTrue(SemanticVersion.isOlderThan(null, "1.0.0"));
        assertTrue(SemanticVersion.isOlderThan("", "1.0.0"));
        assertTrue(SemanticVersion.isOlderThan("versão-de-teste", "1.0.0"));
        assertTrue(SemanticVersion.isOlderThan("abc.def.ghi", "1.0.0"));
    }

    @Test // AC-07
    void minimoZeroNaoBloqueiaNinguem() {
        // O default do sistema: enquanto a mínima for 0.0.0, nenhuma versão fica de fora.
        assertFalse(SemanticVersion.isOlderThan("0.0.0", "0.0.0"));
        assertFalse(SemanticVersion.isOlderThan("1.0.0", "0.0.0"));
        assertFalse(SemanticVersion.isOlderThan(null, "0.0.0"));
        assertFalse(SemanticVersion.isOlderThan(ClientVersion.UNKNOWN, "0.0.0"));
    }

    @Test // AC-08
    void espacosNaoConfundem() {
        assertEquals(0, SemanticVersion.compare(" 1.2.0 ", "1.2.0"));
    }
}
