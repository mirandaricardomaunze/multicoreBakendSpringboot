package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.Dimension;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Harness de Tradução e Filtragem de Estados em PT-MZ")
class TableStatusPtMzHarnessTest {

    private static final Path GUI_ROOT = Path.of("src", "main", "java", "mz", "multicore", "erp", "gui");

    @Test
    @DisplayName("UIHelper.humanStatus traduz todos os estados de negócio canónicos para Português de Moçambique")
    void humanStatus_translatesAllCanonicalBusinessStatuses() {
        // Encomendas e Vendas
        assertEquals("Pendente", UIHelper.humanStatus("PENDING"));
        assertEquals("Pendente de aprovação", UIHelper.humanStatus("PENDING_APPROVAL"));
        assertEquals("Aguarda separação", UIHelper.humanStatus("AWAITING_SEPARATION"));
        assertEquals("Em separação", UIHelper.humanStatus("IN_SEPARATION"));
        assertEquals("Separado", UIHelper.humanStatus("SEPARATED"));
        assertEquals("Guia por aprovar", UIHelper.humanStatus("GUIDE_PENDING"));
        assertEquals("Expedido por guia", UIHelper.humanStatus("GUIDED"));
        assertEquals("Transferência por aprovar", UIHelper.humanStatus("TRANSFER_PENDING"));
        assertEquals("Transferido para a loja", UIHelper.humanStatus("TRANSFERRED"));
        assertEquals("Faturado", UIHelper.humanStatus("BILLED"));
        assertEquals("Faturado", UIHelper.humanStatus("INVOICED"));
        assertEquals("Cancelado", UIHelper.humanStatus("CANCELLED"));

        // Facturas e Notas
        assertEquals("Rascunho", UIHelper.humanStatus("DRAFT"));
        assertEquals("Aprovação de desconto", UIHelper.humanStatus("PENDING_DISCOUNT_APPROVAL"));
        assertEquals("Aprovado", UIHelper.humanStatus("APPROVED"));
        assertEquals("Parcialmente paga", UIHelper.humanStatus("PARTIALLY_PAID"));
        assertEquals("Paga", UIHelper.humanStatus("PAID"));
        assertEquals("Rejeitado", UIHelper.humanStatus("REJECTED"));
        assertEquals("Em atraso", UIHelper.humanStatus("OVERDUE"));

        // Compras e Recepção
        assertEquals("Encomendado", UIHelper.humanStatus("ORDERED"));
        assertEquals("Parcialmente recebido", UIHelper.humanStatus("PARTIALLY_RECEIVED"));
        assertEquals("Recebido", UIHelper.humanStatus("RECEIVED"));

        // Recibos e Caixa
        assertEquals("Concluído", UIHelper.humanStatus("COMPLETED"));

        // Contratos e Utilizadores
        assertEquals("Activo", UIHelper.humanStatus("ACTIVE"));
        assertEquals("Suspenso", UIHelper.humanStatus("SUSPENDED"));
        assertEquals("Expirado", UIHelper.humanStatus("EXPIRED"));
        assertEquals("Rescindido", UIHelper.humanStatus("TERMINATED"));
        assertEquals("SuperAdmin", UIHelper.humanStatus("SUPERADMIN"));

        // Frotas
        assertEquals("Operacional", UIHelper.humanStatus("OPERATIONAL"));
        assertEquals("Manutenção", UIHelper.humanStatus("MAINTENANCE"));
        assertEquals("Inspecção", UIHelper.humanStatus("INSPECTION"));
        assertEquals("Fora de serviço", UIHelper.humanStatus("OUT_OF_SERVICE"));
        assertEquals("Vendido", UIHelper.humanStatus("SOLD"));

        // Produção
        assertEquals("Planeada", UIHelper.humanStatus("PLANEADA"));
        assertEquals("Em produção", UIHelper.humanStatus("EM_PRODUCAO"));
        assertEquals("Concluído", UIHelper.humanStatus("CONCLUIDA"));

        // Financeiro e Pagamentos
        assertEquals("Débito", UIHelper.humanStatus("DEBIT"));
        assertEquals("Crédito", UIHelper.humanStatus("CREDIT"));
        assertEquals("Dinheiro", UIHelper.humanStatus("CASH"));
        assertEquals("Transferência", UIHelper.humanStatus("BANK_TRANSFER"));
        assertEquals("Cartão", UIHelper.humanStatus("CARD"));
        assertEquals("M-Pesa", UIHelper.humanStatus("MPESA"));
        assertEquals("E-Mola", UIHelper.humanStatus("EMOLA"));
    }

