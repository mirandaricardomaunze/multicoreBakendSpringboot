package mz.multicore.erp.gui.sector;

import mz.multicore.erp.desktop.client.*;
import mz.multicore.erp.gui.ComercialPanel;
import mz.multicore.erp.gui.ConfigPanel;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.modules.units.dto.UnitOfMeasureDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * UI harness for business-sector phase F1 ({@code docs/SECTORES_ACTIVIDADE_HARNESS.md}, SA-UI-xx).
 * It guarantees the new screens arrive without disturbing existing ones: same tab strips, legacy constructors
 * intact, tables inside cards and display rules matching the server.
 */
class SectorFoundationUiHarnessTest {

    @Test
    @DisplayName("SA-UI-01: O Comercial fica como era: 10 separadores e Promoções sem secções embutidas")
    void comercialStaysUntouched() {
        ComercialPanel panel = new ComercialPanel(mock(ComercialApiClient.class), mock(InventoryApiClient.class),
                mock(FinanceApiClient.class), mock(CreditNoteApiClient.class), mock(DebitNoteApiClient.class),
                mock(POSApiClient.class), mock(MovimentosApiClient.class), mock(PromotionApiClient.class),
                mock(PrintApiClient.class), mock(ProductionApiClient.class), mock(FleetApiClient.class), () -> {});
        JTabbedPane tabs = firstTabbedPane(panel);
        assertThat(tabs.getTabCount()).isEqualTo(10);
        assertThat(tabs.getComponentAt(tabs.indexOfTab("Promoções")))
                .isInstanceOf(mz.multicore.erp.gui.PromotionsPanel.class);
    }

    @Test
    @DisplayName("SA-UI-02: Sem cliente das unidades, o detalhe do produto não ganha o separador Unidades")
    void stockUnchangedWithoutUnitsClient() {
        JPanel stock = new JPanel();
        assertThat(SectorDesktopWiring.productUnitsTab(stock, 1L)).isNull();
        SectorDesktopWiring.attachUnits(stock, mock(UnitsApiClient.class));
        assertThat(SectorDesktopWiring.productUnitsTab(stock, 1L)).isInstanceOf(ProductUnitsPanel.class);
        assertThat(SectorDesktopWiring.productUnitsTab(stock, null)).isNull();
    }

    @Test
    @DisplayName("SA-UI-03: As Configurações ganham o Perfil de Actividade e a Balança deste Posto no fim, sem mudar a ordem das outras")
    void configAppendsActivityTab() {
        ConfigPanel panel = new ConfigPanel(mock(UserApiClient.class), mock(AuditApiClient.class),
                mock(BackupApiClient.class), mock(DocumentConfigApiClient.class), mock(SupportApiClient.class),
                mock(MySubscriptionApiClient.class), mock(SystemMonitoringApiClient.class), mock(ActivityApiClient.class));
        JTabbedPane tabs = firstTabbedPane(panel);
        assertThat(tabs.getTabCount()).isEqualTo(9);
        assertThat(tabs.getTitleAt(0)).isEqualTo("Log de Auditoria Geral");
        assertThat(tabs.getTitleAt(6)).isEqualTo("Perfil de Actividade");
        assertThat(tabs.getComponentAt(6)).isInstanceOf(ActivityProfilePanel.class);
        assertThat(tabs.getTitleAt(7)).as("the workstation scale follows the activity profile").isEqualTo("Balança deste Posto");
        assertThat(tabs.getTitleAt(8)).as("then the cash drawer of the workstation").isEqualTo("Gaveta deste Posto");
    }

    @Test
    @DisplayName("SA-UI-04: Todas as tabelas novas estão dentro de um card ModernPanel")
    void tablesLiveInsideCards() {
        List<Component> tables = new ArrayList<>();
        collect(new PriceListsPanel(mock(PricingApiClient.class), mock(ComercialApiClient.class)), JTable.class, tables);
        collect(new ProductUnitsPanel(null, 1L), JTable.class, tables);
        assertThat(tables).hasSize(4);
        for (Component table : tables) {
            assertThat(SwingUtilities.getAncestorOfClass(ModernPanel.class, table))
                    .as("tabela %s fora de card", table).isNotNull();
        }
    }

