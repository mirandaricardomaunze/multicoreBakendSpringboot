package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.purchases.dto.BusinessUnitDTO;
import mz.multicore.erp.modules.purchases.dto.CreateBusinessUnitRequest;
import mz.multicore.erp.modules.purchases.dto.SupplierDTO;
import mz.multicore.erp.modules.purchases.dto.UpdateBusinessUnitRequest;

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

    // KPI value labels
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

        // 1. Linha de KPIs uniforme (STANDARD_CARD_HEIGHT = 96px)
        JPanel kpiGrid = KpiCard.createGrid(3, 8, 8);
        kpiGrid.add(KpiCard.createCard("Total de Negócios", kpiTotalValue, "Linhas cadastradas", "fas-briefcase", UIHelper.ACCENT_BLUE));
        kpiGrid.add(KpiCard.createCard("Negócios Activos", kpiActiveValue, "Em operação comercial", "fas-check-circle", UIHelper.APPROVED_GREEN));
        kpiGrid.add(KpiCard.createCard("Artigos Vinculados", kpiProductsValue, "Total de produtos associados", "fas-boxes", UIHelper.ACCENT));

        // 2. Card de Tabela canónico em ModernPanel(16)
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

        // Larguras canónicas de coluna
        int[] widths = {100, 220, 200, 95, 240, 100};
        for (int i = 0; i < widths.length && i < table.getColumnModel().getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        table.getColumnModel().getColumn(3).setCellRenderer(TableCellRenderers.quantity());
        table.getColumnModel().getColumn(5).setCellRenderer(TableCellRenderers.status());
        UIHelper.ensureHeadersFit(table);

        // Duplo clique na linha para abrir edição directa (padrão universal DTI)
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

        // Filtros e Ações
        searchField = TableFilter.searchField("Código, nome ou fornecedor…");
        JComboBox<String> statusFilter = TableFilter.combo("Todos os estados", "Activo", "Inactivo");

        TableFilter.install(table, searchField, new TableFilter.ColumnFilter(statusFilter, 5));

        ModernButton refreshBtn = UIHelper.createSecondaryButton("Actualizar");
        refreshBtn.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refreshBtn.addActionListener(e -> {
            searchField.setText("");
            loadBusinessUnits();
        });

        ModernButton newBtn = UIHelper.createSuccessButton("Novo Negócio");
        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        newBtn.addActionListener(e -> openBusinessUnitDialog(null));

        ActionMenuButton actionsMenu = UIHelper.createActionMenuButton("Ações")
                .addAction("Editar", UIHelper.icon("fas-edit", 14), () -> {
                    BusinessUnitDTO sel = selectedUnit();
                    if (sel != null) openBusinessUnitDialog(sel);
                })
                .addAction("Activar/Desactivar", UIHelper.icon("fas-power-off", 14), this::toggleSelectedUnit);

        JPanel filtersBar = TableFilter.bar(
                searchField,
                TableFilter.label("Estado:"), statusFilter
        );

        // Botão de espreitar detalhes laterais (Quick-Peek) integrado na barra de filtros
        ModernButton peekToggleBtn = UIHelper.createSecondaryButton("");
        peekToggleBtn.setIcon(UIHelper.icon("fas-columns", 12));
        peekToggleBtn.setToolTipText("Espreitar detalhes da linha de negócio (Espaço)");
        peekToggleBtn.getAccessibleContext().setAccessibleName("Espreitar detalhes");
        peekToggleBtn.setPreferredSize(new Dimension(30, UIHelper.FORM_CONTROL_HEIGHT));

        JPanel filtersRow = new JPanel(new BorderLayout(8, 0));
        filtersRow.setOpaque(false);
        filtersRow.setBorder(new EmptyBorder(0, 0, 6, 0));
        filtersRow.add(filtersBar, BorderLayout.WEST);
        filtersRow.add(peekToggleBtn, BorderLayout.EAST);

        tableCard.add(UIHelper.tableCardTop("Linhas de Negócio / Distribuição", filtersRow, refreshBtn, actionsMenu, newBtn), BorderLayout.NORTH);
        tableCard.add(scroll, BorderLayout.CENTER);
        tableCard.add(ClientTablePagination.install(table), BorderLayout.SOUTH);

        // Controlador Quick-Peek acionado com a barra de espaço ou botão de alternância
        TableQuickPeekController peekController = TableQuickPeekController.install(table, tableCard, this::populateQuickPeek);
        peekToggleBtn.addActionListener(e -> peekController.toggle());

        panel.add(kpiGrid, BorderLayout.NORTH);
        panel.add(tableCard, BorderLayout.CENTER);
        return panel;
    }

    private void populateQuickPeek(QuickPeekPanel peek, int modelRow) {
        if (businessUnitsList == null || modelRow < 0 || modelRow >= businessUnitsList.size()) return;
        BusinessUnitDTO bu = businessUnitsList.get(modelRow);
        boolean active = bu.active();

        peek.setHeaderIcon("fas-briefcase", UIHelper.ACCENT);
        peek.setTitle(bu.name() != null ? bu.name() : "");
        peek.setSubtitle("Código: " + bu.code() + " • " + (bu.supplierName() != null ? bu.supplierName() : "Fornecedor livre"));
        peek.setStatus(active ? "ACTIVO" : "INACTIVO", active ? UIHelper.APPROVED_GREEN : UIHelper.REJECTED_RED);

        List<QuickPeekPanel.PeekItem> items = new ArrayList<>();
        items.add(new QuickPeekPanel.PeekItem("Código Comercial", bu.code(), true));
        items.add(new QuickPeekPanel.PeekItem("Nome da Linha", bu.name(), false));
        items.add(new QuickPeekPanel.PeekItem("Fornecedor Principal", bu.supplierName() != null ? bu.supplierName() : "—", false));
        items.add(new QuickPeekPanel.PeekItem("Artigos Vinculados", String.valueOf(bu.productCount()), true));
        items.add(new QuickPeekPanel.PeekItem("Descrição", bu.description() != null && !bu.description().isBlank() ? bu.description() : "—", false));
        peek.setItems(items);

        peek.getOpenFullButton().setText("Editar Linha de Negócio");
        peek.setOnOpenFullAction(ignored -> openBusinessUnitDialog(bu));
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
        BusinessUnitDTO sel = selectedUnit();
        if (sel == null) return;

        UIHelper.runWithProgress(table, "A alterar estado…", () -> {
            owner.purchaseApiClient.toggleActiveBusinessUnit(sel.id());
            return null;
        }, ignored -> {
            ToastManager.success(table, "Estado da linha de negócio " + sel.code() + " actualizado.");
            loadBusinessUnits();
        }, error -> owner.showPurchaseNotice(FeedbackType.ERROR, "Erro ao alterar estado", error.getMessage()));
    }

    private void openBusinessUnitDialog(BusinessUnitDTO edit) {
        JTextField codeField = new JTextField(edit != null ? edit.code() : "");
        JTextField nameField = new JTextField(edit != null ? edit.name() : "");
        JTextField descField = new JTextField(edit != null && edit.description() != null ? edit.description() : "");

        UIHelper.styleTextField(codeField);
        UIHelper.styleTextField(nameField);
        UIHelper.styleTextField(descField);

        JComboBox<SupplierOption> supplierCombo = new JComboBox<>();
        UIHelper.styleComboBox(supplierCombo);
        supplierCombo.addItem(new SupplierOption(null, "— Nenhum / Fornecedor livre —"));

        List<SupplierDTO> suppliers = owner.suppliersList;
        int selectedSupplierIdx = 0;
        if (suppliers != null) {
            for (int i = 0; i < suppliers.size(); i++) {
                SupplierDTO s = suppliers.get(i);
                supplierCombo.addItem(new SupplierOption(s.id(), s.name()));
                if (edit != null && edit.supplierId() != null && edit.supplierId().equals(s.id())) {
                    selectedSupplierIdx = i + 1;
                }
            }
        }
        supplierCombo.setSelectedIndex(selectedSupplierIdx);

        JComponent form = UIHelper.createDialogForm(
                "Código (ex.: UNL, BAT, FOOD):", codeField,
                "Nome da Linha de Negócio:", nameField,
                "Fornecedor Fabricante Associado:", supplierCombo,
                "Descrição Comercial:", descField
        );

        String title = edit == null ? "Nova Linha de Negócio" : "Editar Linha de Negócio " + edit.code();
        ModernFormDialog dialog = new ModernFormDialog(
                SwingUtilities.getWindowAncestor(table),
                title,
                "fas-briefcase",
                "Associa produtos e compras à divisão comercial (ex.: Unilever, BAT, Alimentares).",
                form
        );

        // Garante contenção canónica nos limites do ecrã do Windows (máx 610px)
        UIHelper.applyStandardModalBounds(dialog.getDialog(), 520, 480);

        dialog.setOnSaveAsync(() -> {
            String code = codeField.getText().trim().toUpperCase();
            String name = nameField.getText().trim();
            String desc = descField.getText().trim();
            SupplierOption selSup = (SupplierOption) supplierCombo.getSelectedItem();
            Long supplierId = selSup != null ? selSup.id : null;

            if (code.isBlank()) {
                SwingUtilities.invokeLater(codeField::requestFocusInWindow);
                throw new IllegalArgumentException("Indique o código do negócio (ex.: UNL, BAT, FOOD).");
            }
            if (name.isBlank()) {
                SwingUtilities.invokeLater(nameField::requestFocusInWindow);
                throw new IllegalArgumentException("Indique o nome da linha de negócio.");
            }

            return () -> {
                if (edit == null) {
                    CreateBusinessUnitRequest req = new CreateBusinessUnitRequest(code, name, supplierId, desc.isBlank() ? null : desc);
                    return owner.purchaseApiClient.createBusinessUnit(req);
                } else {
                    UpdateBusinessUnitRequest req = new UpdateBusinessUnitRequest(code, name, supplierId, desc.isBlank() ? null : desc, edit.active());
                    return owner.purchaseApiClient.updateBusinessUnit(edit.id(), req);
                }
            };
        });

        SwingUtilities.invokeLater(codeField::requestFocusInWindow);

        if (dialog.showDialog()) {
            ToastManager.success(table, "Linha de negócio gravada com sucesso.");
            loadBusinessUnits();
        }
    }

    private record SupplierOption(Long id, String name) {
        @Override
        public String toString() { return name; }
    }
}
