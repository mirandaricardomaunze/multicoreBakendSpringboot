package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.CreateInstallmentPlanRequest;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.comercial.dto.InvoiceInstallmentDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Diálogo modal para visualização e reestruturação de plano de prestações de uma fatura.
 * Ver {@code docs/VENCIMENTOS_FRACIONADOS_PRESTACOES_SPEC.md}.
 */
public class InvoiceInstallmentsDialog {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat CURRENCY_FMT = new DecimalFormat("#,##0.00 MT");

    public static void show(Window parent, InvoiceDTO invoice, ComercialApiClient api, Runnable onPlanUpdated) {
        show(parent, api, invoice, onPlanUpdated);
    }

    public static void show(Window parent, ComercialApiClient api, InvoiceDTO invoice, Runnable onPlanUpdated) {
        if (invoice == null) {
            ToastManager.show(parent, FeedbackType.WARNING, "Seleccione uma fatura para gerir as prestações.");
            return;
        }

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);
        content.setPreferredSize(new Dimension(720, 500));

        // 1. Cabeçalho contextual
        JPanel headerCard = new JPanel(new GridLayout(1, 4, 10, 0));
        headerCard.setOpaque(true);
        headerCard.setBackground(UIHelper.BG_CARD);
        headerCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.GRID),
                new EmptyBorder(10, 12, 10, 12)
        ));

        headerCard.add(createHeaderItem("Fatura", invoice.invoiceNumber() != null ? invoice.invoiceNumber() : "—"));
        headerCard.add(createHeaderItem("Cliente", invoice.buyerName() != null ? invoice.buyerName() : "Consumidor Final"));
        headerCard.add(createHeaderItem("Total Faturado", CURRENCY_FMT.format(invoice.totalAmount() != null ? invoice.totalAmount() : BigDecimal.ZERO)));

        headerCard.add(createHeaderItem("Saldo Devedor", CURRENCY_FMT.format(invoice.outstandingAmount())));

        content.add(headerCard, BorderLayout.NORTH);

        // 2. Tabela de Prestações
        String[] cols = {"Parcela", "Vencimento", "Valor da Parcela", "Liquidado", "Saldo Restante", "Estado"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        JTable table = new JTable(model);
        UIHelper.styleTable(table);
        table.getColumnModel().getColumn(0).setPreferredWidth(90);
        table.getColumnModel().getColumn(1).setPreferredWidth(110);
        table.getColumnModel().getColumn(2).setPreferredWidth(130);
        table.getColumnModel().getColumn(3).setPreferredWidth(120);
        table.getColumnModel().getColumn(4).setPreferredWidth(130);
        table.getColumnModel().getColumn(5).setPreferredWidth(120);

        table.getColumnModel().getColumn(2).setCellRenderer(TableCellRenderers.quantity());
        table.getColumnModel().getColumn(3).setCellRenderer(TableCellRenderers.quantity());
        table.getColumnModel().getColumn(4).setCellRenderer(TableCellRenderers.quantity());
        table.getColumnModel().getColumn(5).setCellRenderer(TableCellRenderers.status());

        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);

        // 3. Barra de Configuração de Plano
        JPanel planConfigBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        planConfigBar.setOpaque(false);

        JSpinner countSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 12, 1));
        UIHelper.styleSpinner(countSpinner);

        JSpinner intervalSpinner = new JSpinner(new SpinnerNumberModel(30, 7, 90, 5));
        UIHelper.styleSpinner(intervalSpinner);

        planConfigBar.add(new JLabel("Número de Prestações:"));
        planConfigBar.add(countSpinner);
        planConfigBar.add(Box.createHorizontalStrut(6));
        planConfigBar.add(new JLabel("Intervalo (Dias):"));
        planConfigBar.add(intervalSpinner);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 8));
        centerPanel.setOpaque(false);
        centerPanel.add(planConfigBar, BorderLayout.NORTH);
        centerPanel.add(scroll, BorderLayout.CENTER);
        content.add(centerPanel, BorderLayout.CENTER);

        ModernFormDialog dialog = new ModernFormDialog(
                parent,
                "Prestações e Vencimentos Fracionados",
                "fas-calendar-alt",
                content
        );

        Runnable loadInstallments = () -> UIHelper.runWithProgress(dialog.getDialog(), "A carregar plano de prestações…",
                () -> api.getInvoiceInstallments(invoice.id()),
                list -> {
                    model.setRowCount(0);
                    if (list == null || list.isEmpty()) {
                        LocalDate due = invoice.dueDate() != null ? invoice.dueDate() : LocalDate.now().plusDays(30);
                        model.addRow(new Object[]{
                                "1 / 1",
                                due.format(DATE_FMT),
                                CURRENCY_FMT.format(invoice.totalAmount()),
                                CURRENCY_FMT.format(invoice.amountPaid()),
                                CURRENCY_FMT.format(invoice.outstandingAmount()),
                                UIHelper.humanStatus(invoice.status().name())
                        });
                        return;
                    }
                    for (InvoiceInstallmentDTO inst : list) {
                        model.addRow(new Object[]{
                                inst.installmentNumber() + " / " + inst.totalInstallments(),
                                inst.dueDate() != null ? inst.dueDate().format(DATE_FMT) : "—",
                                CURRENCY_FMT.format(inst.amount()),
                                CURRENCY_FMT.format(inst.paidAmount()),
                                CURRENCY_FMT.format(inst.remainingBalance()),
                                UIHelper.humanStatus(inst.status())
                        });
                    }
                },
                error -> ToastManager.show(dialog.getDialog(), FeedbackType.ERROR, "Erro ao carregar prestações: " + error.getMessage())
        );

        loadInstallments.run();

        ModernButton generateBtn = UIHelper.createPrimaryButton("Estruturar Plano");
        generateBtn.setIcon(UIHelper.icon("fas-calculator", 14));
        generateBtn.addActionListener(e -> {
            int parts = (int) countSpinner.getValue();
            int days = (int) intervalSpinner.getValue();

            CreateInstallmentPlanRequest req = new CreateInstallmentPlanRequest(
                    invoice.id(),
                    parts,
                    days,
                    LocalDate.now().plusDays(days),
                    List.of()
            );

            UIHelper.runWithProgress(dialog.getDialog(), "A gerar plano de prestações…",
                    () -> api.createInvoiceInstallmentPlan(invoice.id(), req),
                    saved -> {
                        ToastManager.show(dialog.getDialog(), FeedbackType.SUCCESS, "Plano de " + parts + " prestações criado com sucesso.");
                        loadInstallments.run();
                        if (onPlanUpdated != null) onPlanUpdated.run();
                    },
                    err -> ToastManager.show(dialog.getDialog(), FeedbackType.ERROR, "Erro ao gerar plano: " + err.getMessage())
            );
        });

        ModernButton payBtn = UIHelper.createSuccessButton("Amortizar Parcela");
        payBtn.setIcon(UIHelper.icon("fas-hand-holding-usd", 14));
        payBtn.addActionListener(e -> {
            BigDecimal val = UIHelper.promptAmount("Amortização de Prestação", "fas-hand-holding-usd",
                    "Indique o valor a amortizar nas prestações", "Valor (MT) *:", BigDecimal.ZERO);
            if (val == null || val.compareTo(BigDecimal.ZERO) <= 0) return;
            UIHelper.runWithProgress(dialog.getDialog(), "A carregar contas…", api::treasuryAccounts, accounts -> {
                Long accountId = TreasuryAccountChooser.choose(dialog.getDialog(), "Receber Prestação",
                        "O valor entra por recibo (RC) na conta escolhida.", accounts, false);
                if (accountId == null) return;
                UIHelper.runWithProgress(dialog.getDialog(), "A emitir recibo da prestação…",
                        () -> api.payInvoiceInstallment(invoice.id(), val, accountId),
                        updated -> {
                            ToastManager.show(dialog.getDialog(), FeedbackType.SUCCESS, "Recibo emitido; prestações actualizadas.");
                            loadInstallments.run();
                            if (onPlanUpdated != null) onPlanUpdated.run();
                        },
                        err -> ToastManager.show(dialog.getDialog(), FeedbackType.ERROR, "Erro ao receber: " + err.getMessage())
                );
            }, err -> ToastManager.show(dialog.getDialog(), FeedbackType.ERROR, "Erro ao carregar contas: " + err.getMessage()));
        });

        dialog.addActionButton(generateBtn);
        dialog.addActionButton(payBtn);
        dialog.setConfirmButton("Fechar", "fas-times");
        dialog.setOnSave(dialog::close);

        dialog.showDialog();
    }

    private static JPanel createHeaderItem(String label, String value) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        l.setForeground(UIHelper.TEXT_MUTED);

        JLabel v = new JLabel(value);
        v.setFont(v.getFont().deriveFont(Font.BOLD, 12f));
        v.setForeground(UIHelper.TEXT_LIGHT);

        p.add(l, BorderLayout.NORTH);
        p.add(v, BorderLayout.CENTER);
        return p;
    }
}
