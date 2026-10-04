package mz.multicore.erp.gui.components;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gestor thread-safe do Histórico de Itens Recentes e Quick-Recall do Multicore ERP.
 * Mantém uma lista ordenada (MRU no topo, LRU na cauda) com capacidade máxima configurável
 * e persistência atómica local em JSON.
 */
public class RecentItemsHistoryManager {

    private static final Logger LOGGER = Logger.getLogger(RecentItemsHistoryManager.class.getName());
    public static final int DEFAULT_CAPACITY = 20;
    private static final RecentItemsHistoryManager INSTANCE = new RecentItemsHistoryManager();

    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final List<RecentItem> items = new ArrayList<>();
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();
    private final File storageFile;
    private final int capacity;

    private RecentItemsHistoryManager() {
        this(resolveDefaultFile(), DEFAULT_CAPACITY);
    }

    public RecentItemsHistoryManager(File storageFile, int capacity) {
        this.storageFile = storageFile;
        this.capacity = capacity > 0 ? capacity : DEFAULT_CAPACITY;
        loadHistory();
    }

    public static RecentItemsHistoryManager getInstance() {
        return INSTANCE;
    }

    private static File resolveDefaultFile() {
        String home = System.getProperty("user.home", ".");
        File dir = new File(home, ".multicore");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, "recent_items.json");
    }

    /**
     * Regista uma entrada rápida no histórico recente.
     */
    public void record(String category, Long recordId, String title, String subtitle, String targetView, String iconCode) {
        String id = (category != null ? category.toUpperCase() : "ITEM") + ":" +
                (targetView != null ? targetView : "") + ":" +
                (recordId != null ? recordId : title);
        record(new RecentItem(id, category, title, subtitle, targetView, recordId, iconCode, System.currentTimeMillis()));
    }

    /**
     * Insere ou promove um item ao topo da lista (MRU).
     */
    public synchronized void record(RecentItem item) {
        if (item == null) {
            return;
        }

        // Remove item duplicado com mesma chave identificadora
        items.removeIf(existing -> existing.id().equalsIgnoreCase(item.id()));

        // Insere no topo
        items.add(0, item);

        // Despeja os itens que excedem a capacidade máxima (LRU)
        while (items.size() > capacity) {
            items.remove(items.size() - 1);
        }

        saveHistory();
        notifyListeners();
    }

    public synchronized List<RecentItem> getRecentItems() {
        return Collections.unmodifiableList(new ArrayList<>(items));
    }

    public synchronized void remove(String id) {
        if (id == null) return;
        boolean removed = items.removeIf(i -> i.id().equalsIgnoreCase(id));
        if (removed) {
            saveHistory();
            notifyListeners();
        }
    }

    public synchronized void clear() {
        items.clear();
        saveHistory();
        notifyListeners();
    }

    public int getCapacity() {
        return capacity;
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null && !changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }

    public void removeChangeListener(Runnable listener) {
        if (listener != null) {
            changeListeners.remove(listener);
        }
    }

    private void notifyListeners() {
        for (Runnable listener : changeListeners) {
            try {
                listener.run();
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Erro ao notificar ouvinte de histórico recente: " + ex.getMessage(), ex);
            }
        }
    }

    private synchronized void loadHistory() {
        items.clear();
        if (storageFile == null || !storageFile.exists()) {
            return;
        }

        try {
            List<RecentItem> loaded = mapper.readValue(storageFile, new TypeReference<List<RecentItem>>() {});
            if (loaded != null) {
                for (RecentItem item : loaded) {
                    if (item != null && items.size() < capacity) {
                        items.add(item);
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Falha ao carregar histórico recente de disco: " + ex.getMessage());
            items.clear();
        }
    }

    private synchronized void saveHistory() {
        if (storageFile == null) {
            return;
        }
        try {
            File parent = storageFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            mapper.writeValue(storageFile, items);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Falha ao guardar histórico recente em disco: " + ex.getMessage(), ex);
        }
    }
}
