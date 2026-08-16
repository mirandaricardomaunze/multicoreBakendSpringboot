package com.phcpro.gui;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.gui.components.ModernButton;
import com.phcpro.gui.components.ModernFormDialog;
import com.phcpro.gui.components.ModernPanel;
import com.phcpro.gui.components.TableFilter;
import com.phcpro.gui.components.TableCellRenderers;
import com.phcpro.gui.components.DateField;
import com.phcpro.gui.components.DecimalField;
import com.phcpro.gui.components.FormField;
import com.phcpro.gui.components.MoneyField;
import com.phcpro.gui.components.UIHelper;
import com.phcpro.modules.fiscal.dto.CreateTaxRateRequest;
import com.phcpro.modules.fiscal.dto.CreateWithholdingRequest;
import com.phcpro.modules.fiscal.dto.IvaSummaryDTO;
import com.phcpro.modules.fiscal.dto.TaxRateDTO;
import com.phcpro.modules.fiscal.dto.WithholdingRecordDTO;
import com.phcpro.modules.fiscal.dto.FiscalSalesExportDTO;
import com.phcpro.desktop.client.FiscalApiClient;
import com.phcpro.modules.printing.PdfFileSaver;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class FiscalPanel extends JPanel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] TAX_TYPES = {
            "IVA_STANDARD", "IVA_REDUCED", "IVA_ZERO", "IVA_EXEMPT",
            "WITHHOLDING", "CORPORATE_INCOME", "EXCISE"
    };
    private static final String[] WITHHOLDING_CATEGORIES = {
            "SERVICES", "RENT", "NON_RESIDENT", "OTHER"
    };

    private final FiscalApiClient fiscalApiClient;

    // IVA tab
    private JSpinner ivaYearSpinner;
    private JSpinner ivaMonthSpinner;
    private JLabel ivaOutputLbl, ivaInputLbl, ivaNetLbl, ivaSalesBaseLbl, ivaPurchasesBaseLbl;
    private DefaultTableModel ivaSalesModel;
    private DefaultTableModel ivaPurchasesModel;

    // Tax rates tab
    private DefaultTableModel taxRatesModel;
    private JTable taxRatesTable;
    private List<TaxRateDTO> taxRatesList = new ArrayList<>();

    // Withholdings tab
    private DefaultTableModel withholdingsModel;
    private JTable withholdingsTable;
    private List<WithholdingRecordDTO> withholdingsList = new ArrayList<>();
    private JSpinner payrollYearSpinner;
    private JSpinner payrollMonthSpinner;
    private DefaultTableModel payrollFiscalModel;
    private JLabel payrollIrpsLabel;
    private JLabel payrollInssLabel;

    public FiscalPanel(FiscalApiClient fiscalApiClient) {
        this.fiscalApiClient = fiscalApiClient;

        setLayout(new BorderLayout(0, 10));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        add(UIHelper.createHeading("Área Fiscal — Moçambique"), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        UIHelper.styleTabbedPanePHC(tabs);
        tabs.addTab("Apuramento IVA",   UIHelper.icon("fas-percent", 16, UIHelper.TEXT_LIGHT),       buildIvaTab());
        tabs.addTab("Taxas Fiscais",    UIHelper.icon("fas-balance-scale", 16, UIHelper.TEXT_LIGHT), buildTaxRatesTab());
        tabs.addTab("Retenções na Fonte", UIHelper.icon("fas-hand-holding-usd", 16, UIHelper.TEXT_LIGHT), buildWithholdingsTab());
        tabs.addTab("IRPS & INSS Salarial", UIHelper.icon("fas-users-cog", 16, UIHelper.TEXT_LIGHT), buildPayrollFiscalTab());
        tabs.addTab("Declarações",      UIHelper.icon("fas-file-pdf", 16, UIHelper.TEXT_LIGHT),      buildDeclarationsTab());

        add(tabs, BorderLayout.CENTER);
    }

    public void onPanelSelected() {
        refreshAll();
    }

    public void refreshAll() {
        loadTaxRates();
        loadWithholdings();
        recomputeIva();
        loadPayrollFiscal();
    }

    private JPanel buildPayrollFiscalTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controls.setOpaque(false);
        controls.add(filterLabel("Período:"));
        payrollMonthSpinner = new JSpinner(new SpinnerNumberModel(LocalDate.now().getMonthValue(), 1, 12, 1));
        payrollYearSpinner = new JSpinner(new SpinnerNumberModel(LocalDate.now().getYear(), 2000, 2100, 1));
        ModernButton refresh = UIHelper.createSecondaryButton("Actualizar Mapa");
        refresh.addActionListener(e -> loadPayrollFiscal());
        ModernButton printBtn = UIHelper.createSecondaryButton("Imprimir Mapa Fiscal");
        printBtn.setIcon(UIHelper.icon("fas-print", 14));
        printBtn.addActionListener(e -> printPayrollFiscalMap());
        controls.add(payrollMonthSpinner);
        controls.add(payrollYearSpinner);
        controls.add(refresh);
        controls.add(printBtn);

        payrollIrpsLabel = new JLabel("IRPS: 0.00 MT");
        payrollInssLabel = new JLabel("INSS total: 0.00 MT");
        payrollIrpsLabel.setForeground(UIHelper.TEXT_LIGHT);
        payrollInssLabel.setForeground(UIHelper.TEXT_LIGHT);
        controls.add(Box.createRigidArea(new Dimension(20, 0)));
        controls.add(payrollIrpsLabel);
        controls.add(payrollInssLabel);
        tab.add(controls, BorderLayout.NORTH);

        payrollFiscalModel = new DefaultTableModel(
                new String[]{"Nº", "Colaborador", "NUIT", "Nº INSS", "Bruto", "Tributável", "IRPS", "INSS Trab.", "INSS Patronal"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(payrollFiscalModel);
        UIHelper.styleTable(table);
        for (int column = 4; column <= 8; column++) {
            table.getColumnModel().getColumn(column).setCellRenderer(TableCellRenderers.money());
        }
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        tab.add(scroll, BorderLayout.CENTER);
        return tab;
    }

    private void loadPayrollFiscal() {
        if (payrollFiscalModel == null) return;
        int year = (Integer) payrollYearSpinner.getValue();
        int month = (Integer) payrollMonthSpinner.getValue();
        UIHelper.loadAsync(this, () -> fiscalApiClient.fiscalSummary(year, month), this::applyPayrollFiscal,
                error -> showLoadError("mapa fiscal salarial", error));
    }

    private void applyPayrollFiscal(com.phcpro.modules.hr.dto.PayrollFiscalSummaryDTO summary) {
        payrollFiscalModel.setRowCount(0);
        for (var line : summary.lines()) {
            payrollFiscalModel.addRow(new Object[]{
                    line.employeeNumber(), line.employeeName(), line.taxId(), line.inssNumber(),
                    line.grossPay(), line.taxableIncome(), line.irps(), line.employeeInss(), line.employerInss()
            });
        }
        payrollIrpsLabel.setText(String.format("IRPS: %,.2f MT", summary.irpsWithheld()));
        payrollInssLabel.setText(String.format("INSS total: %,.2f MT", summary.totalInss()));
    }

    private void printPayrollFiscalMap() {
        int year = (Integer) payrollYearSpinner.getValue();
        int month = (Integer) payrollMonthSpinner.getValue();
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.runWithProgress(this, "A gerar mapa fiscal…",
                () -> fiscalApiClient.renderPayrollFiscalMap(companyId, year, month),
                pdf -> PdfFileSaver.saveAndOpen(pdf,
                        "mapa-fiscal-salarial-" + year + "-" + String.format("%02d", month)),
                error -> showActionError(error));
    }

    // ─── Tab 1: Apuramento IVA ────────────────────────────────────────────

    private JPanel buildIvaTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        // Period selector + totals card
        JPanel topRow = new JPanel(new BorderLayout(20, 0));
        topRow.setOpaque(false);

        JPanel periodPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        periodPanel.setOpaque(false);
        periodPanel.add(filterLabel("Período:"));
        ivaMonthSpinner = new JSpinner(new SpinnerNumberModel(LocalDate.now().getMonthValue(), 1, 12, 1));
        ivaYearSpinner = new JSpinner(new SpinnerNumberModel(LocalDate.now().getYear(), 2000, 2100, 1));
        ((JSpinner.NumberEditor) ivaYearSpinner.getEditor()).getFormat().setGroupingUsed(false);
        ivaMonthSpinner.addChangeListener(e -> recomputeIva());
        ivaYearSpinner.addChangeListener(e -> recomputeIva());
        periodPanel.add(ivaMonthSpinner);
        periodPanel.add(new JLabel("/"));
        periodPanel.add(ivaYearSpinner);

        com.phcpro.gui.components.ActionMenuButton documentsBtn = UIHelper.createActionMenuButton("Documentos")
                .addAction("Imprimir Declaração IVA", UIHelper.icon("fas-print", 14), this::printIvaDeclaration)
                .addAction("Exportar SAF-T (Vendas)", UIHelper.icon("fas-file-export", 14), this::exportSaft)
                .addAction("Validar SAF-T", UIHelper.icon("fas-check-circle", 14), this::validateSaft);
        periodPanel.add(Box.createRigidArea(new Dimension(20, 0)));
        periodPanel.add(documentsBtn);

        topRow.add(periodPanel, BorderLayout.WEST);
        tab.add(topRow, BorderLayout.NORTH);

        // Center: KPI cards row + sales/purchases tables stacked
        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);

        // KPI cards
        JPanel kpis = new JPanel(new GridLayout(1, 3, 12, 0));
        kpis.setOpaque(false);
        ivaSalesBaseLbl = new JLabel("0.00 MT", SwingConstants.LEFT);
        ivaOutputLbl = new JLabel("0.00 MT", SwingConstants.LEFT);
        ivaInputLbl = new JLabel("0.00 MT", SwingConstants.LEFT);
        ivaPurchasesBaseLbl = new JLabel("0.00 MT", SwingConstants.LEFT);
        ivaNetLbl = new JLabel("0.00 MT", SwingConstants.LEFT);
        for (JLabel l : new JLabel[]{ivaSalesBaseLbl, ivaOutputLbl, ivaInputLbl, ivaPurchasesBaseLbl, ivaNetLbl}) {
            l.setFont(new Font(UIHelper.FONT, Font.BOLD, 19));
            l.setForeground(Color.WHITE);
        }
        kpis.add(kpiCard("IVA LIQUIDADO (VENDAS)", ivaOutputLbl,
                "Base: ", ivaSalesBaseLbl, UIHelper.KPI_PURPLE_DARK, UIHelper.KPI_PURPLE_END));
        kpis.add(kpiCard("IVA DEDUZIDO (COMPRAS)", ivaInputLbl,
                "Base: ", ivaPurchasesBaseLbl, UIHelper.KPI_INFO_DARK, UIHelper.KPI_INFO_END));
        kpis.add(kpiCard("IVA LÍQUIDO", ivaNetLbl,
                null, null, UIHelper.KPI_INFO_END, UIHelper.APPROVED_GREEN));
        center.add(kpis, BorderLayout.NORTH);

        // Split tables (sales / purchases)
        JPanel split = new JPanel(new GridLayout(1, 2, 12, 0));
        split.setOpaque(false);

        ivaSalesModel = new DefaultTableModel(new String[]{"Documento", "Cliente", "Base", "IVA", "Total"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable salesTable = new JTable(ivaSalesModel);
        UIHelper.styleTable(salesTable);
        for (int column = 2; column <= 4; column++) {
            salesTable.getColumnModel().getColumn(column).setCellRenderer(TableCellRenderers.money());
        }
        split.add(wrapTable("Vendas do Período", salesTable));

        ivaPurchasesModel = new DefaultTableModel(new String[]{"Documento", "Fornecedor", "Base", "IVA", "Total"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable purchasesTable = new JTable(ivaPurchasesModel);
        UIHelper.styleTable(purchasesTable);
        for (int column = 2; column <= 4; column++) {
            purchasesTable.getColumnModel().getColumn(column).setCellRenderer(TableCellRenderers.money());
        }
        split.add(wrapTable("Compras do Período", purchasesTable));

        center.add(split, BorderLayout.CENTER);
        tab.add(center, BorderLayout.CENTER);
        return tab;
    }

    private ModernPanel kpiCard(String title, JLabel value, String subPrefix, JLabel subValue,
                                 Color start, Color end) {
        ModernPanel card = new ModernPanel(12, start, end);
        card.setLayout(new BorderLayout(6, 4));
        card.setBorder(new EmptyBorder(12, 14, 12, 14));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 10));
        titleLbl.setForeground(UIHelper.KPI_INFO_SOFT);
        card.add(titleLbl, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        if (subValue != null) {
            JPanel sub = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            sub.setOpaque(false);
            JLabel pre = new JLabel(subPrefix);
            pre.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
            pre.setForeground(UIHelper.KPI_INFO_SOFT);
            subValue.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
            subValue.setForeground(UIHelper.KPI_INFO_SOFT);
            sub.add(pre);
            sub.add(subValue);
            card.add(sub, BorderLayout.SOUTH);
        }
        return card;
    }

    private JPanel wrapTable(String title, JTable table) {
        JPanel wrap = new JPanel(new BorderLayout(0, 6));
        wrap.setOpaque(false);
        wrap.add(UIHelper.createSubheading(title), BorderLayout.NORTH);
        ModernPanel card = new ModernPanel(14);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(10, 10, 10, 10));
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        card.add(scroll, BorderLayout.CENTER);
        wrap.add(card, BorderLayout.CENTER);
        return wrap;
    }

    private void recomputeIva() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        int year = (Integer) ivaYearSpinner.getValue();
        int month = (Integer) ivaMonthSpinner.getValue();
        UIHelper.loadAsync(this, () -> fiscalApiClient.ivaSummary(companyId, year, month),
                this::applyIvaSummary, error -> showLoadError("apuramento do IVA", error));
    }

    private void applyIvaSummary(IvaSummaryDTO s) {
        ivaSalesBaseLbl.setText(String.format("%,.2f MT", s.salesBase()));
        ivaPurchasesBaseLbl.setText(String.format("%,.2f MT", s.purchasesBase()));
        ivaOutputLbl.setText(String.format("%,.2f MT", s.outputTax()));
        ivaInputLbl.setText(String.format("%,.2f MT", s.inputTax()));
        BigDecimal net = s.netDue();
        String prefix = net.compareTo(BigDecimal.ZERO) >= 0 ? "A pagar: " : "A recuperar: ";
        ivaNetLbl.setText(prefix + String.format("%,.2f MT", net.abs()));

        ivaSalesModel.setRowCount(0);
        for (var l : s.sales()) {
            ivaSalesModel.addRow(new Object[]{
                    l.documentNumber(), l.partner(),
                    l.base(), l.tax(), l.total()
            });
        }
        ivaPurchasesModel.setRowCount(0);
        for (var l : s.purchases()) {
            ivaPurchasesModel.addRow(new Object[]{
                    l.documentNumber(), l.partner(),
                    l.base(), l.tax(), l.total()
            });
        }
    }

    private void printIvaDeclaration() {
        int year = (Integer) ivaYearSpinner.getValue();
        int month = (Integer) ivaMonthSpinner.getValue();
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.runWithProgress(this, "A gerar declaração de IVA…",
                () -> fiscalApiClient.renderIvaDeclaration(companyId, year, month),
                pdf -> PdfFileSaver.saveAndOpen(pdf,
                        "declaracao-iva-" + year + "-" + String.format("%02d", month)),
                this::showActionError);
    }

    private void exportSaft() {
        int year = (Integer) ivaYearSpinner.getValue();
        int month = (Integer) ivaMonthSpinner.getValue();
        java.time.YearMonth ym = java.time.YearMonth.of(year, month);
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.runWithProgress(this, "A preparar SAF-T…",
                () -> fiscalApiClient.exportSaft(companyId, ym.atDay(1), ym.atEndOfMonth()), export -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Guardar exportação SAF-T (Vendas)");
            chooser.setSelectedFile(new java.io.File(
                    "saft_vendas_" + year + "-" + String.format("%02d", month) + ".xml"));
            if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            java.io.File target = chooser.getSelectedFile();
            UIHelper.runWithProgress(this, "A gravar SAF-T…", () -> {
                java.nio.file.Files.writeString(target.toPath(), export.xml());
                return target;
            }, file -> JOptionPane.showMessageDialog(this,
                    "Exportação SAF-T gravada (" + export.numberOfInvoices() + " faturas).\n"
                            + "Total: " + String.format("%,.2f MT", export.totalGross()) + "\n"
                            + file.getAbsolutePath(), "SAF-T Exportado", JOptionPane.INFORMATION_MESSAGE),
                    this::showActionError);
        }, this::showActionError);
    }

    private void validateSaft() {
        int year = (Integer) ivaYearSpinner.getValue();
        int month = (Integer) ivaMonthSpinner.getValue();
        java.time.YearMonth ym = java.time.YearMonth.of(year, month);
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.runWithProgress(this, "A validar SAF-T…",
                () -> fiscalApiClient.validateSaft(companyId, ym.atDay(1), ym.atEndOfMonth()), r -> {
            if (!r.xsdConfigured()) {
                JOptionPane.showMessageDialog(this, r.message(), "Validação SAF-T", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (r.valid()) {
                JOptionPane.showMessageDialog(this, "SAF-T válido face à XSD.", "Validação SAF-T",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }
            StringBuilder sb = new StringBuilder(r.message()).append("\n\n");
            int shown = Math.min(r.errors().size(), 15);
            for (int i = 0; i < shown; i++) sb.append("• ").append(r.errors().get(i)).append('\n');
            if (r.errors().size() > shown) sb.append("… (+").append(r.errors().size() - shown).append(" erro(s))");
            JTextArea area = new JTextArea(sb.toString(), 16, 60);
            area.setEditable(false);
            UIHelper.styleTextArea(area);
            JOptionPane.showMessageDialog(this, new JScrollPane(area),
                    "Validação SAF-T — erros", JOptionPane.ERROR_MESSAGE);
        }, this::showActionError);
    }

    // ─── Tab 2: Taxas Fiscais ──────────────────────────────────────────────

    private JPanel buildTaxRatesTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createSubheading("Tabela de Taxas Fiscais"), BorderLayout.WEST);

        ModernButton newBtn = UIHelper.createSuccessButton("Nova Taxa");
        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        ModernButton editBtn = UIHelper.createPrimaryButton("Editar");
        editBtn.setIcon(UIHelper.icon("fas-edit", 14));
        ModernButton toggleBtn = UIHelper.createSecondaryButton("Activar / Desactivar");
        toggleBtn.setIcon(UIHelper.icon("fas-power-off", 14));
        newBtn.addActionListener(e -> openTaxRateDialog(null));
        editBtn.addActionListener(e -> {
            TaxRateDTO sel = selectedTaxRate();
            if (sel != null) openTaxRateDialog(sel);
        });
        toggleBtn.addActionListener(e -> toggleSelectedTaxRate());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(toggleBtn);
        actions.add(editBtn);
        actions.add(newBtn);
        header.add(actions, BorderLayout.EAST);
        tab.add(header, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        taxRatesModel = new DefaultTableModel(
                new String[]{"Código", "Designação", "Tipo", "Taxa (%)", "Base Legal", "Estado"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        taxRatesTable = new JTable(taxRatesModel);
        UIHelper.styleTable(taxRatesTable);
        JScrollPane scroll = new JScrollPane(taxRatesTable);
        UIHelper.styleScrollPane(scroll);

        JTextField trSearch = TableFilter.searchField("Código, designação, tipo ou base legal…");
        JComboBox<String> trEstado = TableFilter.combo("Todos os estados", "ATIVA", "INATIVA");
        TableFilter.install(taxRatesTable, trSearch, new TableFilter.ColumnFilter(trEstado, 5));
        JPanel trBar = TableFilter.bar(trSearch, TableFilter.label("Estado:"), trEstado);
        trBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(trBar, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private void loadTaxRates() {
        if (taxRatesModel == null) return;
        UIHelper.loadAsync(this, fiscalApiClient::getAllTaxRates, this::applyTaxRates,
                error -> showLoadError("taxas fiscais", error));
    }

    private void applyTaxRates(List<TaxRateDTO> loaded) {
        taxRatesModel.setRowCount(0);
        taxRatesList = loaded;
        for (var t : taxRatesList) {
            BigDecimal pct = t.rate().multiply(BigDecimal.valueOf(100));
            taxRatesModel.addRow(new Object[]{
                    t.code(), t.name(), t.type(),
                    pct.stripTrailingZeros().toPlainString() + " %",
                    t.legalBasis() == null ? "" : t.legalBasis(),
                    t.active() ? "ATIVA" : "INATIVA"
            });
        }
    }

    private TaxRateDTO selectedTaxRate() {
        int row = TableFilter.selectedModelRow(taxRatesTable);
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma taxa.", "Aviso", JOptionPane.WARNING_MESSAGE);
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
        UIHelper.styleTextField(codeField);
        UIHelper.styleTextField(nameField);
        UIHelper.styleTextField(legalField);

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
        if (dialog.showDialog()) {
            loadTaxRates();
        }
    }

    private void toggleSelectedTaxRate() {
        TaxRateDTO sel = selectedTaxRate();
        if (sel == null) return;
        UIHelper.runWithProgress(this, "A actualizar taxa fiscal…", () -> {
            if (sel.active()) fiscalApiClient.deactivateTaxRate(sel.id());
            else fiscalApiClient.activateTaxRate(sel.id());
            return null;
        }, ignored -> loadTaxRates(), this::showActionError);
    }

    // ─── Tab 3: Retenções na Fonte ────────────────────────────────────────

    private JPanel buildWithholdingsTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createSubheading("Retenções na Fonte"), BorderLayout.WEST);

        ModernButton newBtn = UIHelper.createSuccessButton("Registar Retenção");
        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        ModernButton deliverBtn = UIHelper.createSuccessButton("Marcar como Entregue");
        deliverBtn.setIcon(UIHelper.icon("fas-check", 14));
        ModernButton deleteBtn = UIHelper.createDangerButton("Eliminar");
        deleteBtn.setIcon(UIHelper.icon("fas-trash", 14));
        newBtn.addActionListener(e -> openWithholdingDialog());
        deliverBtn.addActionListener(e -> deliverWithholding());
        deleteBtn.addActionListener(e -> deleteWithholding());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(deleteBtn);
        actions.add(deliverBtn);
        actions.add(newBtn);
        header.add(actions, BorderLayout.EAST);
        tab.add(header, BorderLayout.NORTH);

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
        for (int column : new int[]{5, 7, 8}) {
            withholdingsTable.getColumnModel().getColumn(column).setCellRenderer(TableCellRenderers.money());
        }
        withholdingsTable.getColumnModel().getColumn(9).setCellRenderer(TableCellRenderers.status());
        JScrollPane scroll = new JScrollPane(withholdingsTable);
        UIHelper.styleScrollPane(scroll);

        JTextField whSearch = TableFilter.searchField("Beneficiário, NUIT, descrição ou categoria…");
        JComboBox<String> whEstado = TableFilter.combo("Todos os estados",
                "PENDING", "DELIVERED", "CANCELLED");
        JComboBox<String> whPeriodo = TableFilter.periodCombo();
        TableFilter.install(withholdingsTable, whSearch,
                java.util.List.of(new TableFilter.ColumnFilter(whEstado, 9)),
                java.util.List.of(new TableFilter.PeriodFilter(whPeriodo, 0)));
        JPanel whBar = TableFilter.bar(whSearch,
                TableFilter.label("Estado:"), whEstado,
                TableFilter.label("Data:", "fas-calendar-alt"), whPeriodo);
        whBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(whBar, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private void loadWithholdings() {
        if (withholdingsModel == null) return;
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(this, () -> fiscalApiClient.getWithholdings(companyId), this::applyWithholdings,
                error -> showLoadError("retenções na fonte", error));
    }

    private void applyWithholdings(List<WithholdingRecordDTO> loaded) {
        withholdingsModel.setRowCount(0);
        withholdingsList = loaded;
        for (var w : withholdingsList) {
            BigDecimal pct = w.taxRate().multiply(BigDecimal.valueOf(100));
            withholdingsModel.addRow(new Object[]{
                    w.recordDate().format(DATE_FMT),
                    w.beneficiaryName(),
                    w.beneficiaryTaxId() == null ? "" : w.beneficiaryTaxId(),
                    w.serviceDescription(),
                    w.taxCategory(),
                    w.baseAmount(),
                    pct.stripTrailingZeros().toPlainString() + " %",
                    w.withheldAmount(), w.netPaid(),
                    w.status(),
                    w.deliveredAt() == null ? "-" : w.deliveredAt().format(DATE_FMT)
            });
        }
    }

    private void showLoadError(String area, Throwable error) {
        JOptionPane.showMessageDialog(this, "Não foi possível carregar " + area + ": " + error.getMessage(),
                "Erro de ligação", JOptionPane.ERROR_MESSAGE);
    }

    private WithholdingRecordDTO selectedWithholding() {
        int row = TableFilter.selectedModelRow(withholdingsTable);
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione um registo.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return withholdingsList.get(row);
    }

    private void openWithholdingDialog() {
        JTextField nameField = new JTextField();
        JTextField taxIdField = new JTextField();
        JTextField descField = new JTextField();
        JComboBox<String> catCombo = new JComboBox<>(WITHHOLDING_CATEGORIES);
        MoneyField baseField = new MoneyField("0");
        DecimalField rateField = new DecimalField("0.10", 4, false);
        DateField dateField = new DateField(LocalDate.now());
        UIHelper.styleComboBox(catCombo);
        UIHelper.styleTextField(nameField);
        UIHelper.styleTextField(taxIdField);
        UIHelper.styleTextField(descField);

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
        if (dialog.showDialog()) {
            loadWithholdings();
        }
    }

    private void deliverWithholding() {
        var sel = selectedWithholding();
        if (sel == null) return;
        UIHelper.runWithProgress(this, "A entregar retenção…",
                () -> { fiscalApiClient.deliverWithholding(sel.id()); return null; },
                ignored -> loadWithholdings(), this::showActionError);
    }

    private void deleteWithholding() {
        var sel = selectedWithholding();
        if (sel == null) return;
        int ok = JOptionPane.showConfirmDialog(this, "Eliminar este registo?", "Confirmar",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;
        UIHelper.runWithProgress(this, "A eliminar retenção…",
                () -> { fiscalApiClient.deleteWithholding(sel.id()); return null; },
                ignored -> loadWithholdings(), this::showActionError);
    }

    private void showActionError(Throwable error) {
        JOptionPane.showMessageDialog(this, "Erro: " + error.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
    }

    // ─── Tab 4: Declarações ───────────────────────────────────────────────

    private JPanel buildDeclarationsTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(20, 20, 20, 20));

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 12));
        card.setBorder(new EmptyBorder(24, 24, 24, 24));

        JLabel title = new JLabel("Documentos para a Autoridade Tributária");
        title.setFont(new Font(UIHelper.FONT, Font.BOLD, 18));
        title.setForeground(UIHelper.TEXT_LIGHT);

        JLabel hint = new JLabel("<html>Estes documentos são exportados em PDF profissional, prontos para impressão e arquivo. " +
                "Inclua todas as faturas aprovadas/pagas e compras não-canceladas do período selecionado.</html>");
        hint.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        hint.setForeground(UIHelper.TEXT_MUTED);

        ModernButton ivaDocBtn = UIHelper.createSecondaryButton("Declaração Mensal de IVA");
        ivaDocBtn.setIcon(UIHelper.icon("fas-file-pdf", 14));
        ivaDocBtn.setPreferredSize(new Dimension(280, 44));
        ivaDocBtn.addActionListener(e -> printIvaDeclaration());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        actions.setOpaque(false);
        actions.add(ivaDocBtn);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        hint.setAlignmentX(Component.LEFT_ALIGNMENT);
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(title);
        content.add(Box.createRigidArea(new Dimension(0, 8)));
        content.add(hint);
        content.add(Box.createRigidArea(new Dimension(0, 18)));
        content.add(actions);

        card.add(content, BorderLayout.NORTH);
        tab.add(card, BorderLayout.NORTH);
        return tab;
    }

    private JLabel filterLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(UIHelper.TEXT_MUTED);
        l.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        return l;
    }
}
