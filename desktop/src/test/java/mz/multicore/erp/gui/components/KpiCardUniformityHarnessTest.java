package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class KpiCardUniformityHarnessTest {

    @Test
    @DisplayName("KPI-01: Uniformidade de dimensões e altura padrão (96px) em todas as variantes de cartões")
    void testStandardCardHeightUniformity() {
        assertEquals(96, KpiCard.STANDARD_CARD_HEIGHT, "A altura canónica de todos os cartões deve ser 96px");
        assertEquals(140, KpiCard.STANDARD_CARD_MIN_WIDTH, "A largura mínima de todos os cartões deve ser 140px");

        // 1. Cartão com gradiente (Dashboard style)
        JLabel dashVal = new JLabel("12.500,00 MT");
        ModernPanel dashCard = KpiCard.create(
                "VENDAS POS", "fas-cash-register", UIHelper.KPI_SUCCESS_SOFT,
                dashVal, new JLabel("12 vendas"), UIHelper.KPI_INFO_DARK, UIHelper.KPI_INFO_END
        );
        assertEquals(KpiCard.STANDARD_CARD_HEIGHT, dashCard.getPreferredSize().height,
                "Cartão gradiente deve ter altura padrão de 86px");
        assertEquals(KpiCard.STANDARD_CARD_HEIGHT, dashCard.getMinimumSize().height);

        // 2. Cartão universal reutilizável (createCard com JLabel)
        JLabel userVal = new JLabel("42");
        ModernPanel userCard = KpiCard.createCard(
                "TOTAL UTILIZADORES", userVal, "Contas registadas", "fas-users", UIHelper.ACCENT_BLUE
        );
        assertEquals(KpiCard.STANDARD_CARD_HEIGHT, userCard.getPreferredSize().height,
                "Cartão universal com JLabel deve ter altura padrão de 86px");
        assertEquals(KpiCard.STANDARD_CARD_HEIGHT, userCard.getMinimumSize().height);

        // 3. Cartão universal reutilizável (createCard com String direta)
        ModernPanel stringCard = KpiCard.createCard(
                "SALDO BANCÁRIO", "150.000,00 MT", "Posição oficial", "fas-university", UIHelper.APPROVED_GREEN
        );
        assertEquals(KpiCard.STANDARD_CARD_HEIGHT, stringCard.getPreferredSize().height,
                "Cartão universal com String deve ter altura padrão de 86px");
        assertEquals(KpiCard.STANDARD_CARD_HEIGHT, stringCard.getMinimumSize().height);

        // 4. Cartão métrico executivo (createMetricCard)
        JLabel metricVal = new JLabel("3.500,00 MT");
        ModernPanel metricCard = KpiCard.createMetricCard(
                "PERDA TOTAL", metricVal, "Quebras do mês", "fas-dollar-sign", UIHelper.REJECTED_RED, new BigDecimal("-2.5")
        );
        assertEquals(KpiCard.STANDARD_CARD_HEIGHT, metricCard.getPreferredSize().height,
                "Cartão métrico executivo deve ter altura padrão de 86px");
        assertEquals(KpiCard.STANDARD_CARD_HEIGHT, metricCard.getMinimumSize().height);
    }

    @Test
    @DisplayName("KPI-02: Grelha padrão unificada createGrid() com suporte a 4 e N colunas")
    void testStandardGridCreation() {
        JPanel grid4 = KpiCard.createGrid(4);
        assertNotNull(grid4);
        assertFalse(grid4.isOpaque(), "Grelha deve ser transparente para herdar o background do painel");
        assertTrue(grid4.getLayout() instanceof GridLayout);
        GridLayout gl = (GridLayout) grid4.getLayout();
        assertEquals(4, gl.getColumns());
        assertEquals(10, gl.getHgap());
        assertEquals(10, gl.getVgap());

        JPanel grid2 = KpiCard.createGrid(2, 8, 12);
        GridLayout gl2 = (GridLayout) grid2.getLayout();
        assertEquals(2, gl2.getColumns());
        assertEquals(8, gl2.getHgap());
        assertEquals(12, gl2.getVgap());
    }

    @Test
    @DisplayName("KPI-03: Cartão com TrendBadge reativo e cálculo percentual")
    void testTrendBadgeIntegration() {
        KpiCard.TrendBadge badge = new KpiCard.TrendBadge(new BigDecimal("12.5"), "vs mês anterior");
        assertTrue(badge.isPositive());
        assertFalse(badge.isNegative());
        assertFalse(badge.isZero());
        assertTrue(badge.getText().contains("12.5%"));

        JLabel valLbl = new JLabel("50.000,00 MT");
        ModernPanel card = KpiCard.createCard(
                "FATURAÇÃO", valLbl, badge, new JLabel("Ticket Médio: 500 MT"), "fas-chart-line", UIHelper.ACCENT_BLUE
        );
        assertNotNull(card);
        assertEquals(KpiCard.STANDARD_CARD_HEIGHT, card.getPreferredSize().height);
    }
}
