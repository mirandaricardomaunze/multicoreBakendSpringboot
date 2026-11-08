package mz.multicore.erp.gui.sector;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.PickupApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.pickup.dto.MaterialPickupDTO;
import mz.multicore.erp.modules.pickup.dto.PickupAccountDTO;
import mz.multicore.erp.modules.pickup.dto.PickupAccountSummaryDTO;
import mz.multicore.erp.modules.pickup.dto.PickupBalanceLineDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Paid material awaiting pickup (SA-C07, {@code docs/MATERIAL_POR_LEVANTAR_SPEC.md}). Accounts on the left;
 * the selected invoice's balance per article and its slip history on the right. The main action, "Entregar
 * Material", issues an LV slip and offers to print it for the gate.
 */
public class PickupsPanel extends JPanel {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String[] ACCOUNT_COLUMNS = {"Fatura", "Data", "Cliente", "Por levantar", "Prazo", "Estado"};
    private static final String[] BALANCE_COLUMNS = {"Código", "Artigo", "Unid.", "Faturado", "Creditado", "Levantado", "Por levantar"};
    private static final String[] SLIP_COLUMNS = {"Talão", "Data", "Levantado por", "Viatura", "Linhas", "Estado"};

    private final PickupApiClient api;
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();
    private final DefaultTableModel accountModel = readOnly(ACCOUNT_COLUMNS);
    private final JTable accountTable = new JTable(accountModel);
    private final DefaultTableModel balanceModel = readOnly(BALANCE_COLUMNS);
    private final JTable balanceTable = new JTable(balanceModel);
    private final DefaultTableModel slipModel = readOnly(SLIP_COLUMNS);
    private final JTable slipTable = new JTable(slipModel);

    private final ModernButton registerBtn = UIHelper.createSuccessButton("Registar Fatura");
    private final ModernButton deliverBtn = UIHelper.createSuccessButton("Entregar Material");
    private final ModernButton printBtn = UIHelper.createSecondaryButton("Imprimir Talão");
    private final ModernButton cancelBtn = UIHelper.createDangerButton("Anular Talão");
    private final JLabel detailTitle = new JLabel(" ");
    private final JLabel detailMeta = new JLabel(" ");
    private final CardLayout detailCards = new CardLayout();
    private final JPanel detailHolder = new JPanel(detailCards);

    private final List<PickupAccountSummaryDTO> accounts = new ArrayList<>();
    private PickupAccountDTO current;
    private java.util.function.Consumer<List<PickupAccountSummaryDTO>> onLoaded = list -> { };

    public PickupsPanel(PickupApiClient api) {
        this.api = api;
        setLayout(new BorderLayout(0, 10));
        setOpaque(false);
        setBorder(new EmptyBorder(15, 5, 5, 5));
        add(feedback, BorderLayout.NORTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildAccountsCard(), buildDetail());
        split.setOpaque(false);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setResizeWeight(0.45);
        split.setDividerSize(10);
        ResponsiveSupport.adaptiveSplit(split, 980);
        split.setContinuousLayout(true);
        add(split, BorderLayout.CENTER);
        syncButtons();
    }

    // ─── Layout ─────────────────────────────────────────────────────────────────

