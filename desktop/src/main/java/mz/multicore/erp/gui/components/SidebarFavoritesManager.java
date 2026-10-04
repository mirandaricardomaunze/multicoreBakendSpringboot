package mz.multicore.erp.gui.components;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gestor thread-safe de Atalhos Favoritos da Barra Lateral executiva.
 * Persiste as escolhas do utilizador em formato JSON no diretório de dados do utilizador.
 */
public class SidebarFavoritesManager {

    private static final Logger LOGGER = Logger.getLogger(SidebarFavoritesManager.class.getName());
    private static final List<String> DEFAULT_FAVORITES = List.of("Painel Inicial", "POS — Caixa", "Stock & Armazéns");
    private static final SidebarFavoritesManager INSTANCE = new SidebarFavoritesManager();

    private final List<String> favorites = new CopyOnWriteArrayList<>();
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();
    private final File storageFile;

    private SidebarFavoritesManager() {
        String home = System.getProperty("user.home", ".");
        File dir = new File(home, ".multicore");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        this.storageFile = new File(dir, "user_favorites.json");
        loadFavorites();
    }

    public static SidebarFavoritesManager getInstance() {
        return INSTANCE;
    }

    public List<String> getFavorites() {
        return Collections.unmodifiableList(new ArrayList<>(favorites));
    }

    public boolean isFavorite(String label) {
        if (label == null || label.isBlank()) return false;
        return favorites.stream().anyMatch(f -> f.equalsIgnoreCase(label.trim()));
    }

    public void toggleFavorite(String label) {
        if (label == null || label.isBlank()) return;
        String trimmed = label.trim();
        if (isFavorite(trimmed)) {
            removeFavorite(trimmed);
        } else {
            addFavorite(trimmed);
        }
    }

    public void addFavorite(String label) {
        if (label == null || label.isBlank()) return;
        String trimmed = label.trim();
        if (!isFavorite(trimmed)) {
            favorites.add(trimmed);
            saveFavorites();
            notifyListeners();
        }
    }

    public void removeFavorite(String label) {
        if (label == null || label.isBlank()) return;
        String trimmed = label.trim();
        boolean removed = favorites.removeIf(f -> f.equalsIgnoreCase(trimmed));
        if (removed) {
            saveFavorites();
            notifyListeners();
        }
    }

    public void setFavorites(List<String> newFavs) {
        favorites.clear();
        if (newFavs != null) {
            for (String f : newFavs) {
                if (f != null && !f.isBlank()) {
                    favorites.add(f.trim());
                }
            }
        }
        saveFavorites();
        notifyListeners();
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
                LOGGER.log(Level.WARNING, "Erro ao notificar ouvinte de alteração de favoritos: " + ex.getMessage(), ex);
            }
        }
    }

    private synchronized void loadFavorites() {
        favorites.clear();
        if (!storageFile.exists()) {
            favorites.addAll(DEFAULT_FAVORITES);
            saveFavorites();
            return;
        }

        try (FileReader reader = new FileReader(storageFile, StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[1024];
            int read;
            while ((read = reader.read(buf)) != -1) {
                sb.append(buf, 0, read);
            }
            String content = sb.toString().trim();
            if (content.startsWith("[") && content.endsWith("]")) {
                String inner = content.substring(1, content.length() - 1);
                String[] parts = inner.split(",");
                for (String p : parts) {
                    String clean = p.trim().replace("\"", "");
                    if (!clean.isBlank()) {
                        favorites.add(clean);
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Falha ao ler ficheiro de favoritos. A restaurar padrões: " + ex.getMessage());
            favorites.clear();
            favorites.addAll(DEFAULT_FAVORITES);
        }

        if (favorites.isEmpty()) {
            favorites.addAll(DEFAULT_FAVORITES);
        }
    }

    private synchronized void saveFavorites() {
        try (FileWriter writer = new FileWriter(storageFile, StandardCharsets.UTF_8)) {
            StringBuilder sb = new StringBuilder();
            sb.append("[\n");
            for (int i = 0; i < favorites.size(); i++) {
                sb.append("  \"").append(favorites.get(i).replace("\"", "\\\"")).append("\"");
                if (i < favorites.size() - 1) {
                    sb.append(",");
                }
                sb.append("\n");
            }
            sb.append("]");
            writer.write(sb.toString());
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Erro ao guardar preferências de favoritos: " + ex.getMessage(), ex);
        }
    }
}
