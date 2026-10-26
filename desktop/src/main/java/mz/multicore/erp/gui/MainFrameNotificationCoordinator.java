package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.CollapsibleSidebar;
import mz.multicore.erp.gui.components.TopNavBar;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

/** Coordenador do sino de notificações da barra superior, popups de pré-visualização e badges de alerta. */
final class MainFrameNotificationCoordinator {

    private final NotificationFeed notificationFeed;
    private final NotificationReadStore notificationReadStore;
    private final JLabel notificationBadgeLabel = new JLabel("0");
    private final Consumer<String> onNavigate;
    private TopNavBar topBar;
    private CollapsibleSidebar sidebar;
    private int notificationBadgeLoadVersion;

    MainFrameNotificationCoordinator(NotificationFeed notificationFeed, NotificationReadStore notificationReadStore,
                                     Consumer<String> onNavigate) {
        this.notificationFeed = notificationFeed;
        this.notificationReadStore = notificationReadStore;
        this.onNavigate = onNavigate;
        initBadgeLabel();
    }

    void attachNavComponents(TopNavBar topBar, CollapsibleSidebar sidebar) {
        this.topBar = topBar;
        this.sidebar = sidebar;
    }

    private void initBadgeLabel() {
        notificationBadgeLabel.setOpaque(true);
        notificationBadgeLabel.setBackground(UIHelper.REJECTED_RED);
        notificationBadgeLabel.setForeground(Color.WHITE);
        notificationBadgeLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 9));
        notificationBadgeLabel.setHorizontalAlignment(JLabel.CENTER);
        notificationBadgeLabel.setBorder(BorderFactory.createEmptyBorder(1, 4, 1, 4));
        notificationBadgeLabel.setVisible(false);
    }

    JComponent buildNotificationBell() {
        JPanel bell = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 5));
        bell.setOpaque(false);
        bell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bell.setToolTipText("Notificações");

        JLabel icon = new JLabel(UIHelper.icon("fas-bell", 19, UIHelper.TEXT_MUTED));
        bell.add(icon);
        bell.add(notificationBadgeLabel);

        bell.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent e) { showNotificationPreview(bell); }
        });
        SwingUtilities.invokeLater(this::refreshNotificationBadgeAsync);
        return bell;
    }

    void showNotificationPreview(JComponent anchor) {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null) return;
        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.BORDER),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        JMenuItem loading = new JMenuItem("A carregar notificações…");
        loading.setEnabled(false);
        popup.add(loading);
        popup.show(anchor, -220, anchor.getHeight());

        new SwingWorker<List<NotificationFeed.NotificationItem>, Void>() {
            @Override protected List<NotificationFeed.NotificationItem> doInBackground() {
                return notificationFeed.load(companyId);
            }

            @Override protected void done() {
                try {
                    List<NotificationFeed.NotificationItem> items = get();
                    List<NotificationFeed.NotificationItem> unread = notificationReadStore.unread(items);
                    updateNotificationBadge(unread.size());
                    popup.removeAll();
                    if (unread.isEmpty()) {
                        JMenuItem empty = new JMenuItem("Não há notificações por ler.");
                        empty.setEnabled(false);
                        popup.add(empty);
                    } else {
                        unread.stream().limit(5).forEach(item -> {
                            JMenu itemMenu = new JMenu(item.type() + " · " + shorten(item.title(), 44));
                            itemMenu.setToolTipText(item.detail());
                            JMenuItem open = new JMenuItem("Abrir módulo", UIHelper.icon("fas-external-link-alt", 12));
                            open.addActionListener(e -> onNavigate.accept(item.moduleCard()));
                            JMenuItem markRead = new JMenuItem("Marcar como lida", UIHelper.icon("fas-check", 12));
                            markRead.addActionListener(e -> {
                                notificationReadStore.markRead(item);
                                updateNotificationBadge(notificationReadStore.unreadCount(items));
                                popup.setVisible(false);
                            });
                            itemMenu.add(open);
                            itemMenu.add(markRead);
                            popup.add(itemMenu);
                        });
                    }
                    popup.addSeparator();
                    JMenuItem markAll = new JMenuItem("Marcar todas como lidas", UIHelper.icon("fas-check-double", 13));
                    markAll.setEnabled(!unread.isEmpty());
                    markAll.addActionListener(e -> {
                        notificationReadStore.markAllRead(items);
                        updateNotificationBadge(0);
                        popup.setVisible(false);
                    });
                    popup.add(markAll);
                    JMenuItem viewAll = new JMenuItem("Ver todas", UIHelper.icon("fas-list", 13));
                    viewAll.addActionListener(e -> {
                        if (topBar != null) topBar.setActive("__notifications__");
                        onNavigate.accept("notifications");
                    });
                    popup.add(viewAll);
                    popup.setVisible(false);
                    popup.show(anchor, -220, anchor.getHeight());
                } catch (Exception ex) {
                    popup.removeAll();
                    JMenuItem error = new JMenuItem("Não foi possível carregar as notificações.");
                    error.setEnabled(false);
                    popup.add(error);
                    popup.setVisible(false);
                    popup.show(anchor, -260, anchor.getHeight());
                }
            }
        }.execute();
    }

    void refreshNotificationBadgeAsync() {
        if (notificationFeed == null) return;
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null) return;
        int version = ++notificationBadgeLoadVersion;
        new SwingWorker<Integer, Void>() {
            @Override protected Integer doInBackground() {
                return notificationReadStore.unreadCount(notificationFeed.load(companyId));
            }

            @Override protected void done() {
                if (version != notificationBadgeLoadVersion) return;
                try {
                    updateNotificationBadge(get());
                } catch (Exception ignored) {
                    updateNotificationBadge(0);
                }
            }
        }.execute();
    }

    void updateNotificationBadge(int count) {
        if (notificationBadgeLabel != null) {
            notificationBadgeLabel.setText(count > 99 ? "99+" : String.valueOf(count));
            notificationBadgeLabel.setVisible(count > 0);
            notificationBadgeLabel.setToolTipText(count + " notificação" + (count == 1 ? " pendente" : " pendentes"));
        }
        if (sidebar != null) {
            sidebar.setBadge("Notificações", count);
        }
    }

    private static String shorten(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value == null ? "" : value;
        return value.substring(0, maxLength - 1) + "…";
    }
}
