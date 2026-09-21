package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.*;
import mz.multicore.erp.gui.components.ClientTablePagination;
import mz.multicore.erp.gui.components.UIHelper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class UniversalTablePaginationHarnessTest {

    @BeforeAll
    static void setupHeadless() {
        System.setProperty("java.awt.headless", "true");
    }

    @Test
    @DisplayName("UPAG-01: Estilo de tabela instala paginação automaticamente quando adicionada a card BorderLayout")
    void testAutomaticPaginationInstalledViaUIHelper() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DefaultTableModel model = new DefaultTableModel(new String[]{"Código", "Nome"}, 0);
            for (int i = 0; i < 70; i++) model.addRow(new Object[]{"COD" + i, "Item " + i});
            JTable table = new JTable(model);
            UIHelper.styleTable(table);

            JScrollPane sp = new JScrollPane(table);
            JPanel card = new JPanel(new BorderLayout());
            card.add(sp, BorderLayout.CENTER);

            assertTrue(ClientTablePagination.isInstalled(table), "Paginação de cliente deve ser instalada automaticamente");
            Component south = ((BorderLayout) card.getLayout()).getLayoutComponent(BorderLayout.SOUTH);
            assertThat(south).isInstanceOf(UIHelper.PaginationSouthComposite.class);
            UIHelper.PaginationSouthComposite comp = (UIHelper.PaginationSouthComposite) south;
            assertNotNull(comp.getPagination(), "Deve conter barra de paginação");
            assertNull(comp.getExtraFooter(), "Não deve ter rodapé extra inicialmente");
            assertThat(table.getRowCount()).isEqualTo(50); // Página padrão de 50 registos
        });
    }

    @Test
    @DisplayName("UPAG-02: Preserva rodapé pré-existente em BorderLayout.SOUTH dentro do PaginationSouthComposite")
    void testCompositeWithPreExistingFooter() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel card = new JPanel(new BorderLayout());
            JLabel totalsLabel = new JLabel("Total: 50.000,00 MT");
            card.add(totalsLabel, BorderLayout.SOUTH);

            DefaultTableModel model = new DefaultTableModel(new String[]{"ID", "Valor"}, 0);
            for (int i = 0; i < 60; i++) model.addRow(new Object[]{i, i * 100});
            JTable table = new JTable(model);
            UIHelper.styleTable(table);

            JScrollPane sp = new JScrollPane(table);
            card.add(sp, BorderLayout.CENTER);

            Component south = ((BorderLayout) card.getLayout()).getLayoutComponent(BorderLayout.SOUTH);
            assertThat(south).isInstanceOf(UIHelper.PaginationSouthComposite.class);
            UIHelper.PaginationSouthComposite comp = (UIHelper.PaginationSouthComposite) south;
            assertNotNull(comp.getPagination(), "Paginação deve estar presente");
            assertThat(comp.getExtraFooter()).isSameAs(totalsLabel);
        });
    }

    @Test
    @DisplayName("UPAG-03: Guarda de SOUTH intercepta adições tardias de rodapé e funde no PaginationSouthComposite")
    void testCompositeWithLateAddedFooter() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel card = new JPanel(new BorderLayout());
            DefaultTableModel model = new DefaultTableModel(new String[]{"Item"}, 0);
            for (int i = 0; i < 30; i++) model.addRow(new Object[]{"Linha " + i});
            JTable table = new JTable(model);
            UIHelper.styleTable(table);

            JScrollPane sp = new JScrollPane(table);
            card.add(sp, BorderLayout.CENTER);

            // Simula adição tardia de rodapé como ocorre em vários painéis
            JPanel actionFooter = new JPanel();
            card.add(actionFooter, BorderLayout.SOUTH);

            Component south = ((BorderLayout) card.getLayout()).getLayoutComponent(BorderLayout.SOUTH);
            assertThat(south).isInstanceOf(UIHelper.PaginationSouthComposite.class);
            UIHelper.PaginationSouthComposite comp = (UIHelper.PaginationSouthComposite) south;
            assertNotNull(comp.getPagination(), "Paginação não deve ser sobrescrita pela adição tardia");
            assertThat(comp.getExtraFooter()).isSameAs(actionFooter);
        });
    }

    @Test
    @DisplayName("UPAG-04: Respeita propriedade DISABLED para opt-out explícito")
    void testDisabledOptOut() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            DefaultTableModel model = new DefaultTableModel(new String[]{"Item"}, 0);
            JTable table = new JTable(model);
            table.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
            UIHelper.styleTable(table);

            JScrollPane sp = new JScrollPane(table);
            JPanel card = new JPanel(new BorderLayout());
            card.add(sp, BorderLayout.CENTER);

            assertFalse(ClientTablePagination.isInstalled(table), "Tabela com DISABLED não deve ter paginação");
            Component south = ((BorderLayout) card.getLayout()).getLayoutComponent(BorderLayout.SOUTH);
            assertNull(south, "Não deve haver rodapé SOUTH adicionado");
        });
    }

    @Test
    @DisplayName("UPAG-05: Painéis reais instanciados contêm paginação ou opt-out configurado em suas tabelas")
    void testRealPanelsTablesHavePaginationOrOptOut() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            ComercialApiClient comercialApi = Mockito.mock(ComercialApiClient.class);
            PrintApiClient printApi = Mockito.mock(PrintApiClient.class);
            CreditRiskApiClient creditRiskApi = Mockito.mock(CreditRiskApiClient.class);
            AccountStatementApiClient stmtApi = Mockito.mock(AccountStatementApiClient.class);
            ClientesPanel clientesPanel = new ClientesPanel(comercialApi, printApi, creditRiskApi, stmtApi);

            List<JTable> tables = findAllTables(clientesPanel);
            assertThat(tables).isNotEmpty();
            for (JTable t : tables) {
                boolean hasPagination = ClientTablePagination.isInstalled(t);
                boolean isDisabled = Boolean.TRUE.equals(t.getClientProperty(ClientTablePagination.DISABLED));
                boolean hasNoFooter = Boolean.TRUE.equals(t.getClientProperty("noTableFooter"));
                assertTrue(hasPagination || isDisabled || hasNoFooter,
                        "Tabela de ClientesPanel deve ter paginação instalada ou opt-out configurado");
            }
        });
    }

    private static List<JTable> findAllTables(Container root) {
        List<JTable> result = new ArrayList<>();
        for (Component c : root.getComponents()) {
            if (c instanceof JTable table) {
                result.add(table);
            } else if (c instanceof Container container) {
                result.addAll(findAllTables(container));
            }
        }
        return result;
    }
}
