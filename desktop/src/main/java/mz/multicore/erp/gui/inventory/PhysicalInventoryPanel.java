package mz.multicore.erp.gui.inventory;

import mz.multicore.erp.desktop.client.InventoryPhysicalCountingApiClient;
import mz.multicore.erp.gui.components.ActionMenuButton;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.KpiCard;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.ModernMessageDialog;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.ToastManager;
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

    private ActionMenuButton sessionActionsMenu;
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
        setLayout(new BorderLayout(0, 10));
        setOpaque(false);
        setBorder(new EmptyBorder(15, 5, 5, 5));

        initTable();
        initActions();
        add(buildCenterPanel(), BorderLayout.CENTER);

        refreshSessions();
    }

    private void initActions() {
        // Action Toolbar — canónica: acções à direita sem sobreposição
        newSessionBtn = UIHelper.createPrimaryButton("Nova Sessão");
        newSessionBtn.setIcon(UIHelper.icon("fas-plus", 13, Color.WHITE));
        newSessionBtn.addActionListener(e -> openNewSessionDialog());

        sessionActionsMenu = UIHelper.createActionMenuButton("Ciclo da Sessão")
                .addAction("Iniciar Contagem", UIHelper.icon("fas-play", 13, UIHelper.APPROVED_GREEN), this::handleStartCounting)
                .addAction("Fecho & Acerto", UIHelper.icon("fas-check-double", 13, UIHelper.PENDING_YELLOW), this::handleCloseSession)
                .addAction("Cancelar Sessão", UIHelper.icon("fas-times", 13, UIHelper.REJECTED_RED), this::handleCancelSession)
                .addAction("Actualizar", UIHelper.icon("fas-sync-alt", 13, UIHelper.ACCENT_BLUE), this::refreshSessions);

        printPdfBtn = UIHelper.createSecondaryButton("Dossiê PDF");
        printPdfBtn.setIcon(UIHelper.icon("fas-file-pdf", 13, Color.WHITE));
        printPdfBtn.setToolTipText("Visualizar e Imprimir Dossiê do Inventário");
        printPdfBtn.addActionListener(e -> handlePrintPdf());

    }

    private JPanel buildCenterPanel() {
        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);

        // KPI Section — grid canónico com altura compacta e fundo unificado
        JPanel kpiGrid = KpiCard.createGrid(4);

        kpiTotalItems = new JLabel("0");
        kpiCountedItems = new JLabel("0");
        kpiSurplusVal = new JLabel("0.00 MT");
        kpiDeficitVal = new JLabel("0.00 MT");

        kpiGrid.add(KpiCard.createMetricCard("Total de Produtos", kpiTotalItems, "Itens no inventário", "fas-boxes", UIHelper.ACCENT_BLUE));
        kpiGrid.add(KpiCard.createMetricCard("Produtos Contados", kpiCountedItems, "Itens bipados/registados", "fas-tasks", UIHelper.APPROVED_GREEN));
        kpiGrid.add(KpiCard.createMetricCard("Sobras (MT)", kpiSurplusVal, "Impacto positivo de stock", "fas-arrow-up", UIHelper.ACCENT_CYAN));
        kpiGrid.add(KpiCard.createMetricCard("Faltas (MT)", kpiDeficitVal, "Impacto negativo de stock", "fas-arrow-down", UIHelper.REJECTED_RED));

        center.add(kpiGrid, BorderLayout.NORTH);
        center.add(buildTableCard(), BorderLayout.CENTER);
        return center;
    }

    private ModernPanel buildTableCard() {
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(15, 15, 15, 15));

        card.add(UIHelper.tableCardTop("Inventário Físico & Reconciliação", buildCardToolbar(),
                printPdfBtn, sessionActionsMenu, newSessionBtn), BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(itemsTable);
        UIHelper.styleScrollPane(scroll);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildCardToolbar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setOpaque(false);

        // Esquerda: Sessão Activa + Campo de Pesquisa livre
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);

        JLabel sessionLbl = TableFilter.label("Sessão:", "fas-clipboard-list");
        sessionCombo = new JComboBox<>();
        UIHelper.styleComboBox(sessionCombo);
        sessionCombo.setPreferredSize(new Dimension(190, UIHelper.FORM_CONTROL_HEIGHT));
        sessionCombo.addActionListener(e -> onSessionSelected());

        statusLabel = new JLabel(" [-] ");
        statusLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        statusLabel.setForeground(UIHelper.TEXT_LIGHT);

        countModeLabel = new JLabel("");
        countModeLabel.setFont(new Font(UIHelper.FONT, Font.ITALIC, 11));
        countModeLabel.setForeground(UIHelper.ACCENT_BLUE);

        JTextField searchField = TableFilter.searchField("Pesquisar na contagem…");
        searchField.setPreferredSize(new Dimension(200, UIHelper.FORM_CONTROL_HEIGHT));
        TableFilter.install(itemsTable, searchField);

        left.add(sessionLbl);
        left.add(sessionCombo);
        left.add(statusLabel);
        left.add(countModeLabel);
        left.add(searchField);
        bar.add(left, BorderLayout.WEST);

        // Direita: Leitor de Código de Barras / SKU + Qtd + Botão Registar
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        JLabel barcodeLbl = TableFilter.label("Bipar Barcode / SKU:", "fas-barcode");

        barcodeScanField = new JTextField(10);
        UIHelper.styleTextField(barcodeScanField);
        barcodeScanField.setPreferredSize(new Dimension(130, UIHelper.FORM_CONTROL_HEIGHT));
        barcodeScanField.setToolTipText("Bipar ou introduzir Código de Barras / SKU (Enter para adicionar)");
        barcodeScanField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    handleBarcodeScan();
                }
            }
        });

        JLabel qtyLbl = TableFilter.label("Qtd:");

        manualQtyField = new JTextField("1", 3);
        UIHelper.styleTextField(manualQtyField);
        manualQtyField.setPreferredSize(new Dimension(45, UIHelper.FORM_CONTROL_HEIGHT));
        manualQtyField.setHorizontalAlignment(JTextField.RIGHT);

        recordCountBtn = UIHelper.createPrimaryButton("Registar");
        recordCountBtn.setIcon(UIHelper.icon("fas-barcode", 13, Color.WHITE));
        recordCountBtn.addActionListener(e -> handleBarcodeScan());

        right.add(barcodeLbl);
        right.add(barcodeScanField);
        right.add(qtyLbl);
        right.add(manualQtyField);
        right.add(recordCountBtn);
        bar.add(right, BorderLayout.EAST);

        return bar;
    }

    private void initTable() {
        String[] cols = {"Código", "Produto", "Esperado", "Contado", "Diferença", "Custo Unit. (MT)", "Impacto (MT)", "Notas"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 3 || column == 7; // Permite editar contagem e notas directamente
            }
        };

        itemsTable = new JTable(tableModel);
        UIHelper.styleTable(itemsTable);

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

        sessionActionsMenu.setActionEnabled(0, isDraft);
        sessionActionsMenu.setActionEnabled(1, isInProgress || isDraft);
        sessionActionsMenu.setActionEnabled(2, !isClosed);
        sessionActionsMenu.setEnabled(!isClosed);
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
        }, ex -> ToastManager.show(this, FeedbackType.ERROR, "Erro no bipamento: " + ex.getMessage()));
    }

    private void handleStartCounting() {
        if (currentSession == null) return;
        UIHelper.submitAsync(sessionActionsMenu, () -> apiClient.startCounting(currentSession.id()), updated -> {
            this.currentSession = updated;
            updateKpiAndButtons();
            ToastManager.success(this, "Contagem de inventário iniciada.");
        }, null);
    }

    private void handleCloseSession() {
        if (currentSession == null) return;
        boolean confirmed = ModernMessageDialog.confirm(SwingUtilities.getWindowAncestor(this),
                FeedbackType.WARNING,
                "Fecho e Acerto de Stock",
                "Tem certeza que deseja encerrar o inventário " + currentSession.inventoryNumber() + "?\nO stock lógico dos produtos será ajustado automaticamente para igualar a contagem física.",
                "Encerrar Inventário");
        if (confirmed) {
            UIHelper.submitAsync(sessionActionsMenu, () -> apiClient.closeAndAdjustStock(currentSession.id()), updated -> {
                this.currentSession = updated;
                populateTable(updated);
                updateKpiAndButtons();
                ToastManager.success(this, "Inventário concluído e stock ajustado com sucesso.");
            }, null);
        }
    }

    private void handleCancelSession() {
        if (currentSession == null) return;
        boolean confirmed = ModernMessageDialog.confirm(SwingUtilities.getWindowAncestor(this),
                FeedbackType.WARNING,
                "Cancelar Inventário",
                "Deseja cancelar a sessão " + currentSession.inventoryNumber() + "?",
                "Cancelar Sessão");
        if (confirmed) {
            UIHelper.submitAsync(sessionActionsMenu, () -> apiClient.cancelSession(currentSession.id()), updated -> {
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

        ModernFormDialog dialog = new ModernFormDialog(SwingUtilities.getWindowAncestor(this),
                "Abertura de Nova Sessão de Inventário", "fas-clipboard-list",
                "Defina o âmbito da contagem física", panel)
                .setConfirmButton("Abrir Sessão", "fas-play");
        if (dialog.showDialog()) {
            String desc = descField.getText().trim();
            CreateInventorySessionRequest req = new CreateInventorySessionRequest(desc, blindCheckBox.isSelected(), null);
            UIHelper.submitAsync(newSessionBtn, () -> apiClient.createSession(req), created -> {
                refreshSessions();
            }, null);
        }
    }

    JTable getItemsTable() {
        return itemsTable;
    }
}
