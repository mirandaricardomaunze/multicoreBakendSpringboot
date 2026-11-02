package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.FiscalApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.fiscal.dto.CreateTaxRateRequest;
import mz.multicore.erp.modules.fiscal.dto.TaxRateDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Aba de parametrização e gestão de taxas fiscais e impostos. */
final class FiscalTaxRatesTab {

    private static final String[] TAX_TYPES = {
            "IVA_STANDARD", "IVA_REDUCED", "IVA_ZERO", "IVA_EXEMPT",
            "WITHHOLDING", "CORPORATE_INCOME", "EXCISE"
    };

    private final FiscalPanel owner;
    private final FiscalApiClient fiscalApiClient;

    private DefaultTableModel taxRatesModel;
    private JTable taxRatesTable;
    private List<TaxRateDTO> taxRatesList = new ArrayList<>();

    FiscalTaxRatesTab(FiscalPanel owner, FiscalApiClient fiscalApiClient) {
        this.owner = owner;
        this.fiscalApiClient = fiscalApiClient;
    }

    public JPanel buildTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        ModernButton newBtn = UIHelper.createSuccessButton("Nova Taxa");
        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        ModernButton editBtn = UIHelper.createPrimaryButton("Editar");
        editBtn.setIcon(UIHelper.icon("fas-edit", 14));
        ModernButton toggleBtn = UIHelper.createSecondaryButton("Activar / Desactivar");
        toggleBtn.setIcon(UIHelper.icon("fas-power-off", 14));
        newBtn.addActionListener(e -> openTaxRateDialog(null));
        editBtn.addActionListener(e -> { TaxRateDTO sel = selectedTaxRate(); if (sel != null) openTaxRateDialog(sel); });
        toggleBtn.addActionListener(e -> toggleSelectedTaxRate());

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        taxRatesModel = new DefaultTableModel(
                new String[]{"Código", "Designação", "Tipo", "Taxa (%)", "Base Legal", "Estado"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        taxRatesTable = new JTable(taxRatesModel);
        UIHelper.styleTable(taxRatesTable);
        UIHelper.ensureHeadersFit(taxRatesTable);
        JScrollPane scroll = new JScrollPane(taxRatesTable);
        UIHelper.styleScrollPane(scroll);

        TableQuickPeekController.install(taxRatesTable, card, (peek, mr) -> {
            int row = taxRatesTable.convertRowIndexToModel(mr);
            peek.setHeaderIcon("fas-balance-scale", UIHelper.ACCENT_BLUE);
            peek.setTitle(String.valueOf(taxRatesModel.getValueAt(row, 1)));
            peek.setSubtitle("Código: " + taxRatesModel.getValueAt(row, 0) + " • " + taxRatesModel.getValueAt(row, 2));
            peek.setStatus(String.valueOf(taxRatesModel.getValueAt(row, 5)), UIHelper.ACCENT_BLUE);
            List<QuickPeekPanel.PeekItem> items = new ArrayList<>();
            items.add(new QuickPeekPanel.PeekItem("Taxa Aplicável", String.valueOf(taxRatesModel.getValueAt(row, 3)), true));
            items.add(new QuickPeekPanel.PeekItem("Base Legal", String.valueOf(taxRatesModel.getValueAt(row, 4)), false));
            peek.setItems(items);
        });

        JTextField trSearch = TableFilter.searchField("Código, designação, tipo ou base legal…");
        JComboBox<String> trEstado = TableFilter.combo("Todos os estados", "Activa", "Inactiva");
        TableFilter.install(taxRatesTable, trSearch, new TableFilter.ColumnFilter(trEstado, 5));
        JPanel trBar = TableFilter.bar(trSearch, TableFilter.label("Estado:"), trEstado);
        trBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        ModernButton trRefreshBtn = UIHelper.createRefreshButton(this::loadTaxRates);
        card.add(UIHelper.tableCardTop("Tabela de Taxas Fiscais", trBar,
                trRefreshBtn, toggleBtn, editBtn, newBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    public void loadTaxRates() {
        if (taxRatesModel == null) return;
        UIHelper.loadAsync(owner, fiscalApiClient::getAllTaxRates, this::applyTaxRates,
                error -> owner.showLoadError("taxas fiscais", error));
    }

    private void applyTaxRates(List<TaxRateDTO> loaded) {
        taxRatesModel.setRowCount(0);
        taxRatesList = loaded;
        for (var t : taxRatesList) {
            BigDecimal pct = t.rate().multiply(BigDecimal.valueOf(100));
            taxRatesModel.addRow(new Object[]{
                    t.code(), t.name(), UIHelper.humanStatus(t.type()),
                    pct.stripTrailingZeros().toPlainString() + " %",
                    t.legalBasis() == null ? "" : t.legalBasis(),
                    t.active() ? "Activa" : "Inactiva"
            });
        }
    }

    private TaxRateDTO selectedTaxRate() {
        int row = TableFilter.selectedModelRow(taxRatesTable);
        if (row < 0) {
            ToastManager.show(owner, FeedbackType.WARNING, "Seleccione uma taxa.");
            return null;
        }
        return taxRatesList.get(row);
    }

    private void openTaxRateDialog(TaxRateDTO existing) {
        JTextField codeField = new JTextField(existing == null ? "" : existing.code());
        JTextField nameField = new JTextField(existing == null ? "" : existing.name());
        JComboBox<String> typeCombo = new JComboBox<>(TAX_TYPES);
        if (existing != null) typeCombo.setSelectedItem(existing.type());
        UIHelper.styleComboBox(typeCombo);
        DecimalField rateField = new DecimalField(
                existing == null ? "0.16" : existing.rate().toPlainString(), 4, false);
        JTextField legalField = new JTextField(existing == null || existing.legalBasis() == null ? "" : existing.legalBasis());
        UIHelper.styleTextField(codeField); UIHelper.styleTextField(nameField); UIHelper.styleTextField(legalField);
        if (existing != null) UIHelper.setReadOnly(codeField, true);

        FormField codeForm = new FormField("Código", codeField, true, null);
        FormField nameForm = new FormField("Designação", nameField, true, null);

        JPanel form = UIHelper.createDialogForm(
                "", codeForm,
                "", nameForm,
                "Tipo:", typeCombo,
                "Taxa (fração, ex: 0.16 para 16%):", rateField,
                "Base Legal:", legalField
        );

        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow,
                existing == null ? "Nova Taxa Fiscal" : "Editar Taxa Fiscal", "fas-percent",
                "Configuração de imposto/taxa", form);
        dialog.setOnSaveAsync(() -> {
            if (!(codeForm.validateRequired() & nameForm.validateRequired()))
                throw new IllegalArgumentException("Corrija os campos assinalados.");
            CreateTaxRateRequest req = new CreateTaxRateRequest(
                    codeField.getText().trim(),
                    nameField.getText().trim(),
                    (String) typeCombo.getSelectedItem(),
                    rateField.value(),
                    legalField.getText().trim().isEmpty() ? null : legalField.getText().trim()
            );
            return () -> {
                if (existing == null) fiscalApiClient.createTaxRate(req);
                else fiscalApiClient.updateTaxRate(existing.id(), req);
                return null;
            };
        });
        if (dialog.showDialog()) loadTaxRates();
    }

    private void toggleSelectedTaxRate() {
        TaxRateDTO sel = selectedTaxRate();
        if (sel == null) return;
        UIHelper.runWithProgress(owner, "A actualizar taxa fiscal…", () -> {
            if (sel.active()) fiscalApiClient.deactivateTaxRate(sel.id());
            else fiscalApiClient.activateTaxRate(sel.id());
            return null;
        }, ignored -> loadTaxRates(), owner::showActionError);
    }
}
