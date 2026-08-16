package mz.multicore.erp.gui;

import mz.multicore.erp.gui.NotificationFeed.NotificationItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;

/**
 * Estado <b>lida / não-lida</b> das notificações — do lado do cliente, porque as notificações são
 * <b>derivadas</b> (agregam alertas em tempo real: aprovações, stock, validades, assinatura), e não
 * entidades persistidas com id. Cada item tem uma chave estável (combinação dos campos visíveis) e
 * as chaves das lidas são guardadas nas Preferences do utilizador, para sobreviverem ao reinício.
 *
 * <p>À prova de falha: se as Preferences não estiverem disponíveis, funciona só em memória. Ver
 * docs/NOTIFICACOES_LIDAS_SPEC.md.</p>
 */
public final class NotificationReadStore {

    private static final String NODE = "mz/multicore/erp/gui/notifications";
    private static final String KEY = "readKeys";
    private static final int MAX_KEYS = 200; // as notificações antigas deixam de existir na origem

    private final Preferences prefs; // nulo = modo memória (testes / Preferences indisponíveis)
    private final Set<String> read = new LinkedHashSet<>();

    public NotificationReadStore() {
        this(safeNode());
    }

    NotificationReadStore(Preferences prefs) {
        this.prefs = prefs;
        if (prefs != null) {
            String stored = prefs.get(KEY, "");
            if (!stored.isBlank()) {
                for (String k : stored.split("\n")) {
                    if (!k.isBlank()) read.add(k);
                }
            }
        }
    }

    /** Identidade estável de uma notificação derivada (não há id — combina os campos visíveis). */
    public static String keyOf(NotificationItem item) {
        return item.type() + "|" + item.title() + "|" + item.detail() + "|" + item.when();
    }

    public boolean isRead(NotificationItem item) {
        return read.contains(keyOf(item));
    }

    public void markRead(NotificationItem item) {
        if (read.add(keyOf(item))) save();
    }

    public void markAllRead(Collection<NotificationItem> items) {
        boolean changed = false;
        for (NotificationItem item : items) {
            changed |= read.add(keyOf(item));
        }
        if (changed) save();
    }

    /** Subconjunto ainda por ler, preservando a ordem de entrada. */
    public List<NotificationItem> unread(List<NotificationItem> items) {
        return items.stream().filter(i -> !isRead(i)).toList();
    }

    public int unreadCount(List<NotificationItem> items) {
        return (int) items.stream().filter(i -> !isRead(i)).count();
    }

    private void save() {
        if (prefs == null) return;
        // Mantém só as últimas MAX_KEYS (evita crescer sem limite nas Preferences).
        if (read.size() > MAX_KEYS) {
            List<String> keys = new ArrayList<>(read);
            keys = keys.subList(keys.size() - MAX_KEYS, keys.size());
            read.clear();
            read.addAll(keys);
        }
        try {
            prefs.put(KEY, String.join("\n", read));
        } catch (Exception ignored) {
            // Preferences indisponíveis / valor demasiado longo não devem rebentar a UI.
        }
    }

    private static Preferences safeNode() {
        try {
            return Preferences.userRoot().node(NODE);
        } catch (Exception ex) {
            return null; // sem persistência, mas continua a funcionar em memória
        }
    }
}
