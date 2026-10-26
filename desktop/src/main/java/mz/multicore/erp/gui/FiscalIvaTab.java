package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.FiscalApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.fiscal.dto.IvaSummaryDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Aba de apuramento mensal de IVA moçambicano, Modelo A e exportação SAF-T. */
final class FiscalIvaTab {

    private final FiscalPanel owner;
    private final FiscalApiClient fiscalApiClient;

    private JSpinner ivaYearSpinner;
    private JSpinner ivaMonthSpinner;
    private MoneyField ivaPreviousCreditField;
    private JLabel ivaOutputLbl, ivaInputLbl, ivaNetLbl, ivaPreviousCreditLbl;
    private JLabel ivaSalesSubLbl, ivaPurchasesSubLbl, ivaPrevCreditSubLbl, ivaStatusSubLbl;
    private DefaultTableModel ivaSalesModel;
    private DefaultTableModel ivaPurchasesModel;

    FiscalIvaTab(FiscalPanel owner, FiscalApiClient fiscalApiClient) {
        this.owner = owner;
        this.fiscalApiClient = fiscalApiClient;
    }

    public JPanel buildTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        JPanel topRow = new JPanel(new BorderLayout(20, 0));
        topRow.setOpaque(false);

        JPanel periodPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        periodPanel.setOpaque(false);
        periodPanel.add(filterLabel("Período:"));
        ivaMonthSpinner = new JSpinner(new SpinnerNumberModel(LocalDate.now().getMonthValue(), 1, 12, 1));
        ivaYearSpinner = UIHelper.createYearSpinner(LocalDate.now().getYear());
        ivaMonthSpinner.addChangeListener(e -> recomputeIva());
        ivaYearSpinner.addChangeListener(e -> recomputeIva());
        periodPanel.add(ivaMonthSpinner);
        periodPanel.add(new JLabel("/"));
        periodPanel.add(ivaYearSpinner);

        periodPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        periodPanel.add(filterLabel("Crédito Anterior (MT):"));
        ivaPreviousCreditField = new MoneyField("0.00");
        ivaPreviousCreditField.setPreferredSize(new Dimension(100, UIHelper.FORM_CONTROL_HEIGHT));
        ivaPreviousCreditField.addActionListener(e -> recomputeIva());
        periodPanel.add(ivaPreviousCreditField);

        ModernButton recalcBtn = UIHelper.createSecondaryButton("Recalcular");
        recalcBtn.setIcon(UIHelper.icon("fas-sync-alt", 12));
        recalcBtn.addActionListener(e -> recomputeIva());
        periodPanel.add(recalcBtn);

        ActionMenuButton documentsBtn = UIHelper.createActionMenuButton("Documentos")
                .addAction("Imprimir Declaração IVA", UIHelper.icon("fas-print", 14), this::printIvaDeclaration)
                .addAction("Exportar SAF-T (Vendas)", UIHelper.icon("fas-file-export", 14), this::exportSaft)
                .addAction("Validar SAF-T", UIHelper.icon("fas-check-circle", 14), this::validateSaft);
        periodPanel.add(Box.createRigidArea(new Dimension(16, 0)));
        periodPanel.add(documentsBtn);

        topRow.add(periodPanel, BorderLayout.WEST);
        tab.add(topRow, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);

        JPanel kpis = KpiCard.createGrid(4);
        ivaOutputLbl = new JLabel("0,00 MT", SwingConstants.LEFT);
        ivaSalesSubLbl = new JLabel("Base: 0,00 MT", SwingConstants.LEFT);
        ivaInputLbl = new JLabel("0,00 MT", SwingConstants.LEFT);
        ivaPurchasesSubLbl = new JLabel("Base: 0,00 MT", SwingConstants.LEFT);
        ivaPreviousCreditLbl = new JLabel("0,00 MT", SwingConstants.LEFT);
        ivaPrevCreditSubLbl = new JLabel("Reporte anterior", SwingConstants.LEFT);
        ivaNetLbl = new JLabel("0,00 MT", SwingConstants.LEFT);
        ivaStatusSubLbl = new JLabel("A calcular...", SwingConstants.LEFT);
        kpis.add(KpiCard.createMetricCard("IVA LIQUIDADO (VENDAS)", ivaOutputLbl, ivaSalesSubLbl, "fas-arrow-trend-up", UIHelper.KPI_PURPLE_DARK));
        kpis.add(KpiCard.createMetricCard("IVA DEDUZIDO (COMPRAS)", ivaInputLbl, ivaPurchasesSubLbl, "fas-arrow-trend-down", UIHelper.KPI_INFO_DARK));
        kpis.add(KpiCard.createMetricCard("CRÉDITO ANTERIOR", ivaPreviousCreditLbl, ivaPrevCreditSubLbl, "fas-history", UIHelper.ACCENT_BLUE));
        kpis.add(KpiCard.createMetricCard("SALDO FISCAL LÍQUIDO", ivaNetLbl, ivaStatusSubLbl, "fas-scale-balanced", UIHelper.APPROVED_GREEN));
        center.add(kpis, BorderLayout.NORTH);

