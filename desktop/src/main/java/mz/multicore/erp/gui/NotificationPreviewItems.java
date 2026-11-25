package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import java.util.function.Consumer;

/** Itens do popup de pré-visualização do sino: entrada por notificação (abrir / marcar como lida) e linhas informativas. */
final class NotificationPreviewItems {

    private static final int MAX_TITLE_LENGTH = 44;

    private NotificationPreviewItems() {}

    /** Linha informativa não clicável (a carregar, vazio, erro). */
    static JMenuItem disabled(String text) {
        JMenuItem item = new JMenuItem(text);
        item.setEnabled(false);
        return item;
    }

    /** Submenu de uma notificação por ler, com "Abrir módulo" e "Marcar como lida". */
    static JMenu itemMenu(NotificationFeed.NotificationItem item, Consumer<String> onNavigate, Runnable onMarkRead) {
        JMenu itemMenu = new JMenu(item.type() + " · " + shorten(item.title(), MAX_TITLE_LENGTH));
        itemMenu.setToolTipText(item.detail());
        JMenuItem open = new JMenuItem("Abrir módulo", UIHelper.icon("fas-external-link-alt", 12));
        open.addActionListener(e -> onNavigate.accept(item.moduleCard()));
        JMenuItem markRead = new JMenuItem("Marcar como lida", UIHelper.icon("fas-check", 12));
        markRead.addActionListener(e -> onMarkRead.run());
        itemMenu.add(open);
        itemMenu.add(markRead);
        return itemMenu;
    }

    static String shorten(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value == null ? "" : value;
        return value.substring(0, maxLength - 1) + "…";
    }
}
