package mz.multicore.erp.gui.tv;

import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.comercial.dto.InvoiceLineDTO;
import mz.multicore.erp.modules.purchases.dto.BusinessUnitDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JComponent;
import java.awt.Component;
import java.awt.Container;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Harness de auditoria e testes unitários para a decomposição do ExecutiveTvDashboardDialog.
 */
class ExecutiveTvDashboardDecompositionHarnessTest {

    private static final String TV_PACKAGE_PATH = "src/main/java/mz/multicore/erp/gui/tv/";

    @Test
    @DisplayName("Garante limites estritos de linhas físicas em todos os componentes de TV")
    void testLineCountThresholds() throws IOException {
        assertLineCount("ExecutiveTvDashboardDialog.java", 215);
        assertLineCount("ExecutiveTvTheme.java", 120);
        assertLineCount("ExecutiveTvCalculationHelper.java", 100);
        assertLineCount("ExecutiveTvHeaderBuilder.java", 230);
        assertLineCount("ExecutiveTvConsolidatedViewBuilder.java", 230);
        assertLineCount("ExecutiveTvUnitViewBuilder.java", 225);
        assertLineCount("ExecutiveTvTimerCoordinator.java", 120);
    }

    private void assertLineCount(String fileName, int maxLines) throws IOException {
        File file = new File(TV_PACKAGE_PATH + fileName);
        assertTrue(file.exists(), "Ficheiro não encontrado: " + file.getAbsolutePath());
        long lines = Files.lines(file.toPath()).count();
        assertTrue(lines <= maxLines,
                String.format("Ficheiro %s excedeu o teto estrito: %d linhas (máximo permitido: %d)", fileName, lines, maxLines));
    }

    @Test
    @DisplayName("Garante 100% de erradicação de JOptionPane no pacote de TV Executiva")
    void testZeroJOptionPaneUsage() throws IOException {
        File dir = new File(TV_PACKAGE_PATH);
        File[] javaFiles = dir.listFiles((d, name) -> name.endsWith(".java"));
        assertNotNull(javaFiles, "Diretório não encontrado: " + dir.getAbsolutePath());

        for (File f : javaFiles) {
            String content = Files.readString(f.toPath());
            assertFalse(content.contains("JOptionPane"),
                    "Uso proibido de JOptionPane detectado em: " + f.getName());
        }
    }

