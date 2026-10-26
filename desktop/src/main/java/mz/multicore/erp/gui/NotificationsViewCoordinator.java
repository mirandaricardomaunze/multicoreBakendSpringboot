package mz.multicore.erp.gui;

import mz.multicore.erp.gui.NotificationFeed.NotificationItem;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.InlineFeedbackPanel;
import mz.multicore.erp.gui.components.QuickPeekPanel;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.UIHelper;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.List;
import java.util.function.Consumer;

/**
 * Coordenador de apresentação, sumários textuais e ações de leitura do painel de notificações.
 */
public final class NotificationsViewCoordinator {

    public static final String READ_LABEL = "Lida";
    public static final String UNREAD_LABEL = "Por ler";

    private NotificationsViewCoordinator() {}

    public static Object[] rowFor(NotificationItem item, boolean read) {
        return new Object[]{item.type(), item.title(), item.detail(), item.when(),
                read ? READ_LABEL : UNREAD_LABEL};
    }

    public static String summaryText(int total, int unread) {
        if (total == 0) return "Não há notificações pendentes.";
        if (unread == 0) return total == 1 ? "1 notificação, já lida." : total + " notificações, todas lidas.";
        return unread + " por ler de " + total + (total == 1 ? " notificação." : " notificações.");
    }

    public static void populateQuickPeek(QuickPeekPanel peek, DefaultTableModel tableModel, int row, Runnable onOpenModule) {
        peek.setHeaderIcon("fas-bell", UIHelper.ACCENT_BLUE);
        peek.setTitle(String.valueOf(tableModel.getValueAt(row, 1)));
        peek.setSubtitle("Tipo: " + tableModel.getValueAt(row, 0) + " • " + tableModel.getValueAt(row, 3));
        peek.setStatus(String.valueOf(tableModel.getValueAt(row, 4)), UIHelper.ACCENT_BLUE);
        peek.setItems(List.of(
                new QuickPeekPanel.PeekItem("Tipo", String.valueOf(tableModel.getValueAt(row, 0)), false),
                new QuickPeekPanel.PeekItem("Data / Estado", String.valueOf(tableModel.getValueAt(row, 3)), false),
                new QuickPeekPanel.PeekItem("Detalhes Completos", String.valueOf(tableModel.getValueAt(row, 2)), true)));
        peek.getOpenFullButton().setText("Abrir Módulo");
        peek.setOnOpenFullAction(v -> onOpenModule.run());
    }

    public static void markSelectedRead(JTable table, List<NotificationItem> items,
                                        NotificationReadStore readStore,
                                        InlineFeedbackPanel feedback,
                                        Runnable onRender) {
        int row = TableFilter.selectedModelRow(table);
        if (row < 0) {
            feedback.show(FeedbackType.WARNING, "Seleccione uma notificação",
                    "Escolha uma notificação para marcar como lida.", null, null);
            return;
        }
        readStore.markRead(items.get(row));
        onRender.run();
    }

    public static void openSelectedModule(JTable table, List<NotificationItem> items,
                                          Consumer<String> navigator,
                                          InlineFeedbackPanel feedback) {
        int row = TableFilter.selectedModelRow(table);
        if (row < 0) {
            feedback.show(FeedbackType.WARNING, "Seleccione uma notificação",
                    "Escolha uma notificação para abrir o respectivo módulo.", null, null);
            return;
        }
        navigator.accept(items.get(row).moduleCard());
    }
}
