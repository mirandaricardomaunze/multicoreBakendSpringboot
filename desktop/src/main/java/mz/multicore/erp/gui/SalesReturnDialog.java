package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.session.SignedInUser;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Assistente de Devolução de Faturação no Backoffice com Reintegração FEFO e Emissão de NC.
 * Ver {@code docs/DEVOLUCOES_BACKOFFICE_FEFO_SPEC.md}.
 */
public class SalesReturnDialog {

    private static final DecimalFormat CURRENCY_FMT = new DecimalFormat("#,##0.00 MT");

    public static void show(Window parent, ComercialApiClient api, InvoiceDTO invoice, Runnable onSuccess) {
        if (invoice == null) {
            ToastManager.show(parent, FeedbackType.WARNING, "Seleccione uma fatura para devolução.");
            return;
        }

        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setOpaque(false);
        root.setPreferredSize(new Dimension(920, 540));

        // 1. Cabeçalho contextual
        JPanel headerCard = new JPanel(new GridLayout(1, 4, 10, 0));
        headerCard.setOpaque(true);
        headerCard.setBackground(UIHelper.BG_CARD);
        headerCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.GRID),
                new EmptyBorder(10, 12, 10, 12)
        ));

        headerCard.add(createHeaderItem("Fatura Original:", invoice.invoiceNumber()));
        headerCard.add(createHeaderItem("Cliente:", invoice.clientName() != null ? invoice.clientName() : "Consumidor Final"));
        headerCard.add(createHeaderItem("Total Fatura:", CURRENCY_FMT.format(invoice.totalAmount())));
        BigDecimal paid = invoice.amountPaid() != null ? invoice.amountPaid() : BigDecimal.ZERO;
        BigDecimal balance = invoice.totalAmount().subtract(paid);
        headerCard.add(createHeaderItem("Saldo Devedor:", CURRENCY_FMT.format(balance)));

        root.add(headerCard, BorderLayout.NORTH);

        // 2. Grelha de linhas devolvíveis
        ModernPanel tableCard = new ModernPanel(16);
        tableCard.setLayout(new BorderLayout(0, 8));
        tableCard.setBorder(new EmptyBorder(10, 12, 10, 12));

        String[] cols = {"ID", "Produto", "SKU", "Preço Unit.", "Faturado", "Já Devolvido", "Disponível", "Cx.", "Qtd a Devolver", "Reintegrar Stock?"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return c == 8 || c == 9; // Quantidade e Checkbox de Restock
            }

            @Override
            public Class<?> getColumnClass(int c) {
                if (c == 9) return Boolean.class;
                return Object.class;
            }
        };

        JTable table = new JTable(model);
        UIHelper.styleTable(table);
        table.putClientProperty(ClientTablePagination.DISABLED, Boolean.TRUE);

        table.getColumnModel().getColumn(0).setMinWidth(0);
        table.getColumnModel().getColumn(0).setMaxWidth(0);
        table.getColumnModel().getColumn(0).setWidth(0);
        table.getColumnModel().getColumn(3).setCellRenderer(TableCellRenderers.money());
        table.getColumnModel().getColumn(8).setCellEditor(new DefaultCellEditor(new QuantityField("0", false)));

        UIHelper.ensureHeadersFit(table);
        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);

        JLabel totalCreditLbl = new JLabel("Total a Creditar: 0.00 MT (incl. IVA)");
        totalCreditLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        totalCreditLbl.setForeground(UIHelper.APPROVED_GREEN);

        List<ReturnableInvoiceLineDTO> linesData = new ArrayList<>();

        Runnable recalcTotal = () -> {
            BigDecimal totalCredit = BigDecimal.ZERO;
            for (int i = 0; i < model.getRowCount(); i++) {
                Object val = model.getValueAt(i, 8);
                BigDecimal qty = BigDecimal.ZERO;
                if (val instanceof BigDecimal bd) qty = bd;
                else if (val != null) {
                    try { qty = new BigDecimal(val.toString().trim()); } catch (Exception ignored) {}
                }
                if (qty.signum() > 0 && i < linesData.size()) {
                    ReturnableInvoiceLineDTO line = linesData.get(i);
                    BigDecimal lineSub = line.unitPrice().multiply(qty);
                    BigDecimal lineTax = lineSub.multiply(line.taxRate());
                    totalCredit = totalCredit.add(lineSub.add(lineTax));
                }
            }
            totalCreditLbl.setText("Total a Creditar: " + CURRENCY_FMT.format(totalCredit) + " (incl. IVA)");
        };

        model.addTableModelListener(e -> {
            if (e.getColumn() == 8) {
                int r = e.getFirstRow();
                if (r >= 0 && r < linesData.size()) {
                    Object val = model.getValueAt(r, 8);
                    BigDecimal qty = BigDecimal.ZERO;
                    if (val instanceof BigDecimal bd) qty = bd;
                    else if (val != null) {
                        try { qty = new BigDecimal(val.toString().trim()); } catch (Exception ignored) {}
                    }
                    ReturnableInvoiceLineDTO line = linesData.get(r);
                    if (qty.compareTo(line.returnableQuantity()) > 0) {
                        ToastManager.show(tableCard, FeedbackType.WARNING, "Qtd a devolver não pode exceder o disponível (" + line.returnableQuantity() + ")");
                        model.setValueAt(line.returnableQuantity(), r, 8);
                    }
                }
                recalcTotal.run();
            }
        });

        tableCard.add(UIHelper.tableCardTop("Linhas da Fatura e Seleção de Devolução", totalCreditLbl), BorderLayout.NORTH);
        tableCard.add(scroll, BorderLayout.CENTER);

        // 3. Opções e Motivo
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 6));
        optionsPanel.setOpaque(false);

        JComboBox<String> reasonCombo = new JComboBox<>(new String[]{"Devolução de Artigo (RETURN)", "Defeito / Avaria (DEFECT)", "Acordo Comercial (COMMERCIAL_DISCOUNT)"});
        UIHelper.styleComboBox(reasonCombo);

        JTextField descField = new JTextField(24);
        UIHelper.styleTextField(descField);
        descField.putClientProperty("JTextField.placeholderText", "Observações ou motivo da devolução...");

        JCheckBox autoApproveCheck = new JCheckBox("Aprovação Imediata da Nota de Crédito", SignedInUser.isManagerOrAdmin());
        autoApproveCheck.setOpaque(false);
        autoApproveCheck.setForeground(UIHelper.TEXT_LIGHT);
        autoApproveCheck.setEnabled(SignedInUser.isManagerOrAdmin());

        optionsPanel.add(new JLabel("Motivo:"));
        optionsPanel.add(reasonCombo);
        optionsPanel.add(new JLabel("Observações:"));
        optionsPanel.add(descField);
        optionsPanel.add(autoApproveCheck);

        tableCard.add(optionsPanel, BorderLayout.SOUTH);
        root.add(tableCard, BorderLayout.CENTER);

        ModernFormDialog dialog = new ModernFormDialog(parent, "Assistente de Devolução de Faturação",
                "fas-undo-alt", "Emissão assistida de Nota de Crédito com reintegração de stock FEFO", root);

        UIHelper.applyStandardModalBounds(dialog.getDialog(), 940, 580);

        Runnable loadLines = () -> {
            UIHelper.runWithProgress(tableCard, "A carregar linhas da fatura...", () ->
                    api.getReturnableInvoiceLines(invoice.id()),
                    lines -> {
                        linesData.clear();
                        linesData.addAll(lines);
                        model.setRowCount(0);
                        for (ReturnableInvoiceLineDTO l : lines) {
                            model.addRow(new Object[]{
                                    l.invoiceLineId(),
                                    l.productName(),
                                    l.productCode(),
                                    l.unitPrice(),
                                    l.invoicedQuantity(),
                                    l.alreadyReturnedQuantity(),
                                    l.returnableQuantity(),
                                    l.boxes(),
                                    BigDecimal.ZERO,
                                    Boolean.TRUE
                            });
                        }
                    },
                    err -> ToastManager.show(tableCard, FeedbackType.ERROR, "Erro ao carregar linhas: " + err.getMessage())
            );
        };

        dialog.setConfirmButton("Emitir Nota de Crédito", "fas-file-invoice");
        dialog.setOnSave(() -> {
            List<SalesReturnWizardLineRequest> reqLines = new ArrayList<>();
            for (int i = 0; i < model.getRowCount(); i++) {
                Object val = model.getValueAt(i, 8);
                BigDecimal qty = BigDecimal.ZERO;
                if (val instanceof BigDecimal bd) qty = bd;
                else if (val != null) {
                    try { qty = new BigDecimal(val.toString().trim()); } catch (Exception ignored) {}
                }
                if (qty.signum() > 0) {
                    Long lineId = (Long) model.getValueAt(i, 0);
                    Boolean restock = (Boolean) model.getValueAt(i, 9);
                    reqLines.add(new SalesReturnWizardLineRequest(lineId, qty, restock != null && restock));
                }
            }

            if (reqLines.isEmpty()) {
                ToastManager.show(dialog.getDialog(), FeedbackType.WARNING, "Indique a quantidade a devolver em pelo menos uma linha.");
                return;
            }

            String selReason = reasonCombo.getSelectedIndex() == 0 ? "RETURN" : (reasonCombo.getSelectedIndex() == 1 ? "DEFECT" : "COMMERCIAL_DISCOUNT");
            SalesReturnWizardRequest req = new SalesReturnWizardRequest(
                    invoice.id(),
                    selReason,
                    null,
                    autoApproveCheck.isSelected(),
                    descField.getText().trim(),
                    reqLines
            );

            UIHelper.runWithProgress(dialog.getDialog(), "A processar devolução e Nota de Crédito...", () ->
                    api.processSalesReturn(invoice.id(), req),
                    result -> {
                        ToastManager.success(parent, "Nota de Crédito " + result.creditNoteNumber() + " emitida com sucesso!");
                        dialog.close();
                        if (onSuccess != null) onSuccess.run();
                    },
                    err -> ToastManager.show(dialog.getDialog(), FeedbackType.ERROR, "Erro na devolução: " + err.getMessage())
            );
        });

        loadLines.run();
        dialog.showDialog();
    }

    private static JPanel createHeaderItem(String label, String value) {
        JPanel p = new JPanel(new BorderLayout(0, 2));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        l.setForeground(UIHelper.TEXT_MUTED);

        JLabel v = new JLabel(value != null ? value : "-");
        v.setFont(v.getFont().deriveFont(Font.BOLD, 12f));
        v.setForeground(UIHelper.TEXT_LIGHT);

        p.add(l, BorderLayout.NORTH);
        p.add(v, BorderLayout.CENTER);
        return p;
    }
}