    @Test
    @DisplayName("SA-UI-05: O desktop mostra o mesmo factor fixo que o servidor calcula")
    void fixedFactorMatchesServerRule() {
        UnitOfMeasureDTO kg = unit("KG", "MASS", "1", 3);
        UnitOfMeasureDTO g = unit("G", "MASS", "0.001", 0);
        UnitOfMeasureDTO ton = unit("TON", "MASS", "1000", 3);
        UnitOfMeasureDTO saco = unit("SACO", "PACKAGE", null, 0);
        UnitOfMeasureDTO m3 = unit("M3", "VOLUME", "1000", 3);
        assertThat(ProductUnitFormDialog.fixedFactor(g, kg)).isEqualByComparingTo("0.001");
        assertThat(ProductUnitFormDialog.fixedFactor(ton, kg)).isEqualByComparingTo("1000");
        assertThat(ProductUnitFormDialog.fixedFactor(kg, ton)).isEqualByComparingTo("0.001");
        assertThat(ProductUnitFormDialog.fixedFactor(saco, kg)).isNull();
        assertThat(ProductUnitFormDialog.fixedFactor(m3, kg)).as("grandezas diferentes").isNull();
    }

    @Test
    @DisplayName("SA-UI-06: Números lidos e mostrados no formato da aplicação")
    void numbersFollowCentralFormat() {
        assertThat(ProductUnitFormDialog.parse("1.234,5")).isEqualByComparingTo("1234.5");
        assertThat(ProductUnitFormDialog.parse("40")).isEqualByComparingTo("40");
        assertThat(ProductUnitFormDialog.parse("0")).isNull();
        assertThat(ProductUnitFormDialog.parse("abc")).isNull();
        assertThat(ProductUnitsPanel.plain(new BigDecimal("40.000000"))).doesNotContain(",");
        assertThat(ProductUnitsPanel.plain(new BigDecimal("0.001000"))).contains("001");
        assertThat(PriceListsPanel.difference(new BigDecimal("900"), new BigDecimal("1000"))).startsWith("-10");
        assertThat(PriceListsPanel.difference(new BigDecimal("1000"), new BigDecimal("1000"))).isEqualTo("Igual");
        assertThat(PriceListsPanel.difference(new BigDecimal("1100"), new BigDecimal("1000"))).startsWith("+10");
    }

    @Test
    @DisplayName("SA-UI-07: Cada perfil tem ícone e cor; ecrãs novos sem emojis nem cores soltas")
    void visualsAndSourceRules() throws Exception {
        for (String code : List.of("GENERAL_RETAIL", "GROCERY", "WHOLESALE", "BUTCHER", "BAKERY", "BUILDING_MATERIALS",
                "BLOCK_FACTORY", "HARDWARE", "FRUIT_VEG", "BEVERAGES", "GAS", "FASHION", "AGRO", "REPACKING", "SERVICES")) {
            assertThat(SectorVisuals.profile(code).iconCode()).as(code).startsWith("fas-");
        }
        Path dir = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui", "sector");
        try (Stream<Path> files = Files.list(dir)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file);
                assertThat(source).as(file.toString()).doesNotContain("new Color(", "JOptionPane", "getSelectedRow()");
                // Two-column forms put the main field beside an unrelated one (seen in "Registar Material por Levantar").
                assertThat(source.replace("createDialogForm(1, new Object[]{", ""))
                        .as("single-column dialog forms in %s", file).doesNotContain("createDialogForm(");
                assertThat(source.codePoints().filter(cp -> cp >= 0x1F300).count()).as("emojis em %s", file).isZero();
            }
        }
    }

    private static UnitOfMeasureDTO unit(String code, String dimension, String factor, int decimals) {
        return new UnitOfMeasureDTO(1L, code, code, code.toLowerCase(), dimension, dimension,
                factor == null ? null : new BigDecimal(factor), decimals, true);
    }

    private static JTabbedPane firstTabbedPane(Container root) {
        List<Component> found = new ArrayList<>();
        collect(root, JTabbedPane.class, found);
        return found.isEmpty() ? null : (JTabbedPane) found.get(0);
    }

    private static void collect(Container root, Class<?> type, List<Component> out) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) out.add(child);
            if (child instanceof Container container) collect(container, type, out);
        }
    }
}
