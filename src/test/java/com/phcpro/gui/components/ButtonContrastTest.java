package com.phcpro.gui.components;

import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regressão do botão invisível: no tema <b>claro</b>, o estado inactivo de um segmented
 * control (ex.: <i>Venda POS | Histórico de Vendas</i>) usa {@code BG_CARD}, que é branco
 * puro. Como o {@code ModernButton} pintava sempre o texto a branco, o botão desaparecia
 * até o rato lhe passar por cima. No tema escuro o problema não existia — daí ter passado.
 */
class ButtonContrastTest {

    /** Mínimo WCAG AA para texto de tamanho normal. */
    private static final double AA = 4.5;

    @Test // BC-01
    void textoSobreFundoBranco_naoPodeSerBranco() {
        Color fg = UIHelper.readableTextOn(Color.WHITE);

        assertNotEquals(Color.WHITE, fg, "texto branco sobre fundo branco é invisível");
        assertTrue(UIHelper.contrastRatio(Color.WHITE, fg) >= AA,
                "contraste insuficiente sobre branco: " + UIHelper.contrastRatio(Color.WHITE, fg));
    }

    @Test // BC-02
    void estadoInactivoDoSegmentedControl_eLegivelNosDoisTemas() {
        for (Theme theme : new Theme[]{Theme.LIGHT, Theme.DARK}) {
            // Fundo inactivo (card) e o de hover (border) — os dois usados por selectView.
            for (Color background : new Color[]{theme.card, theme.border}) {
                Color fg = UIHelper.readableTextOn(background);
                double ratio = UIHelper.contrastRatio(background, fg);
                assertTrue(ratio >= AA, "tema " + theme.id + ": contraste " + ratio + " sobre " + background);
            }
        }
    }

    @Test // BC-03
    void fundosDeAccaoContinuamComTextoBranco() {
        // Não-regressão de design: sobre o azul o texto preto até contrastaria mais, mas o
        // sistema quer branco nos botões de acção. A regra é por limiar, não por máximo.
        for (Color background : new Color[]{
                new Color(59, 130, 246),   // azul   — acção primária
                new Color(22, 163, 74),    // verde  — Abrir Caixa
                new Color(220, 38, 38),    // vermelho — Fechar Caixa
                Theme.DARK.card}) {        // tema escuro inalterado
            assertEquals(Color.WHITE, UIHelper.readableTextOn(background),
                    "fundo " + background + " devia manter texto branco");
        }
    }

    @Test // BC-04
    void setColors_actualizaOTextoJuntoComOFundo() {
        ModernButton button = new ModernButton("Histórico de Vendas");

        button.setColors(Theme.LIGHT.card, Theme.LIGHT.border);

        assertNotEquals(Color.WHITE, button.getForeground(),
                "ao passar a inactivo com fundo claro, o texto tem de escurecer");
        assertTrue(UIHelper.contrastRatio(Theme.LIGHT.card, button.getForeground()) >= AA);
    }
}
