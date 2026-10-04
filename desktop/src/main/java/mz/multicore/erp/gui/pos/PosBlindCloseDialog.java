package mz.multicore.erp.gui.pos;

import com.fasterxml.jackson.databind.ObjectMapper;
import mz.multicore.erp.desktop.client.POSApiClient;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.PrintPreviewDialog;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.pos.dto.CashDenominationDTO;
import mz.multicore.erp.modules.pos.dto.PosZReportDTO;
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
 * Diálogo modal para fecho cego de caixa (Blind Drop), contagem física discriminada
 * por notas e moedas de Meticais (MZN), apuramento de divergência (quebra/sobra)
 * e emissão do Relatório Z.
 */
public class PosBlindCloseDialog extends JDialog {

    private static final BigDecimal[] NOTE_DENOMINATIONS = {
            new BigDecimal("1000"),
            new BigDecimal("500"),
            new BigDecimal("200"),
            new BigDecimal("100"),
            new BigDecimal("50"),
            new BigDecimal("20")
    };

    private static final BigDecimal[] COIN_DENOMINATIONS = {
            new BigDecimal("10"),
            new BigDecimal("5"),
            new BigDecimal("2"),
            new BigDecimal("1"),
            new BigDecimal("0.50")
    };

    private final POSApiClient posApiClient;
    private final TillSessionDTO session;
    private final Runnable onCloseSuccess;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Denominações e campos de contagem
    private final Map<BigDecimal, JTextField> countFields = new LinkedHashMap<>();
    private final Map<BigDecimal, JLabel> subtotalLabels = new LinkedHashMap<>();

    // Modo alternativo (introdução direta)
    private JRadioButton rbBreakdown;
    private JRadioButton rbDirectAmount;
    private JPanel breakdownCard;
    private JPanel directAmountCard;
    private JTextField directAmountField;

    // Totais e Observações
    private JLabel lblTotalCounted;
    private JLabel lblTotalNotes;
    private JLabel lblTotalCoins;
    private JTextArea notesArea;

    // Painel de resultado pós-fecho
    private JPanel resultCard;
    private JLabel lblStatusResult;
    private JLabel lblExpected;
    private JLabel lblCounted;
    private JLabel lblDiff;
    private JLabel lblAuditedNotes;

    // Botões de ação
    private ModernButton btnConfirm;
    private ModernButton btnPrintZ;
    private ModernButton btnCancel;

    public PosBlindCloseDialog(Window owner, POSApiClient posApiClient, TillSessionDTO session, Runnable onCloseSuccess) {
        super(owner, "Fecho Cego de Caixa — Sessão #" + session.id(), ModalityType.APPLICATION_MODAL);
        this.posApiClient = posApiClient;
        this.session = session;
        this.onCloseSuccess = onCloseSuccess;

        setSize(720, 760);
        setMinimumSize(new Dimension(680, 680));
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createContentPanel(), BorderLayout.CENTER);
        add(createFooterPanel(), BorderLayout.SOUTH);

