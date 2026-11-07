package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.NotificationFeed.NotificationItem;
import mz.multicore.erp.gui.components.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * Página central das notificações operacionais da empresa ativa. Partilha com o sino o mesmo
 * {@link NotificationReadStore} — o que se marca aqui reflecte-se no badge e vice-versa.
 */
public class NotificationsPanel extends JPanel {

    static final String READ_LABEL = NotificationsViewCoordinator.READ_LABEL;
    static final String UNREAD_LABEL = NotificationsViewCoordinator.UNREAD_LABEL;

    private final NotificationFeed feed;
    private final NotificationReadStore readStore;
    private final Consumer<String> navigator;
    private final IntConsumer unreadCountListener;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JLabel summaryLabel;
    private final ModernButton refreshButton;
    private final ActionMenuButton readingMenu;
    private List<NotificationItem> items = new ArrayList<>();
    private int refreshVersion;
    private final InlineFeedbackPanel feedback = new InlineFeedbackPanel();

    public NotificationsPanel(NotificationFeed feed, NotificationReadStore readStore,
                              Consumer<String> navigator, IntConsumer onUnreadCountChanged) {
        this.feed = feed;
        this.readStore = readStore;
        this.navigator = navigator;
        this.unreadCountListener = onUnreadCountChanged;
        setLayout(new BorderLayout(0, 16));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(22, 25, 22, 25));
        summaryLabel = new JLabel("Alertas operacionais da empresa ativa");
        summaryLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        summaryLabel.setForeground(UIHelper.TEXT_MUTED);
        add(feedback, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(18, 18, 18, 18));

        String[] columns = {"Tipo", "Notificação", "Detalhes", "Data / Estado", "Leitura"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        UIHelper.styleTable(table);
        UIHelper.ensureHeadersFit(table);
        table.setFillsViewportHeight(true);
        table.putClientProperty("emptyText", "Sem notificações.");
        int[] widths = {100, 240, 330, 120, 80};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        TableQuickPeekController.install(table, card, (peek, mr) -> {
            int row = mr; // o controlador já entrega a linha do modelo
            NotificationsViewCoordinator.populateQuickPeek(peek, tableModel, row, this::openSelectedModule);
        });

        JTextField search = TableFilter.searchField("Pesquisar notificações…");
        JComboBox<String> type = TableFilter.combo("Todos os tipos", "Aprovações", "Stock", "Validades", "Assinatura");
        JComboBox<String> readState = TableFilter.combo("Lidas e por ler", UNREAD_LABEL, READ_LABEL);
        TableFilter.install(table, search,
                new TableFilter.ColumnFilter(type, 0),
                new TableFilter.ColumnFilter(readState, 4));

        ModernButton openButton = UIHelper.createPrimaryButton("Abrir módulo");
        openButton.setIcon(UIHelper.icon("fas-external-link-alt", 14));
        openButton.addActionListener(e -> openSelectedModule());

        refreshButton = UIHelper.createRefreshButton(() -> refreshData());

        readingMenu = UIHelper.createActionMenuButton("Leitura")
                .addAction("Marcar como lida", UIHelper.icon("fas-check", 14), this::markSelectedRead)
                .addAction("Marcar todas como lidas", UIHelper.icon("fas-check-double", 14), this::markAllRead);
        readingMenu.setActionEnabled(1, false);

        JPanel filters = UIHelper.filterBar(new JComponent[]{
                search, TableFilter.label("Tipo:"), type, TableFilter.label("Leitura:"), readState}, null);
        filters.setBorder(new EmptyBorder(0, 0, 10, 0));

        JPanel filterStack = new JPanel(new BorderLayout(0, 6));
        filterStack.setOpaque(false);
        filterStack.add(summaryLabel, BorderLayout.NORTH);
        filterStack.add(filters, BorderLayout.CENTER);
        card.add(UIHelper.tableCardTop("Notificações", filterStack, refreshButton, readingMenu, openButton), BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        card.add(scroll, BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);
    }

    public void onPanelSelected() {
        refreshData();
    }

    private void refreshData() {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        int version = ++refreshVersion;
        refreshButton.setEnabled(false);
        summaryLabel.setText("A carregar notificações…");
        UIHelper.loadAsync(this,
                () -> feed.load(companyId),
                loaded -> {
                    refreshButton.setEnabled(true);
                    if (version != refreshVersion) return;
                    setItems(loaded);
                },
                error -> {
                    refreshButton.setEnabled(true);
                    if (version != refreshVersion) return;
                    summaryLabel.setText("Não foi possível carregar as notificações.");
                    feedback.show(FeedbackType.ERROR, "Não foi possível carregar as notificações",
                            rootMessage(error), "Tentar novamente", NotificationsPanel.this::refreshData);
                });
    }

    void setItems(List<NotificationItem> loaded) {
        items = new ArrayList<>(loaded);
        renderRows();
    }

    private void renderRows() {
        tableModel.setRowCount(0);
        for (NotificationItem item : items) {
            tableModel.addRow(rowFor(item, readStore.isRead(item)));
        }
        int unread = readStore.unreadCount(items);
        summaryLabel.setText(summaryText(items.size(), unread));
        readingMenu.setActionEnabled(1, unread > 0);
        unreadCountListener.accept(unread);
    }

    static Object[] rowFor(NotificationItem item, boolean read) {
        return NotificationsViewCoordinator.rowFor(item, read);
    }

    static String summaryText(int total, int unread) {
        return NotificationsViewCoordinator.summaryText(total, unread);
    }

    private void markSelectedRead() {
        NotificationsViewCoordinator.markSelectedRead(table, items, readStore, feedback, this::renderRows);
    }

    private void markAllRead() {
        if (!items.isEmpty()) { readStore.markAllRead(items); renderRows(); }
    }

    private void openSelectedModule() {
        NotificationsViewCoordinator.openSelectedModule(table, items, navigator, feedback);
    }

    private static String rootMessage(Throwable ex) {
        Throwable c = ex;
        while (c.getCause() != null) c = c.getCause();
        return c.getMessage() == null ? "Falha inesperada." : c.getMessage();
    }
}
