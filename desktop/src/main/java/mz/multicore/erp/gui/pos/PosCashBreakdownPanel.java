package mz.multicore.erp.gui.pos;

import com.fasterxml.jackson.databind.ObjectMapper;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.pos.dto.CashDenominationDTO;

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
 * Painel de contagem física de caixa (por cédulas e moedas ou montante global).
 */
final class PosCashBreakdownPanel {

    private static final BigDecimal[] NOTE_DENOMINATIONS = {
            new BigDecimal("1000"), new BigDecimal("500"), new BigDecimal("200"),
            new BigDecimal("100"), new BigDecimal("50"), new BigDecimal("20")
    };

    private static final BigDecimal[] COIN_DENOMINATIONS = {
            new BigDecimal("10"), new BigDecimal("5"), new BigDecimal("2"),
            new BigDecimal("1"), new BigDecimal("0.50")
    };

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

    PosCashBreakdownPanel() {
        breakdownCard = createDenominationsCard();
        directAmountCard = createDirectAmountCard();
        directAmountCard.setVisible(false);
        recalculate();
    }

    void addTo(JPanel container) {
        container.add(createModeSelectorPanel());
        container.add(Box.createVerticalStrut(12));
        container.add(breakdownCard);
        container.add(directAmountCard);
        container.add(Box.createVerticalStrut(12));
        container.add(createTotalSummaryCard());
    }