    private JComponent buildAccountsCard() {
        UIHelper.styleTable(accountTable);
        accountTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        accountTable.getColumnModel().getColumn(5).setCellRenderer(TableCellRenderers.status());
        accountTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) loadSelected();
        });
        JScrollPane scroll = new JScrollPane(accountTable);
        UIHelper.styleScrollPane(scroll);
        TableEmptyState.install(scroll);

        JTextField search = TableFilter.searchField("Fatura ou cliente…");
        search.setPreferredSize(new Dimension(170, UIHelper.FORM_CONTROL_HEIGHT));
        JComboBox<String> state = TableFilter.combo("Todos os estados", "Aberto", "Concluído", "Cancelado");
        UIHelper.styleComboBox(state);
        state.setPreferredSize(new Dimension(150, UIHelper.FORM_CONTROL_HEIGHT));
        state.setSelectedItem("Aberto");
        TableFilter.install(accountTable, search, new TableFilter.ColumnFilter(state, 5));
        ModernButton yardBtn = UIHelper.createSecondaryButton("");
        yardBtn.setIcon(UIHelper.icon("fas-warehouse", 13, UIHelper.ACCENT_CYAN));
        yardBtn.setToolTipText("Material vendido que ainda está no estaleiro (ajuda a conferir a contagem física)");
        yardBtn.getAccessibleContext().setAccessibleName("Material vendido no estaleiro");
        yardBtn.setPreferredSize(new Dimension(38, UIHelper.FORM_CONTROL_HEIGHT));
        yardBtn.addActionListener(e -> PendingYardStockDialog.show(this, api, feedback));
        JPanel filters = TableFilter.bar(search, TableFilter.label("Estado:"), state, yardBtn);
        filters.setBorder(new EmptyBorder(0, 0, 10, 0));

        registerBtn.setIcon(UIHelper.icon("fas-plus", 14));
        registerBtn.setToolTipText("O cliente pagou e vai levantar aos poucos");
        registerBtn.addActionListener(e -> RegisterPickupAccountDialog.show(this, api, feedback, opened -> {
            ToastManager.success(this, "Fatura " + opened.summary().invoiceNumber() + " registada como material por levantar.");
            reload(opened.summary().id());
        }));
        ModernButton refreshBtn = UIHelper.createRefreshButton(() -> reload(selectedId()));

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(15, 15, 15, 15));
        card.add(ResponsiveSupport.cardTop("Material por Levantar", filters, refreshBtn, registerBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        card.add(ClientTablePagination.install(accountTable), BorderLayout.SOUTH);
        card.setMinimumSize(new Dimension(420, 200));
        return card;
    }

    private JComponent buildDetail() {
        detailHolder.setOpaque(false);
        detailHolder.add(emptyDetail(), "empty");
        detailHolder.add(filledDetail(), "detail");
        detailCards.show(detailHolder, "empty");
        return detailHolder;
    }

    private JComponent emptyDetail() {
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new GridBagLayout());
        JPanel inner = new JPanel(new GridLayout(3, 1, 0, 8));
        inner.setOpaque(false);
        inner.add(new JLabel(UIHelper.icon("fas-people-carry", 40, UIHelper.ACCENT_ORANGE), SwingConstants.CENTER));
        JLabel title = new JLabel("Pagou tudo, levanta aos poucos", SwingConstants.CENTER);
        title.setFont(new Font(UIHelper.FONT, Font.BOLD, 16));
        title.setForeground(UIHelper.TEXT_LIGHT);
        inner.add(title);
        JLabel text = new JLabel("<html><div style='text-align:center;width:320px'>Escolha uma fatura à esquerda, ou registe "
                + "uma nova. Cada entrega gera um talão LV com o que saiu e o que ainda falta levantar. O stock já saiu "
                + "na fatura: os levantamentos não o voltam a mexer.</div></html>", SwingConstants.CENTER);
        text.setForeground(UIHelper.TEXT_MUTED);
        inner.add(text);
        card.add(inner);
        return card;
    }

    private JComponent filledDetail() {
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        ModernPanel header = new ModernPanel(16);
        header.setLayout(new BorderLayout(12, 0));
        header.setBorder(new EmptyBorder(14, 16, 14, 16));
        header.add(new JLabel(UIHelper.icon("fas-file-invoice", 24, UIHelper.MODULE_COMERCIAL)), BorderLayout.WEST);
        JPanel text = new JPanel(new GridLayout(2, 1, 0, 2));
        text.setOpaque(false);
        detailTitle.setFont(new Font(UIHelper.FONT, Font.BOLD, 16));
        detailTitle.setForeground(UIHelper.TEXT_LIGHT);
        detailMeta.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        detailMeta.setForeground(UIHelper.TEXT_MUTED);
        text.add(detailTitle);
        text.add(detailMeta);
        header.add(text, BorderLayout.CENTER);
        deliverBtn.setIcon(UIHelper.icon("fas-truck-loading", 14));
        deliverBtn.setToolTipText("Registar o que sai agora e imprimir o talão para o portão");
        deliverBtn.addActionListener(e -> deliver());
        header.add(UIHelper.actionsBar(deliverBtn), BorderLayout.EAST);
        panel.add(header, BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        UIHelper.styleTabbedPaneMulticore(tabs);
        tabs.addTab("Saldo por Artigo", UIHelper.icon("fas-balance-scale", 15, UIHelper.APPROVED_GREEN), balanceCard());
        tabs.addTab("Talões de Levantamento", UIHelper.icon("fas-receipt", 15, UIHelper.ACCENT_BLUE), slipCard());
        panel.add(tabs, BorderLayout.CENTER);
        return panel;
    }

    private JComponent balanceCard() {
        UIHelper.styleTable(balanceTable);
        for (int c = 3; c <= 6; c++) balanceTable.getColumnModel().getColumn(c).setCellRenderer(TableCellRenderers.quantity());
        JScrollPane scroll = new JScrollPane(balanceTable);
        UIHelper.styleScrollPane(scroll);
        TableEmptyState.install(scroll);
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(14, 14, 14, 14));
        card.add(ResponsiveSupport.cardTop("O que o cliente ainda tem no estaleiro", null), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JComponent slipCard() {
        UIHelper.styleTable(slipTable);
        slipTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        slipTable.getColumnModel().getColumn(5).setCellRenderer(TableCellRenderers.status());
        slipTable.getSelectionModel().addListSelectionListener(e -> syncButtons());
        JScrollPane scroll = new JScrollPane(slipTable);
        UIHelper.styleScrollPane(scroll);
        TableEmptyState.install(scroll);
        printBtn.setIcon(UIHelper.icon("fas-print", 14));
        printBtn.addActionListener(e -> printSelected());
        cancelBtn.setIcon(UIHelper.icon("fas-ban", 14));
        cancelBtn.setToolTipText("Só gestão. A quantidade volta ao saldo do cliente.");
        cancelBtn.addActionListener(e -> cancelSelected());
        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(14, 14, 14, 14));
        card.add(ResponsiveSupport.cardTop("Talões emitidos", null, cancelBtn, printBtn), BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    // ─── Data ───────────────────────────────────────────────────────────────────

    /** Receives every loaded list, so the module page can update its indicators. */
    public void onLoaded(java.util.function.Consumer<List<PickupAccountSummaryDTO>> listener) {
        this.onLoaded = listener == null ? list -> { } : listener;
    }

    public void reload() {
        reload(selectedId());
    }

    private void reload(Long keepId) {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null || api == null) return;
        UIHelper.loadAsync(this, () -> api.accounts(companyId), loaded -> applyAccounts(loaded, keepId),
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar o material por levantar",
                        error.getMessage(), "Tentar novamente", this::reload));
    }

    private void applyAccounts(List<PickupAccountSummaryDTO> loaded, Long keepId) {
        accounts.clear();
        accounts.addAll(loaded);
        accountModel.setRowCount(0);
        for (PickupAccountSummaryDTO a : accounts) {
            accountModel.addRow(new Object[]{a.invoiceNumber(), a.invoiceDate() == null ? "—" : UIHelper.formatDate(a.invoiceDate()),
                    a.clientName(), a.pendingLineCount() + " de " + a.lineCount() + (a.lineCount() == 1 ? " artigo" : " artigos"),
                    deadlineText(a), a.status()});
        }
        onLoaded.accept(List.copyOf(accounts));
        if (keepId != null) {
            for (int i = 0; i < accounts.size(); i++) {
                if (!accounts.get(i).id().equals(keepId)) continue;
                int view = accountTable.convertRowIndexToView(i);
                if (view >= 0) accountTable.setRowSelectionInterval(view, view);
                loadSelected();
                break;
            }
        }
        if (selectedSummary() == null) detailCards.show(detailHolder, "empty");
        syncButtons();
    }

    private void loadSelected() {
        PickupAccountSummaryDTO selected = selectedSummary();
        if (selected == null) {
            current = null;
            detailCards.show(detailHolder, "empty");
            syncButtons();
            return;
        }
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        UIHelper.loadAsync(this, () -> api.account(companyId, selected.id()), this::applyDetail,
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível abrir a fatura", error.getMessage(),
                        "Tentar novamente", this::loadSelected));
    }

    private void applyDetail(PickupAccountDTO account) {
        PickupAccountSummaryDTO selected = selectedSummary();
        if (selected == null || !selected.id().equals(account.summary().id())) return;
        current = account;
        PickupAccountSummaryDTO s = account.summary();
        detailTitle.setText(s.clientName() + "  ·  " + s.invoiceNumber());
        detailMeta.setText(s.statusLabel() + "  ·  " + (s.warehouseName() == null ? "" : s.warehouseName() + "  ·  ")
                + deadlineText(s) + (s.lastPickupAt() == null ? "" : "  ·  último levantamento " + DATE_TIME.format(s.lastPickupAt()))
                + (account.invoiceActive() ? "" : "  ·  fatura anulada"));
        balanceModel.setRowCount(0);
        for (PickupBalanceLineDTO line : account.lines()) {
            balanceModel.addRow(new Object[]{line.productSku(), line.productName(), unit(line.unitCode()),
                    line.invoiced(), line.credited(), line.picked(), line.balance()});
        }
        slipModel.setRowCount(0);
        for (MaterialPickupDTO slip : account.pickups()) {
            slipModel.addRow(new Object[]{slip.number(), slip.pickedAt() == null ? "—" : DATE_TIME.format(slip.pickedAt()),
                    slip.collectorName(), slip.vehiclePlate() == null ? "—" : slip.vehiclePlate(), slip.lines().size(), slip.status()});
        }
        detailCards.show(detailHolder, "detail");
        syncButtons();
    }

    // ─── Actions ────────────────────────────────────────────────────────────────

    private void deliver() {
        if (current == null) return;
        IssuePickupDialog.show(this, api, current, slip -> {
            reload(current.summary().id());
            ToastManager.success(this, "Talão " + slip.number() + " emitido.");
            if (UIHelper.confirm(this, "Imprimir o talão " + slip.number() + " para o portão?", "Confirmar")) print(slip);
        });
    }

    private void printSelected() {
        MaterialPickupDTO slip = selectedSlip();
        if (slip != null) print(slip);
    }

    private void print(MaterialPickupDTO slip) {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        UIHelper.loadAsync(this, () -> api.slipPdf(companyId, slip.id()),
                pdf -> PrintPreviewDialog.show(this, pdf, "levantamento-" + slip.number().replace('/', '-')),
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível gerar o talão", error.getMessage(), null, null));
    }

    private void cancelSelected() {
        MaterialPickupDTO slip = selectedSlip();
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (slip == null || companyId == null || current == null) return;
        String reason = UIHelper.promptRequiredText("Anular Talão", "fas-ban", "Talão " + slip.number()
                + ": a quantidade volta ao saldo do cliente.", "Motivo da anulação:");
        if (reason == null || reason.isBlank()) return;
        UIHelper.submitAsync(cancelBtn, () -> api.cancel(companyId, slip.id(), reason.trim()), cancelled -> {
            reload(current.summary().id());
            ToastManager.success(this, "Talão " + cancelled.number() + " anulado.");
        }, error -> feedback.show(FeedbackType.ERROR, "Não foi possível anular o talão", error.getMessage(), null, null));
    }

    // ─── Support ────────────────────────────────────────────────────────────────

    private void syncButtons() {
        boolean open = current != null && "OPEN".equals(current.summary().status()) && current.invoiceActive();
        deliverBtn.setEnabled(open);
        MaterialPickupDTO slip = selectedSlip();
        printBtn.setEnabled(slip != null);
        cancelBtn.setEnabled(slip != null && "ISSUED".equals(slip.status()));
    }

    private PickupAccountSummaryDTO selectedSummary() {
        int row = TableFilter.selectedModelRow(accountTable);
        return row < 0 || row >= accounts.size() ? null : accounts.get(row);
    }

    private Long selectedId() {
        PickupAccountSummaryDTO selected = selectedSummary();
        return selected == null ? null : selected.id();
    }

    private MaterialPickupDTO selectedSlip() {
        int row = TableFilter.selectedModelRow(slipTable);
        return current == null || row < 0 || row >= current.pickups().size() ? null : current.pickups().get(row);
    }

    private static String deadlineText(PickupAccountSummaryDTO a) {
        if (a.deadline() == null) return "Sem prazo";
        return (a.overdue() ? "Prazo vencido " : "Até ") + UIHelper.formatDate(a.deadline());
    }

    static String unit(String code) {
        return code == null ? "" : code.toLowerCase();
    }

    private static DefaultTableModel readOnly(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }
}
