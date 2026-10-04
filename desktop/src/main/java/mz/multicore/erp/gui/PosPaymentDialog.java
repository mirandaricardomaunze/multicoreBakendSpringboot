package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.pos.dto.PosPaymentRequest;
import mz.multicore.erp.modules.pos.dto.StoreVoucherDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Diálogo executivo de pagamento do checkout POS (Quick Tender).
 * Inclui botões temáticos para notas de Meticais (MT), comutação de métodos de pagamento
 * (Numerário, Cartão, Vale de Compras / Store Credit, M-Pesa, e-Mola, Transferência),
 * consulta reativa de saldos de vales e visor de troco de alto contraste.
 */
final class PosPaymentDialog {

    private PosPaymentDialog() {}

    static PosPaymentRequest show(BigDecimal total, Long accountId) {
        return show(total, accountId, null);
    }

    static List<PosPaymentRequest> showPayments(
            BigDecimal total, Long accountId, mz.multicore.erp.desktop.client.POSApiClient apiClient) {
        PosPaymentRequest req = show(total, accountId, apiClient);
        return req != null ? Collections.singletonList(req) : null;
    }

    static PosPaymentRequest show(
            BigDecimal total, Long accountId, mz.multicore.erp.desktop.client.POSApiClient apiClient) {
        BigDecimal safeTotal = total != null ? total.setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        String[] methodCodes = {"CASH", "CARD", "STORE_CREDIT", "MPESA", "EMOLA", "BANK_TRANSFER"};
        String[] methodNames = {"Numerário", "Cartão POS", "Vale Compras", "M-Pesa", "e-Mola", "Transferência"};
        String[] methodIcons = {"fas-money-bill-wave", "fas-credit-card", "fas-ticket-alt", "fas-mobile-alt", "fas-mobile-alt", "fas-university"};

        JComboBox<String> methodCombo = new JComboBox<>(methodNames);
        UIHelper.styleComboBox(methodCombo);

        JTextField totalField = new JTextField(String.format("%,.2f MT", safeTotal));
        UIHelper.styleTextField(totalField);
        totalField.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        totalField.setEditable(false);

        JTextField tenderedField = new JTextField(safeTotal.toPlainString());
        UIHelper.styleTextField(tenderedField);
        tenderedField.setFont(new Font(UIHelper.FONT, Font.BOLD, 15));

        JTextField refField = new JTextField();
        UIHelper.styleTextField(refField);
        refField.putClientProperty("JTextField.placeholderText", "Nº Comprovativo / ID Transacção");

        ModernButton pushUssdBtn = new ModernButton("Push USSD", UIHelper.ACCENT_ORANGE, UIHelper.ACCENT_ORANGE.darker());
        pushUssdBtn.setIcon(UIHelper.icon("fas-mobile-alt", 12, Color.WHITE));
        pushUssdBtn.setForeground(Color.WHITE);
        pushUssdBtn.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        pushUssdBtn.setPreferredSize(new Dimension(115, 32));
        pushUssdBtn.setVisible(false);

        ModernButton checkVoucherBtn = new ModernButton("Verificar Vale", UIHelper.ACCENT_CYAN, UIHelper.ACCENT_CYAN.darker());
        checkVoucherBtn.setIcon(UIHelper.icon("fas-check-circle", 12, Color.WHITE));
        checkVoucherBtn.setForeground(Color.WHITE);
        checkVoucherBtn.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        checkVoucherBtn.setPreferredSize(new Dimension(125, 32));
        checkVoucherBtn.setVisible(false);

        JPanel refActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        refActions.setOpaque(false);
        refActions.add(pushUssdBtn);
        refActions.add(checkVoucherBtn);

        JPanel refRow = new JPanel(new BorderLayout(6, 0));
        refRow.setOpaque(false);
        refRow.add(refField, BorderLayout.CENTER);
        refRow.add(refActions, BorderLayout.EAST);

        // Visor de Troco / Saldo em Destaque Executivo
        ModernPanel changeDisplay = new ModernPanel(12);
        changeDisplay.setBackground(UIHelper.ROW_ALT);
        changeDisplay.setBorder(new EmptyBorder(10, 16, 10, 16));
        changeDisplay.setLayout(new BorderLayout(8, 2));

        JLabel changeTitleLabel = new JLabel("TROCO A DEVOLVER");
        changeTitleLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        changeTitleLabel.setForeground(UIHelper.TEXT_MUTED);

        JLabel changeValueLabel = new JLabel("0,00 MT");
        changeValueLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 22));
        changeValueLabel.setForeground(UIHelper.APPROVED_GREEN);
        changeValueLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        changeDisplay.add(changeTitleLabel, BorderLayout.WEST);
        changeDisplay.add(changeValueLabel, BorderLayout.EAST);

        pushUssdBtn.addActionListener(e -> {
            int sel = methodCombo.getSelectedIndex();
            mz.multicore.erp.modules.pos.dto.MobilePaymentProvider provider = "EMOLA".equals(methodCodes[sel])
                    ? mz.multicore.erp.modules.pos.dto.MobilePaymentProvider.EMOLA
                    : mz.multicore.erp.modules.pos.dto.MobilePaymentProvider.MPESA;
            String approvedRef = MobilePaymentModal.show(changeDisplay, provider, safeTotal, apiClient);
            if (approvedRef != null && !approvedRef.trim().isEmpty()) {
                refField.setText(approvedRef);
                changeTitleLabel.setText("PAGAMENTO MÓVEL CONFIRMADO");
                changeTitleLabel.setForeground(UIHelper.APPROVED_GREEN);
                changeValueLabel.setText("Ref: " + approvedRef);
                changeValueLabel.setForeground(UIHelper.APPROVED_GREEN);
            }
        });

        checkVoucherBtn.addActionListener(e -> {
            String code = refField.getText().trim();
            if (code.isEmpty()) {
                changeTitleLabel.setText("CÓDIGO DO VALE OBRIGATÓRIO");
                changeTitleLabel.setForeground(UIHelper.PENDING_YELLOW);
                changeValueLabel.setText("—");
                return;
            }
            if (apiClient == null) return;
            Long companyId = CurrentUserContext.getCurrentCompanyId();
            Optional<StoreVoucherDTO> opt = apiClient.getVoucher(code, companyId);
            if (opt.isEmpty()) {
                changeTitleLabel.setText("VALE DE COMPRAS NÃO ENCONTRADO");
                changeTitleLabel.setForeground(UIHelper.REJECTED_RED);
                changeValueLabel.setText("Inválido");
                changeValueLabel.setForeground(UIHelper.REJECTED_RED);
                return;
            }
            StoreVoucherDTO v = opt.get();
            if (!v.isActive() || v.isExpired()) {
                changeTitleLabel.setText("VALE EXPIRADO OU JÁ UTILIZADO");
                changeTitleLabel.setForeground(UIHelper.REJECTED_RED);
                changeValueLabel.setText("0,00 MT");
                changeValueLabel.setForeground(UIHelper.REJECTED_RED);
                return;
            }
            if (v.remainingAmount().compareTo(safeTotal) < 0) {
                changeTitleLabel.setText("SALDO INSUFICIENTE NO VALE (" + v.clientName() + ")");
                changeTitleLabel.setForeground(UIHelper.PENDING_YELLOW);
                changeValueLabel.setText(String.format("%,.2f MT", v.remainingAmount()));
                changeValueLabel.setForeground(UIHelper.PENDING_YELLOW);
            } else {
                changeTitleLabel.setText("VALE VÁLIDO · " + v.clientName());
                changeTitleLabel.setForeground(UIHelper.APPROVED_GREEN);
                changeValueLabel.setText(String.format("Saldo: %,.2f MT", v.remainingAmount()));
                changeValueLabel.setForeground(UIHelper.APPROVED_GREEN);
            }
        });

        // Painel de Cédulas Nacionais Rápidas (Meticais)
        JPanel quickCashPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 4));
        quickCashPanel.setOpaque(false);
        addTenderButton(quickCashPanel, "Exacto", safeTotal, tenderedField);
        for (int denomination : new int[]{50, 100, 200, 500, 1000, 2000}) {
            addTenderButton(quickCashPanel, denomination + " MT",
                    BigDecimal.valueOf(denomination), tenderedField);
        }

        JLabel quickLabel = new JLabel("Cédulas rápidas (Meticais):");
        quickLabel.setForeground(UIHelper.TEXT_MUTED);
        quickLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        quickLabel.setBorder(new EmptyBorder(6, 4, 2, 4));

        JPanel quickCashSection = new JPanel();
        quickCashSection.setLayout(new BoxLayout(quickCashSection, BoxLayout.Y_AXIS));
        quickCashSection.setOpaque(false);
        quickLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        quickCashPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        quickCashSection.add(quickLabel);
        quickCashSection.add(quickCashPanel);

        // Seletor visual rápido de métodos em botões estilizados
        JPanel methodChipsPanel = new JPanel(new GridLayout(1, methodNames.length, 6, 0));
        methodChipsPanel.setOpaque(false);
        ModernButton[] methodButtons = new ModernButton[methodNames.length];
        for (int i = 0; i < methodNames.length; i++) {
            final int idx = i;
            ModernButton btn = new ModernButton(methodNames[i], UIHelper.BUTTON_NEUTRAL, UIHelper.BUTTON_NEUTRAL_HOVER);
            btn.setIcon(UIHelper.icon(methodIcons[i], 12, Color.WHITE));
            btn.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
            btn.setForeground(Color.WHITE);
            btn.setPreferredSize(new Dimension(0, 36));
            btn.addActionListener(e -> methodCombo.setSelectedIndex(idx));
            methodButtons[i] = btn;
            methodChipsPanel.add(btn);
        }

        // Lógica reativa de recálculo de troco e comutação de método
        Runnable recompute = () -> {
            int selectedMethod = methodCombo.getSelectedIndex();
            boolean isCash = selectedMethod == 0;
            boolean isVoucher = "STORE_CREDIT".equals(methodCodes[selectedMethod]);
            boolean isMobile = "MPESA".equals(methodCodes[selectedMethod]) || "EMOLA".equals(methodCodes[selectedMethod]);

            for (int i = 0; i < methodButtons.length; i++) {
                if (i == selectedMethod) {
                    methodButtons[i].setBackground(UIHelper.ACCENT_BLUE);
                    methodButtons[i].setForeground(Color.WHITE);
                } else {
                    methodButtons[i].setBackground(UIHelper.BUTTON_NEUTRAL);
                    methodButtons[i].setForeground(Color.WHITE);
                }
            }

            tenderedField.setEnabled(isCash);
            quickCashSection.setVisible(isCash);
            refField.setEnabled(!isCash);

            pushUssdBtn.setVisible(isMobile && apiClient != null);
            if (isMobile) {
                pushUssdBtn.setText("EMOLA".equals(methodCodes[selectedMethod]) ? "Push e-Mola" : "Push M-Pesa");
            }

            checkVoucherBtn.setVisible(isVoucher && apiClient != null);
            if (isVoucher) {
                refField.putClientProperty("JTextField.placeholderText", "Código do Vale (ex.: VALE-XXXX-XXXX)");
            } else if (isMobile) {
                refField.putClientProperty("JTextField.placeholderText", "Nº Telemóvel ou ID Transacção");
            } else {
                refField.putClientProperty("JTextField.placeholderText", "Nº Comprovativo / ID Transacção");
            }

            if (!isCash) {
                if (isVoucher) {
                    changeTitleLabel.setText("VALE DE COMPRAS");
                    changeValueLabel.setText("Insira e valide o código");
                    changeValueLabel.setForeground(UIHelper.TEXT_MUTED);
                } else if (!isMobile || refField.getText().trim().isEmpty()) {
                    changeTitleLabel.setText("MÉTODO ELECTRÓNICO");
                    changeValueLabel.setText("Troco: —");
                    changeValueLabel.setForeground(UIHelper.TEXT_MUTED);
                }
                return;
            }

            try {
                String raw = tenderedField.getText().trim().replace(",", ".");
                BigDecimal tendered = raw.isEmpty() ? BigDecimal.ZERO : new BigDecimal(raw);
                BigDecimal diff = tendered.subtract(safeTotal);
                if (diff.compareTo(BigDecimal.ZERO) < 0) {
                    changeTitleLabel.setText("VALOR EM FALTA");
                    changeTitleLabel.setForeground(UIHelper.PENDING_YELLOW);
                    changeValueLabel.setForeground(UIHelper.PENDING_YELLOW);
                    changeValueLabel.setText(String.format("-%s MT", String.format("%,.2f", diff.abs())));
                } else {
                    changeTitleLabel.setText("TROCO A DEVOLVER");
                    changeTitleLabel.setForeground(UIHelper.TEXT_MUTED);
                    changeValueLabel.setForeground(UIHelper.APPROVED_GREEN);
                    changeValueLabel.setText(String.format("%,.2f MT", diff));
                }
            } catch (NumberFormatException ex) {
                changeTitleLabel.setText("VALOR INVÁLIDO");
                changeTitleLabel.setForeground(UIHelper.PENDING_YELLOW);
                changeValueLabel.setText("—");
            }
        };

        methodCombo.addActionListener(e -> recompute.run());
        UIHelper.onTextChange(tenderedField, recompute);
        recompute.run();

        JPanel form = UIHelper.createDialogForm(
                "Método de pagamento:", methodChipsPanel,
                "Total da venda:", totalField,
                "Valor entregue pelo cliente:", tenderedField,
                "Referência / Código:", refRow
        );

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        panel.add(form, BorderLayout.NORTH);

        JPanel centerFooter = new JPanel();
        centerFooter.setLayout(new BoxLayout(centerFooter, BoxLayout.Y_AXIS));
        centerFooter.setOpaque(false);
        centerFooter.add(quickCashSection);
        centerFooter.add(Box.createRigidArea(new Dimension(0, 8)));
        centerFooter.add(changeDisplay);
        panel.add(centerFooter, BorderLayout.CENTER);

        PosPaymentRequest[] result = {null};
        boolean ok = new ModernFormDialog(UIHelper.mainWindow, "Pagamento POS", "fas-cash-register",
                "Receba o pagamento do cliente e confira o troco", panel)
                .setConfirmButton("Confirmar Pagamento", "fas-check")
                .setOnSave(() -> {
                    int methodIdx = methodCombo.getSelectedIndex();
                    if (methodIdx == 0) {
                        BigDecimal tendered;
                        try {
                            tendered = new BigDecimal(tenderedField.getText().trim().replace(",", "."));
                        } catch (NumberFormatException ex) {
                            throw new IllegalArgumentException("Valor entregue inválido.");
                        }
                        if (tendered.compareTo(safeTotal) < 0) {
                            throw new IllegalArgumentException("O valor entregue é inferior ao total a pagar.");
                        }
                        result[0] = new PosPaymentRequest(
                                "CASH", safeTotal, tendered, null, null);
                    } else {
                        String method = methodCodes[methodIdx];
                        String ref = refField.getText().trim();
                        if ("STORE_CREDIT".equals(method)) {
                            if (ref.isEmpty()) {
                                throw new IllegalArgumentException("Indique o código do vale de compras.");
                            }
                            if (apiClient != null) {
                                Long companyId = CurrentUserContext.getCurrentCompanyId();
                                Optional<StoreVoucherDTO> opt = apiClient.getVoucher(ref, companyId);
                                if (opt.isEmpty()) {
                                    throw new IllegalArgumentException("Vale de compras não encontrado: " + ref);
                                }
                                StoreVoucherDTO v = opt.get();
                                if (!v.isActive() || v.isExpired()) {
                                    throw new IllegalArgumentException("O vale " + ref + " está expirado ou já foi utilizado.");
                                }
                                if (v.remainingAmount().compareTo(safeTotal) < 0) {
                                    throw new IllegalArgumentException(String.format(
                                            "Saldo do vale (%,.2f MT) é insuficiente para cobrir o total (%,.2f MT).",
                                            v.remainingAmount(), safeTotal));
                                }
                            }
                            result[0] = new PosPaymentRequest(
                                    "STORE_CREDIT", safeTotal, safeTotal, ref.toUpperCase(), null);
                        } else {
                            result[0] = new PosPaymentRequest(
                                    method, safeTotal, safeTotal, ref.isEmpty() ? null : ref, accountId);
                        }
                    }
                })
                .showDialog();

        return ok ? result[0] : null;
    }

    private static void addTenderButton(JPanel parent, String label, BigDecimal value, JTextField target) {
        boolean isExact = "Exacto".equals(label);
        Color base = isExact ? UIHelper.ACCENT_BLUE : UIHelper.APPROVED_GREEN;
        Color hover = isExact ? UIHelper.ACCENT_BLUE_HOVER : UIHelper.APPROVED_GREEN_HOVER;
        ModernButton button = new ModernButton(label, base, hover);
        button.setForeground(Color.WHITE);
        button.setPreferredSize(new Dimension(isExact ? 80 : 66, 32));
        button.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        button.addActionListener(e -> target.setText(value.stripTrailingZeros().toPlainString()));
        parent.add(button);
    }
}
