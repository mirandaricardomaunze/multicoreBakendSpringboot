package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.Test;
import org.kordamp.ikonli.fontawesome5.FontAwesomeBrands;
import org.kordamp.ikonli.fontawesome5.FontAwesomeRegular;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Harness UP-01..UP-09: valores legíveis, rodapé certo e dados internos fora do ecrã. */
class UiPrecisionHarnessTest {

    private static final Path GUI = Path.of("src/main/java/mz/multicore/erp/gui");

    private List<Path> sources() throws Exception {
        try (Stream<Path> files = Files.walk(GUI)) {
            return files.filter(file -> file.toString().endsWith(".java")).toList();
        }
    }

    @Test // UP-01
    void todosOsIconesExistem() throws Exception {
        Set<String> known = new HashSet<>();
        for (var icon : FontAwesomeSolid.values()) known.add(icon.getDescription());
        for (var icon : FontAwesomeRegular.values()) known.add(icon.getDescription());
        for (var icon : FontAwesomeBrands.values()) known.add(icon.getDescription());
        Pattern code = Pattern.compile("\"(fa[srb]-[a-z0-9-]+)\"");
        List<String> missing = new ArrayList<>();
        for (Path file : sources()) {
            Matcher matcher = code.matcher(Files.readString(file));
            while (matcher.find()) {
                if (!known.contains(matcher.group(1))) missing.add(file.getFileName() + ": " + matcher.group(1));
            }
        }
        assertEquals(List.of(), missing);
    }

    @Test // UP-02
    void acaoDeCriarUsaSempreVerde() throws Exception {
        Pattern blue = Pattern.compile("createPrimaryButton\\(\"(Nov[oa]|Registar|Emitir|Criar|Adicionar|Gerar)\\b");
        List<String> wrong = new ArrayList<>();
        for (Path file : sources()) {
            Matcher matcher = blue.matcher(Files.readString(file));
            while (matcher.find()) wrong.add(file.getFileName() + ": " + matcher.group());
        }
        assertEquals(List.of(), wrong);
    }

    @Test // UP-03
    void rodapeContaAsLinhasDeTabelaSemOrdenador() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DefaultTableModel model = new DefaultTableModel(new String[]{"Documento"}, 0);
            JTable table = new JTable(model);
            UIHelper.styleTable(table);
            JPanel card = new JPanel(new BorderLayout());
            card.add(new JScrollPane(table), BorderLayout.CENTER);
            ClientTablePagination.install(table);
            Object installed = table.getClientProperty("clientPagination.instance");
            model.addRow(new Object[]{"FT-2026/1"});
            assertEquals(installed, table.getClientProperty("clientPagination.instance"));
        });
        SwingUtilities.invokeAndWait(() -> { });
    }

    @Test // UP-04, UP-05
    void valorNuncaFicaCortadoETextoLivrePodeEncolher() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DefaultTableModel model = new DefaultTableModel(new String[]{"Cliente", "Total", "Data"}, 0);
            model.addRow(new Object[]{"Sociedade Comercial e Industrial de Distribuição do Niassa e Cabo Delgado",
                    new BigDecimal("471612885.40"), "02/11/2026 04:39"});
            JTable table = new JTable(model);
            UIHelper.styleTable(table);
            TableColumnFit.fit(table);
            for (int column : new int[]{1, 2}) {
                int needed = table.prepareRenderer(table.getCellRenderer(0, column), 0, column).getPreferredSize().width;
                assertTrue(table.getColumnModel().getColumn(column).getMinWidth() >= needed);
            }
            assertTrue(table.getColumnModel().getColumn(0).getMinWidth() < 200);
        });
    }

    @Test // UP-06
    void valorDoCartaoReduzAteCaber() {
        JLabel label = new JLabel("471.612.885,40 MT");
        label.setFont(new java.awt.Font(UIHelper.FONT, java.awt.Font.BOLD, 20));
        int available = label.getFontMetrics(label.getFont()).stringWidth(label.getText()) - 40;
        KpiTextFit.apply(label, available, 20, 11);
        assertTrue(label.getFont().getSize() < 20);
        assertTrue(label.getFontMetrics(label.getFont()).stringWidth(label.getText()) <= available);
    }

    @Test // UP-07
    void loteGeradoPeloSistemaNaoAparece() {
        assertEquals("—", BatchDisplay.number("AUTO-1793563383357"));
        assertEquals("—", BatchDisplay.number("LEGACY-1"));
        assertEquals("L-2026-07", BatchDisplay.number("L-2026-07"));
    }

    @Test // UP-08
    void validadeSemValorRealNaoAparece() {
        assertNull(BatchDisplay.expiry(LocalDate.of(9999, 12, 31)));
        assertEquals(LocalDate.of(2027, 1, 31), BatchDisplay.expiry(LocalDate.of(2027, 1, 31)));
    }

    @Test // UP-09
    void acaoDeAuditoriaFicaEmPortugues() {
        assertEquals("Encomenda a fornecedor: recepção parcial",
                AuditActionLabels.label("PURCHASE_ORDER_RECEIVE_PARTIAL"));
        assertEquals("Fatura: emissão", AuditActionLabels.label("INVOICE_ISSUE"));
        assertFalse(AuditActionLabels.label("POS_CLOSE_SESSION").contains("_"));
    }
}