    @Test
    @DisplayName("Valida agregação de vendas, faturamento e ranking de top artigos")
    void testCalculationHelperPureLogic() {
        LocalDateTime now = LocalDateTime.now();
        InvoiceLineDTO line1 = new InvoiceLineDTO(1L, 10L, "Artigo Alpha", "ALPHA", new BigDecimal("10.00"),
                new BigDecimal("100.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), BigDecimal.ZERO, null, null);
        InvoiceLineDTO line2 = new InvoiceLineDTO(2L, 20L, "Artigo Beta", "BETA", new BigDecimal("5.00"),
                new BigDecimal("50.00"), BigDecimal.ZERO, new BigDecimal("250.00"), BigDecimal.ZERO, null, null);

        InvoiceDTO inv1 = new InvoiceDTO(1L, "FT-001", 100L, "Cliente A", "999999999",
                new BigDecimal("1000.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), BigDecimal.ZERO,
                mz.multicore.erp.modules.comercial.model.InvoiceStatus.APPROVED, null, List.of(line1),
                now, "admin", null, 0, null, 1L, "Unidade 1");

        InvoiceDTO inv2 = new InvoiceDTO(2L, "FT-002", 101L, "Cliente B", "888888888",
                new BigDecimal("250.00"), BigDecimal.ZERO, new BigDecimal("250.00"), new BigDecimal("250.00"),
                mz.multicore.erp.modules.comercial.model.InvoiceStatus.PAID, null, List.of(line2),
                now, "admin", null, 0, null, 2L, "Unidade 2");

        List<InvoiceDTO> invoices = List.of(inv1, inv2);

        // Filtragem
        List<InvoiceDTO> filtered = ExecutiveTvCalculationHelper.filterInvoices(invoices, ExecutiveTvDashboardDialog.Period.HOJE);
        assertEquals(2, filtered.size());

        // Faturamento total por unidade
        BigDecimal bu1Total = ExecutiveTvCalculationHelper.calculateTotalBilled(filtered, 1L);
        assertEquals(0, new BigDecimal("1000.00").compareTo(bu1Total));

        BigDecimal bu2Total = ExecutiveTvCalculationHelper.calculateTotalBilled(filtered, 2L);
        assertEquals(0, new BigDecimal("250.00").compareTo(bu2Total));

        // Ranking de produtos
        List<ExecutiveTvCalculationHelper.ProductMetric> metrics = ExecutiveTvCalculationHelper.aggregateTopProducts(filtered, null);
        assertEquals(2, metrics.size());
        assertEquals("Artigo Alpha", metrics.get(0).name());
        assertEquals(0, new BigDecimal("1000.00").compareTo(metrics.get(0).revenue()));
        assertEquals("Artigo Beta", metrics.get(1).name());
        assertEquals(0, new BigDecimal("250.00").compareTo(metrics.get(1).revenue()));
    }

    @Test
    @DisplayName("Valida consistência visual dos crachás de status sem emojis e KPIs corporativos")
    void testStatusBadgeAndKpiContract() {
        JLabel badge100 = ExecutiveTvTheme.buildStatusBadge(105.0);
        assertTrue(badge100.getText().contains("META SUPERADA"));
        assertNoForbiddenUnicode(badge100.getText());

        JLabel badge85 = ExecutiveTvTheme.buildStatusBadge(85.0);
        assertTrue(badge85.getText().contains("NO RITMO"));
        assertNoForbiddenUnicode(badge85.getText());

        JLabel badge60 = ExecutiveTvTheme.buildStatusBadge(60.0);
        assertTrue(badge60.getText().contains("ATENÇÃO"));
        assertNoForbiddenUnicode(badge60.getText());

        JLabel badge30 = ExecutiveTvTheme.buildStatusBadge(30.0);
        assertTrue(badge30.getText().contains("ALERTA"));
        assertNoForbiddenUnicode(badge30.getText());

        JPanel kpiDark = ExecutiveTvTheme.buildTvKpi("TESTE", "100,00 MT", "Subtítulo", java.awt.Color.BLUE, false);
        assertNotNull(kpiDark);

        JPanel kpiLight = ExecutiveTvTheme.buildTvKpi("TESTE", "100,00 MT", "Subtítulo", java.awt.Color.BLUE, true);
        assertNotNull(kpiLight);
    }

    @Test
    void opaqueTvBadgesHaveSolidBackgroundInBothThemes() {
        for (boolean lightMode : new boolean[]{true, false}) {
            assertEquals(255, ExecutiveTvTheme.getHeaderBgColor(lightMode).getAlpha());
            for (double pct : new double[]{0, 60, 85, 105}) {
                assertOpaqueBackground(ExecutiveTvTheme.buildStatusBadge(pct, lightMode));
            }

            ExecutiveTvHeaderBuilder.HeaderActions actions = (ExecutiveTvHeaderBuilder.HeaderActions)
                    Proxy.newProxyInstance(getClass().getClassLoader(),
                            new Class<?>[]{ExecutiveTvHeaderBuilder.HeaderActions.class},
                            (proxy, method, args) -> null);
            ExecutiveTvHeaderBuilder headerBuilder = new ExecutiveTvHeaderBuilder(actions);
            JPanel header = headerBuilder.build(lightMode, ExecutiveTvDashboardDialog.Period.HOJE);
            assertOpaqueBackground(header);
            headerBuilder.applyTheme(!lightMode);
            assertOpaqueBackground(header);

            BusinessUnitDTO unit = new BusinessUnitDTO(987654L, "AUDIT", "Auditoria", null, null, null, true);
            JPanel consolidated = ExecutiveTvConsolidatedViewBuilder.build(
                    List.of(unit), List.of(), ExecutiveTvDashboardDialog.Period.HOJE, lightMode);
            assertOpaqueBackground(consolidated);

            SellerPerformanceDialog sellers = new SellerPerformanceDialog(null, List.of(), "Hoje", lightMode);
            try {
                assertOpaqueBackground(sellers.getContentPane());
            } finally {
                sellers.dispose();
            }
        }
    }

    private void assertOpaqueBackground(Component component) {
        if (component instanceof JComponent ui && ui.isOpaque()) {
            assertNotNull(ui.getBackground());
            assertEquals(255, ui.getBackground().getAlpha(), component.getClass().getSimpleName());
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) assertOpaqueBackground(child);
        }
    }

    private void assertNoForbiddenUnicode(String text) {
        for (char c : text.toCharArray()) {
            // Rejeita emojis ou caracteres especiais da faixa suplementar ou símbolos gráficos
            assertTrue(c < 0x2000 || c > 0x2bff, "Caractere potencialmente não suportado em Java 2D Windows: " + c);
        }
    }
}
