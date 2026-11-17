package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Harness of empty tables that say what is missing and offer the next step (docs/ESTADO_VAZIO_SPEC.md). */
class EmptyStateGuidanceHarnessTest {

    @Test
    @DisplayName("EV-01: o título do cartão dá a frase do estado vazio (só títulos no plural)")
    void ev01_wordingFromTitle() {
        assertThat(EmptyWording.fromTitle("Cotações")).isEqualTo("Sem cotações.");
        assertThat(EmptyWording.fromTitle("Faturas de Compra Registadas")).isEqualTo("Sem faturas de compra registadas.");
        assertThat(EmptyWording.fromTitle("Lotes & Validades")).isEqualTo("Sem lotes e validades.");
        assertThat(EmptyWording.fromTitle("Movimentos — Todos os Documentos")).isEqualTo("Sem movimentos.");
        assertThat(EmptyWording.fromTitle("Retenções na Fonte (IVA)")).isEqualTo("Sem retenções na fonte.");
        assertThat(EmptyWording.fromTitle("Plano de Contas")).as("singular: fica o genérico").isNull();
        assertThat(EmptyWording.fromTitle("Razão")).isNull();
    }

    @Test
    @DisplayName("EV-02: tabela que nasce vazia mostra a frase e o botão principal, ligados depois do primeiro desenho")
    void ev02_linkAfterFirstPaintStillShows() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTable table = new JTable(new DefaultTableModel(new Object[]{"Nº", "Cliente"}, 0));
            JScrollPane scroll = new JScrollPane(table);
            TableEmptyState.install(scroll);
            JPanel overlay = overlay(scroll);
            assertThat(title(overlay)).isEqualTo("Sem registos.");

            ModernButton main = ButtonStyles.main(UIHelper.createPrimaryButton("Nova Cotação"));
            table.putClientProperty("emptyText", "Sem cotações.");
            table.putClientProperty(TableEmptyState.EMPTY_ACTION, main);

            assertThat(title(overlay)).isEqualTo("Sem cotações.");
            ModernButton action = (ModernButton) overlay.getClientProperty("actionButton");
            assertThat(action.isVisible()).isTrue();
            assertThat(action.getText()).isEqualTo("Nova Cotação");
        });
    }

    @Test
    @DisplayName("EV-03: com linhas escondidas pelos filtros diz \"Nenhum resultado com estes filtros.\"")
    void ev03_filteredWording() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DefaultTableModel model = new DefaultTableModel(new Object[]{"Nº"}, 0);
            model.addRow(new Object[]{"1"});
            JTable table = new JTable(model);
            table.putClientProperty("emptyText", "Sem cotações.");
            assertThat(TableEmptyState.resolveText(table)).isEqualTo(TableEmptyState.FILTERED_TEXT);
        });
    }

    @Test
    @DisplayName("EV-04: o cabeçalho do cartão liga título, botão principal e filtros à tabela do mesmo cartão")
    void ev04_cardHeaderWiresTheTable() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTable table = new JTable(new DefaultTableModel(new Object[]{"Nº"}, 0));
            ModernButton main = ButtonStyles.main(UIHelper.createPrimaryButton("Registar Compra"));
            JPanel card = new JPanel(new BorderLayout());
            JPanel top = UIHelper.tableCardTop("Faturas de Compra Registadas", null, main);
            card.add(top, BorderLayout.NORTH);
            card.add(new JScrollPane(table), BorderLayout.CENTER);
            JPanel host = new JPanel();
            host.add(card);
            javax.swing.JFrame frame = new javax.swing.JFrame();
            frame.add(host);
            frame.pack();
            frame.addNotify();
            // hierarchy events fire on addNotify; fall back to an explicit pass for headless runs
            if (table.getClientProperty(TableEmptyState.EMPTY_ACTION) == null) {
                top.dispatchEvent(new java.awt.event.HierarchyEvent(top, java.awt.event.HierarchyEvent.HIERARCHY_CHANGED,
                        top, card, java.awt.event.HierarchyEvent.PARENT_CHANGED));
            }
            assertThat(table.getClientProperty("emptyText")).isEqualTo("Sem faturas de compra registadas.");
            assertThat(table.getClientProperty(TableEmptyState.EMPTY_ACTION)).isSameAs(main);
            frame.dispose();
        });
    }

    @Test
    @DisplayName("EV-05: ligações instaladas centralmente (TableEmptyState escuta, TableCardTop deriva a frase)")
    void ev05_wiring() throws IOException {
        Path components = Path.of("src/main/java/mz/multicore/erp/gui/components");
        assertThat(Files.readString(components.resolve("TableEmptyState.java")))
                .contains("for (String key : new String[]{EMPTY_ACTION, EMPTY_FILTERS, EMPTY_TEXT, EMPTY_SUBTEXT})");
        assertThat(Files.readString(components.resolve("TableCardTop.java"))).contains("EmptyWording.fromTitle(heading)");
    }

    private static JPanel overlay(JScrollPane scroll) {
        for (Component c : scroll.getComponents()) {
            if (c instanceof JPanel panel && panel.getClientProperty("titleLabel") != null) return panel;
        }
        throw new AssertionError("overlay not installed");
    }

    private static String title(JPanel overlay) {
        return ((JLabel) overlay.getClientProperty("titleLabel")).getText();
    }
}
