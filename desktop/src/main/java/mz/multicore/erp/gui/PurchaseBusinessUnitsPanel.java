package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.purchases.dto.BusinessUnitDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestão de Negócios / Linhas de Distribuição (ex.: Unilever, BAT, Food).
 * Segue estritamente os padrões canónicos de tabelas corporativas do Multicore ERP:
 * - KPIs padronizados de 96px de altura em linha única horizontal via KpiCard.
 * - Card ModernPanel(16) contendo cabeçalho com filtros, pesquisa, paginação e quick-peek com barra de espaço.
 * - Duplo clique na linha para abrir a edição rápida da linha de negócio.
 * - Modal contido na área útil via UIHelper.applyStandardModalBounds com validação assíncrona segura.
 */
final class PurchaseBusinessUnitsPanel {

    private final ComprasPanel owner;
    private JTextField searchField;
    private DefaultTableModel model;
    private JTable table;
    private List<BusinessUnitDTO> businessUnitsList = new ArrayList<>();

    private final JLabel kpiTotalValue = new JLabel("0");
    private final JLabel kpiActiveValue = new JLabel("0");
    private final JLabel kpiProductsValue = new JLabel("0");

    PurchaseBusinessUnitsPanel(ComprasPanel owner) {
        this.owner = owner;
    }

    public JPanel buildPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(UIHelper.BG_DARK);
        panel.setBorder(new EmptyBorder(4, 0, 0, 0));

        JPanel kpiGrid = KpiCard.createGrid(3, 8, 8);
        kpiGrid.add(KpiCard.createCard("Total de Negócios", kpiTotalValue, "Linhas cadastradas", "fas-briefcase", UIHelper.ACCENT_BLUE));
        kpiGrid.add(KpiCard.createCard("Negócios Activos", kpiActiveValue, "Em operação comercial", "fas-check-circle", UIHelper.APPROVED_GREEN));
        kpiGrid.add(KpiCard.createCard("Artigos Vinculados", kpiProductsValue, "Total de produtos associados", "fas-boxes", UIHelper.ACCENT));

        ModernPanel tableCard = new ModernPanel(16);
        tableCard.setLayout(new BorderLayout(0, 8));
        tableCard.setBorder(new EmptyBorder(12, 12, 12, 12));

        String[] cols = {"Código", "Nome da Linha de Negócio", "Fornecedor Principal", "Artigos", "Descrição Comercial", "Estado"};
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        UIHelper.styleTable(table);

        int[] widths = {100, 220, 200, 95, 240, 100};
        for (int i = 0; i < widths.length && i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.getColumnModel().getColumn(3).setCellRenderer(TableCellRenderers.quantity());
        table.getColumnModel().getColumn(5).setCellRenderer(TableCellRenderers.status());
        UIHelper.ensureHeadersFit(table);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    BusinessUnitDTO sel = selectedUnit();
                    if (sel != null) openBusinessUnitDialog(sel);
                }
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);

        searchField = TableFilter.searchField("Código, nome ou fornecedor…");
        JComboBox<String> statusFilter = TableFilter.combo("Todos os estados", "Activo", "Inactivo");
        TableFilter.install(table, searchField, new TableFilter.ColumnFilter(statusFilter, 5));

        ModernButton refreshBtn = UIHelper.createSecondaryButton("Actualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refreshBtn.addActionListener(e -> { searchField.setText(""); loadBusinessUnits(); });

        ModernButton newBtn = UIHelper.createSuccessButton("Novo Negócio");
        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        newBtn.addActionListener(e -> openBusinessUnitDialog(null));

        ActionMenuButton actionsMenu = UIHelper.createActionMenuButton("Gerir Unidade")
                .addAction("Editar", UIHelper.icon("fas-edit", 14), () -> { BusinessUnitDTO sel = selectedUnit(); if (sel != null) openBusinessUnitDialog(sel); })
                .addAction("Activar/Desactivar", UIHelper.icon("fas-power-off", 14), this::toggleSelectedUnit);

        ModernButton peekToggleBtn = UIHelper.createSecondaryButton("");
        peekToggleBtn.setIcon(UIHelper.icon("fas-columns", 12));
        peekToggleBtn.setToolTipText("Espreitar detalhes da linha de negócio (Espaço)");
        peekToggleBtn.getAccessibleContext().setAccessibleName("Espreitar detalhes");
        peekToggleBtn.setPreferredSize(new Dimension(30, UIHelper.FORM_CONTROL_HEIGHT));

        JPanel filtersRow = new JPanel(new BorderLayout(8, 0));
        filtersRow.setOpaque(false);
        filtersRow.setBorder(new EmptyBorder(0, 0, 6, 0));
        filtersRow.add(TableFilter.bar(searchField, TableFilter.label("Estado:"), statusFilter), BorderLayout.WEST);
        filtersRow.add(peekToggleBtn, BorderLayout.EAST);

        tableCard.add(UIHelper.tableCardTop("Linhas de Negócio / Distribuição", filtersRow, refreshBtn, actionsMenu, newBtn), BorderLayout.NORTH);
        tableCard.add(scroll, BorderLayout.CENTER);
        tableCard.add(ClientTablePagination.install(table), BorderLayout.SOUTH);

        TableQuickPeekController peekController = TableQuickPeekController.install(table, tableCard,
                (peek, mr) -> PurchaseBusinessUnitsOperationsCoordinator.populateQuickPeek(peek, businessUnitsList, mr, this::openBusinessUnitDialog));
        peekToggleBtn.addActionListener(e -> peekController.toggle());

        panel.add(kpiGrid, BorderLayout.NORTH);
        panel.add(tableCard, BorderLayout.CENTER);
        return panel;
    }

    public void loadBusinessUnits() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        if (companyId == null) return;

        UIHelper.loadAsync(table, () -> owner.purchaseApiClient.getBusinessUnitsByCompany(companyId), list -> {
            this.businessUnitsList = list != null ? list : new ArrayList<>();
            model.setRowCount(0);

            long activeCount = 0;
            long totalProducts = 0;

            for (BusinessUnitDTO bu : businessUnitsList) {
                if (bu.active()) activeCount++;
                totalProducts += bu.productCount();

                model.addRow(new Object[]{
                        bu.code(),
                        bu.name(),
                        bu.supplierName() != null ? bu.supplierName() : "—",
                        bu.productCount(),
                        bu.description() != null && !bu.description().isBlank() ? bu.description() : "—",
                        bu.active() ? "Activo" : "Inactivo"
                });
            }

            kpiTotalValue.setText(String.valueOf(businessUnitsList.size()));
            kpiActiveValue.setText(String.valueOf(activeCount));
            kpiProductsValue.setText(String.valueOf(totalProducts));

            UIHelper.ensureHeadersFit(table);
        }, error -> owner.showPurchaseNotice(FeedbackType.ERROR, "Erro ao carregar linhas de negócio", error.getMessage()));
    }

    private BusinessUnitDTO selectedUnit() {
        int row = TableFilter.selectedModelRow(table);
        if (row < 0 || row >= businessUnitsList.size()) {
            owner.showPurchaseNotice(FeedbackType.WARNING, "Seleccione um negócio", "Escolha uma linha de negócio na tabela para continuar.");
            return null;
        }
        return businessUnitsList.get(row);
    }

    private void toggleSelectedUnit() {
        PurchaseBusinessUnitsOperationsCoordinator.toggleActive(table, owner, selectedUnit(), this::loadBusinessUnits);
    }

    private void openBusinessUnitDialog(BusinessUnitDTO edit) {
        BusinessUnitFormDialog.show(table, owner, edit, this::loadBusinessUnits);
    }
}
