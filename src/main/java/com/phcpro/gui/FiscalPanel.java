package com.phcpro.gui;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.gui.components.ModernButton;
import com.phcpro.gui.components.ModernPanel;
import com.phcpro.gui.components.UIHelper;
import com.phcpro.modules.fiscal.dto.CreateTaxRateRequest;
import com.phcpro.modules.fiscal.dto.CreateWithholdingRequest;
import com.phcpro.modules.fiscal.dto.IvaSummaryDTO;
import com.phcpro.modules.fiscal.dto.TaxRateDTO;
import com.phcpro.modules.fiscal.dto.WithholdingRecordDTO;
import com.phcpro.modules.fiscal.service.FiscalSummaryService;
import com.phcpro.modules.fiscal.service.TaxRateService;
import com.phcpro.modules.fiscal.service.WithholdingService;
import com.phcpro.modules.printing.IvaDeclarationPrintService;
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

    private final TaxRateService taxRateService;
    private final WithholdingService withholdingService;
    private final FiscalSummaryService fiscalSummaryService;
    private final IvaDeclarationPrintService ivaDeclarationPrintService;

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

    public FiscalPanel(
            TaxRateService taxRateService,
            WithholdingService withholdingService,
            FiscalSummaryService fiscalSummaryService,
            IvaDeclarationPrintService ivaDeclarationPrintService
    ) {
        this.taxRateService = taxRateService;
        this.withholdingService = withholdingService;
        this.fiscalSummaryService = fiscalSummaryService;
        this.ivaDeclarationPrintService = ivaDeclarationPrintService;

        setLayout(new BorderLayout(0, 10));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        add(UIHelper.createHeading("Área Fiscal — Moçambique"), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        UIHelper.styleTabbedPane(tabs);
        tabs.addTab("Apuramento IVA",   UIHelper.icon("fas-percent", 16, UIHelper.TEXT_LIGHT),       buildIvaTab());
        tabs.addTab("Taxas Fiscais",    UIHelper.icon("fas-balance-scale", 16, UIHelper.TEXT_LIGHT), buildTaxRatesTab());
        tabs.addTab("Retenções na Fonte", UIHelper.icon("fas-hand-holding-usd", 16, UIHelper.TEXT_LIGHT), buildWithholdingsTab());
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
        ivaMonthSpinner.setPreferredSize(new Dimension(70, 32));
        ivaYearSpinner.setPreferredSize(new Dimension(90, 32));
        ivaMonthSpinner.addChangeListener(e -> recomputeIva());
        ivaYearSpinner.addChangeListener(e -> recomputeIva());
        periodPanel.add(ivaMonthSpinner);
        periodPanel.add(new JLabel("/"));
        periodPanel.add(ivaYearSpinner);

        ModernButton printBtn = new ModernButton("Imprimir Declaração IVA", new Color(99, 102, 241), new Color(129, 140, 248));
        printBtn.setIcon(UIHelper.icon("fas-print", 14));
        printBtn.addActionListener(e -> printIvaDeclaration());
        periodPanel.add(Box.createRigidArea(new Dimension(20, 0)));
        periodPanel.add(printBtn);

        topRow.add(periodPanel, BorderLayout.WEST);
        tab.add(topRow, BorderLayout.NORTH);

        // Center: KPI cards row + sales/purchases tables stacked
        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);

        // KPI cards
        JPanel kpis = new JPanel(new GridLayout(1, 3, 12, 0));
        kpis.setOpaque(false);
        kpis.setPreferredSize(new Dimension(0, 110));
        ivaSalesBaseLbl = new JLabel("0.00 MT", SwingConstants.LEFT);
        ivaOutputLbl = new JLabel("0.00 MT", SwingConstants.LEFT);
        ivaInputLbl = new JLabel("0.00 MT", SwingConstants.LEFT);
        ivaPurchasesBaseLbl = new JLabel("0.00 MT", SwingConstants.LEFT);
        ivaNetLbl = new JLabel("0.00 MT", SwingConstants.LEFT);
        for (JLabel l : new JLabel[]{ivaSalesBaseLbl, ivaOutputLbl, ivaInputLbl, ivaPurchasesBaseLbl, ivaNetLbl}) {
            l.setFont(new Font("Segoe UI", Font.BOLD, 19));
            l.setForeground(Color.WHITE);
        }
        kpis.add(kpiCard("IVA LIQUIDADO (VENDAS)", ivaOutputLbl,
                "Base: ", ivaSalesBaseLbl, new Color(109, 40, 217), new Color(147, 51, 234)));
        kpis.add(kpiCard("IVA DEDUZIDO (COMPRAS)", ivaInputLbl,
                "Base: ", ivaPurchasesBaseLbl, new Color(9, 79, 172), new Color(13, 148, 136)));
        kpis.add(kpiCard("IVA LÍQUIDO", ivaNetLbl,
                null, null, new Color(13, 148, 136), new Color(20, 184, 166)));
        center.add(kpis, BorderLayout.NORTH);

        // Split tables (sales / purchases)
        JPanel split = new JPanel(new GridLayout(1, 2, 12, 0));
        split.setOpaque(false);

        ivaSalesModel = new DefaultTableModel(new String[]{"Documento", "Cliente", "Base", "IVA", "Total"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable salesTable = new JTable(ivaSalesModel);
        UIHelper.styleTable(salesTable);
        split.add(wrapTable("Vendas do Período", salesTable));

        ivaPurchasesModel = new DefaultTableModel(new String[]{"Documento", "Fornecedor", "Base", "IVA", "Total"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable purchasesTable = new JTable(ivaPurchasesModel);
        UIHelper.styleTable(purchasesTable);
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
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        titleLbl.setForeground(new Color(224, 242, 254));
        card.add(titleLbl, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        if (subValue != null) {
            JPanel sub = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            sub.setOpaque(false);
            JLabel pre = new JLabel(subPrefix);
            pre.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            pre.setForeground(new Color(224, 242, 254));
            subValue.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            subValue.setForeground(new Color(224, 242, 254));
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
        IvaSummaryDTO s = fiscalSummaryService.computeMonth(companyId, year, month);

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
                    String.format("%,.2f", l.base()),
                    String.format("%,.2f", l.tax()),
                    String.format("%,.2f", l.total())
            });
        }
        ivaPurchasesModel.setRowCount(0);
        for (var l : s.purchases()) {
            ivaPurchasesModel.addRow(new Object[]{
                    l.documentNumber(), l.partner(),
                    String.format("%,.2f", l.base()),
                    String.format("%,.2f", l.tax()),
                    String.format("%,.2f", l.total())
            });
        }
    }

    private void printIvaDeclaration() {
        try {
            int year = (Integer) ivaYearSpinner.getValue();
            int month = (Integer) ivaMonthSpinner.getValue();
            byte[] pdf = ivaDeclarationPrintService.render(
                    CurrentUserContext.getCurrentCompanyId(), year, month);
            PdfFileSaver.saveAndOpen(pdf, "declaracao-iva-" + year + "-" + String.format("%02d", month));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ─── Tab 2: Taxas Fiscais ──────────────────────────────────────────────

    private JPanel buildTaxRatesTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createSubheading("Tabela de Taxas Fiscais"), BorderLayout.WEST);

        ModernButton newBtn = new ModernButton("Nova Taxa", new Color(16, 185, 129), new Color(52, 211, 153));
        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        ModernButton editBtn = new ModernButton("Editar", UIHelper.ACCENT_BLUE, UIHelper.ACCENT_BLUE.brighter());
        editBtn.setIcon(UIHelper.icon("fas-edit", 14));
        ModernButton toggleBtn = new ModernButton("Activar / Desactivar", new Color(107, 114, 128), new Color(156, 163, 175));
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
        card.add(scroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private void loadTaxRates() {
        if (taxRatesModel == null) return;
        taxRatesModel.setRowCount(0);
        taxRatesList = taxRateService.getAll();
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
        int row = taxRatesTable.getSelectedRow();
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
        JTextField rateField = new JTextField(existing == null ? "0.16" : existing.rate().toPlainString());
        JTextField legalField = new JTextField(existing == null || existing.legalBasis() == null ? "" : existing.legalBasis());
        UIHelper.styleTextField(codeField);
        UIHelper.styleTextField(nameField);
        UIHelper.styleTextField(rateField);
        UIHelper.styleTextField(legalField);

        if (existing != null) codeField.setEditable(false);

        JPanel form = UIHelper.createDialogForm(
                "Código:", codeField,
                "Designação:", nameField,
                "Tipo:", typeCombo,
                "Taxa (fração, ex: 0.16 para 16%):", rateField,
                "Base Legal:", legalField
        );

        int opt = JOptionPane.showConfirmDialog(this, UIHelper.makeDialogScrollable(form),
                existing == null ? "Nova Taxa Fiscal" : "Editar Taxa Fiscal",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opt != JOptionPane.OK_OPTION) return;

        try {
            CreateTaxRateRequest req = new CreateTaxRateRequest(
                    codeField.getText().trim(),
                    nameField.getText().trim(),
                    (String) typeCombo.getSelectedItem(),
                    new BigDecimal(rateField.getText().trim()),
                    legalField.getText().trim().isEmpty() ? null : legalField.getText().trim()
            );
            if (existing == null) taxRateService.create(req);
            else taxRateService.update(existing.id(), req);
            loadTaxRates();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Taxa inválida.", "Erro", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void toggleSelectedTaxRate() {
        TaxRateDTO sel = selectedTaxRate();
        if (sel == null) return;
        try {
            if (sel.active()) taxRateService.deactivate(sel.id());
            else taxRateService.activate(sel.id());
            loadTaxRates();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ─── Tab 3: Retenções na Fonte ────────────────────────────────────────

    private JPanel buildWithholdingsTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(UIHelper.createSubheading("Retenções na Fonte"), BorderLayout.WEST);

        ModernButton newBtn = new ModernButton("Registar Retenção", new Color(16, 185, 129), new Color(52, 211, 153));
        newBtn.setIcon(UIHelper.icon("fas-plus", 14));
        ModernButton deliverBtn = new ModernButton("Marcar como Entregue", UIHelper.APPROVED_GREEN, UIHelper.APPROVED_GREEN.brighter());
        deliverBtn.setIcon(UIHelper.icon("fas-check", 14));
        ModernButton deleteBtn = new ModernButton("Eliminar", UIHelper.REJECTED_RED, UIHelper.REJECTED_RED.brighter());
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
        JScrollPane scroll = new JScrollPane(withholdingsTable);
        UIHelper.styleScrollPane(scroll);
        card.add(scroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    private void loadWithholdings() {
        if (withholdingsModel == null) return;
        withholdingsModel.setRowCount(0);
        withholdingsList = withholdingService.findByCompany(CurrentUserContext.getCurrentCompanyId());
        for (var w : withholdingsList) {
            BigDecimal pct = w.taxRate().multiply(BigDecimal.valueOf(100));
            withholdingsModel.addRow(new Object[]{
                    w.recordDate().format(DATE_FMT),
                    w.beneficiaryName(),
                    w.beneficiaryTaxId() == null ? "" : w.beneficiaryTaxId(),
                    w.serviceDescription(),
                    w.taxCategory(),
                    String.format("%,.2f", w.baseAmount()),
                    pct.stripTrailingZeros().toPlainString() + " %",
                    String.format("%,.2f", w.withheldAmount()),
                    String.format("%,.2f", w.netPaid()),
                    w.status(),
                    w.deliveredAt() == null ? "-" : w.deliveredAt().format(DATE_FMT)
            });
        }
    }

    private WithholdingRecordDTO selectedWithholding() {
        int row = withholdingsTable.getSelectedRow();
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
        JTextField baseField = new JTextField("0");
        JTextField rateField = new JTextField("0.10");
        JTextField dateField = new JTextField(LocalDate.now().toString());
        UIHelper.styleComboBox(catCombo);
        UIHelper.styleTextField(nameField);
        UIHelper.styleTextField(taxIdField);
        UIHelper.styleTextField(descField);
        UIHelper.styleTextField(baseField);
        UIHelper.styleTextField(rateField);
        UIHelper.styleTextField(dateField);

        JPanel form = UIHelper.createDialogForm(
                "Data (yyyy-MM-dd):", dateField,
                "Beneficiário:", nameField,
                "NUIT do Beneficiário:", taxIdField,
                "Descrição do Serviço:", descField,
                "Categoria:", catCombo,
                "Base (MT):", baseField,
                "Taxa (fração, ex: 0.10):", rateField
        );

        int opt = JOptionPane.showConfirmDialog(this, UIHelper.makeDialogScrollable(form),
                "Registar Retenção na Fonte", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opt != JOptionPane.OK_OPTION) return;

        try {
            CreateWithholdingRequest req = new CreateWithholdingRequest(
                    CurrentUserContext.getCurrentCompanyId(),
                    LocalDate.parse(dateField.getText().trim()),
                    nameField.getText().trim(),
                    taxIdField.getText().trim().isEmpty() ? null : taxIdField.getText().trim(),
                    descField.getText().trim(),
                    new BigDecimal(baseField.getText().trim()),
                    new BigDecimal(rateField.getText().trim()),
                    (String) catCombo.getSelectedItem()
            );
            withholdingService.create(req);
            loadWithholdings();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deliverWithholding() {
        var sel = selectedWithholding();
        if (sel == null) return;
        try {
            withholdingService.markDelivered(sel.id());
            loadWithholdings();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteWithholding() {
        var sel = selectedWithholding();
        if (sel == null) return;
        int ok = JOptionPane.showConfirmDialog(this, "Eliminar este registo?", "Confirmar",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok != JOptionPane.YES_OPTION) return;
        try {
            withholdingService.delete(sel.id());
            loadWithholdings();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
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
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(UIHelper.TEXT_LIGHT);

        JLabel hint = new JLabel("<html>Estes documentos são exportados em PDF profissional, prontos para impressão e arquivo. " +
                "Inclua todas as faturas aprovadas/pagas e compras não-canceladas do período selecionado.</html>");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        hint.setForeground(UIHelper.TEXT_MUTED);

        ModernButton ivaDocBtn = new ModernButton("Declaração Mensal de IVA",
                new Color(13, 148, 136), new Color(20, 184, 166));
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
        l.setFont(new Font("Segoe UI", Font.BOLD, 12));
        return l;
    }
}
