package mz.multicore.erp.gui.quality;

import mz.multicore.erp.desktop.client.PrintApiClient;
import mz.multicore.erp.desktop.client.QualityApiClient;
import mz.multicore.erp.gui.QualityEquipmentFormDialog;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.quality.dto.EquipmentDTO;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.function.Supplier;

public class QualityCatalogTab extends JPanel {

    private final QualityApiClient qualityApiClient;
    private final PrintApiClient printApiClient;
    private final Supplier<Long> companyIdSupplier;
    private final Runnable onDataChanged;

    private DefaultTableModel equipmentsTableModel;
    private JTable equipmentsTable;
    private TablePager equipmentsPager;
    private final JTextField equipmentsSearchField = new JTextField(15);

    public QualityCatalogTab(QualityApiClient qualityApiClient, PrintApiClient printApiClient,
                             Supplier<Long> companyIdSupplier, Runnable onDataChanged) {
        super(new BorderLayout());
        this.qualityApiClient = qualityApiClient;
        this.printApiClient = printApiClient;
        this.companyIdSupplier = companyIdSupplier;
        this.onDataChanged = onDataChanged;

        setOpaque(false);
        initTab();
    }

    private void initTab() {
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createEmptyBorder(12, 12, 8, 12));

        String[] cols = {"ID", "Código", "Nome do Equipamento", "Categoria", "Prazo de Validade", "Aviso Prévio", "Estado"};
        equipmentsTableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        equipmentsTable = new JTable(equipmentsTableModel);
        equipmentsTable.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);
        UIHelper.styleTable(equipmentsTable);
        UIHelper.ensureHeadersFit(equipmentsTable);

        equipmentsTable.getColumnModel().getColumn(0).setMinWidth(0);
        equipmentsTable.getColumnModel().getColumn(0).setMaxWidth(0);
        equipmentsTable.getColumnModel().getColumn(0).setPreferredWidth(0);

        UIHelper.styleTextField(equipmentsSearchField);
        equipmentsSearchField.putClientProperty("JTextField.placeholderText", "Pesquisar por código ou nome...");
        equipmentsSearchField.setPreferredSize(new Dimension(220, UIHelper.FORM_CONTROL_HEIGHT));
        equipmentsSearchField.addActionListener(e -> loadPage(0, 25));

        JPanel filterBar = TableFilter.bar(equipmentsSearchField);

        ModernButton refreshBtn = UIHelper.createRefreshButton(this::refresh);
        ModernButton newEqBtn = UIHelper.createPrimaryButton("+ Novo Equipamento");
        newEqBtn.addActionListener(e -> openEquipmentForm(null));

        ActionMenuButton eqMenu = UIHelper.createActionMenuButton("Gerir Equipamento");
        eqMenu.addAction("Editar Equipamento", UIHelper.icon("fas-edit", 13), this::editSelectedEquipment);
        eqMenu.addAction("Exportar Catálogo (PDF)", UIHelper.icon("fas-file-pdf", 13), this::exportEquipmentsList);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);
        actions.add(refreshBtn);
        actions.add(newEqBtn);
        actions.add(eqMenu);

        card.add(UIHelper.tableCardTop("Catálogo de Equipamentos & Prazos", actions, filterBar), BorderLayout.NORTH);
        card.add(new JScrollPane(equipmentsTable), BorderLayout.CENTER);

        equipmentsPager = new TablePager(this::loadPage);
        card.add(equipmentsPager, BorderLayout.SOUTH);

        add(card, BorderLayout.CENTER);
    }

    public void refresh() {
        loadPage(0, 25);
    }

    public void loadPage(int page, int size) {
        final Long companyId = companyIdSupplier != null ? companyIdSupplier.get() : 1L;
        if (qualityApiClient == null) return;

        String search = equipmentsSearchField.getText().trim();

        UIHelper.runWithProgress(this, "A carregar catálogo de equipamentos...", () -> {
            return qualityApiClient.searchEquipments(companyId, search, page, size);
        }, resp -> {
            equipmentsTableModel.setRowCount(0);
            for (EquipmentDTO eq : resp.items()) {
                equipmentsTableModel.addRow(new Object[]{
                        eq.id(),
                        eq.code(),
                        eq.name(),
                        eq.categoryLabel(),
                        eq.periodValue() + " " + eq.periodUnitLabel(),
                        eq.warningDaysBefore() + " dias",
                        eq.active() ? "Ativo" : "Inativo"
                });
            }
            if (equipmentsPager != null) {
                equipmentsPager.apply(resp);
            }
        }, err -> ToastManager.show(this, FeedbackType.ERROR, "Erro ao carregar catálogo: " + err.getMessage()));
    }

    private void openEquipmentForm(EquipmentDTO eq) {
        Window win = SwingUtilities.getWindowAncestor(this);
        QualityEquipmentFormDialog dlg = new QualityEquipmentFormDialog(win, qualityApiClient, eq, onDataChanged);
        dlg.setVisible(true);
    }

    private void editSelectedEquipment() {
        int row = equipmentsTable.getSelectedRow();
        if (row < 0) {
            ToastManager.show(this, FeedbackType.INFO, "Por favor selecione um equipamento da tabela para editar.");
            return;
        }
        int modelRow = equipmentsTable.convertRowIndexToModel(row);
        Long id = (Long) equipmentsTableModel.getValueAt(modelRow, 0);
        final Long cid = companyIdSupplier != null ? companyIdSupplier.get() : 1L;
        UIHelper.runWithProgress(this, "A obter equipamento...", () -> {
            return qualityApiClient.searchEquipments(cid, null, 0, 200).items().stream()
                    .filter(e -> e.id().equals(id))
                    .findFirst()
                    .orElse(null);
        }, eq -> {
            if (eq != null) openEquipmentForm(eq);
        }, null);
    }

    private void exportEquipmentsList() {
        if (printApiClient == null) {
            ToastManager.show(this, FeedbackType.WARNING, "Serviço de impressão não disponível.");
            return;
        }
        TableExportAction.export(this, printApiClient, equipmentsTable, "Catálogo Geral de Equipamentos e Prazos", "catalogo-equipamentos");
    }
}