    @Test
    @DisplayName("TableFilter.cellMatches suporta correspondência bidirecional entre opções em PT-MZ e enums no modelo")
    void cellMatches_bidirectionalMatchingBetweenPtMzAndEnums() {
        // Opção do combo em PT-MZ vs enum em inglês no modelo
        assertTrue(TableFilter.cellMatches("PENDING", "Pendente"));
        assertTrue(TableFilter.cellMatches("PENDING", "Por facturar"));
        assertTrue(TableFilter.cellMatches("PENDING_APPROVAL", "Pendente de aprovação"));
        assertTrue(TableFilter.cellMatches("AWAITING_SEPARATION", "Aguarda separação"));
        assertTrue(TableFilter.cellMatches("IN_SEPARATION", "Em separação"));
        assertTrue(TableFilter.cellMatches("SEPARATED", "Separado"));
        assertTrue(TableFilter.cellMatches("BILLED", "Facturado"));
        assertTrue(TableFilter.cellMatches("BILLED", "Faturado"));
        assertTrue(TableFilter.cellMatches("CANCELLED", "Cancelado"));
        assertTrue(TableFilter.cellMatches("ORDERED", "Encomendado"));
        assertTrue(TableFilter.cellMatches("DRAFT", "Rascunho"));
        assertTrue(TableFilter.cellMatches("PAID", "Paga"));
        assertTrue(TableFilter.cellMatches("APPROVED", "Aprovado"));
        assertTrue(TableFilter.cellMatches("REJECTED", "Rejeitado"));
        assertTrue(TableFilter.cellMatches("ACTIVE", "Activo"));
        assertTrue(TableFilter.cellMatches("SUSPENDED", "Suspenso"));
        assertTrue(TableFilter.cellMatches("TERMINATED", "Rescindido"));

        // Caso o modelo já guarde o texto humanizado
        assertTrue(TableFilter.cellMatches("Pendente", "Pendente"));
        assertTrue(TableFilter.cellMatches("Faturado", "Faturado"));
        assertTrue(TableFilter.cellMatches("Facturado", "Faturado"));
        assertTrue(TableFilter.cellMatches("Aguarda separação", "Aguarda separação"));

        // Opção universal "Todos" deve aceitar qualquer valor
        assertTrue(TableFilter.cellMatches("PENDING", "Todos os estados"));
        assertTrue(TableFilter.cellMatches("BILLED", "Todos"));
        assertTrue(TableFilter.cellMatches(null, "Todos os estados"));
    }

    @Test
    @DisplayName("TableCellRenderers.status renderiza badges com rótulo PT-MZ e cores semânticas correctas")
    void statusRenderer_rendersPortugueseBadgesWithSemanticColors() {
        TableCellRenderer renderer = TableCellRenderers.status();
        DefaultTableModel model = new DefaultTableModel(new Object[]{"ID", "Estado"}, 0);
        model.addRow(new Object[]{1L, "PENDING"});
        model.addRow(new Object[]{2L, "BILLED"});
        model.addRow(new Object[]{3L, "CANCELLED"});
        model.addRow(new Object[]{4L, "AWAITING_SEPARATION"});
        model.addRow(new Object[]{5L, "ORDERED"});

        JTable table = new JTable(model);

        // Linha 0: PENDING -> "Pendente", Amarelo
        Component comp0 = renderer.getTableCellRendererComponent(table, model.getValueAt(0, 1), false, false, 0, 1);
        assertThat(comp0).isInstanceOf(javax.swing.JLabel.class);
        javax.swing.JLabel label0 = (javax.swing.JLabel) comp0;
        assertEquals("Pendente", label0.getText());
        assertEquals(UIHelper.PENDING_YELLOW, label0.getForeground());

        // Linha 1: BILLED -> "Faturado", Verde
        Component comp1 = renderer.getTableCellRendererComponent(table, model.getValueAt(1, 1), false, false, 1, 1);
        javax.swing.JLabel label1 = (javax.swing.JLabel) comp1;
        assertEquals("Faturado", label1.getText());
        assertEquals(UIHelper.APPROVED_GREEN, label1.getForeground());

        // Linha 2: CANCELLED -> "Cancelado", Vermelho
        Component comp2 = renderer.getTableCellRendererComponent(table, model.getValueAt(2, 1), false, false, 2, 1);
        javax.swing.JLabel label2 = (javax.swing.JLabel) comp2;
        assertEquals("Cancelado", label2.getText());
        assertEquals(UIHelper.REJECTED_RED, label2.getForeground());

        // Linha 3: AWAITING_SEPARATION -> "Aguarda separação", Amarelo
        Component comp3 = renderer.getTableCellRendererComponent(table, model.getValueAt(3, 1), false, false, 3, 1);
        javax.swing.JLabel label3 = (javax.swing.JLabel) comp3;
        assertEquals("Aguarda separação", label3.getText());
        assertEquals(UIHelper.PENDING_YELLOW, label3.getForeground());

        // Linha 4: ORDERED -> "Encomendado", Amarelo
        Component comp4 = renderer.getTableCellRendererComponent(table, model.getValueAt(4, 1), false, false, 4, 1);
        javax.swing.JLabel label4 = (javax.swing.JLabel) comp4;
        assertEquals("Encomendado", label4.getText());
        assertEquals(UIHelper.PENDING_YELLOW, label4.getForeground());
    }