    private JPanel createModeSelectorPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 2)); panel.setOpaque(false); panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lblMode = new JLabel("Método de Contagem:"); lblMode.setFont(new Font(UIHelper.FONT, Font.BOLD, 12)); lblMode.setForeground(UIHelper.TEXT_LIGHT);
        rbBreakdown = new JRadioButton("Contagem Física por Notas e Moedas (Recomendado)", true);
        rbBreakdown.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12)); rbBreakdown.setForeground(UIHelper.TEXT_LIGHT); rbBreakdown.setOpaque(false);
        rbDirectAmount = new JRadioButton("Introdução Direta de Montante Global");
        rbDirectAmount.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12)); rbDirectAmount.setForeground(UIHelper.TEXT_LIGHT); rbDirectAmount.setOpaque(false);
        ButtonGroup group = new ButtonGroup(); group.add(rbBreakdown); group.add(rbDirectAmount);
        rbBreakdown.addActionListener(e -> switchMode(true)); rbDirectAmount.addActionListener(e -> switchMode(false));
        panel.add(lblMode); panel.add(rbBreakdown); panel.add(rbDirectAmount);
        return panel;
    }

    private void switchMode(boolean isBreakdown) {
        breakdownCard.setVisible(isBreakdown); directAmountCard.setVisible(!isBreakdown); recalculate();
        if (breakdownCard.getParent() != null) { breakdownCard.getParent().revalidate(); breakdownCard.getParent().repaint(); }
    }

    private JPanel createDenominationsCard() {
        ModernPanel card = new ModernPanel(14); card.setLayout(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14)); card.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel header = new JLabel("Contagem Física de Cédulas e Moedas da Gaveta");
        header.setFont(new Font(UIHelper.FONT, Font.BOLD, 13)); header.setForeground(UIHelper.TEXT_LIGHT);
        header.setIcon(UIHelper.icon("fas-coins", 14, UIHelper.PENDING_YELLOW));
        card.add(header, BorderLayout.NORTH);

        JPanel gridPanel = new JPanel(new GridLayout(1, 2, 15, 0)); gridPanel.setOpaque(false);
        JPanel notesCol = new JPanel(); notesCol.setLayout(new BoxLayout(notesCol, BoxLayout.Y_AXIS)); notesCol.setOpaque(false);
        JLabel notesTitle = new JLabel("Notas (Cédulas MZN)"); notesTitle.setFont(new Font(UIHelper.FONT, Font.BOLD, 12)); notesTitle.setForeground(UIHelper.APPROVED_GREEN);
        notesTitle.setIcon(UIHelper.icon("fas-money-bill-wave", 13, UIHelper.APPROVED_GREEN));
        notesCol.add(notesTitle); notesCol.add(Box.createVerticalStrut(8));
        for (BigDecimal denom : NOTE_DENOMINATIONS) { notesCol.add(createDenominationRow(denom, true)); notesCol.add(Box.createVerticalStrut(5)); }

        JPanel coinsCol = new JPanel(); coinsCol.setLayout(new BoxLayout(coinsCol, BoxLayout.Y_AXIS)); coinsCol.setOpaque(false);
        JLabel coinsTitle = new JLabel("Moedas Metálicas"); coinsTitle.setFont(new Font(UIHelper.FONT, Font.BOLD, 12)); coinsTitle.setForeground(UIHelper.PENDING_YELLOW);
        coinsTitle.setIcon(UIHelper.icon("fas-coins", 13, UIHelper.PENDING_YELLOW));
        coinsCol.add(coinsTitle); coinsCol.add(Box.createVerticalStrut(8));
        for (BigDecimal denom : COIN_DENOMINATIONS) { coinsCol.add(createDenominationRow(denom, false)); coinsCol.add(Box.createVerticalStrut(5)); }

        gridPanel.add(notesCol); gridPanel.add(coinsCol);
        card.add(gridPanel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createDenominationRow(BigDecimal denom, boolean isNote) {
        JPanel row = new JPanel(new BorderLayout(8, 0)); row.setBackground(UIHelper.ROW_ALT);
        row.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UIHelper.BORDER, 1), BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        row.setMaximumSize(new Dimension(Short.MAX_VALUE, 38));

        String labelText = denom.compareTo(BigDecimal.ONE) >= 0 ? String.format("%,.0f MT", denom) : String.format("%,.2f MT", denom);
        JLabel lblDenom = new JLabel(labelText); lblDenom.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        lblDenom.setForeground(isNote ? UIHelper.TEXT_LIGHT : UIHelper.PENDING_YELLOW); lblDenom.setPreferredSize(new Dimension(65, 26));

        JTextField countField = new JTextField("0", 4); countField.setHorizontalAlignment(JTextField.CENTER);
        UIHelper.styleTextField(countField); countField.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        countField.setPreferredSize(new Dimension(50, 26));
        countField.getDocument().addDocumentListener(createDocListener(this::recalculate));
        countFields.put(denom, countField);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0)); buttonsPanel.setOpaque(false);
        buttonsPanel.add(createMiniButton("+1", () -> increment(denom, 1)));
        buttonsPanel.add(createMiniButton("+5", () -> increment(denom, 5)));

        JLabel lblSubtotal = new JLabel("0,00 MT"); lblSubtotal.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        lblSubtotal.setForeground(UIHelper.TEXT_MUTED); lblSubtotal.setHorizontalAlignment(SwingConstants.RIGHT);
        lblSubtotal.setPreferredSize(new Dimension(75, 26));
        subtotalLabels.put(denom, lblSubtotal);

        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0)); centerPanel.setOpaque(false);
        centerPanel.add(countField); centerPanel.add(buttonsPanel);

        row.add(lblDenom, BorderLayout.WEST); row.add(centerPanel, BorderLayout.CENTER); row.add(lblSubtotal, BorderLayout.EAST);
        return row;
    }

    private JButton createMiniButton(String text, Runnable action) {
        JButton btn = new JButton(text); btn.setFont(new Font(UIHelper.FONT, Font.BOLD, 10));
        btn.setBackground(UIHelper.BORDER); btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false); btn.setMargin(new Insets(1, 4, 1, 4));
        btn.setPreferredSize(new Dimension(28, 22)); btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> action.run());
        return btn;
    }

    private void increment(BigDecimal denom, int delta) {
        int current = parseCount(countFields.get(denom));
        countFields.get(denom).setText(String.valueOf(Math.max(0, current + delta)));
    }

    private int parseCount(JTextField f) {
        if (f == null) return 0;
        try {
            String txt = f.getText().trim();
            return txt.isEmpty() ? 0 : Integer.parseInt(txt);
        } catch (NumberFormatException ignored) { return 0; }
    }

    private JPanel createDirectAmountCard() {
        ModernPanel card = new ModernPanel(14); card.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 12));
        card.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15)); card.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lbl = new JLabel("Montante Total Contado (MT):"); lbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 13)); lbl.setForeground(UIHelper.TEXT_LIGHT);
        directAmountField = new JTextField(12); UIHelper.styleTextField(directAmountField);
        directAmountField.setFont(new Font(UIHelper.FONT, Font.BOLD, 15));
        directAmountField.getDocument().addDocumentListener(createDocListener(this::recalculate));
        card.add(lbl); card.add(directAmountField);
        return card;
    }

    private JPanel createTotalSummaryCard() {
        ModernPanel card = new ModernPanel(14); card.setLayout(new BorderLayout(15, 0)); card.setBackground(UIHelper.ROW_ALT);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UIHelper.ACCENT_BLUE, 1), BorderFactory.createEmptyBorder(10, 16, 10, 16)));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel left = new JPanel(); left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS)); left.setOpaque(false);
        JLabel title = new JLabel("TOTAL FÍSICO CONTADO NA GAVETA");
        title.setFont(new Font(UIHelper.FONT, Font.BOLD, 11)); title.setForeground(UIHelper.TEXT_MUTED);

        lblTotalCounted = new JLabel("0,00 MT");
        lblTotalCounted.setFont(new Font(UIHelper.FONT, Font.BOLD, 22)); lblTotalCounted.setForeground(UIHelper.ACCENT_CYAN);
        left.add(title); left.add(lblTotalCounted);

        JPanel right = new JPanel(new GridLayout(2, 1, 2, 2)); right.setOpaque(false);
        lblTotalNotes = new JLabel("Notas: 0,00 MT"); lblTotalNotes.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12)); lblTotalNotes.setForeground(UIHelper.TEXT_LIGHT);
        lblTotalCoins = new JLabel("Moedas: 0,00 MT"); lblTotalCoins.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12)); lblTotalCoins.setForeground(UIHelper.TEXT_LIGHT);
        right.add(lblTotalNotes); right.add(lblTotalCoins);

        card.add(left, BorderLayout.WEST); card.add(right, BorderLayout.EAST);
        return card;
    }

    void recalculate() {
        if (lblTotalCounted == null) return;
        if (rbDirectAmount != null && rbDirectAmount.isSelected()) {
            String txt = directAmountField != null ? directAmountField.getText().trim().replace(",", ".") : "";
            BigDecimal amount = BigDecimal.ZERO;
            if (!txt.isEmpty()) {
                try { amount = new BigDecimal(txt).setScale(2, RoundingMode.HALF_UP); } catch (Exception ignored) {}
            }
            lblTotalCounted.setText(String.format("%,.2f MT", amount));
            lblTotalNotes.setText("Notas: —");
            lblTotalCoins.setText("Moedas: —");
            return;
        }

        BigDecimal totalNotes = BigDecimal.ZERO, totalCoins = BigDecimal.ZERO;
        for (Map.Entry<BigDecimal, JTextField> entry : countFields.entrySet()) {
            BigDecimal denom = entry.getKey();
            int count = parseCount(entry.getValue());
            BigDecimal subtotal = denom.multiply(BigDecimal.valueOf(count));
            JLabel subLabel = subtotalLabels.get(denom);
            if (subLabel != null) {
                subLabel.setText(String.format("%,.2f MT", subtotal));
                subLabel.setForeground(count > 0 ? UIHelper.TEXT_LIGHT : UIHelper.TEXT_MUTED);
            }
            if (denom.compareTo(BigDecimal.valueOf(20)) >= 0) totalNotes = totalNotes.add(subtotal);
            else totalCoins = totalCoins.add(subtotal);
        }
        BigDecimal grandTotal = totalNotes.add(totalCoins);
        lblTotalCounted.setText(String.format("%,.2f MT", grandTotal));
        lblTotalNotes.setText(String.format("Notas: %,.2f MT", totalNotes));
        lblTotalCoins.setText(String.format("Moedas: %,.2f MT", totalCoins));
    }

    BigDecimal calculateCountedAmount() {
        if (rbDirectAmount != null && rbDirectAmount.isSelected()) {
            String txt = directAmountField != null ? directAmountField.getText().trim().replace(",", ".") : "";
            if (txt.isEmpty()) return BigDecimal.ZERO;
            return new BigDecimal(txt).setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<BigDecimal, JTextField> entry : countFields.entrySet()) {
            total = total.add(entry.getKey().multiply(BigDecimal.valueOf(parseCount(entry.getValue()))));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    String buildDenominationsJson() {
        if (rbDirectAmount != null && rbDirectAmount.isSelected()) return null;
        List<CashDenominationDTO> list = new ArrayList<>();
        for (Map.Entry<BigDecimal, JTextField> entry : countFields.entrySet()) {
            int count = parseCount(entry.getValue());
            if (count > 0) list.add(new CashDenominationDTO(entry.getKey(), count));
        }
        if (list.isEmpty()) return null;
        try { return objectMapper.writeValueAsString(list); } catch (Exception ignored) { return null; }
    }

    void setControlsEnabled(boolean enabled) {
        if (rbBreakdown != null) rbBreakdown.setEnabled(enabled);
        if (rbDirectAmount != null) rbDirectAmount.setEnabled(enabled);
        if (directAmountField != null) directAmountField.setEnabled(enabled);
        for (JTextField f : countFields.values()) f.setEnabled(enabled);
    }

    private DocumentListener createDocListener(Runnable action) {
        return new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { action.run(); }
            @Override public void removeUpdate(DocumentEvent e) { action.run(); }
            @Override public void changedUpdate(DocumentEvent e) { action.run(); }
        };
    }
}
