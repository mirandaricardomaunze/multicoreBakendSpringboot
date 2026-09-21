package mz.multicore.erp.gui.inventory;

import mz.multicore.erp.desktop.client.InventoryPhysicalCountingApiClient;
import mz.multicore.erp.gui.components.KpiCard;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.inventory.dto.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.util.List;

public class PhysicalInventoryPanel extends JPanel {

    private final InventoryPhysicalCountingApiClient apiClient;

    private JComboBox<String> sessionCombo;
    private JLabel statusLabel;
    private JLabel countModeLabel;
    private JTextField barcodeScanField;
    private JTextField manualQtyField;
    private ModernButton recordCountBtn;

    private ModernButton startBtn;
    private ModernButton closeBtn;
    private ModernButton cancelBtn;
    private ModernButton printPdfBtn;
    private ModernButton newSessionBtn;

    private JLabel kpiTotalItems;
    private JLabel kpiCountedItems;
    private JLabel kpiSurplusVal;
    private JLabel kpiDeficitVal;

    private JTable itemsTable;
    private DefaultTableModel tableModel;

    private List<InventorySessionDTO> activeSessions = List.of();
    private InventorySessionDTO currentSession;

    public PhysicalInventoryPanel(InventoryPhysicalCountingApiClient apiClient) {
        this.apiClient = apiClient;
        setLayout(new BorderLayout(0, 12));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(12, 16, 12, 16));

        add(buildTopBar(), BorderLayout.NORTH);
        add(buildCenterPanel(), BorderLayout.CENTER);

