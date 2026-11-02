package mz.multicore.erp.gui.inventory;

import mz.multicore.erp.desktop.client.InventoryPhysicalCountingApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.inventory.dto.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.util.List;

/** Painel de Contagem Física, Reconciliação e Acerto Automático de Inventário. */
public class PhysicalInventoryPanel extends JPanel {

    private final InventoryPhysicalCountingApiClient apiClient;
    private final PhysicalInventoryOperationsCoordinator coordinator;

    private JComboBox<String> sessionCombo;
    private JLabel statusLabel, countModeLabel;
    private JTextField barcodeScanField, manualQtyField, searchField;
    private ModernButton recordCountBtn, printPdfBtn, newSessionBtn, refreshBtn, peekToggleBtn;
    private ActionMenuButton sessionActionsMenu;
    private JLabel kpiTotalItems, kpiCountedItems, kpiSurplusVal, kpiDeficitVal;
    private JTable itemsTable;
    private DefaultTableModel tableModel;

    private List<InventorySessionDTO> activeSessions = List.of();
    private InventorySessionDTO currentSession;

    public PhysicalInventoryPanel(InventoryPhysicalCountingApiClient apiClient) {
        this.apiClient = apiClient;
        this.coordinator = new PhysicalInventoryOperationsCoordinator(apiClient);

        setLayout(new BorderLayout(0, 6));
        setOpaque(false);
        setBorder(new EmptyBorder(6, 4, 4, 4));

        initTable();
        initActions();
        add(buildCenterPanel(), BorderLayout.CENTER);
        refreshSessions();
    }

    private void initActions() {
        newSessionBtn = UIHelper.createSuccessButton("Nova Sessão");
        newSessionBtn.setIcon(UIHelper.icon("fas-plus", 13, Color.WHITE));
        newSessionBtn.addActionListener(e -> coordinator.openNewSessionDialog(this, newSessionBtn, this::refreshSessions));

        sessionActionsMenu = UIHelper.createActionMenuButton("Ciclo da Sessão")
                .addAction("Iniciar Contagem", UIHelper.icon("fas-play", 13, UIHelper.APPROVED_GREEN),
                        () -> coordinator.handleStartCounting(this, currentSession, sessionActionsMenu, this::applySessionUpdated))
                .addAction("Fecho & Acerto", UIHelper.icon("fas-check-double", 13, UIHelper.PENDING_YELLOW),
                        () -> coordinator.handleCloseSession(this, currentSession, sessionActionsMenu, this::applySessionUpdated))
                .addAction("Cancelar Sessão", UIHelper.icon("fas-times", 13, UIHelper.REJECTED_RED),
                        () -> coordinator.handleCancelSession(this, currentSession, sessionActionsMenu, this::applySessionUpdated));

        refreshBtn = UIHelper.createRefreshButton(this::refreshSessions);
        printPdfBtn = UIHelper.createSecondaryButton("Dossiê PDF");
        printPdfBtn.setIcon(UIHelper.icon("fas-file-pdf", 13, Color.WHITE));
        printPdfBtn.setToolTipText("Visualizar e Imprimir Dossiê do Inventário");
        printPdfBtn.addActionListener(e -> coordinator.handlePrintPdf(this, currentSession, printPdfBtn));
    }

    private JPanel buildCenterPanel() {
        JPanel center = new JPanel(new BorderLayout(0, 6));
        center.setOpaque(false);

        JPanel kpiGrid = KpiCard.createGrid(4);
        kpiTotalItems = new JLabel("0"); kpiCountedItems = new JLabel("0");
        kpiSurplusVal = new JLabel("0.00 MT"); kpiDeficitVal = new JLabel("0.00 MT");

        kpiGrid.add(KpiCard.createInteractiveMetricCard("Total de Produtos", kpiTotalItems, "Itens no inventário", "fas-boxes", UIHelper.ACCENT_BLUE,
                "Clique para limpar filtros e ver todos os itens", () -> { if (searchField != null) searchField.setText(""); }));
        kpiGrid.add(KpiCard.createInteractiveMetricCard("Produtos Contados", kpiCountedItems, "Itens bipados/registados", "fas-tasks", UIHelper.APPROVED_GREEN,
                "Clique para focar a pesquisa", () -> { if (searchField != null) searchField.requestFocusInWindow(); }));
        kpiGrid.add(KpiCard.createInteractiveMetricCard("Sobras (MT)", kpiSurplusVal, "Impacto positivo de stock", "fas-arrow-up", UIHelper.ACCENT_CYAN,
                "Clique para filtrar produtos com sobra", () -> { if (searchField != null) searchField.setText("+"); }));
        kpiGrid.add(KpiCard.createInteractiveMetricCard("Faltas (MT)", kpiDeficitVal, "Impacto negativo de stock", "fas-arrow-down", UIHelper.REJECTED_RED,
                "Clique para filtrar produtos com falta", () -> { if (searchField != null) searchField.setText("-"); }));

        center.add(kpiGrid, BorderLayout.NORTH);
        center.add(buildTableCard(), BorderLayout.CENTER);
        return center;
    }

