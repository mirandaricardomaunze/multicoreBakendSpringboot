package mz.multicore.erp.gui.pos;

import com.fasterxml.jackson.databind.ObjectMapper;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.POSApiClient;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.pos.dto.CashDenominationDTO;
import mz.multicore.erp.modules.pos.dto.PosZReportDTO;
import mz.multicore.erp.modules.pos.dto.ShiftHandoverRequest;
import mz.multicore.erp.modules.pos.dto.ShiftReconciliationDTO;
import mz.multicore.erp.modules.pos.dto.TillSessionDTO;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dialogo modal para passagem de turno entre operadores. Permite contagem
 * cega da gaveta (notas/moedas MZN) e seleccao do operador seguinte,
 * registando uma reconciliacao parcial sem fechar a sessao.
 */
public class PosShiftHandoverDialog extends JDialog {

    private static final BigDecimal[] NOTE_DENOMINATIONS = {
            new BigDecimal("1000"), new BigDecimal("500"), new BigDecimal("200"),
            new BigDecimal("100"), new BigDecimal("50"), new BigDecimal("20")
    };

    private static final BigDecimal[] COIN_DENOMINATIONS = {
            new BigDecimal("10"), new BigDecimal("5"), new BigDecimal("2"),
            new BigDecimal("1"), new BigDecimal("0.50")
    };

    private final POSApiClient posApiClient;
    private final TillSessionDTO session;
    private final Runnable onSuccess;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<BigDecimal, JTextField> countFields = new LinkedHashMap<>();
    private final Map<BigDecimal, JLabel> subtotalLabels = new LinkedHashMap<>();

    private JRadioButton rbBreakdown;
    private JRadioButton rbDirectAmount;
    private JPanel breakdownCard;
    private JPanel directAmountCard;
    private JTextField directAmountField;

    private JLabel lblTotalCounted;
    private JLabel lblTotalNotes;
    private JLabel lblTotalCoins;
    private JTextArea notesArea;
    private JTextField incomingOperatorField;

    private JPanel resultCard;
    private JLabel lblExpected;
    private JLabel lblCounted;
    private JLabel lblDiff;

    private ModernButton btnConfirm;
    private ModernButton btnCancel;

    public PosShiftHandoverDialog(Window owner, POSApiClient posApiClient,
                                   TillSessionDTO session, Runnable onSuccess) {
        super(owner, "Passagem de Turno — Sessao #" + session.id(), ModalityType.APPLICATION_MODAL);
        this.posApiClient = posApiClient;
        this.session = session;
        this.onSuccess = onSuccess;

        setSize(720, 760);
        setMinimumSize(new Dimension(680, 680));
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createContentPanel(), BorderLayout.CENTER);
        add(createFooterPanel(), BorderLayout.SOUTH);