    @Test
    @DisplayName("Varredura estática garante ausência de enums em inglês nos combos de filtro TableFilter.combo")
    void staticScan_noEnglishStatusEnumsInFilterCombos() throws IOException {
        Pattern pattern = Pattern.compile("TableFilter\\.combo\\s*\\([^;]+\\)", Pattern.DOTALL);
        List<String> forbiddenEnums = List.of(
                "\"PENDING\"", "\"BILLED\"", "\"ORDERED\"", "\"AWAITING_SEPARATION\"",
                "\"IN_SEPARATION\"", "\"SEPARATED\"", "\"GUIDE_PENDING\"", "\"GUIDED\"",
                "\"ACTIVE\"", "\"SUSPENDED\"", "\"CANCELLED\"", "\"DRAFT\"",
                "\"CASH\"", "\"BANK_TRANSFER\"", "\"CARD\""
        );

        try (Stream<Path> paths = Files.walk(GUI_ROOT)) {
            List<Path> javaFiles = paths.filter(p -> p.toString().endsWith(".java")).toList();
            for (Path path : javaFiles) {
                String content = Files.readString(path);
                Matcher matcher = pattern.matcher(content);
                while (matcher.find()) {
                    String comboSnippet = matcher.group();
                    for (String forbidden : forbiddenEnums) {
                        assertThat(comboSnippet)
                                .withFailMessage("Ficheiro %s contém enum em inglês %s em TableFilter.combo: %s",
                                        path.getFileName(), forbidden, comboSnippet)
                                .doesNotContain(forbidden);
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("TableFilter.bar mantém todos os componentes estritamente em linha horizontal única sem quebra vertical")
    void tableFilterBar_singleRowHorizontalLayout() {
        javax.swing.JTextField c1 = new javax.swing.JTextField(20);
        c1.setPreferredSize(new Dimension(200, 36));
        javax.swing.JComboBox<String> c2 = new javax.swing.JComboBox<>(new String[]{"Opção 1"});
        c2.setPreferredSize(new Dimension(150, 36));
        javax.swing.JComboBox<String> c3 = new javax.swing.JComboBox<>(new String[]{"Opção 2"});
        c3.setPreferredSize(new Dimension(150, 36));
        javax.swing.JComboBox<String> c4 = new javax.swing.JComboBox<>(new String[]{"Opção 3"});
        c4.setPreferredSize(new Dimension(150, 36));
        javax.swing.JButton c5 = new javax.swing.JButton("Ação");
        c5.setPreferredSize(new Dimension(100, 36));

        JPanel bar = TableFilter.bar(c1, c2, c3, c4, c5);
        // Mesmo com largura reduzida (ex: 400px, onde FlowLayout normal empilharia em 2 ou 3 linhas):
        bar.setSize(400, 36);
        bar.doLayout();

        // Todos os componentes devem estar na mesma linha vertical (y <= 4px):
        for (int i = 0; i < bar.getComponentCount(); i++) {
            Component child = bar.getComponent(i);
            assertTrue(child.getY() <= 4,
                    "Componente " + i + " não deve ser empilhado para a 2ª linha (y=" + child.getY() + ")");
        }
    }
}
