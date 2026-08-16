package mz.multicore.erp.gui;

import mz.multicore.erp.gui.NotificationFeed.NotificationItem;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes do estado lida/não-lida das notificações. Modo memória (Preferences null) para não poluir
 * as preferências do utilizador. Cenários NL-01..NL-05.
 */
class NotificationReadStoreTest {

    private static NotificationItem item(String type, String title) {
        return new NotificationItem(type, title, "detalhe", "quando", "card", 2);
    }

    private static NotificationReadStore memoryStore() {
        return new NotificationReadStore((Preferences) null);
    }

    @Test // NL-01
    void keyOf_combinaCamposVisiveis() {
        assertEquals("Stock|Stock baixo|detalhe|quando", NotificationReadStore.keyOf(item("Stock", "Stock baixo")));
    }

    @Test // NL-02
    void markRead_marcaSoAquele() {
        NotificationReadStore store = memoryStore();
        NotificationItem a = item("Stock", "A");
        NotificationItem b = item("Stock", "B");
        assertFalse(store.isRead(a));
        store.markRead(a);
        assertTrue(store.isRead(a));
        assertFalse(store.isRead(b));
    }

    @Test // NL-03
    void unread_eContagem() {
        NotificationReadStore store = memoryStore();
        NotificationItem a = item("Stock", "A");
        NotificationItem b = item("Stock", "B");
        store.markRead(a);
        assertEquals(1, store.unreadCount(List.of(a, b)));
        assertEquals(List.of(b), store.unread(List.of(a, b)));
    }

    @Test // NL-04
    void markAllRead_zeraContagem() {
        NotificationReadStore store = memoryStore();
        NotificationItem a = item("X", "A");
        NotificationItem b = item("Y", "B");
        store.markAllRead(List.of(a, b));
        assertEquals(0, store.unreadCount(List.of(a, b)));
    }

    @Test // NL-05
    void itensIdenticos_partilhamEstado() {
        NotificationReadStore store = memoryStore();
        store.markRead(item("Stock", "A"));
        // Uma nova instância com os mesmos campos é considerada a mesma notificação (chave estável).
        assertTrue(store.isRead(item("Stock", "A")));
    }
}