        refreshSessions();
    }

    private JPanel buildTopBar() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);

        // Header Title & Subtitle
        JPanel titlePanel = new JPanel(new GridLayout(2, 1, 0, 2));
        titlePanel.setOpaque(false);
        JLabel title = new JLabel("Inventário Físico & Reconciliação de Stock");
        title.setFont(new Font(UIHelper.FONT, Font.BOLD, 18));
        title.setForeground(UIHelper.TEXT_LIGHT);
        JLabel subtitle = new JLabel("Auditoria periódica de armazém, leitura por código de barras e acerto automático de stock");
        subtitle.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        subtitle.setForeground(UIHelper.TEXT_MUTED);
        titlePanel.add(title);
        titlePanel.add(subtitle);

        // Action Toolbar
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        toolbar.setOpaque(false);

        newSessionBtn = UIHelper.createPrimaryButton("Nova Sessão");
        newSessionBtn.setIcon(UIHelper.icon("fas-plus", 13, Color.WHITE));
        newSessionBtn.addActionListener(e -> openNewSessionDialog());

        startBtn = UIHelper.createSuccessButton("Iniciar Contagem");
        startBtn.setIcon(UIHelper.icon("fas-play", 13, Color.WHITE));
        startBtn.addActionListener(e -> handleStartCounting());

        closeBtn = UIHelper.createWarningButton("Fechamento & Acerto");
        closeBtn.setIcon(UIHelper.icon("fas-check-double", 13, Color.WHITE));
        closeBtn.addActionListener(e -> handleCloseSession());

        cancelBtn = UIHelper.createDangerButton("Cancelar");
        cancelBtn.setIcon(UIHelper.icon("fas-times", 13, Color.WHITE));
        cancelBtn.addActionListener(e -> handleCancelSession());

        printPdfBtn = UIHelper.createSecondaryButton("Dossiê PDF");
        printPdfBtn.setIcon(UIHelper.icon("fas-file-pdf", 13, Color.WHITE));
        printPdfBtn.addActionListener(e -> handlePrintPdf());

        toolbar.add(newSessionBtn);
        toolbar.add(startBtn);
        toolbar.add(closeBtn);
        toolbar.add(cancelBtn);
        toolbar.add(printPdfBtn);

        panel.add(titlePanel, BorderLayout.WEST);
        panel.add(toolbar, BorderLayout.EAST);
        return panel;
    }

    private JPanel buildCenterPanel() {
        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);

        // KPI Section
        JPanel kpiGrid = new JPanel(new GridLayout(1, 4, 10, 0));
        kpiGrid.setOpaque(false);

        kpiTotalItems = new JLabel("0");
        kpiCountedItems = new JLabel("0");
        kpiSurplusVal = new JLabel("0.00 MT");
        kpiDeficitVal = new JLabel("0.00 MT");

        kpiGrid.add(KpiCard.createMetricCard("Total de Produtos", kpiTotalItems, "Itens no inventário", "fas-boxes", UIHelper.ACCENT_BLUE));
        kpiGrid.add(KpiCard.createMetricCard("Produtos Contados", kpiCountedItems, "Itens bipados/registados", "fas-tasks", UIHelper.APPROVED_GREEN));
        kpiGrid.add(KpiCard.createMetricCard("Sobras (MT)", kpiSurplusVal, "Impacto positivo de stock", "fas-arrow-up", UIHelper.APPROVED_GREEN));
        kpiGrid.add(KpiCard.createMetricCard("Faltas (MT)", kpiDeficitVal, "Impacto negativo de stock", "fas-arrow-down", UIHelper.REJECTED_RED));

        center.add(kpiGrid, BorderLayout.NORTH);

        // Scan & Table Container
        JPanel body = new JPanel(new BorderLayout(0, 8));
        body.setOpaque(false);

        body.add(buildScanBar(), BorderLayout.NORTH);
        body.add(buildTablePanel(), BorderLayout.CENTER);

        center.add(body, BorderLayout.CENTER);
        return center;
    }

    private JPanel buildScanBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        panel.setBackground(UIHelper.BG_CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.BORDER),
                new EmptyBorder(6, 10, 6, 10)
        ));

        JLabel sessionLbl = new JLabel("Sessão Activa:");
        sessionLbl.setForeground(UIHelper.TEXT_LIGHT);
        sessionLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));

        sessionCombo = new JComboBox<>();
        UIHelper.styleComboBox(sessionCombo);
        sessionCombo.setPreferredSize(new Dimension(220, UIHelper.FORM_CONTROL_HEIGHT));
        sessionCombo.addActionListener(e -> onSessionSelected());

        statusLabel = new JLabel(" [-] ");
        statusLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        statusLabel.setForeground(UIHelper.TEXT_LIGHT);

        countModeLabel = new JLabel("");
        countModeLabel.setFont(new Font(UIHelper.FONT, Font.ITALIC, 11));
        countModeLabel.setForeground(UIHelper.ACCENT_BLUE);

        JLabel barcodeLbl = new JLabel("Bipar Barcode / SKU:");
        barcodeLbl.setForeground(UIHelper.TEXT_LIGHT);
        barcodeLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));

        barcodeScanField = new JTextField(12);
        UIHelper.styleTextField(barcodeScanField);
        barcodeScanField.setToolTipText("Bipar ou introduzir Código de Barras / SKU (Enter para adicionar +1)");
        barcodeScanField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleBarcodeScan();
                }
            }
        });

        JLabel qtyLbl = new JLabel("Qtd:");
        qtyLbl.setForeground(UIHelper.TEXT_LIGHT);
        qtyLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));

        manualQtyField = new JTextField("1", 4);
        UIHelper.styleTextField(manualQtyField);
        manualQtyField.setHorizontalAlignment(JTextField.RIGHT);

        recordCountBtn = UIHelper.createPrimaryButton("Registar");
        recordCountBtn.setIcon(UIHelper.icon("fas-barcode", 13, Color.WHITE));
        recordCountBtn.addActionListener(e -> handleBarcodeScan());

        panel.add(sessionLbl);
        panel.add(sessionCombo);
        panel.add(statusLabel);
        panel.add(countModeLabel);
        panel.add(Box.createHorizontalStrut(16));
        panel.add(barcodeLbl);
        panel.add(barcodeScanField);
        panel.add(qtyLbl);
        panel.add(manualQtyField);
        panel.add(recordCountBtn);

        return panel;
    }

    private JPanel buildTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        String[] cols = {"Código", "Produto", "Esperado", "Contado", "Diferença", "Custo Unit. (MT)", "Impacto (MT)", "Notas"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3 || column == 7; // Permite editar contagem e notas directamente
            }
        };

        itemsTable = new JTable(tableModel);
        UIHelper.styleTable(itemsTable);
        itemsTable.setRowHeight(28);

        // Custom Cell Renderer para Semáforo de Variação
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? UIHelper.BG_CARD : UIHelper.ROW_ALT);
                    if (column == 4 || column == 6) { // Diferença e Impacto
                        String valStr = value != null ? value.toString() : "0";
                        if (valStr.startsWith("+") || (!valStr.startsWith("-") && !valStr.equals("0") && !valStr.equals("0.00"))) {
                            c.setForeground(UIHelper.APPROVED_GREEN); // Verde sobra
                        } else if (valStr.startsWith("-")) {
                            c.setForeground(UIHelper.REJECTED_RED); // Vermelho falta
                        } else {
                            c.setForeground(UIHelper.TEXT_LIGHT);
                        }
                    } else {
                        c.setForeground(UIHelper.TEXT_LIGHT);
                    }
                }
                return c;
            }
        };

        for (int i = 0; i < itemsTable.getColumnCount(); i++) {
            itemsTable.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        // Listener para edição direta na célula de Contado
        tableModel.addTableModelListener(e -> {
            if (e.getType() == javax.swing.event.TableModelEvent.UPDATE && e.getColumn() == 3) {
                int row = e.getFirstRow();
                if (row >= 0 && row < itemsTable.getRowCount() && currentSession != null) {
                    try {
                        String code = (String) tableModel.getValueAt(row, 0);
                        Object valObj = tableModel.getValueAt(row, 3);
                        BigDecimal count = new BigDecimal(valObj.toString().trim());
                        InventoryItemDTO item = currentSession.items().stream()
                                .filter(i -> i.productCode().equals(code))
                                .findFirst().orElse(null);
                        if (item != null) {
                            UpdateInventoryItemCountRequest req = new UpdateInventoryItemCountRequest(item.id(), count, null, false, null);
                            UIHelper.submitAsync(recordCountBtn, () -> apiClient.recordCount(currentSession.id(), req), res -> {
                                this.currentSession = res;
                                updateKpiAndButtons();
                            }, null);
                        }
                    } catch (Exception ignored) {}
                }
            }
        });

        JScrollPane scroll = new JScrollPane(itemsTable);
        UIHelper.styleScrollPane(scroll);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private void refreshSessions() {
        UIHelper.loadAsync(this, () -> apiClient.listSessions(), sessions -> {
            this.activeSessions = sessions;
            sessionCombo.removeAllItems();
            for (InventorySessionDTO s : sessions) {
                sessionCombo.addItem(s.inventoryNumber() + " - " + s.description());
            }
            if (!sessions.isEmpty()) {
                sessionCombo.setSelectedIndex(0);
                onSessionSelected();
            } else {
                currentSession = null;
                updateKpiAndButtons();
            }
        }, null);
    }

    private void onSessionSelected() {
        int idx = sessionCombo.getSelectedIndex();
        if (idx >= 0 && idx < activeSessions.size()) {
            Long id = activeSessions.get(idx).id();
            UIHelper.loadAsync(this, () -> apiClient.getSessionById(id), session -> {
                this.currentSession = session;
                populateTable(session);
                updateKpiAndButtons();
            }, null);
        }
    }

    private void populateTable(InventorySessionDTO session) {
        tableModel.setRowCount(0);
        if (session == null || session.items() == null) return;

        for (InventoryItemDTO item : session.items()) {
            String expStr = session.blindCounting() ? "***" : String.format("%.2f", item.expectedQuantity());
            String diffStr = session.blindCounting() ? "***" : String.format("%+.2f", item.difference());
            String impStr = session.blindCounting() ? "***" : String.format("%+.2f", item.financialImpact());

            tableModel.addRow(new Object[]{
                    item.productCode(),
                    item.productName(),
                    expStr,
                    String.format("%.2f", item.countedQuantity()),
                    diffStr,
                    String.format("%.2f", item.unitCost()),
                    impStr,
                    item.notes() != null ? item.notes() : ""
            });
        }
    }

    private void updateKpiAndButtons() {
        if (currentSession == null) {
            kpiTotalItems.setText("0");
            kpiCountedItems.setText("0");
            kpiSurplusVal.setText("0.00 MT");
            kpiDeficitVal.setText("0.00 MT");
            statusLabel.setText(" [Sem Sessão] ");
            countModeLabel.setText("");
            enableControls(false);
            return;
        }

        kpiTotalItems.setText(String.valueOf(currentSession.totalItems()));
        kpiCountedItems.setText(String.valueOf(currentSession.itemsCounted()));
        kpiSurplusVal.setText(String.format("%,.2f MT", currentSession.totalSurplusValue()));
        kpiDeficitVal.setText(String.format("%,.2f MT", currentSession.totalDeficitValue()));

        statusLabel.setText(" [" + currentSession.status().getDescription() + "] ");
        countModeLabel.setText(currentSession.blindCounting() ? "(Contagem Cega)" : "(Contagem Aberta)");

        boolean isDraft = currentSession.status() == InventoryStatus.DRAFT;
        boolean isInProgress = currentSession.status() == InventoryStatus.IN_PROGRESS;
        boolean isClosed = currentSession.status() == InventoryStatus.CLOSED;

        startBtn.setEnabled(isDraft);
        closeBtn.setEnabled(isInProgress || isDraft);
        cancelBtn.setEnabled(!isClosed);
        printPdfBtn.setEnabled(true);
        enableControls(isInProgress || isDraft);
    }

    private void enableControls(boolean enable) {
        barcodeScanField.setEnabled(enable);
        manualQtyField.setEnabled(enable);
        recordCountBtn.setEnabled(enable);
    }

    private void handleBarcodeScan() {
        String code = barcodeScanField.getText() != null ? barcodeScanField.getText().trim() : "";
        if (code.isEmpty() || currentSession == null) return;

        BigDecimal qty = BigDecimal.ONE;
        try {
            qty = new BigDecimal(manualQtyField.getText().trim());
        } catch (Exception ignored) {}

        UpdateInventoryItemCountRequest req = new UpdateInventoryItemCountRequest(null, qty, code, true, null);
        UIHelper.submitAsync(recordCountBtn, () -> apiClient.recordCount(currentSession.id(), req), updated -> {
            this.currentSession = updated;
            populateTable(updated);
            updateKpiAndButtons();
            barcodeScanField.setText("");
            barcodeScanField.requestFocusInWindow();
        }, ex -> JOptionPane.showMessageDialog(this, ex.getMessage(), "Erro no Bipamento", JOptionPane.ERROR_MESSAGE));
    }

    private void handleStartCounting() {
        if (currentSession == null) return;
        UIHelper.submitAsync(startBtn, () -> apiClient.startCounting(currentSession.id()), updated -> {
            this.currentSession = updated;
            updateKpiAndButtons();
            JOptionPane.showMessageDialog(this, "Contagem de inventário iniciada!", "Inventário Físico", JOptionPane.INFORMATION_MESSAGE);
        }, null);
    }

    private void handleCloseSession() {
        if (currentSession == null) return;
        int opt = JOptionPane.showConfirmDialog(this,
                "Tem certeza que deseja encerrar o inventário " + currentSession.inventoryNumber() + "?\nO stock lógico dos produtos será ajustado automaticamente para igualar a contagem física.",
                "Fechamento & Acerto de Stock", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (opt == JOptionPane.YES_OPTION) {
            UIHelper.submitAsync(closeBtn, () -> apiClient.closeAndAdjustStock(currentSession.id()), updated -> {
                this.currentSession = updated;
                populateTable(updated);
                updateKpiAndButtons();
                JOptionPane.showMessageDialog(this, "Inventário concluído e stock ajustado com sucesso!", "Inventário Concluído", JOptionPane.INFORMATION_MESSAGE);
            }, null);
        }
    }

    private void handleCancelSession() {
        if (currentSession == null) return;
        int opt = JOptionPane.showConfirmDialog(this,
                "Deseja cancelar a sessão " + currentSession.inventoryNumber() + "?",
                "Cancelar Inventário", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (opt == JOptionPane.YES_OPTION) {
            UIHelper.submitAsync(cancelBtn, () -> apiClient.cancelSession(currentSession.id()), updated -> {
                this.currentSession = updated;
                updateKpiAndButtons();
            }, null);
        }
    }

    private void handlePrintPdf() {
        if (currentSession == null) return;
        UIHelper.submitAsync(printPdfBtn, () -> apiClient.renderPdf(currentSession.id()), pdfBytes -> {
            UIHelper.previewOrPrintPdf(this, pdfBytes, "Dossiê de Inventário Físico - " + currentSession.inventoryNumber());
        }, null);
    }

    private void openNewSessionDialog() {
        JTextField descField = new JTextField("Inventário Físico Armazém " + java.time.LocalDate.now());
        UIHelper.styleTextField(descField);
        JCheckBox blindCheckBox = new JCheckBox("Activar Contagem Cega (Oculta stock esperado até o fecho)");
        blindCheckBox.setForeground(UIHelper.TEXT_LIGHT);
        blindCheckBox.setOpaque(false);

        JPanel panel = new JPanel(new GridLayout(2, 1, 0, 8));
        panel.add(UIHelper.createFilterGroup("Descrição:", descField));
        panel.add(blindCheckBox);

        int res = JOptionPane.showConfirmDialog(this, panel, "Abertura de Nova Sessão de Inventário", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            String desc = descField.getText().trim();
            CreateInventorySessionRequest req = new CreateInventorySessionRequest(desc, blindCheckBox.isSelected(), null);
            UIHelper.submitAsync(newSessionBtn, () -> apiClient.createSession(req), created -> {
                refreshSessions();
            }, null);
        }
    }
}
