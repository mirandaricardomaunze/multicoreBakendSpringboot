package com.phcpro.gui;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.gui.NotificationFeed.NotificationItem;
import com.phcpro.gui.components.ModernButton;
import com.phcpro.gui.components.ModernPanel;
import com.phcpro.gui.components.TableFilter;
import com.phcpro.gui.components.UIHelper;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
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

    static final String READ_LABEL = "Lida";
    static final String UNREAD_LABEL = "Por ler";

    private final NotificationFeed feed;
    private final NotificationReadStore readStore;
    private final Consumer<String> navigator;
    private final IntConsumer unreadCountListener;
    private final DefaultTableModel tableModel;
    private final JTable table;
    private final JLabel summaryLabel;
    private final ModernButton refreshButton;
    private final ModernButton markReadButton;
    private final ModernButton markAllButton;
    private List<NotificationItem> items = new ArrayList<>();
    private int refreshVersion;

    /**
     * @param onUnreadCountChanged recebe o número de não-lidas sempre que a lista ou o estado muda
     *                             (o {@code MainFrame} liga-o ao badge do sino).
     */
    public NotificationsPanel(NotificationFeed feed, NotificationReadStore readStore,
                              Consumer<String> navigator, IntConsumer onUnreadCountChanged) {
        this.feed = feed;
        this.readStore = readStore;
        this.navigator = navigator;
        this.unreadCountListener = onUnreadCountChanged;
        setLayout(new BorderLayout(0, 16));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(22, 25, 22, 25));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel titles = new JPanel();
        titles.setOpaque(false);
        titles.setLayout(new javax.swing.BoxLayout(titles, javax.swing.BoxLayout.Y_AXIS));
        JLabel title = UIHelper.createHeading("Notificações");
        summaryLabel = new JLabel("Alertas operacionais da empresa ativa");
        summaryLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 13));
        summaryLabel.setForeground(UIHelper.TEXT_MUTED);
        titles.add(title);
        titles.add(summaryLabel);
        header.add(titles, BorderLayout.WEST);
        add(header, BorderLayout.NORTH);

        ModernPanel card = new ModernPanel(16);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(new EmptyBorder(18, 18, 18, 18));

        String[] columns = {"Tipo", "Notificação", "Detalhes", "Data / Estado", "Leitura"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(tableModel);
        UIHelper.styleTable(table);
        table.setFillsViewportHeight(true);
        table.putClientProperty("emptyText", "Sem notificações.");
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.getColumnModel().getColumn(1).setPreferredWidth(240);
        table.getColumnModel().getColumn(2).setPreferredWidth(330);
        table.getColumnModel().getColumn(3).setPreferredWidth(120);
        table.getColumnModel().getColumn(4).setPreferredWidth(80);

        JTextField search = TableFilter.searchField("Pesquisar notificações…");
        JComboBox<String> type = TableFilter.combo("Todos os tipos",
                "Aprovações", "Stock", "Validades", "Assinatura");
        JComboBox<String> readState = TableFilter.combo("Lidas e por ler", UNREAD_LABEL, READ_LABEL);
        TableFilter.install(table, search,
                new TableFilter.ColumnFilter(type, 0),
                new TableFilter.ColumnFilter(readState, 4));
        JPanel filterBar = TableFilter.bar(search, TableFilter.label("Tipo:"), type,
                TableFilter.label("Leitura:"), readState);
        filterBar.setBorder(new EmptyBorder(0, 0, 10, 0));
        card.add(filterBar, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(table);
        UIHelper.styleScrollPane(scroll);
        card.add(scroll, BorderLayout.CENTER);

        ModernButton openButton = UIHelper.createPrimaryButton("Abrir módulo");
        openButton.setIcon(UIHelper.icon("fas-external-link-alt", 14));
        openButton.addActionListener(e -> openSelectedModule());
        markReadButton = UIHelper.createSecondaryButton("Marcar como lida");
        markReadButton.setIcon(UIHelper.icon("fas-check", 14));
        markReadButton.addActionListener(e -> markSelectedRead());
        markAllButton = UIHelper.createSecondaryButton("Marcar todas como lidas");
        markAllButton.setIcon(UIHelper.icon("fas-check-double", 14));
        markAllButton.addActionListener(e -> markAllRead());
        markAllButton.setEnabled(false);
        refreshButton = UIHelper.createSecondaryButton("Atualizar");
        refreshButton.setIcon(UIHelper.icon("fas-sync-alt", 14));
        refreshButton.addActionListener(e -> refreshData());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.add(openButton);
        actions.add(markReadButton);
        actions.add(markAllButton);
        actions.add(refreshButton);
        card.add(actions, BorderLayout.SOUTH);
        add(card, BorderLayout.CENTER);
    }

    public void onPanelSelected() {
        refreshData();
    }

    /**
     * Carregamento pelo caminho canónico ({@code UIHelper.loadAsync}): cursor de espera,
     * propagação do contexto de utilizador/empresa e entrega do erro no EDT, tudo num só sítio
     * em vez de um {@code SwingWorker} escrito à mão aqui.
     *
     * <p>O contador de versão mantém-se: o {@code loadAsync} descarta respostas de um tenant que
     * deixou de estar activo, mas não respostas fora de ordem <b>da mesma empresa</b> — é o que
     * acontece a carregar "Atualizar" duas vezes seguidas.
     */
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
                    JOptionPane.showMessageDialog(NotificationsPanel.this,
                            "Erro ao carregar notificações: " + rootMessage(error),
                            "Erro", JOptionPane.ERROR_MESSAGE);
                });
    }

    void setItems(List<NotificationItem> loaded) {
        items = new ArrayList<>(loaded);
        renderRows();
    }

    /** Repinta a tabela a partir de {@link #items} + estado de leitura (sem ir à rede). */
    private void renderRows() {
        tableModel.setRowCount(0);
        for (NotificationItem item : items) {
            tableModel.addRow(rowFor(item, readStore.isRead(item)));
        }
        int unread = readStore.unreadCount(items);
        summaryLabel.setText(summaryText(items.size(), unread));
        markAllButton.setEnabled(unread > 0);
        unreadCountListener.accept(unread);
    }

    /** Linha da tabela — pura, testável (NL-06). */
    static Object[] rowFor(NotificationItem item, boolean read) {
        return new Object[]{item.type(), item.title(), item.detail(), item.when(),
                read ? READ_LABEL : UNREAD_LABEL};
    }

    /** Texto do resumo — puro, testável (NL-07). */
    static String summaryText(int total, int unread) {
        if (total == 0) return "Não há notificações pendentes.";
        if (unread == 0) return total == 1 ? "1 notificação, já lida." : total + " notificações, todas lidas.";
        return unread + " por ler de " + total + (total == 1 ? " notificação." : " notificações.");
    }

    private void markSelectedRead() {
        int row = TableFilter.selectedModelRow(table);
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma notificação para marcar como lida.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        readStore.markRead(items.get(row));
        renderRows();
    }

    private void markAllRead() {
        if (items.isEmpty()) return;
        readStore.markAllRead(items);
        renderRows();
    }

    private void openSelectedModule() {
        int row = TableFilter.selectedModelRow(table);
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma notificação para abrir o respetivo módulo.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        navigator.accept(items.get(row).moduleCard());
    }

    private static String rootMessage(Throwable ex) {
        Throwable cause = ex;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() == null ? "Falha inesperada." : cause.getMessage();
    }
}
