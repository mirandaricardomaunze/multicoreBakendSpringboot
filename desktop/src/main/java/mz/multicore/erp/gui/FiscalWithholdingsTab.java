package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.FiscalApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.fiscal.dto.CreateWithholdingRequest;
import mz.multicore.erp.modules.fiscal.dto.WithholdingRecordDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Aba de registo e controlo de retenções na fonte. */
final class FiscalWithholdingsTab {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] WITHHOLDING_CATEGORIES = {
            "SERVICES", "RENT", "NON_RESIDENT", "OTHER"
    };

    private final FiscalPanel owner;
    private final FiscalApiClient fiscalApiClient;

    private DefaultTableModel withholdingsModel;
    private JTable withholdingsTable;
    private List<WithholdingRecordDTO> withholdingsList = new ArrayList<>();

    FiscalWithholdingsTab(FiscalPanel owner, FiscalApiClient fiscalApiClient) {
        this.owner = owner;
        this.fiscalApiClient = fiscalApiClient;
    }

    public JPanel buildTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));
        ModernButton newBtn = UIHelper.createSuccessButton("Registar Retenção");
        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        ModernButton deliverBtn = UIHelper.createSuccessButton("Marcar como Entregue");
        deliverBtn.setIcon(UIHelper.icon("fas-check", 14));
        ModernButton deleteBtn = UIHelper.createDangerButton("Eliminar");
        deleteBtn.setIcon(UIHelper.icon("fas-trash", 14));
        newBtn.addActionListener(e -> openWithholdingDialog());
        deliverBtn.addActionListener(e -> deliverWithholding());
        deleteBtn.addActionListener(e -> deleteWithholding());

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        withholdingsModel = new DefaultTableModel(
                new String[]{"Data", "Beneficiário", "NUIT", "Descrição", "Categoria",
                             "Base (MT)", "Taxa", "Retido (MT)", "Líquido (MT)", "Estado", "Entregue em"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        withholdingsTable = new JTable(withholdingsModel);
        UIHelper.styleTable(withholdingsTable);
        UIHelper.ensureHeadersFit(withholdingsTable);
        for (int column : new int[]{5, 7, 8}) {
            withholdingsTable.getColumnModel().getColumn(column).setCellRenderer(TableCellRenderers.money());
        }
        withholdingsTable.getColumnModel().getColumn(9).setCellRenderer(TableCellRenderers.status());
        JScrollPane scroll = new JScrollPane(withholdingsTable);
        UIHelper.styleScrollPane(scroll);

        TableQuickPeekController.install(withholdingsTable, card, (peek, mr) -> {
            int row = mr; // o controlador já entrega a linha do modelo
            peek.setHeaderIcon("fas-hand-holding-usd", UIHelper.ACCENT_BLUE);
            peek.setTitle(String.valueOf(withholdingsModel.getValueAt(row, 1)));
            peek.setSubtitle("NUIT: " + withholdingsModel.getValueAt(row, 2) + " • " + withholdingsModel.getValueAt(row, 4));
            peek.setStatus(String.valueOf(withholdingsModel.getValueAt(row, 9)), UIHelper.ACCENT_BLUE);
            peek.setItems(List.of(
                    new QuickPeekPanel.PeekItem("Data", peek.cell(withholdingsModel.getValueAt(row, 0), 0), false),
                    new QuickPeekPanel.PeekItem("Descrição", peek.cell(withholdingsModel.getValueAt(row, 3), 3), false),
                    new QuickPeekPanel.PeekItem("Base Tributável", peek.cell(withholdingsModel.getValueAt(row, 5), 5), false),
                    new QuickPeekPanel.PeekItem("Taxa Retenção", peek.cell(withholdingsModel.getValueAt(row, 6), 6), false),
                    new QuickPeekPanel.PeekItem("Valor Retido", peek.cell(withholdingsModel.getValueAt(row, 7), 7), true),
                    new QuickPeekPanel.PeekItem("Valor Líquido", peek.cell(withholdingsModel.getValueAt(row, 8), 8), false),
                    new QuickPeekPanel.PeekItem("Entregue em", peek.cell(withholdingsModel.getValueAt(row, 10), 10), false)
            ));
        });

        JTextField whSearch = TableFilter.searchField("Beneficiário, NUIT, descrição ou categoria…");
        JComboBox<String> whEstado = TableFilter.combo("Todos os estados", "Pendente", "Entregue", "Cancelado");
        JComboBox<String> whPeriodo = TableFilter.periodCombo();
        TableFilter.install(withholdingsTable, whSearch,
                List.of(new TableFilter.ColumnFilter(whEstado, 9)),
                List.of(new TableFilter.PeriodFilter(whPeriodo, 0)));
        JPanel whBar = TableFilter.bar(whSearch,
                TableFilter.label("Estado:"), whEstado,
                TableFilter.label("Data:", "fas-calendar-alt"), whPeriodo);
        whBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        ModernButton whRefreshBtn = UIHelper.createRefreshButton(this::loadWithholdings);
        card.add(UIHelper.tableCardTop("Retenções na Fonte", whBar,
                whRefreshBtn, deleteBtn, deliverBtn, newBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    public void loadWithholdings() {
        if (withholdingsModel == null) return;
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(owner, () -> fiscalApiClient.getWithholdings(companyId), this::applyWithholdings,
                error -> owner.showLoadError("retenções na fonte", error));
    }

    private void applyWithholdings(List<WithholdingRecordDTO> loaded) {
        withholdingsModel.setRowCount(0);
        withholdingsList = loaded;
        for (var w : withholdingsList) {
            BigDecimal pct = w.taxRate().multiply(BigDecimal.valueOf(100));
            withholdingsModel.addRow(new Object[]{
                    w.recordDate().format(DATE_FMT), w.beneficiaryName(),
                    w.beneficiaryTaxId() == null ? "" : w.beneficiaryTaxId(), w.serviceDescription(),
                    w.taxCategory(), w.baseAmount(), pct.stripTrailingZeros().toPlainString() + " %",
                    w.withheldAmount(), w.netPaid(), w.status(),
                    w.deliveredAt() == null ? "-" : w.deliveredAt().format(DATE_FMT)
            });
        }
    }

    private WithholdingRecordDTO selectedWithholding() {
        int row = TableFilter.selectedModelRow(withholdingsTable);
        if (row < 0) {
            ToastManager.show(owner, FeedbackType.WARNING, "Seleccione um registo.");
            return null;
        }
        return withholdingsList.get(row);
    }

    private void openWithholdingDialog() {
        JTextField nameField = new JTextField(), taxIdField = new JTextField(), descField = new JTextField();
        JComboBox<String> catCombo = new JComboBox<>(WITHHOLDING_CATEGORIES);
        MoneyField baseField = new MoneyField("0");
        DecimalField rateField = new DecimalField("0.10", 4, false);
        DateField dateField = new DateField(LocalDate.now());
        UIHelper.styleComboBox(catCombo);
        UIHelper.styleTextField(nameField); UIHelper.styleTextField(taxIdField); UIHelper.styleTextField(descField);

        FormField nameForm = new FormField("Beneficiário", nameField, true, null);
        FormField descForm = new FormField("Descrição do Serviço", descField, true, null);

        JPanel form = UIHelper.createDialogForm(
                "Data (yyyy-MM-dd):", dateField,
                "", nameForm,
                "NUIT do Beneficiário:", taxIdField,
                "", descForm,
                "Categoria:", catCombo,
                "Base (MT):", baseField,
                "Taxa (fração, ex: 0.10):", rateField
        );

        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Registar Retenção na Fonte",
                "fas-percent", "Imposto retido na fonte", form);
        dialog.setOnSaveAsync(() -> {
            if (!(nameForm.validateRequired() & descForm.validateRequired()))
                throw new IllegalArgumentException("Corrija os campos assinalados.");
            CreateWithholdingRequest req = new CreateWithholdingRequest(
                    CurrentUserContext.getCurrentCompanyId(),
                    dateField.value(),
                    nameField.getText().trim(),
                    taxIdField.getText().trim().isEmpty() ? null : taxIdField.getText().trim(),
                    descField.getText().trim(),
                    baseField.value(), rateField.value(),
                    (String) catCombo.getSelectedItem()
            );
            return () -> { fiscalApiClient.createWithholding(req); return null; };
        });
        if (dialog.showDialog()) loadWithholdings();
    }

    private void deliverWithholding() {
        var sel = selectedWithholding();
        if (sel == null) return;
        UIHelper.runWithProgress(owner, "A entregar retenção…",
                () -> { fiscalApiClient.deliverWithholding(sel.id()); return null; },
                ignored -> loadWithholdings(), owner::showActionError);
    }

    private void deleteWithholding() {
        var sel = selectedWithholding();
        if (sel == null) return;
        boolean confirmed = ModernMessageDialog.confirm(SwingUtilities.getWindowAncestor(owner),
                FeedbackType.WARNING, "Eliminar Retenção na Fonte",
                "Tem a certeza que deseja eliminar o registo de retenção de " + sel.beneficiaryName() + "?", "Eliminar");
        if (!confirmed) return;
        UIHelper.runWithProgress(owner, "A eliminar retenção…",
                () -> { fiscalApiClient.deleteWithholding(sel.id()); return null; },
                ignored -> loadWithholdings(), owner::showActionError);
    }
}
