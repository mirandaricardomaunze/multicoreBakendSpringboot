package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HARNESS-TBLF-001: Matriz de Testes da Barra Universal de Filtro Rápido em Tabelas")
public class TableQuickFilterHarnessTest {

    private DefaultTableModel model;
    private JTable table;
    private TableQuickFilterBar filterBar;

    @BeforeEach
    void setUp() {
        // Criar modelo e tabela com 4 registos de teste
        String[] columns = {"Cidade", "Departamento", "Ano"};
        model = new DefaultTableModel(columns, 0);
        model.addRow(new Object[]{"Maputo", "Comercial", "2026"});
        model.addRow(new Object[]{"Beira", "Armazém", "2025"});
        model.addRow(new Object[]{"Nampula", "Comercial", "2026"});
        model.addRow(new Object[]{"maputo", "Financeiro", "2024"});

        table = new JTable(model);
        filterBar = new TableQuickFilterBar(table);
    }

    @Test
    @DisplayName("TBLF-01: Inicialização do componente e configuração do TableRowSorter")
    void testTblf01_InitializationAndSorterSetup() {
        assertNotNull(filterBar.getTable(), "A tabela associada não deve ser nula");
        assertNotNull(filterBar.getSorter(), "O TableRowSorter deve ser inicializado");
        assertSame(filterBar.getSorter(), table.getRowSorter(), "O sorter da tabela deve corresponder ao do componente");
        assertEquals(4, filterBar.getTotalCount(), "O total inicial de linhas deve ser 4");
        assertEquals(4, filterBar.getFilteredCount(), "As linhas filtradas iniciais devem ser 4");
        assertNotNull(filterBar.getFilterField(), "O campo de pesquisa deve existir");
        assertNotNull(filterBar.getClearButton(), "O botão de limpar deve existir");
        assertNotNull(filterBar.getCountLabel(), "O rótulo de contagem deve existir");
    }

    @Test
    @DisplayName("TBLF-02: Filtragem simples case-insensitive")
    void testTblf02_SimpleCaseInsensitiveFiltering() {
        filterBar.setFilterText("maputo");
        // Deve encontrar "Maputo" e "maputo"
        assertEquals(2, filterBar.getFilteredCount(), "Deve encontrar 2 registos com 'maputo' ignorando maiúsculas");

        filterBar.setFilterText("BEIRA");
        assertEquals(1, filterBar.getFilteredCount(), "Deve encontrar 1 registo com 'BEIRA'");
    }

    @Test
    @DisplayName("TBLF-03: Filtragem multi-termo (AND lógico entre colunas)")
    void testTblf03_MultiTermAndFiltering() {
        // Termo 1 em Departamento ("Comercial"), Termo 2 em Ano ("2026")
        filterBar.setFilterText("comercial 2026");
        assertEquals(2, filterBar.getFilteredCount(), "Deve encontrar 'Maputo Comercial 2026' e 'Nampula Comercial 2026'");

        // Termo que não coexiste na mesma linha
        filterBar.setFilterText("comercial 2025");
        assertEquals(0, filterBar.getFilteredCount(), "Não deve encontrar nenhuma linha que combine 'comercial' e '2025'");
    }

    @Test
    @DisplayName("TBLF-04: Limpeza de filtro restaura 100% dos registos")
    void testTblf04_ClearFilterRestoresAllRows() {
        filterBar.setFilterText("Beira");
        assertEquals(1, filterBar.getFilteredCount());
        assertTrue(filterBar.getClearButton().isEnabled(), "O botão limpar deve estar ativo quando há texto");

        filterBar.clearFilter();
        assertEquals("", filterBar.getFilterText(), "O campo de texto deve ser limpo");
        assertEquals(4, filterBar.getFilteredCount(), "Todas as 4 linhas devem voltar a estar visíveis");
        assertFalse(filterBar.getClearButton().isEnabled(), "O botão limpar deve estar inativo quando o campo está vazio");
    }

