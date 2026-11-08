package mz.multicore.erp.gui.sector;

import mz.multicore.erp.desktop.client.*;
import mz.multicore.erp.gui.ComercialPanel;
import mz.multicore.erp.gui.components.ModernPanel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** UI harness for paid material awaiting pickup ({@code docs/SECTORES_ACTIVIDADE_HARNESS.md}, LV-UI-xx). */
class PickupUiHarnessTest {

    @Test
    @DisplayName("LV-UI-03: Pickup tables live inside cards")
    void tablesInsideCards() {
        List<Component> tables = new ArrayList<>();
        collect(new PickupsPanel(mock(PickupApiClient.class)), JTable.class, tables);
        assertThat(tables).hasSize(3);
        for (Component table : tables) {
            assertThat(SwingUtilities.getAncestorOfClass(ModernPanel.class, table)).isNotNull();
        }
    }

    @Test
    @DisplayName("GR-UI-01: Stock gains one \"Granel\" tab at the end, with its tables inside cards")
    void stockGainsBulkTabOnce() {
        mz.multicore.erp.gui.StockPanel stock = new mz.multicore.erp.gui.StockPanel(mock(InventoryApiClient.class),
                mock(ComercialApiClient.class), null, null, null, mock(PrintApiClient.class), null);
        JTabbedPane tabs = directTabs(stock);
        int before = tabs.getTabCount();
        assertThat(SectorDesktopWiring.installBulkStock(stock, mock(BulkStockApiClient.class),
                mock(InventoryApiClient.class), mock(ComercialApiClient.class))).isTrue();
        assertThat(SectorDesktopWiring.installBulkStock(stock, mock(BulkStockApiClient.class),
                mock(InventoryApiClient.class), mock(ComercialApiClient.class))).isTrue();
        assertThat(tabs.getTabCount()).isEqualTo(before + 1);
        assertThat(tabs.getTitleAt(before)).isEqualTo("Granel");
        List<Component> tables = new ArrayList<>();
        collect((Container) tabs.getComponentAt(before), JTable.class, tables);
        assertThat(tables).isNotEmpty();
        for (Component table : tables) assertThat(SwingUtilities.getAncestorOfClass(ModernPanel.class, table)).isNotNull();
    }

    @Test
    @DisplayName("UN-UI-01: Invoice and order grids gain Ctrl+U \"Quantidade noutra unidade\" once")
    void draftGridsGainUnitEntry() {
        ComercialPanel panel = comercial();
        assertThat(SectorDesktopWiring.installUnitQuantityEntry(panel, mock(UnitsApiClient.class))).isEqualTo(2);
        assertThat(SectorDesktopWiring.installUnitQuantityEntry(panel, mock(UnitsApiClient.class))).as("idempotent").isZero();
        for (JTable table : mz.multicore.erp.gui.CommercialDraftLineAccess.draftTables(panel)) {
            assertThat(table.getColumnName(1)).isEqualTo("Qtd");
            assertThat(table.getComponentPopupMenu()).isNotNull();
            assertThat(table.getActionMap().get(SectorDesktopWiring.UNIT_ENTRY_KEY)).isNotNull();
            assertThat(mz.multicore.erp.gui.CommercialDraftLineAccess.selectedProductId(panel, table)).isNull();
            assertThat(mz.multicore.erp.gui.CommercialDraftLineAccess.setSelectedQuantity(panel, table, java.math.BigDecimal.TEN))
                    .as("no selection, no write").isFalse();
        }
        assertThat(SectorDesktopWiring.installUnitQuantityEntry(new JPanel(), mock(UnitsApiClient.class))).isZero();
    }

    @Test
    @DisplayName("FR-UI-01: Draft grids gain \"Adicionar frete\" once; zone codes are stable and safe")
    void draftGridsGainFreightEntry() {
        ComercialPanel panel = comercial();
        assertThat(SectorDesktopWiring.installFreightEntry(panel, mock(ComercialApiClient.class))).isEqualTo(2);
        assertThat(SectorDesktopWiring.installFreightEntry(panel, mock(ComercialApiClient.class))).isZero();
        assertThat(FreightZoneDialog.slug("Matola — Machava")).isEqualTo("MATOLA-MACHAVA");
        assertThat(FreightZoneDialog.slug("Boane (até 30 km)")).isEqualTo("BOANE-ATE-30-KM");
        assertThat(FreightZoneDialog.slug("***")).isEqualTo("ZONA");
    }

    @Test
    @DisplayName("ED-UI-01: Grids show Unidade and Frete buttons, remove waits for a selection, total near the buttons")
    void draftToolbarExtras() {
        ComercialPanel panel = comercial();
        SectorDesktopWiring.installUnitQuantityEntry(panel, mock(UnitsApiClient.class));
        SectorDesktopWiring.installFreightEntry(panel, mock(ComercialApiClient.class));
        assertThat(SectorDesktopWiring.installDraftToolbarExtras(panel)).isEqualTo(2);
        assertThat(SectorDesktopWiring.installDraftToolbarExtras(panel)).as("idempotent").isZero();
        for (JTable table : mz.multicore.erp.gui.CommercialDraftLineAccess.draftTables(panel)) {
            Container card = SwingUtilities.getAncestorOfClass(ModernPanel.class, table);
            List<String> texts = new ArrayList<>();
            List<Component> buttons = new ArrayList<>();
            collect(card, AbstractButton.class, buttons);
            for (Component b : buttons) texts.add(((AbstractButton) b).getText());
            assertThat(buttons.stream().map(Component::getName).toList()).contains("Unidade", "Frete");
            AbstractButton remove = (AbstractButton) buttons.stream()
                    .filter(b -> ((AbstractButton) b).getText() != null && ((AbstractButton) b).getText().startsWith("Remover"))
                    .findFirst().orElseThrow();
            assertThat(remove.isEnabled()).as("nothing selected").isFalse();
            List<Component> bars = new ArrayList<>();
            collect(card, DraftTotalsBar.class, bars);
            assertThat(bars).as("professional totals summary in each draft grid").hasSize(1);
        }
        List<Component> labels = new ArrayList<>();
        collect(panel, JLabel.class, labels);
        assertThat(labels.stream().map(l -> ((JLabel) l).getText()).filter(t -> t != null && t.startsWith("Entregar em"))).isNotEmpty();
    }

    private static ComercialPanel comercial() {
        return new ComercialPanel(mock(ComercialApiClient.class), mock(InventoryApiClient.class),
                mock(FinanceApiClient.class), mock(CreditNoteApiClient.class), mock(DebitNoteApiClient.class),
                mock(POSApiClient.class), mock(MovimentosApiClient.class), mock(PromotionApiClient.class),
                mock(PrintApiClient.class), mock(ProductionApiClient.class), mock(FleetApiClient.class), () -> {});
    }

    private static JTabbedPane directTabs(Container root) {
        for (Component child : root.getComponents()) {
            if (child instanceof JTabbedPane tabs) return tabs;
        }
        return null;
    }

    private static void collect(Container root, Class<?> type, List<Component> out) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) out.add(child);
            if (child instanceof Container container) collect(container, type, out);
        }
    }
}
