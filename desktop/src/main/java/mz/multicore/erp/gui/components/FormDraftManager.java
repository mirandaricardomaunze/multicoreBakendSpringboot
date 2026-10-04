package mz.multicore.erp.gui.components;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import mz.multicore.erp.architecture.security.CurrentUserContext;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gestor thread-safe de Auto-Salvamento e Recuperação de Rascunhos de Formulários.
 * Mantém rascunhos em memória e persiste em ficheiros JSON individuais no disco local.
 */
public class FormDraftManager {

    private static final Logger LOGGER = Logger.getLogger(FormDraftManager.class.getName());
    private static final FormDraftManager INSTANCE = new FormDraftManager();

    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final Map<String, FormDraft> memoryCache = new ConcurrentHashMap<>();
    private final List<Runnable> changeListeners = new CopyOnWriteArrayList<>();
    private final File draftsDir;

    private FormDraftManager() {
        this(resolveDefaultDraftsDir());
    }

    public FormDraftManager(File draftsDir) {
        this.draftsDir = draftsDir;
        if (!this.draftsDir.exists()) {
            this.draftsDir.mkdirs();
        }
        loadAllFromDisk();
    }

    public static FormDraftManager getInstance() {
        return INSTANCE;
    }

    private static File resolveDefaultDraftsDir() {
        String home = System.getProperty("user.home", ".");
        File multicoreDir = new File(home, ".multicore");
        File drafts = new File(multicoreDir, "drafts");
        if (!drafts.exists()) {
            drafts.mkdirs();
        }
        return drafts;
    }

    public void saveDraft(String formKey, String title, Map<String, String> payload) {
        if (formKey == null || formKey.isBlank() || payload == null || payload.isEmpty()) {
            return;
        }
        String author = CurrentUserContext.getUsername();
        if (author == null || author.isBlank()) {
            author = "operador";
        }
        FormDraft draft = new FormDraft(formKey.trim(), title, payload, System.currentTimeMillis(), author);
        saveDraft(draft);
    }

    public synchronized void saveDraft(FormDraft draft) {
        if (draft == null) {
            return;
        }
        memoryCache.put(draft.formKey(), draft);
        writeToDisk(draft);
        notifyListeners();
    }

    public synchronized FormDraft getDraft(String formKey) {
        if (formKey == null) return null;
        return memoryCache.get(formKey.trim());
    }

    public boolean hasDraft(String formKey) {
        if (formKey == null) return false;
        return memoryCache.containsKey(formKey.trim());
    }

    public synchronized void clearDraft(String formKey) {
        if (formKey == null) return;
        String cleanKey = formKey.trim();
        memoryCache.remove(cleanKey);
        deleteFromDisk(cleanKey);
        notifyListeners();
    }

    public synchronized List<FormDraft> listAllDrafts() {
        return Collections.unmodifiableList(new ArrayList<>(memoryCache.values()));
    }

    public synchronized void clearAll() {
        memoryCache.clear();
        File[] files = draftsDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files != null) {
            for (File file : files) {
                try {
                    file.delete();
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, "Erro ao apagar rascunho: " + file.getName(), ex);
                }
            }
        }
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
                LOGGER.log(Level.WARNING, "Erro ao notificar ouvinte de rascunhos: " + ex.getMessage(), ex);
            }
        }
    }

    private File fileFor(String formKey) {
        // Sanitiza a chave para nome de ficheiro seguro
        String safeName = formKey.replaceAll("[^a-zA-Z0-9._-]", "_") + ".json";
        return new File(draftsDir, safeName);
    }

    private void writeToDisk(FormDraft draft) {
        try {
            File target = fileFor(draft.formKey());
            mapper.writeValue(target, draft);
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Falha ao gravar rascunho em disco para: " + draft.formKey(), ex);
        }
    }

    private void deleteFromDisk(String formKey) {
        try {
            File target = fileFor(formKey);
            if (target.exists()) {
                target.delete();
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Falha ao eliminar ficheiro de rascunho para: " + formKey, ex);
        }
    }

    private void loadAllFromDisk() {
        memoryCache.clear();
        if (!draftsDir.exists()) {
            return;
        }
        File[] files = draftsDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            try {
                FormDraft draft = mapper.readValue(file, FormDraft.class);
                if (draft != null && draft.formKey() != null) {
                    memoryCache.put(draft.formKey(), draft);
                }
            } catch (Exception ex) {
                LOGGER.log(Level.FINE, "Ficheiro ignorado ou corrompido: " + file.getName());
            }
        }
    }
}
