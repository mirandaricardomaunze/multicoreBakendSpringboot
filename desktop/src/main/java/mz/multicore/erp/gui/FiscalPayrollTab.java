package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.FiscalApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.hr.dto.PayrollFiscalSummaryDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;

/** Aba de apuramento fiscal de salários (IRPS & INSS). */
final class FiscalPayrollTab {

    private final FiscalPanel owner;
    private final FiscalApiClient fiscalApiClient;

    private JSpinner payrollYearSpinner;
    private JSpinner payrollMonthSpinner;
    private DefaultTableModel payrollFiscalModel;
    private JLabel payrollIrpsLabel;
    private JLabel payrollInssLabel;

    FiscalPayrollTab(FiscalPanel owner, FiscalApiClient fiscalApiClient) {
        this.owner = owner;
        this.fiscalApiClient = fiscalApiClient;
    }

    public JPanel buildTab() {
        JPanel tab = new JPanel(new BorderLayout(0, 12));
        tab.setOpaque(false);
        tab.setBorder(new EmptyBorder(15, 5, 5, 5));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controls.setOpaque(false);
        JLabel periodLbl = new JLabel("Período:");
        periodLbl.setForeground(UIHelper.TEXT_MUTED);
        periodLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        controls.add(periodLbl);
        payrollMonthSpinner = new JSpinner(new SpinnerNumberModel(LocalDate.now().getMonthValue(), 1, 12, 1));
        payrollYearSpinner = UIHelper.createYearSpinner(LocalDate.now().getYear());
        ModernButton refresh = UIHelper.createSecondaryButton("Actualizar Mapa");
        refresh.addActionListener(e -> loadPayrollFiscal());
        ModernButton printBtn = UIHelper.createSecondaryButton("Imprimir Mapa Fiscal");
        printBtn.setIcon(UIHelper.icon("fas-print", 14));
        printBtn.addActionListener(e -> printPayrollFiscalMap());
        controls.add(payrollMonthSpinner);
        controls.add(payrollYearSpinner);
        controls.add(refresh);
        controls.add(printBtn);

        payrollIrpsLabel = new JLabel("IRPS: 0,00 MT");
        payrollInssLabel = new JLabel("INSS total: 0,00 MT");
        payrollIrpsLabel.setForeground(UIHelper.TEXT_LIGHT);
        payrollInssLabel.setForeground(UIHelper.TEXT_LIGHT);
        controls.add(Box.createRigidArea(new Dimension(20, 0)));
        controls.add(payrollIrpsLabel);
        controls.add(payrollInssLabel);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        controls.setBorder(new EmptyBorder(0, 0, 12, 0));
        card.add(controls, BorderLayout.NORTH);

        payrollFiscalModel = new DefaultTableModel(
                new String[]{"Nº", "Colaborador", "NUIT", "Nº INSS", "Bruto", "Tributável", "IRPS", "INSS Trab.", "INSS Patronal"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(payrollFiscalModel);
        UIHelper.styleTable(table);
        for (int col = 4; col <= 8; col++) {
            table.getColumnModel().getColumn(col).setCellRenderer(TableCellRenderers.money());
        }
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        card.add(scroll, BorderLayout.CENTER);
        tab.add(card, BorderLayout.CENTER);
        return tab;
    }

    public void loadPayrollFiscal() {
        if (payrollFiscalModel == null) return;
        int year = (Integer) payrollYearSpinner.getValue();
        int month = (Integer) payrollMonthSpinner.getValue();
        UIHelper.loadAsync(owner, () -> fiscalApiClient.fiscalSummary(year, month), this::applyPayrollFiscal,
                error -> owner.showLoadError("mapa fiscal salarial", error));
    }

    private void applyPayrollFiscal(PayrollFiscalSummaryDTO summary) {
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
        UIHelper.runWithProgress(owner, "A gerar mapa fiscal…",
                () -> fiscalApiClient.renderPayrollFiscalMap(companyId, year, month),
                pdf -> PrintPreviewDialog.show(owner, pdf,
                        "mapa-fiscal-salarial-" + year + "-" + String.format("%02d", month) + ".pdf"),
                owner::showActionError);
    }
}