        recalculate();
    }

    // ── Header ──────────────────────────────────────────────────────────────

    private JPanel createHeaderPanel() {
        JPanel header = new ModernPanel(16);
        header.setLayout(new BorderLayout(12, 8));
        header.setBackground(UIHelper.SELECTION_BG);
        header.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JLabel icon = new JLabel(UIHelper.icon("fas-people-arrows", 28, UIHelper.ACCENT_CYAN));
        JLabel title = new JLabel("Passagem de Turno");
        title.setFont(new Font(UIHelper.FONT, Font.BOLD, 18));
        title.setForeground(Color.WHITE);

        String effectiveOp = session.currentOperator() != null
                ? session.currentOperator() : session.operator();
        JLabel subtitle = new JLabel("Operador actual: " + effectiveOp
                + " | Sessao #" + session.id());
        subtitle.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        subtitle.setForeground(UIHelper.TEXT_LIGHT);

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 2));
        textPanel.setOpaque(false);
        textPanel.add(title);
        textPanel.add(subtitle);

        header.add(icon, BorderLayout.WEST);
        header.add(textPanel, BorderLayout.CENTER);
        return header;
    }

    // ── Content ─────────────────────────────────────────────────────────────

    private JComponent createContentPanel() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(UIHelper.BG_DARK);
        content.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));

        // Operador seguinte
        JPanel operatorPanel = new ModernPanel(12);
        operatorPanel.setLayout(new BorderLayout(8, 4));
        operatorPanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        JLabel lblOp = new JLabel("Operador Seguinte (username):");
        lblOp.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        lblOp.setForeground(Color.WHITE);
        incomingOperatorField = new JTextField();
        UIHelper.styleTextField(incomingOperatorField);
        incomingOperatorField.setFont(new Font(UIHelper.FONT, Font.PLAIN, 14));
        incomingOperatorField.putClientProperty("JTextField.placeholderText", "Ex.: operador2");
        operatorPanel.add(lblOp, BorderLayout.NORTH);
        operatorPanel.add(incomingOperatorField, BorderLayout.CENTER);
        operatorPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        operatorPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        content.add(operatorPanel);
        content.add(Box.createVerticalStrut(8));

        // Modo de contagem
        rbBreakdown = new JRadioButton("Contagem discriminada (notas e moedas)");
        rbBreakdown.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        rbBreakdown.setForeground(Color.WHITE);
        rbBreakdown.setOpaque(false);
        rbBreakdown.setSelected(true);

        rbDirectAmount = new JRadioButton("Valor total directo");
        rbDirectAmount.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        rbDirectAmount.setForeground(Color.WHITE);
        rbDirectAmount.setOpaque(false);

        ButtonGroup bg = new ButtonGroup();
        bg.add(rbBreakdown);
        bg.add(rbDirectAmount);

        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        modePanel.setOpaque(false);
        modePanel.add(rbBreakdown);
        modePanel.add(rbDirectAmount);
        modePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(modePanel);
        content.add(Box.createVerticalStrut(8));

        // Breakdown card
        breakdownCard = createBreakdownPanel();
        breakdownCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(breakdownCard);

        // Direct amount card
        directAmountCard = createDirectAmountPanel();
        directAmountCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        directAmountCard.setVisible(false);
        content.add(directAmountCard);

        rbBreakdown.addActionListener(e -> {
            breakdownCard.setVisible(true);
            directAmountCard.setVisible(false);
            recalculate();
        });
        rbDirectAmount.addActionListener(e -> {
            breakdownCard.setVisible(false);
            directAmountCard.setVisible(true);
            recalculate();
        });

        // Total counted
        JPanel totalPanel = new ModernPanel(12);
        totalPanel.setLayout(new GridLayout(1, 3, 12, 0));
        totalPanel.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        lblTotalNotes = createMetricLabel("Notas: 0,00 MT");
        lblTotalCoins = createMetricLabel("Moedas: 0,00 MT");
        lblTotalCounted = createMetricLabel("TOTAL: 0,00 MT");
        lblTotalCounted.setFont(new Font(UIHelper.FONT, Font.BOLD, 16));
        totalPanel.add(lblTotalNotes);
        totalPanel.add(lblTotalCoins);
        totalPanel.add(lblTotalCounted);
        totalPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        totalPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        content.add(Box.createVerticalStrut(8));
        content.add(totalPanel);

        // Notes area
        JPanel notesPanel = new ModernPanel(12);
        notesPanel.setLayout(new BorderLayout(0, 4));
        notesPanel.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        JLabel notesLabel = new JLabel("Observacoes (opcional):");
        notesLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        notesLabel.setForeground(UIHelper.TEXT_LIGHT);
        notesArea = new JTextArea(2, 40);
        notesArea.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        notesArea.setBackground(UIHelper.ROW_ALT);
        notesArea.setForeground(Color.WHITE);
        notesArea.setCaretColor(Color.WHITE);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesPanel.add(notesLabel, BorderLayout.NORTH);
        notesPanel.add(new JScrollPane(notesArea), BorderLayout.CENTER);
        notesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        notesPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        content.add(Box.createVerticalStrut(4));
        content.add(notesPanel);

        // Result card (hidden until handover is done)
        resultCard = createResultCard();
        resultCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        resultCard.setVisible(false);
        content.add(Box.createVerticalStrut(8));
        content.add(resultCard);

        return new JScrollPane(content, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    }

    private JPanel createBreakdownPanel() {
        JPanel panel = new ModernPanel(12);
        panel.setLayout(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(2, 4, 2, 4);

        // Headers
        gbc.gridy = 0;
        addDenominationHeader(panel, gbc);

        int row = 1;
        for (BigDecimal d : NOTE_DENOMINATIONS) {
            addDenominationRow(panel, gbc, row++, d, true);
        }
        // Separator
        gbc.gridy = row++;
        gbc.gridx = 0;
        gbc.gridwidth = 4;
        JSeparator sep = new JSeparator();
        sep.setForeground(UIHelper.BORDER);
        panel.add(sep, gbc);
        gbc.gridwidth = 1;

        for (BigDecimal d : COIN_DENOMINATIONS) {
            addDenominationRow(panel, gbc, row++, d, false);
        }
        return panel;
    }

    private void addDenominationHeader(JPanel panel, GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.weightx = 0.3;
        panel.add(headerLabel("Denominacao"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.25;
        panel.add(headerLabel("Quantidade"), gbc);
        gbc.gridx = 2;
        gbc.weightx = 0.25;
        panel.add(headerLabel("Subtotal"), gbc);
    }

    private JLabel headerLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        lbl.setForeground(UIHelper.TEXT_LIGHT);
        return lbl;
    }

    private void addDenominationRow(JPanel panel, GridBagConstraints gbc, int row,
                                     BigDecimal denomination, boolean isNote) {
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = 0.3;
        String icon = isNote ? "fas-money-bill-wave" : "fas-coins";
        JLabel denomLabel = new JLabel(String.format("%,.2f MT", denomination));
        denomLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        denomLabel.setForeground(Color.WHITE);
        denomLabel.setIcon(UIHelper.icon(icon, 12,
                isNote ? UIHelper.APPROVED_GREEN : UIHelper.ACCENT_CYAN));
        panel.add(denomLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.25;
        JTextField countField = new JTextField("0", 5);
        UIHelper.styleTextField(countField);
        countField.setHorizontalAlignment(JTextField.CENTER);
        countField.getDocument().addDocumentListener(new SimpleDocListener(this::recalculate));
        countFields.put(denomination, countField);
        panel.add(countField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0.25;
        JLabel subtotal = new JLabel("0,00 MT");
        subtotal.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        subtotal.setForeground(UIHelper.TEXT_LIGHT);
        subtotalLabels.put(denomination, subtotal);
        panel.add(subtotal, gbc);
    }

    private JPanel createDirectAmountPanel() {
        JPanel panel = new ModernPanel(12);
        panel.setLayout(new BorderLayout(8, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        JLabel lbl = new JLabel("Total contado na gaveta (MT):");
        lbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        lbl.setForeground(Color.WHITE);
        directAmountField = new JTextField();
        UIHelper.styleTextField(directAmountField);
        directAmountField.setFont(new Font(UIHelper.FONT, Font.BOLD, 18));
        directAmountField.setHorizontalAlignment(JTextField.CENTER);
        directAmountField.getDocument().addDocumentListener(new SimpleDocListener(this::recalculate));
        panel.add(lbl, BorderLayout.NORTH);
        panel.add(directAmountField, BorderLayout.CENTER);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        return panel;
    }

    private JPanel createResultCard() {
        JPanel card = new ModernPanel(12);
        card.setLayout(new GridLayout(3, 1, 0, 4));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.ACCENT_CYAN, 1),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        lblExpected = createMetricLabel("Esperado: —");
        lblCounted = createMetricLabel("Contado: —");
        lblDiff = createMetricLabel("Diferenca: —");
        card.add(lblExpected);
        card.add(lblCounted);
        card.add(lblDiff);
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        return card;
    }

    private JLabel createMetricLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        lbl.setForeground(Color.WHITE);
        return lbl;
    }

    // ── Footer ──────────────────────────────────────────────────────────────

    private JPanel createFooterPanel() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        footer.setBackground(UIHelper.BG_DARK);

        btnCancel = UIHelper.createDangerButton("Cancelar");
        btnCancel.setIcon(UIHelper.icon("fas-times", 14));
        btnCancel.addActionListener(e -> dispose());

        btnConfirm = UIHelper.createSuccessButton("Confirmar Passagem");
        btnConfirm.setIcon(UIHelper.icon("fas-check", 14));
        btnConfirm.addActionListener(e -> performHandover());

        footer.add(btnCancel);
        footer.add(btnConfirm);
        return footer;
    }

    // ── Logic ───────────────────────────────────────────────────────────────

    private void recalculate() {
        if (rbDirectAmount != null && rbDirectAmount.isSelected()) {
            try {
                BigDecimal val = new BigDecimal(directAmountField.getText().trim().replace(",", "."));
                lblTotalCounted.setText(String.format("TOTAL: %,.2f MT", val));
                lblTotalNotes.setText("Notas: —");
                lblTotalCoins.setText("Moedas: —");
            } catch (NumberFormatException e) {
                lblTotalCounted.setText("TOTAL: —");
            }
            return;
        }

        BigDecimal totalNotes = BigDecimal.ZERO;
        BigDecimal totalCoins = BigDecimal.ZERO;

        for (Map.Entry<BigDecimal, JTextField> entry : countFields.entrySet()) {
            BigDecimal denomination = entry.getKey();
            int count = 0;
            try {
                count = Integer.parseInt(entry.getValue().getText().trim());
                if (count < 0) count = 0;
            } catch (NumberFormatException ignored) {}

            BigDecimal subtotal = denomination.multiply(BigDecimal.valueOf(count));
            subtotalLabels.get(denomination).setText(String.format("%,.2f MT", subtotal));

            boolean isNote = denomination.compareTo(new BigDecimal("20")) >= 0;
            if (isNote) {
                totalNotes = totalNotes.add(subtotal);
            } else {
                totalCoins = totalCoins.add(subtotal);
            }
        }

        BigDecimal total = totalNotes.add(totalCoins);
        lblTotalNotes.setText(String.format("Notas: %,.2f MT", totalNotes));
        lblTotalCoins.setText(String.format("Moedas: %,.2f MT", totalCoins));
        lblTotalCounted.setText(String.format("TOTAL: %,.2f MT", total));
    }

    private BigDecimal getCountedCash() {
        if (rbDirectAmount.isSelected()) {
            try {
                return new BigDecimal(directAmountField.getText().trim().replace(",", "."))
                        .setScale(2, RoundingMode.HALF_UP);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<BigDecimal, JTextField> entry : countFields.entrySet()) {
            int count = 0;
            try {
                count = Integer.parseInt(entry.getValue().getText().trim());
                if (count < 0) count = 0;
            } catch (NumberFormatException ignored) {}
            total = total.add(entry.getKey().multiply(BigDecimal.valueOf(count)));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private String buildCashBreakdownJson() {
        if (rbDirectAmount.isSelected()) return null;
        try {
            List<CashDenominationDTO> list = new ArrayList<>();
            for (Map.Entry<BigDecimal, JTextField> entry : countFields.entrySet()) {
                int count = 0;
                try {
                    count = Integer.parseInt(entry.getValue().getText().trim());
                } catch (NumberFormatException ignored) {}
                if (count > 0) {
                    list.add(new CashDenominationDTO(entry.getKey(), count,
                            entry.getKey().multiply(BigDecimal.valueOf(count))));
                }
            }
            return objectMapper.writeValueAsString(list);
        } catch (Exception e) {
            return null;
        }
    }

    private void performHandover() {
        String incomingOperator = incomingOperatorField.getText().trim();
        if (incomingOperator.isBlank()) {
            ToastManager.show(this, FeedbackType.WARNING, "Indique o username do operador seguinte.");
            incomingOperatorField.requestFocusInWindow();
            return;
        }

        BigDecimal countedCash = getCountedCash();
        if (countedCash == null || countedCash.compareTo(BigDecimal.ZERO) < 0) {
            ToastManager.show(this, FeedbackType.WARNING, "Introduza o valor total contado na gaveta.");
            return;
        }

        String effectiveOperator = session.currentOperator() != null
                ? session.currentOperator() : session.operator();

        ShiftHandoverRequest request = new ShiftHandoverRequest(
                session.id(),
                effectiveOperator,
                incomingOperator,
                countedCash,
                buildCashBreakdownJson(),
                notesArea.getText().trim().isEmpty() ? null : notesArea.getText().trim()
        );

        btnConfirm.setEnabled(false);
        btnConfirm.setText("A processar...");

        UIHelper.runWithProgress(this, "A registar passagem de turno",
                () -> posApiClient.performShiftHandover(session.id(), request),
                this::onHandoverSuccess,
                error -> {
                    btnConfirm.setEnabled(true);
                    btnConfirm.setText("Confirmar Passagem");
                    ToastManager.show(this, FeedbackType.ERROR, "Erro na passagem de turno: " + error.getMessage());
                });
    }

    private void onHandoverSuccess(ShiftReconciliationDTO result) {
        btnConfirm.setVisible(false);
        btnCancel.setText("Fechar");

        // Show result
        lblExpected.setText(String.format("Esperado: %,.2f MT", result.expectedCash()));
        lblCounted.setText(String.format("Contado: %,.2f MT", result.countedCash()));
        BigDecimal diff = result.difference();
        String diffColor = diff.compareTo(BigDecimal.ZERO) == 0 ? "exacto"
                : (diff.compareTo(BigDecimal.ZERO) > 0 ? "sobra" : "quebra");
        lblDiff.setText(String.format("Diferenca: %,.2f MT (%s)", diff, diffColor));
        if (diff.compareTo(BigDecimal.ZERO) != 0) {
            lblDiff.setForeground(diff.compareTo(BigDecimal.ZERO) > 0
                    ? UIHelper.PENDING_YELLOW : UIHelper.REJECTED_RED);
        } else {
            lblDiff.setForeground(UIHelper.APPROVED_GREEN);
        }
        resultCard.setVisible(true);

        ToastManager.success(this, "Passagem de turno concluida: "
                + result.outgoingOperator() + " -> " + result.incomingOperator());

        if (onSuccess != null) onSuccess.run();
    }

    // ── Utility ─────────────────────────────────────────────────────────────

    private static class SimpleDocListener implements DocumentListener {
        private final Runnable callback;
        SimpleDocListener(Runnable callback) { this.callback = callback; }
        @Override public void insertUpdate(DocumentEvent e) { callback.run(); }
        @Override public void removeUpdate(DocumentEvent e) { callback.run(); }
        @Override public void changedUpdate(DocumentEvent e) { callback.run(); }
    }
}