        JPanel split = new JPanel(new GridLayout(1, 2, 12, 0));
        split.setOpaque(false);
        ivaSalesModel = createTableModel(new String[]{"Documento", "Cliente", "Base", "IVA", "Total"});
        split.add(wrapTable("Vendas do Período", buildTable(ivaSalesModel)));
        ivaPurchasesModel = createTableModel(new String[]{"Documento", "Fornecedor", "Base", "IVA", "Total"});
        split.add(wrapTable("Compras do Período", buildTable(ivaPurchasesModel)));

        center.add(split, BorderLayout.CENTER);
        tab.add(center, BorderLayout.CENTER);
        return tab;
    }

    private DefaultTableModel createTableModel(String[] headers) {
        return new DefaultTableModel(headers, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    private JTable buildTable(DefaultTableModel model) {
        JTable table = new JTable(model);
        UIHelper.styleTable(table);
        for (int col = 2; col <= 4; col++) {
            table.getColumnModel().getColumn(col).setCellRenderer(TableCellRenderers.money());
        }
        return table;
    }

    private JPanel wrapTable(String title, JTable table) {
        JPanel wrap = new JPanel(new BorderLayout(0, 6));
        wrap.setOpaque(false);
        wrap.add(UIHelper.createSubheading(title), BorderLayout.NORTH);
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(10, 10, 10, 10));
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        card.add(scroll, BorderLayout.CENTER);
        wrap.add(card, BorderLayout.CENTER);
        return wrap;
    }

    private BigDecimal getPreviousCreditInput() {
        if (ivaPreviousCreditField == null) return BigDecimal.ZERO;
        try {
            BigDecimal val = ivaPreviousCreditField.optionalValue();
            return val != null && val.compareTo(BigDecimal.ZERO) > 0 ? val : BigDecimal.ZERO;
        } catch (Exception ignored) {
            return BigDecimal.ZERO;
        }
    }

    public void recomputeIva() {
        if (ivaYearSpinner == null) return;
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        int year = (Integer) ivaYearSpinner.getValue();
        int month = (Integer) ivaMonthSpinner.getValue();
        BigDecimal prevCredit = getPreviousCreditInput();
        UIHelper.loadAsync(owner, () -> fiscalApiClient.ivaSummary(companyId, year, month, prevCredit),
                this::applyIvaSummary, error -> owner.showLoadError("apuramento do IVA", error));
    }

    private void applyIvaSummary(IvaSummaryDTO s) {
        ivaSalesSubLbl.setText(String.format("Base: %,.2f MT", s.salesBase()));
        ivaPurchasesSubLbl.setText(String.format("Base: %,.2f MT", s.purchasesBase()));
        ivaOutputLbl.setText(String.format("%,.2f MT", s.outputTax()));
        ivaInputLbl.setText(String.format("%,.2f MT", s.inputTax()));
        ivaPreviousCreditLbl.setText(String.format("%,.2f MT", s.previousCredit() != null ? s.previousCredit() : BigDecimal.ZERO));

        if ("A_PAGAR".equals(s.fiscalStatus())) {
            ivaNetLbl.setText(String.format("%,.2f MT", s.payableAmount()));
            ivaStatusSubLbl.setText("A entregar ao Estado");
            ivaStatusSubLbl.setForeground(UIHelper.PENDING_YELLOW);
        } else {
            ivaNetLbl.setText(String.format("%,.2f MT", s.creditToCarry()));
            ivaStatusSubLbl.setText("Crédito a reportar");
            ivaStatusSubLbl.setForeground(UIHelper.APPROVED_GREEN);
        }

        ivaSalesModel.setRowCount(0);
        for (var l : s.sales()) {
            ivaSalesModel.addRow(new Object[]{l.documentNumber(), l.partner(), l.base(), l.tax(), l.total()});
        }
        ivaPurchasesModel.setRowCount(0);
        for (var l : s.purchases()) {
            ivaPurchasesModel.addRow(new Object[]{l.documentNumber(), l.partner(), l.base(), l.tax(), l.total()});
        }
    }

    public void printIvaDeclaration() {
        int year = (Integer) ivaYearSpinner.getValue();
        int month = (Integer) ivaMonthSpinner.getValue();
        BigDecimal prevCredit = getPreviousCreditInput();
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.runWithProgress(owner, "A gerar declaração de IVA…",
                () -> fiscalApiClient.renderIvaDeclaration(companyId, year, month, prevCredit),
                pdf -> PrintPreviewDialog.show(owner, pdf,
                        "declaracao-iva-" + year + "-" + String.format("%02d", month) + ".pdf"),
                owner::showActionError);
    }

    private void exportSaft() {
        int year = (Integer) ivaYearSpinner.getValue();
        int month = (Integer) ivaMonthSpinner.getValue();
        java.time.YearMonth ym = java.time.YearMonth.of(year, month);
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.runWithProgress(owner, "A preparar SAF-T…",
                () -> fiscalApiClient.exportSaft(companyId, ym.atDay(1), ym.atEndOfMonth()), export -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Guardar exportação SAF-T (Vendas)");
            chooser.setSelectedFile(new java.io.File(
                    "saft_vendas_" + year + "-" + String.format("%02d", month) + ".xml"));
            if (chooser.showSaveDialog(owner) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            java.io.File target = chooser.getSelectedFile();
            UIHelper.runWithProgress(owner, "A gravar SAF-T…", () -> {
                java.nio.file.Files.writeString(target.toPath(), export.xml());
                return target;
            }, file -> ToastManager.success(owner,
                    "Exportação SAF-T gravada (" + export.numberOfInvoices() + " facturas), total "
                            + String.format("%,.2f MT", export.totalGross()) + ": " + file.getAbsolutePath()),
                    owner::showActionError);
        }, owner::showActionError);
    }

    private void validateSaft() {
        int year = (Integer) ivaYearSpinner.getValue();
        int month = (Integer) ivaMonthSpinner.getValue();
        java.time.YearMonth ym = java.time.YearMonth.of(year, month);
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.runWithProgress(owner, "A validar SAF-T…",
                () -> fiscalApiClient.validateSaft(companyId, ym.atDay(1), ym.atEndOfMonth()), r -> {
            if (!r.xsdConfigured()) {
                ToastManager.show(owner, FeedbackType.WARNING, r.message());
                return;
            }
            if (r.valid()) {
                ToastManager.success(owner, "SAF-T válido face à XSD.");
                return;
            }
            StringBuilder sb = new StringBuilder(r.message()).append("\n\n");
            int shown = Math.min(r.errors().size(), 15);
            for (int i = 0; i < shown; i++) sb.append("• ").append(r.errors().get(i)).append('\n');
            if (r.errors().size() > shown) sb.append("… (+").append(r.errors().size() - shown).append(" erro(s))");
            JTextArea area = new JTextArea(sb.toString(), 16, 60);
            area.setEditable(false);
            UIHelper.styleTextArea(area);
            JPanel form = new JPanel(new BorderLayout(0, 8));
            form.setOpaque(false);
            form.add(new JScrollPane(area), BorderLayout.CENTER);
            ModernFormDialog dlg = new ModernFormDialog(UIHelper.mainWindow, "Validação SAF-T — Inconsistências",
                    "fas-exclamation-triangle", "Erros encontrados na validação face à XSD oficial", form)
                    .setConfirmButton("Fechar", "fas-check");
            dlg.showDialog();
        }, owner::showActionError);
    }

    private JLabel filterLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(UIHelper.TEXT_MUTED);
        l.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        return l;
    }
}
