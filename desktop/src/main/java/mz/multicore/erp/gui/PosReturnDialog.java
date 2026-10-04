package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.*;
import mz.multicore.erp.modules.financeira.dto.TreasuryAccountDTO;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;
import mz.multicore.erp.modules.pos.dto.POSReturnRequest;
import mz.multicore.erp.modules.pos.dto.POSReturnResultDTO;
import mz.multicore.erp.modules.pos.dto.StoreVoucherDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Fluxo de devolução/troca com emissão de Vale de Compras (Store Credit) ou reembolso. */
final class PosReturnDialog {
    private final POSPanel owner;

    PosReturnDialog(POSPanel owner) {
        this.owner = owner;
    }

    public void show() {
        if (owner.warehousesList.isEmpty() || owner.salesHistoryList.isEmpty()) {
            owner.loadMetadata();
            owner.refreshSalesHistory();
        }
        if (owner.warehousesList.isEmpty()) {
            owner.showPosNotice(FeedbackType.WARNING, "Sem armazém de devolução",
                    "Configure um armazém para receber a devolução.");
            return;
        }

        int selectedRow = owner.salesHistoryTable == null ? -1 : TableFilter.selectedModelRow(owner.salesHistoryTable);
        InvoiceDTO invoice = null;
        if (selectedRow >= 0 && selectedRow < owner.salesHistoryList.size()) {
            invoice = owner.salesHistoryList.get(selectedRow);
        } else if (!owner.salesHistoryList.isEmpty()) {
            JComboBox<String> invoiceCombo = new JComboBox<>();
            for (InvoiceDTO inv : owner.salesHistoryList) {
                String clientDesc = inv.clientName() != null && !inv.clientName().isBlank()
                        ? inv.clientName() : "Cliente Geral";
                invoiceCombo.addItem(inv.invoiceNumber() + " · " + clientDesc + " (" + inv.totalAmount() + " MT)");
            }
            UIHelper.styleComboBox(invoiceCombo);
            JPanel selectPanel = new JPanel(new BorderLayout(0, 10));
            selectPanel.setOpaque(false);
            selectPanel.add(new JLabel("Seleccione a fatura a devolver do histórico:"), BorderLayout.NORTH);
            selectPanel.add(invoiceCombo, BorderLayout.CENTER);
            boolean ok = new ModernFormDialog(UIHelper.mainWindow, "Seleccionar Fatura para Devolução",
                    "fas-undo", "Escolha a venda de balcão a devolver", selectPanel)
                    .setConfirmButton("Prosseguir", "fas-arrow-right").showDialog();
            if (!ok) return;
            invoice = owner.salesHistoryList.get(invoiceCombo.getSelectedIndex());
        } else {
            owner.showPosNotice(FeedbackType.WARNING, "Sem histórico de vendas",
                    "Não foram encontradas vendas recentes para efetuar a devolução.");
            return;
        }

        DefaultTableModel linesModel = new DefaultTableModel(
                new String[]{"Linha ID", "Produto", "Qtd Vendida", "Qtd a devolver"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return c == 3; }
        };
        for (var line : invoice.lines()) {
            linesModel.addRow(new Object[]{
                    line.id(),
                    line.productName(),
                    line.quantity().stripTrailingZeros().toPlainString(),
                    "0"
            });
        }
        JTable linesTable = new JTable(linesModel);
        UIHelper.styleTable(linesTable);
        linesTable.getColumnModel().getColumn(0).setMinWidth(0);
        linesTable.getColumnModel().getColumn(0).setMaxWidth(0);
        linesTable.getColumnModel().getColumn(0).setWidth(0);

        JComboBox<String> warehouseReturnCombo = new JComboBox<>();
        for (WarehouseDTO warehouse : owner.warehousesList) {
            warehouseReturnCombo.addItem(warehouse.name());
        }

        String[] methodCodes = {"STORE_CREDIT", "CASH", "CARD", "BANK_TRANSFER", "MPESA", "EMOLA", "CREDIT"};
        String[] methodLabels = {
                "VALE DE COMPRAS (Store Credit)",
                "NUMERÁRIO (Gaveta da Caixa)",
                "CARTÃO (POS Bancário)",
                "TRANSFERÊNCIA BANCÁRIA",
                "M-PESA",
                "E-MOLA",
                "CRÉDITO EM CONTA CORRENTE"
        };
        JComboBox<String> methodCombo = new JComboBox<>(methodLabels);

        JComboBox<String> refundAccountCombo = new JComboBox<>();
        for (TreasuryAccountDTO account : owner.accountsList) {
            refundAccountCombo.addItem(account.name());
        }
        JTextField reasonField = new JTextField("Devolução de cliente");
        UIHelper.styleComboBox(warehouseReturnCombo);
        UIHelper.styleComboBox(methodCombo);
        UIHelper.styleComboBox(refundAccountCombo);
        UIHelper.styleTextField(reasonField);

        Runnable updateAccountVisibility = () -> {
            int sel = methodCombo.getSelectedIndex();
            String code = sel >= 0 && sel < methodCodes.length ? methodCodes[sel] : "STORE_CREDIT";
            boolean needsAccount = "CARD".equals(code) || "BANK_TRANSFER".equals(code)
                    || "MPESA".equals(code) || "EMOLA".equals(code);
            refundAccountCombo.setEnabled(needsAccount);
        };
        methodCombo.addActionListener(e -> updateAccountVisibility.run());
        updateAccountVisibility.run();

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        panel.add(new JScrollPane(linesTable), BorderLayout.CENTER);
        panel.add(UIHelper.createDialogForm(
                "Armazém de entrada:", warehouseReturnCombo,
                "Método de compensação:", methodCombo,
                "Conta para reembolso:", refundAccountCombo,
                "Motivo:", reasonField
        ), BorderLayout.SOUTH);

        final InvoiceDTO targetInvoice = invoice;
        boolean confirmed = new ModernFormDialog(UIHelper.mainWindow, "Devolver / Trocar venda " + targetInvoice.invoiceNumber(),
                "fas-undo", "Devolução com emissão de nota de crédito / vale", panel)
                .setConfirmButton("Confirmar Devolução", "fas-check").showDialog();
        if (!confirmed) {
            return;
        }

        List<CreateCreditNoteLineRequest> lines = new ArrayList<>();
        try {
            for (int i = 0; i < linesModel.getRowCount(); i++) {
                BigDecimal qty = new BigDecimal(String.valueOf(linesModel.getValueAt(i, 3)).trim().replace(",", "."));
                if (qty.compareTo(BigDecimal.ZERO) > 0) {
                    lines.add(new CreateCreditNoteLineRequest((Long) linesModel.getValueAt(i, 0), qty));
                }
            }
        } catch (NumberFormatException ex) {
            owner.showPosNotice(FeedbackType.ERROR, "Quantidade inválida", "Corrija a quantidade indicada nas linhas da devolução.");
            return;
        }
        if (lines.isEmpty()) {
            owner.showPosNotice(FeedbackType.ERROR, "Quantidade obrigatória", "Indique pelo menos uma quantidade a devolver.");
            return;
        }

        int methodIdx = methodCombo.getSelectedIndex();
        String method = methodCodes[methodIdx >= 0 && methodIdx < methodCodes.length ? methodIdx : 0];
        Long accountId = null;
        if (!"CASH".equals(method) && !"CREDIT".equals(method) && !"STORE_CREDIT".equals(method)) {
            int accIdx = refundAccountCombo.getSelectedIndex();
            if (accIdx < 0 || accIdx >= owner.accountsList.size()) {
                owner.showPosNotice(FeedbackType.ERROR, "Conta obrigatória", "Seleccione a conta de tesouraria para o reembolso.");
                return;
            }
            accountId = owner.accountsList.get(accIdx).id();
        }

        POSReturnRequest request = new POSReturnRequest(
                CurrentUserContext.getUsername(), CurrentUserContext.getCurrentCompanyId(), targetInvoice.id(),
                owner.warehousesList.get(warehouseReturnCombo.getSelectedIndex()).id(),
                reasonField.getText().trim(), method, accountId, lines);

        UIHelper.runWithProgress(owner, "A registar devolução", () -> owner.posApiClient.returnSale(request), (POSReturnResultDTO result) -> {
            CreditNoteDTO note = result.creditNote();
            StoreVoucherDTO voucher = result.voucher();

            if (voucher != null) {
                showVoucherIssuedModal(voucher, note);
            } else {
                owner.showPosSuccess("Devolução registada; nota de crédito " + note.noteNumber()
                        + ", total " + note.totalAmount() + " MT.");
            }

            int exchange = JOptionPane.showConfirmDialog(owner,
                    "Pretende lançar agora a venda de troca/substituição?",
                    "Troca no POS", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (exchange == JOptionPane.YES_OPTION) {
                owner.selectView(false);
            }
            owner.refreshSalesHistory();
            owner.refreshSessionState();
            owner.loadMetadata();
        }, error -> owner.showPosNotice(FeedbackType.ERROR,
                "Não foi possível registar a devolução", error.getMessage()));
    }

    private void showVoucherIssuedModal(StoreVoucherDTO voucher, CreditNoteDTO note) {
        ModernPanel card = new ModernPanel(14);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(16, 20, 16, 20));

        JLabel titleLabel = new JLabel("VALE DE COMPRAS EMITIDO");
        titleLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 15));
        titleLabel.setForeground(UIHelper.APPROVED_GREEN);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField codeField = new JTextField(voucher.code());
        codeField.setEditable(false);
        codeField.setFont(new Font(UIHelper.FONT, Font.BOLD, 20));
        codeField.setHorizontalAlignment(JTextField.CENTER);
        codeField.setBackground(UIHelper.ROW_ALT);
        codeField.setBorder(BorderFactory.createLineBorder(UIHelper.APPROVED_GREEN, 2, true));
        codeField.setMaximumSize(new Dimension(300, 44));
        codeField.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel valueLabel = new JLabel(String.format("Valor do Saldo: %,.2f MT", voucher.remainingAmount()));
        valueLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 16));
        valueLabel.setForeground(UIHelper.ACCENT_BLUE);
        valueLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        String expiry = voucher.expiresAt() != null
                ? voucher.expiresAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—";
        JLabel expLabel = new JLabel("Válido até: " + expiry + " · Beneficiário: " + voucher.clientName());
        expLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        expLabel.setForeground(UIHelper.TEXT_MUTED);
        expLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        btnRow.setOpaque(false);

        ModernButton copyBtn = UIHelper.createButton("Copiar Código", UIHelper.icon("fas-copy", 12, Color.WHITE), UIHelper.ACCENT_CYAN, e -> {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(voucher.code()), null);
            ToastManager.success(owner, "Código do vale copiado: " + voucher.code());
        });

        ModernButton printBtn = UIHelper.createPrimaryButton("Imprimir Vale (80mm)");
        printBtn.setIcon(UIHelper.icon("fas-print", 12));
        printBtn.addActionListener(e -> UIHelper.runWithProgress(owner, "A gerar talão do vale",
                () -> owner.posApiClient.renderVoucher(voucher.id()),
                pdf -> PrintPreviewDialog.show(owner, pdf, "vale-" + voucher.code()),
                err -> owner.showPosNotice(FeedbackType.ERROR, "Erro ao imprimir vale", err.getMessage())));

        btnRow.add(copyBtn);
        btnRow.add(printBtn);
        btnRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(titleLabel);
        card.add(Box.createRigidArea(new Dimension(0, 10)));
        card.add(codeField);
        card.add(Box.createRigidArea(new Dimension(0, 8)));
        card.add(valueLabel);
        card.add(Box.createRigidArea(new Dimension(0, 4)));
        card.add(expLabel);
        card.add(Box.createRigidArea(new Dimension(0, 12)));
        card.add(btnRow);

        new ModernFormDialog(UIHelper.mainWindow, "Vale de Compras", "fas-ticket-alt",
                "Comprovativo de crédito ao cliente", card)
                .setConfirmButton("Concluir", "fas-check")
                .showDialog();
    }
}