        recalculateBreakdown();
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UIHelper.ROW_ALT);
        panel.setBorder(BorderFactory.createEmptyBorder(14, 20, 14, 20));

        JLabel title = new JLabel("Fecho Cego de Caixa (Blind Drop)");
        title.setFont(new Font(UIHelper.FONT, Font.BOLD, 17));
        title.setForeground(UIHelper.TEXT_LIGHT);
        title.setIcon(UIHelper.icon("fas-cash-register", 18, UIHelper.ACCENT_CYAN));
        title.setIconTextGap(10);

        String subtitleText = String.format("Operador: %s  |  Sessão #%d  |  Abertura: %,.2f MT",
                session.operator() != null ? session.operator() : "—",
                session.id(),
                session.openingBalance() != null ? session.openingBalance() : BigDecimal.ZERO);
        JLabel subtitle = new JLabel(subtitleText);
        subtitle.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        subtitle.setForeground(UIHelper.TEXT_MUTED);
        subtitle.setBorder(BorderFactory.createEmptyBorder(4, 28, 0, 0));

        panel.add(title, BorderLayout.NORTH);
        panel.add(subtitle, BorderLayout.SOUTH);
        return panel;
    }

    private JComponent createContentPanel() {
        JPanel root = new JPanel();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
        root.setBackground(UIHelper.BG_DARK);
        root.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        // 1. Selector de modo (Contagem detalhada vs Valor Global)
        root.add(createModeSelectorPanel());
        root.add(Box.createVerticalStrut(12));

        // 2. Card de contagem por notas e moedas
        breakdownCard = createDenominationsCard();
        root.add(breakdownCard);

        // 3. Card de montante direto (alternativo)
        directAmountCard = createDirectAmountCard();
        directAmountCard.setVisible(false);
        root.add(directAmountCard);

        root.add(Box.createVerticalStrut(12));

        // 4. Card resumo do Total Físico da Gaveta
        root.add(createTotalSummaryCard());
        root.add(Box.createVerticalStrut(12));

        // 5. Observações / Justificação
        root.add(createNotesCard());
        root.add(Box.createVerticalStrut(12));

        // 6. Painel de reconciliação oficial (revelado após o fecho)
        resultCard = createResultCard();
        resultCard.setVisible(false);
        root.add(resultCard);

        JScrollPane scroll = new JScrollPane(root);
        UIHelper.styleScrollPane(scroll);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        return scroll;
    }

    private JPanel createModeSelectorPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 2));
        panel.setOpaque(false);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblMode = new JLabel("Método de Contagem:");
        lblMode.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        lblMode.setForeground(UIHelper.TEXT_LIGHT);

        rbBreakdown = new JRadioButton("Contagem Física por Notas e Moedas (Recomendado)", true);
        rbBreakdown.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        rbBreakdown.setForeground(UIHelper.TEXT_LIGHT);
        rbBreakdown.setOpaque(false);

        rbDirectAmount = new JRadioButton("Introdução Direta de Montante Global");
        rbDirectAmount.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        rbDirectAmount.setForeground(UIHelper.TEXT_LIGHT);
        rbDirectAmount.setOpaque(false);

        ButtonGroup group = new ButtonGroup();
        group.add(rbBreakdown);
        group.add(rbDirectAmount);

        rbBreakdown.addActionListener(e -> switchMode(true));
        rbDirectAmount.addActionListener(e -> switchMode(false));

        panel.add(lblMode);
        panel.add(rbBreakdown);
        panel.add(rbDirectAmount);
        return panel;
    }

    private void switchMode(boolean isBreakdown) {
        breakdownCard.setVisible(isBreakdown);
        directAmountCard.setVisible(!isBreakdown);
        if (isBreakdown) {
            recalculateBreakdown();
        } else {
            recalculateDirectAmount();
        }
        revalidate();
        repaint();
    }

    private JPanel createDenominationsCard() {
        ModernPanel card = new ModernPanel(14);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel header = new JLabel("Contagem Física de Cédulas e Moedas da Gaveta");
        header.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        header.setForeground(UIHelper.TEXT_LIGHT);
        header.setIcon(UIHelper.icon("fas-coins", 14, UIHelper.PENDING_YELLOW));
        header.setIconTextGap(8);
        card.add(header, BorderLayout.NORTH);

        JPanel gridPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        gridPanel.setOpaque(false);

        // Coluna de Notas
        JPanel notesCol = new JPanel();
        notesCol.setLayout(new BoxLayout(notesCol, BoxLayout.Y_AXIS));
        notesCol.setOpaque(false);

        JLabel notesTitle = new JLabel("Notas (Cédulas MZN)");
        notesTitle.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        notesTitle.setForeground(UIHelper.APPROVED_GREEN);
        notesTitle.setIcon(UIHelper.icon("fas-money-bill-wave", 13, UIHelper.APPROVED_GREEN));
        notesTitle.setIconTextGap(6);
        notesCol.add(notesTitle);
        notesCol.add(Box.createVerticalStrut(8));

        for (BigDecimal denom : NOTE_DENOMINATIONS) {
            notesCol.add(createDenominationRow(denom, true));
            notesCol.add(Box.createVerticalStrut(5));
        }

        // Coluna de Moedas
        JPanel coinsCol = new JPanel();
        coinsCol.setLayout(new BoxLayout(coinsCol, BoxLayout.Y_AXIS));
        coinsCol.setOpaque(false);

        JLabel coinsTitle = new JLabel("Moedas Metálicas");
        coinsTitle.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        coinsTitle.setForeground(UIHelper.PENDING_YELLOW);
        coinsTitle.setIcon(UIHelper.icon("fas-coins", 13, UIHelper.PENDING_YELLOW));
        coinsTitle.setIconTextGap(6);
        coinsCol.add(coinsTitle);
        coinsCol.add(Box.createVerticalStrut(8));

        for (BigDecimal denom : COIN_DENOMINATIONS) {
            coinsCol.add(createDenominationRow(denom, false));
            coinsCol.add(Box.createVerticalStrut(5));
        }

        gridPanel.add(notesCol);
        gridPanel.add(coinsCol);
        card.add(gridPanel, BorderLayout.CENTER);
        return card;
    }

    private JPanel createDenominationRow(BigDecimal denom, boolean isNote) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setBackground(UIHelper.ROW_ALT);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.BORDER, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        row.setMaximumSize(new Dimension(Short.MAX_VALUE, 38));

        // Rótulo da denominação
        String labelText = denom.compareTo(BigDecimal.ONE) >= 0
                ? String.format("%,.0f MT", denom)
                : String.format("%,.2f MT", denom);
        JLabel lblDenom = new JLabel(labelText);
        lblDenom.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        lblDenom.setForeground(isNote ? UIHelper.TEXT_LIGHT : UIHelper.PENDING_YELLOW);
        lblDenom.setPreferredSize(new Dimension(65, 26));

        // Campo numérico de quantidade
        JTextField countField = new JTextField("0", 4);
        countField.setHorizontalAlignment(JTextField.CENTER);
        UIHelper.styleTextField(countField);
        countField.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        countField.setPreferredSize(new Dimension(50, 26));

        countField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { recalculateBreakdown(); }
            @Override public void removeUpdate(DocumentEvent e) { recalculateBreakdown(); }
            @Override public void changedUpdate(DocumentEvent e) { recalculateBreakdown(); }
        });
        countFields.put(denom, countField);

        // Botões de incremento rápido (+1, +5)
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        buttonsPanel.setOpaque(false);

        JButton btnPlus1 = createMiniButton("+1", () -> incrementDenomination(denom, 1));
        JButton btnPlus5 = createMiniButton("+5", () -> incrementDenomination(denom, 5));
        buttonsPanel.add(btnPlus1);
        buttonsPanel.add(btnPlus5);

        // Subtotal da denominação
        JLabel lblSubtotal = new JLabel("0.00 MT");
        lblSubtotal.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        lblSubtotal.setForeground(UIHelper.TEXT_MUTED);
        lblSubtotal.setHorizontalAlignment(SwingConstants.RIGHT);
        lblSubtotal.setPreferredSize(new Dimension(75, 26));
        subtotalLabels.put(denom, lblSubtotal);

        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        centerPanel.setOpaque(false);
        centerPanel.add(countField);
        centerPanel.add(buttonsPanel);

        row.add(lblDenom, BorderLayout.WEST);
        row.add(centerPanel, BorderLayout.CENTER);
        row.add(lblSubtotal, BorderLayout.EAST);
        return row;
    }

    private JButton createMiniButton(String text, Runnable action) {
        JButton btn = new JButton(text);
        btn.setFont(new Font(UIHelper.FONT, Font.BOLD, 10));
        btn.setBackground(UIHelper.BORDER);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setMargin(new java.awt.Insets(1, 4, 1, 4));
        btn.setPreferredSize(new Dimension(28, 22));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> action.run());
        return btn;
    }

    private void incrementDenomination(BigDecimal denom, int delta) {
        JTextField f = countFields.get(denom);
        if (f == null) return;
        int current = 0;
        try {
            String txt = f.getText().trim();
            if (!txt.isEmpty()) current = Integer.parseInt(txt);
        } catch (NumberFormatException ignored) {}
        f.setText(String.valueOf(Math.max(0, current + delta)));
    }

    private JPanel createDirectAmountCard() {
        ModernPanel card = new ModernPanel(14);
        card.setLayout(new FlowLayout(FlowLayout.LEFT, 15, 12));
        card.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel("Montante Total Contado (MT):");
        lbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        lbl.setForeground(UIHelper.TEXT_LIGHT);

        directAmountField = new JTextField(12);
        UIHelper.styleTextField(directAmountField);
        directAmountField.setFont(new Font(UIHelper.FONT, Font.BOLD, 15));
        directAmountField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { recalculateDirectAmount(); }
            @Override public void removeUpdate(DocumentEvent e) { recalculateDirectAmount(); }
            @Override public void changedUpdate(DocumentEvent e) { recalculateDirectAmount(); }
        });

        card.add(lbl);
        card.add(directAmountField);
        return card;
    }

    private JPanel createTotalSummaryCard() {
        ModernPanel card = new ModernPanel(14);
        card.setLayout(new BorderLayout(15, 0));
        card.setBackground(UIHelper.ROW_ALT);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.ACCENT_BLUE, 1),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel title = new JLabel("TOTAL FÍSICO CONTADO NA GAVETA");
        title.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        title.setForeground(UIHelper.TEXT_MUTED);

        lblTotalCounted = new JLabel("0.00 MT");
        lblTotalCounted.setFont(new Font(UIHelper.FONT, Font.BOLD, 22));
        lblTotalCounted.setForeground(UIHelper.ACCENT_CYAN);

        left.add(title);
        left.add(lblTotalCounted);

        JPanel right = new JPanel(new GridLayout(2, 1, 2, 2));
        right.setOpaque(false);

        lblTotalNotes = new JLabel("Notas: 0.00 MT");
        lblTotalNotes.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        lblTotalNotes.setForeground(UIHelper.TEXT_LIGHT);

        lblTotalCoins = new JLabel("Moedas: 0.00 MT");
        lblTotalCoins.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        lblTotalCoins.setForeground(UIHelper.TEXT_LIGHT);

        right.add(lblTotalNotes);
        right.add(lblTotalCoins);

        card.add(left, BorderLayout.WEST);
        card.add(right, BorderLayout.EAST);
        return card;
    }

    private JPanel createNotesCard() {
        ModernPanel card = new ModernPanel(14);
        card.setLayout(new BorderLayout(0, 6));
        card.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel("Observações / Justificação de Diferença (opcional):");
        lbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        lbl.setForeground(UIHelper.TEXT_LIGHT);

        notesArea = new JTextArea(2, 20);
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesArea.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        notesArea.setBackground(UIHelper.BG_CARD);
        notesArea.setForeground(UIHelper.TEXT_LIGHT);
        notesArea.setCaretColor(Color.WHITE);
        notesArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.BORDER, 1),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));

        card.add(lbl, BorderLayout.NORTH);
        card.add(notesArea, BorderLayout.CENTER);
        return card;
    }

    private JPanel createResultCard() {
        ModernPanel card = new ModernPanel(14);
        card.setLayout(new GridLayout(5, 1, 6, 6));
        card.setBackground(UIHelper.BG_CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.BORDER, 1),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        lblStatusResult = new JLabel("Sessão de Caixa Fechada com Sucesso");
        lblStatusResult.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        lblStatusResult.setForeground(UIHelper.APPROVED_GREEN);
        lblStatusResult.setIcon(UIHelper.icon("fas-check-circle", 15, UIHelper.APPROVED_GREEN));
        lblStatusResult.setIconTextGap(8);

        lblExpected = new JLabel("Esperado pelo Sistema: —");
        lblExpected.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        lblExpected.setForeground(UIHelper.TEXT_LIGHT);

        lblCounted = new JLabel("Contado pelo Operador: —");
        lblCounted.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        lblCounted.setForeground(UIHelper.TEXT_LIGHT);

        lblDiff = new JLabel("Diferença: —");
        lblDiff.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        lblDiff.setForeground(UIHelper.TEXT_LIGHT);

        lblAuditedNotes = new JLabel("");
        lblAuditedNotes.setFont(new Font(UIHelper.FONT, Font.ITALIC, 11));
        lblAuditedNotes.setForeground(UIHelper.TEXT_MUTED);

        card.add(lblStatusResult);
        card.add(lblExpected);
        card.add(lblCounted);
        card.add(lblDiff);
        card.add(lblAuditedNotes);
        return card;
    }

    private JPanel createFooterPanel() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setBackground(UIHelper.ROW_ALT);

        btnPrintZ = UIHelper.createPrimaryButton("Imprimir Relatório Z (A4)");
        btnPrintZ.setIcon(UIHelper.icon("fas-print", 13, Color.WHITE));
        btnPrintZ.setEnabled(false);
        btnPrintZ.addActionListener(e -> printZReport());

        btnConfirm = UIHelper.createSuccessButton("Confirmar Fecho de Caixa");
        btnConfirm.setIcon(UIHelper.icon("fas-lock", 13, Color.WHITE));
        btnConfirm.addActionListener(e -> executeClose());

        btnCancel = UIHelper.createDangerButton("Cancelar / Voltar");
        btnCancel.setIcon(UIHelper.icon("fas-times", 13, Color.WHITE));
        btnCancel.addActionListener(e -> dispose());

        footer.add(btnPrintZ);
        footer.add(btnConfirm);
        footer.add(btnCancel);
        return footer;
    }

    private void recalculateBreakdown() {
        if (!rbBreakdown.isSelected()) return;

        BigDecimal totalNotes = BigDecimal.ZERO;
        BigDecimal totalCoins = BigDecimal.ZERO;

        for (Map.Entry<BigDecimal, JTextField> entry : countFields.entrySet()) {
            BigDecimal denom = entry.getKey();
            JTextField field = entry.getValue();

            int count = 0;
            try {
                String txt = field.getText().trim();
                if (!txt.isEmpty()) count = Integer.parseInt(txt);
            } catch (NumberFormatException ignored) {}

            BigDecimal subtotal = denom.multiply(BigDecimal.valueOf(count));
            JLabel subLabel = subtotalLabels.get(denom);
            if (subLabel != null) {
                subLabel.setText(String.format("%,.2f MT", subtotal));
                subLabel.setForeground(count > 0 ? UIHelper.TEXT_LIGHT : UIHelper.TEXT_MUTED);
            }

            if (denom.compareTo(BigDecimal.valueOf(20)) >= 0) {
                totalNotes = totalNotes.add(subtotal);
            } else {
                totalCoins = totalCoins.add(subtotal);
            }
        }

        BigDecimal grandTotal = totalNotes.add(totalCoins);
        lblTotalCounted.setText(String.format("%,.2f MT", grandTotal));
        lblTotalNotes.setText(String.format("Notas: %,.2f MT", totalNotes));
        lblTotalCoins.setText(String.format("Moedas: %,.2f MT", totalCoins));
    }

    private void recalculateDirectAmount() {
        if (!rbDirectAmount.isSelected()) return;

        String txt = directAmountField.getText().trim().replace(",", ".");
        BigDecimal amount = BigDecimal.ZERO;
        if (!txt.isEmpty()) {
            try {
                amount = new BigDecimal(txt).setScale(2, RoundingMode.HALF_UP);
            } catch (Exception ignored) {}
        }
        lblTotalCounted.setText(String.format("%,.2f MT", amount));
        lblTotalNotes.setText("Notas: —");
        lblTotalCoins.setText("Moedas: —");
    }

    private BigDecimal getCalculatedCountedAmount() {
        if (rbDirectAmount.isSelected()) {
            String txt = directAmountField.getText().trim().replace(",", ".");
            if (txt.isEmpty()) return BigDecimal.ZERO;
            return new BigDecimal(txt).setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<BigDecimal, JTextField> entry : countFields.entrySet()) {
            BigDecimal denom = entry.getKey();
            JTextField field = entry.getValue();
            int count = 0;
            try {
                String val = field.getText().trim();
                if (!val.isEmpty()) count = Integer.parseInt(val);
            } catch (NumberFormatException ignored) {}
            total = total.add(denom.multiply(BigDecimal.valueOf(count)));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private String buildDenominationsJson() {
        if (rbDirectAmount.isSelected()) return null;

        List<CashDenominationDTO> list = new ArrayList<>();
        for (Map.Entry<BigDecimal, JTextField> entry : countFields.entrySet()) {
            BigDecimal denom = entry.getKey();
            JTextField field = entry.getValue();
            int count = 0;
            try {
                String val = field.getText().trim();
                if (!val.isEmpty()) count = Integer.parseInt(val);
            } catch (NumberFormatException ignored) {}
            if (count > 0) {
                list.add(new CashDenominationDTO(denom, count));
            }
        }
        if (list.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(list);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void executeClose() {
        BigDecimal countedCash;
        try {
            countedCash = getCalculatedCountedAmount();
            if (countedCash.compareTo(BigDecimal.ZERO) < 0) {
                ToastManager.show(this, FeedbackType.WARNING, "O montante contado não pode ser negativo.");
                return;
            }
        } catch (Exception ex) {
            ToastManager.show(this, FeedbackType.ERROR, "Valor contado inválido. Introduza um montante numérico.");
            return;
        }

        String notes = notesArea.getText().trim();
        String json = buildDenominationsJson();

        try {
            posApiClient.closeSession(session.id(), countedCash, null, notes, json);
            PosZReportDTO z = posApiClient.getZReport(session.id());

            BigDecimal expected = z.expectedCash() != null ? z.expectedCash() : BigDecimal.ZERO;
            lblExpected.setText(String.format("Esperado pelo Sistema: %,.2f MT", expected));
            lblCounted.setText(String.format("Contado pelo Operador: %,.2f MT", countedCash));

            BigDecimal diff = z.difference() != null ? z.difference() : BigDecimal.ZERO;
            if (diff.compareTo(BigDecimal.ZERO) == 0) {
                lblDiff.setText("Diferença: 0.00 MT (Caixa Bate Certo)");
                lblDiff.setForeground(UIHelper.APPROVED_GREEN);
            } else if (diff.compareTo(BigDecimal.ZERO) < 0) {
                lblDiff.setText(String.format("Diferença: %,.2f MT (Falta / Quebra na Gaveta)", diff));
                lblDiff.setForeground(UIHelper.REJECTED_RED);
            } else {
                lblDiff.setText(String.format("Diferença: +%,.2f MT (Sobra na Gaveta)", diff));
                lblDiff.setForeground(UIHelper.ACCENT_CYAN);
            }

            if (!notes.isEmpty()) {
                lblAuditedNotes.setText("Observações: " + notes);
            }

            // Desativa controlos de contagem e ativa impressão
            resultCard.setVisible(true);
            btnConfirm.setEnabled(false);
            btnPrintZ.setEnabled(true);
            rbBreakdown.setEnabled(false);
            rbDirectAmount.setEnabled(false);
            directAmountField.setEnabled(false);
            notesArea.setEnabled(false);
            for (JTextField f : countFields.values()) {
                f.setEnabled(false);
            }

            btnCancel.setText("Fechar Janela");

            if (onCloseSuccess != null) {
                onCloseSuccess.run();
            }

            ToastManager.show(this, FeedbackType.SUCCESS, "Caixa fechado com sucesso! Pode agora imprimir o Relatório Z.");
        } catch (Exception ex) {
            ToastManager.show(this, FeedbackType.ERROR, "Erro ao fechar caixa: " + ex.getMessage());
        }
    }

    private void printZReport() {
        try {
            byte[] pdfBytes = posApiClient.renderZReport(session.id());
            PrintPreviewDialog.show(this, pdfBytes, "relatorio-z-" + session.id() + ".pdf");
        } catch (Exception ex) {
            ToastManager.show(this, FeedbackType.ERROR, "Erro ao gerar PDF do Relatório Z: " + ex.getMessage());
        }
    }
}