    @Test
    @DisplayName("TBLF-05: Métricas precisas do contador reativo")
    void testTblf05_ReactiveCounterMetrics() {
        filterBar.setFilterText("");
        assertTrue(filterBar.getCountLabel().getText().contains("Total: 4 registos"),
                "Texto sem filtro deve ser 'Total: 4 registos'");

        filterBar.setFilterText("Nampula");
        assertTrue(filterBar.getCountLabel().getText().contains("Exibindo 1 de 4 registos"),
                "Texto filtrado deve ser 'Exibindo 1 de 4 registos'");

        filterBar.setFilterText("Inexistente");
        assertTrue(filterBar.getCountLabel().getText().contains("Exibindo 0 de 4 registos"),
                "Texto sem correspondências deve ser 'Exibindo 0 de 4 registos'");
    }

    @Test
    @DisplayName("TBLF-06: Reatividade a mutações dinâmicas no TableModel")
    void testTblf06_ReactivityToTableModelMutations() {
        model.addRow(new Object[]{"Tete", "Minas", "2026"});
        assertEquals(5, filterBar.getTotalCount(), "O total de registos deve atualizar para 5");

        filterBar.setFilterText("minas");
        assertEquals(1, filterBar.getFilteredCount(), "Deve encontrar o novo registo adicionado");

        model.removeRow(4); // Remove a linha recém-adicionada
        assertEquals(0, filterBar.getFilteredCount(), "Não deve encontrar mais linhas com 'minas'");
        assertEquals(4, filterBar.getTotalCount(), "O total deve voltar a 4");
    }

    @Test
    @DisplayName("TBLF-07: Mapeamento de atalhos de teclado (Ctrl+F e Escape)")
    void testTblf07_KeyboardShortcutsMappings() {
        // Atalho Escape no filterField
        KeyStroke escapeKey = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        Object escapeActionKey = filterBar.getFilterField().getInputMap(JComponent.WHEN_FOCUSED).get(escapeKey);
        assertNotNull(escapeActionKey, "O atalho Escape deve estar registado no filterField");
        assertNotNull(filterBar.getFilterField().getActionMap().get(escapeActionKey),
                "A ação de Escape deve estar configurada no ActionMap");

        // Atalho Ctrl+F na Tabela
        int mask = java.awt.event.InputEvent.CTRL_DOWN_MASK;
        try {
            if (!GraphicsEnvironment.isHeadless()) {
                mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
            }
        } catch (Throwable ignored) {
            mask = java.awt.event.InputEvent.CTRL_DOWN_MASK;
        }
        KeyStroke ctrlF = KeyStroke.getKeyStroke(KeyEvent.VK_F, mask);
        Object ctrlFActionKey = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(ctrlF);
        assertNotNull(ctrlFActionKey, "O atalho Ctrl+F deve estar registado na JTable");

        // Utilitários de UIHelper
        TableQuickFilterBar attached = UIHelper.attachQuickFilter(table);
        assertNotNull(attached, "UIHelper.attachQuickFilter deve retornar a instância");
        JPanel wrapped = UIHelper.wrapTableWithQuickFilter(new JScrollPane(table), table);
        assertNotNull(wrapped, "UIHelper.wrapTableWithQuickFilter deve retornar o painel encapsulado");
    }

    @Test
    @DisplayName("TBLF-08: Limite estrito de linhas (<= 1000 linhas)")
    void testTblf08_StrictLineCountCompliance() throws IOException {
        Path path = Paths.get("src/main/java/mz/multicore/erp/gui/components/TableQuickFilterBar.java");
        assertTrue(Files.exists(path), "TableQuickFilterBar.java deve existir");
        long lines = Files.lines(path).count();
        assertTrue(lines <= 1000, "TableQuickFilterBar.java deve ter <= 1000 linhas (actual: " + lines + ")");
    }
}
