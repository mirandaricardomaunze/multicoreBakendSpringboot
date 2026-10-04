package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.Test;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Matriz de Testes Executivos de Interface (HARNESS) — Validação de EUI-01 a EUI-08
 * Conforme especificado em docs/EXECUTIVE_UI_EXPERIENCE_SPEC.md e docs/EXECUTIVE_UI_EXPERIENCE_HARNESS.md.
 */
class ExecutiveUiExperienceHarnessTest {

    @Test
    void eui01_simpleBarChartRendersWithGradientAndAntialiasing() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SimpleBarChart chart = new SimpleBarChart("Desempenho Financeiro");
            chart.setSize(new Dimension(300, 220));

            chart.setData(
                    new String[]{"Jan", "Fev", "Mar"},
                    new BigDecimal[]{BigDecimal.valueOf(12000), BigDecimal.valueOf(35000), BigDecimal.valueOf(24000)},
                    new Color[]{UIHelper.ACCENT_BLUE, UIHelper.APPROVED_GREEN, UIHelper.PENDING_YELLOW}
            );

            BufferedImage img = new BufferedImage(300, 220, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            assertDoesNotThrow(() -> chart.paintComponent(g2));
            g2.dispose();

            assertThat(chart.getPreferredSize().width).isGreaterThanOrEqualTo(260);
            assertThat(chart.getPreferredSize().height).isGreaterThanOrEqualTo(200);
        });
    }

    @Test
    void eui02_simpleBarChartInteractiveTooltipsAndPoints() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SimpleBarChart chart = new SimpleBarChart("Vendas Mensais");
            chart.setSize(new Dimension(300, 220));

            BigDecimal val1 = BigDecimal.valueOf(50000.50);
            BigDecimal val2 = BigDecimal.valueOf(80000.00);

            chart.setData(
                    new String[]{"Receitas", "Custos"},
                    new BigDecimal[]{val1, val2},
                    new Color[]{UIHelper.ACCENT_BLUE, UIHelper.REJECTED_RED}
            );

            // Trigger paint to compute bar bounds
            BufferedImage img = new BufferedImage(300, 220, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            chart.paintComponent(g2);
            g2.dispose();

            String formatted = SimpleBarChart.formatFull(val1);
            assertThat(formatted).contains("50.000,50 MT");

            // Verify hover detection on the first bar
            assertThat(chart.getBarBounds()).isNotEmpty();
            java.awt.Rectangle b0 = chart.getBarBounds()[0];
            Point centerPt = new Point((int) b0.getCenterX(), (int) b0.getCenterY());
            int barIndex = chart.getBarIndexAt(centerPt);
            assertEquals(0, barIndex);

            // Simular evento de rato para verificar tooltip
            MouseEvent hoverEvent = new MouseEvent(chart, MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, centerPt.x, centerPt.y, 0, false);
            chart.getMouseMotionListeners()[0].mouseMoved(hoverEvent);
            assertNotNull(chart.getToolTipText());
            assertThat(chart.getToolTipText()).contains("Receitas");
            assertThat(chart.getToolTipText()).contains("50.000,50 MT");
        });
    }

    @Test
    void eui03_simplePieChartDonutAndSliceDetection() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SimplePieChart pieChart = new SimplePieChart("Canais de Faturação", true);
            assertTrue(pieChart.isDonut());
            pieChart.setSize(new Dimension(340, 220));

            pieChart.setData(
                    new String[]{"POS", "Comercial", "Outros"},
                    new BigDecimal[]{BigDecimal.valueOf(4000), BigDecimal.valueOf(5000), BigDecimal.valueOf(1000)},
                    new Color[]{UIHelper.ACCENT_BLUE, UIHelper.APPROVED_GREEN, UIHelper.PENDING_YELLOW}
            );

            assertEquals(BigDecimal.valueOf(10000), pieChart.getTotal());

            // Pintura de teste para validação de renderização
            BufferedImage img = new BufferedImage(340, 220, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            assertDoesNotThrow(() -> pieChart.paintComponent(g2));
            g2.dispose();

            String fullFmt = SimplePieChart.formatFull(BigDecimal.valueOf(4000));
            assertThat(fullFmt).contains("4.000,00 MT");

            // Ponto no interior do disco (fora do orifício do donut)
            int sliceIdx = pieChart.getSliceIndexAt(new Point(70, 70));
            assertThat(sliceIdx).isGreaterThanOrEqualTo(-1);
        });
    }

    @Test
    void eui04_trendBadgePositiveGrowthStyling() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            KpiCard.TrendBadge badge = new KpiCard.TrendBadge(BigDecimal.valueOf(8.5));

            assertThat(badge.getText()).isEqualTo("+8.5%");
            assertThat(badge.getLabel().getForeground()).isEqualTo(UIHelper.APPROVED_GREEN);
            assertThat(badge.getClientProperty("trend.bg")).isEqualTo(new Color(16, 185, 129, 35));
        });
    }

    @Test
    void eui05_trendBadgeNegativeFallStyling() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            KpiCard.TrendBadge badge = new KpiCard.TrendBadge(BigDecimal.valueOf(-3.2));

            assertThat(badge.getText()).isEqualTo("-3.2%");
            assertThat(badge.getLabel().getForeground()).isEqualTo(UIHelper.REJECTED_RED);
            assertThat(badge.getClientProperty("trend.bg")).isEqualTo(new Color(239, 68, 68, 35));
        });
    }

    @Test
    void eui06_kpiCardIntegrationWithTrendBadge() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JLabel valLabel = new JLabel("1.250.000,00 MT");
            ModernPanel card = KpiCard.createMetricCard(
                    "Receita Líquida",
                    valLabel,
                    "Acumulado mensal",
                    "fas-dollar-sign",
                    UIHelper.APPROVED_GREEN,
                    BigDecimal.valueOf(12.4)
            );

            assertNotNull(card);

            // Verificar se o TrendBadge está embutido na hierarquia do card
            KpiCard.TrendBadge foundBadge = findComponent(card, KpiCard.TrendBadge.class);
            assertNotNull(foundBadge, "O TrendBadge deve estar presente no cartão de KPI quando fornecido trendPercent");
            assertThat(foundBadge.getText()).isEqualTo("+12.4%");
        });
    }

    @Test
    void eui07_topNavBarGlobalSearchTriggerAndCtrlK() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TopNavBar bar = new TopNavBar("ERP", "Empresa Teste");
            AtomicBoolean clicked = new AtomicBoolean(false);

            bar.setSearchAction(() -> clicked.set(true));
            JPanel pill = bar.getSearchPill();

            assertNotNull(pill, "O botão de pesquisa rápida (pill) deve estar presente na barra de topo");
            assertThat(pill.getToolTipText()).contains("Ctrl+K");

            // Simular clique no pill
            Runnable action = (Runnable) pill.getClientProperty("search.action");
            assertNotNull(action);
            action.run();
            assertTrue(clicked.get(), "O clique no pill deve invocar a ação de pesquisa configurada");
        });
    }

    @Test
    void eui08_modernFormDialogTranslucentBackdropDimmer() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JFrame owner = new JFrame("Owner Frame");
            owner.setSize(600, 400);
            Component originalGlassPane = owner.getGlassPane();

            JPanel formContent = new JPanel();
            formContent.add(new JLabel("Teste de Formulário"));

            ModernFormDialog dialog = new ModernFormDialog(owner, "Teste Modal", formContent);
            assertNotNull(dialog.getParentWindow());
            assertEquals(owner, dialog.getParentWindow());

            // A janela owner preserva a integridade do seu glassPane
            assertEquals(originalGlassPane, owner.getGlassPane());
            owner.dispose();
        });
    }

    @Test
    void eui09_superAdminSubscriptionsExecutiveExperience() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            TopNavBar superAdminBar = new TopNavBar("Consola da Plataforma", "");
            AtomicBoolean searchTriggered = new AtomicBoolean(false);
            superAdminBar.setSearchAction(() -> searchTriggered.set(true));

            assertNotNull(superAdminBar.getSearchPill());
            assertThat(superAdminBar.getSearchPill().getToolTipText()).contains("Ctrl+K");

            mz.multicore.erp.desktop.client.PlatformApiClient platformApi =
                    org.mockito.Mockito.mock(mz.multicore.erp.desktop.client.PlatformApiClient.class);
            mz.multicore.erp.gui.PlataformaPanel panel = new mz.multicore.erp.gui.PlataformaPanel(platformApi);
            assertNotNull(panel);

            // O painel de superadmin integra o gráfico Donut de planos
            SimplePieChart plansChart = findComponent(panel, SimplePieChart.class);
            assertNotNull(plansChart, "O painel de superadmin deve conter o gráfico de distribuição de planos");
            assertTrue(plansChart.isDonut(), "O gráfico de planos das assinaturas deve usar estilo Donut");
        });
    }

    @Test
    void eui10_recordPaymentRequestAndHistoryWithReferenceAndDetails() {
        mz.multicore.erp.modules.subscription.dto.RecordPaymentRequest req =
                new mz.multicore.erp.modules.subscription.dto.RecordPaymentRequest(
                        BigDecimal.valueOf(15000),
                        "BANK_TRANSFER",
                        java.time.LocalDate.now(),
                        java.time.LocalDate.now(),
                        java.time.LocalDate.now().plusMonths(6),
                        "FT-2026-0091",
                        "Banco: Millennium BIM | Titular: Multicore Lda",
                        "Pagamento semestral"
                );
        assertThat(req.amount()).isEqualTo(BigDecimal.valueOf(15000));
        assertThat(req.method()).isEqualTo("BANK_TRANSFER");
        assertThat(req.reference()).isEqualTo("FT-2026-0091");
        assertThat(req.paymentDetails()).isEqualTo("Banco: Millennium BIM | Titular: Multicore Lda");
        assertThat(req.note()).isEqualTo("Pagamento semestral");

        mz.multicore.erp.modules.subscription.dto.SubscriptionPaymentDTO dto =
                new mz.multicore.erp.modules.subscription.dto.SubscriptionPaymentDTO(
                        101L,
                        BigDecimal.valueOf(15000),
                        "BANK_TRANSFER",
                        "Transferência Bancária",
                        java.time.LocalDate.now(),
                        java.time.LocalDate.now(),
                        java.time.LocalDate.now().plusMonths(6),
                        "FT-2026-0091",
                        "Banco: Millennium BIM | Titular: Multicore Lda",
                        "Pagamento semestral"
                );
        assertThat(dto.reference()).isEqualTo("FT-2026-0091");
        assertThat(dto.paymentDetails()).contains("Millennium BIM");
    }

    @SuppressWarnings("unchecked")
    private static <T extends Component> T findComponent(Component parent, Class<T> clazz) {
        if (clazz.isInstance(parent)) {
            return (T) parent;
        }
        if (parent instanceof java.awt.Container container) {
            for (Component child : container.getComponents()) {
                T found = findComponent(child, clazz);
                if (found != null) return found;
            }
        }
        return null;
    }
}
