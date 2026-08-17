package mz.multicore.erp.modules.printing;

import com.lowagie.text.Paragraph;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Resumo de carga partilhado pelos documentos que movimentam mercadoria (PS-01..PS-06).
 * Ver docs/PESO_LOGISTICO_DOCUMENTOS_SPEC.md.
 */
class LoadSummaryRendererTest {

    private LoadSummaryRenderer.Item item(String name, String qty, String grossKg, int unitsPerBox) {
        return new LoadSummaryRenderer.Item(name, new BigDecimal(qty),
                grossKg == null ? null : new BigDecimal(grossKg), unitsPerBox);
    }

    private String text(Paragraph paragraph) {
        return paragraph == null ? null : paragraph.getContent();
    }

    @Test // PS-01
    void somaACargaPeloPesoBruto() {
        // 4 × 5,2 kg + 20 × 0,375 kg = 20,8 + 7,5 = 28,3 kg
        Paragraph resumo = LoadSummaryRenderer.build(List.of(
                item("Arroz 5kg", "4", "5.200", 6),
                item("Açúcar 1kg", "20", "0.375", 12)));

        assertNotNull(resumo);
        assertTrue(text(resumo).contains("Carga total: 28.300 kg"), text(resumo));
    }

    @Test // PS-02
    void semPesosNoCadastro_naoImprimeNada() {
        // Imprimir "Carga total: 0,000 kg" diria que a carrinha vai vazia — pior do que nada.
        assertNull(LoadSummaryRenderer.build(List.of(
                item("Arroz 5kg", "4", null, 6),
                item("Açúcar 1kg", "20", null, 12))));
    }

    @Test // PS-03
    void listaVaziaOuNulaNaoImprimeNada() {
        assertNull(LoadSummaryRenderer.build(List.of()));
        assertNull(LoadSummaryRenderer.build(null));
    }

    @Test // PS-04
    void artigosSemPesoNaoImpedemOsOutrosDeContar() {
        Paragraph resumo = LoadSummaryRenderer.build(List.of(
                item("Arroz 5kg", "2", "5.000", 6),
                item("Saco plástico", "100", null, 1)));

        assertNotNull(resumo);
        assertTrue(text(resumo).contains("Carga total: 10.000 kg"), text(resumo));
    }

    @Test // PS-05
    void mostraARepartricaoPorArtigoEmQuantidadeEEmPeso() {
        // O ponto útil: um artigo pode ser muita quantidade e pouco peso.
        Paragraph resumo = LoadSummaryRenderer.build(List.of(
                item("Arroz 5kg", "10", "5.000", 6),
                item("Esferovite", "10", "0.100", 1)));

        String texto = text(resumo);
        assertTrue(texto.contains("50.00% qtd"), texto);
        assertTrue(texto.contains("98.04% peso"), texto);
    }

    @Test // PS-06
    void descreveAQuantidadeEmCaixasEUnidades() {
        Paragraph resumo = LoadSummaryRenderer.build(List.of(
                item("Arroz 5kg", "14", "1.000", 6)));

        // 14 unidades com 6 por caixa = 2 caixas + 2 unidades.
        assertTrue(text(resumo).contains("2 cx + 2 un"), text(resumo));
    }
}