    private ModernPanel buildTableCard() {
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 6));
        card.setBorder(new EmptyBorder(8, 12, 8, 12));
        card.add(UIHelper.tableCardTop("Inventário Físico & Reconciliação", buildCardToolbar(),
                printPdfBtn, sessionActionsMenu, newSessionBtn), BorderLayout.NORTH);

        UIHelper.ensureHeadersFit(itemsTable);
        TableQuickPeekController peekController = TableQuickPeekController.install(itemsTable, card,
                (peek, mr) -> PhysicalInventoryViewCoordinator.populatePeek(peek, tableModel, itemsTable.convertRowIndexToModel(mr)));
        if (peekToggleBtn != null) peekToggleBtn.addActionListener(e -> peekController.toggle());

        JScrollPane scroll = new JScrollPane(itemsTable);
        UIHelper.styleScrollPane(scroll);
        card.add(scroll, BorderLayout.CENTER);
        card.add(ClientTablePagination.install(itemsTable), BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildCardToolbar() {
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

        searchField = TableFilter.searchField("Pesquisar na contagem…");
        searchField.setPreferredSize(new Dimension(200, UIHelper.FORM_CONTROL_HEIGHT));
        TableFilter.install(itemsTable, searchField);

        barcodeScanField = new JTextField(10);
        UIHelper.styleTextField(barcodeScanField);
        barcodeScanField.setPreferredSize(new Dimension(130, UIHelper.FORM_CONTROL_HEIGHT));
        barcodeScanField.setToolTipText("Bipar ou introduzir Código de Barras / SKU (Enter para adicionar)");
        barcodeScanField.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) { if (e.getKeyCode() == KeyEvent.VK_ENTER) handleBarcodeScan(); }
        });

        manualQtyField = new JTextField("1", 3);
        UIHelper.styleTextField(manualQtyField);
        manualQtyField.setPreferredSize(new Dimension(45, UIHelper.FORM_CONTROL_HEIGHT));
        manualQtyField.setHorizontalAlignment(JTextField.RIGHT);

        recordCountBtn = UIHelper.createSuccessButton("Registar");
        recordCountBtn.setIcon(UIHelper.icon("fas-barcode", 13, Color.WHITE));
        recordCountBtn.addActionListener(e -> handleBarcodeScan());

        peekToggleBtn = UIHelper.createSecondaryButton("");
        peekToggleBtn.setIcon(UIHelper.icon("fas-columns", 12));
        peekToggleBtn.setToolTipText("Espreitar item seleccionado (Espaço)");
        peekToggleBtn.getAccessibleContext().setAccessibleName("Espreitar item");
        peekToggleBtn.setPreferredSize(new Dimension(30, UIHelper.FORM_CONTROL_HEIGHT));

        return PhysicalInventoryViewCoordinator.buildToolbar(
                sessionCombo, statusLabel, countModeLabel, searchField, refreshBtn,
                barcodeScanField, manualQtyField, recordCountBtn, peekToggleBtn
        );
    }

    private void initTable() {
        String[] cols = {"Código", "Produto", "Esperado", "Contado", "Diferença", "Custo Unit. (MT)", "Impacto (MT)", "Notas"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int column) { return column == 3 || column == 7; }
        };
        itemsTable = new JTable(tableModel);
        UIHelper.styleTable(itemsTable);
        PhysicalInventoryTableHelper.configureTableInteractions(
                this, itemsTable, tableModel, barcodeScanField, recordCountBtn,
                () -> currentSession, apiClient, this::applySessionUpdated
        );
    }

    private void refreshSessions() {
        if (apiClient == null) return;
        UIHelper.loadAsync(this, apiClient::listSessions, sessions -> {
            this.activeSessions = sessions;
            sessionCombo.removeAllItems();
            for (InventorySessionDTO s : sessions) sessionCombo.addItem(s.inventoryNumber() + " - " + s.description());
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
        if (idx >= 0 && idx < activeSessions.size() && apiClient != null) {
            Long id = activeSessions.get(idx).id();
            UIHelper.loadAsync(this, () -> apiClient.getSessionById(id), this::applySessionUpdated, null);
        }
    }

    private void applySessionUpdated(InventorySessionDTO updated) {
        this.currentSession = updated;
        PhysicalInventoryViewCoordinator.populateTable(tableModel, updated);
        updateKpiAndButtons();
    }

    private void updateKpiAndButtons() {
        PhysicalInventoryViewCoordinator.updateKpiAndButtons(
                currentSession, kpiTotalItems, kpiCountedItems, kpiSurplusVal, kpiDeficitVal,
                statusLabel, countModeLabel, sessionActionsMenu, printPdfBtn,
                barcodeScanField, manualQtyField, recordCountBtn
        );
    }

    public void handleBarcodeScan() {
        if (currentSession == null) {
            ToastManager.show(this, FeedbackType.WARNING, "Abra ou seleccione uma sessão de inventário antes de registar contagens.");
            return;
        }
        String code = barcodeScanField.getText() != null ? barcodeScanField.getText().trim() : "";
        if (code.isEmpty()) {
            ToastManager.show(this, FeedbackType.WARNING, "Indique ou bipe o código do produto antes de registar a contagem.");
            barcodeScanField.requestFocusInWindow();
            return;
        }
        BigDecimal qty = BigDecimal.ONE;
        try { qty = new BigDecimal(manualQtyField.getText().trim()); } catch (Exception ignored) {}
        UpdateInventoryItemCountRequest req = new UpdateInventoryItemCountRequest(null, qty, code, true, null);
        UIHelper.submitAsync(recordCountBtn, () -> apiClient.recordCount(currentSession.id(), req), updated -> {
            applySessionUpdated(updated);
            barcodeScanField.setText("");
            barcodeScanField.requestFocusInWindow();
        }, ex -> ToastManager.show(this, FeedbackType.ERROR, "Erro no bipamento: " + ex.getMessage()));
    }

    JTable getItemsTable() { return itemsTable; }
}
