package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Harness automatizado de validação do subsistema de Histórico de Itens Recentes (HARNESS-RHIS-001).
 * Valida a lógica de inserção LRU/MRU, persistência atómica, deduplicação e restrições de arquitetura.
 */
class RecentItemsHistoryHarnessTest {

    @TempDir
    Path tempDir;

    private File tempStorageFile;
    private RecentItemsHistoryManager manager;

    @BeforeEach
    void setUp() {
        tempStorageFile = tempDir.resolve("test_recent_items.json").toFile();
        manager = new RecentItemsHistoryManager(tempStorageFile, 5);
    }

    @AfterEach
    void tearDown() {
        if (manager != null) {
            manager.clear();
        }
    }

    @Test
    @DisplayName("RHIS-01: Inicialização do RecentItemsHistoryManager")
    void testInitialization() {
        assertNotNull(manager);
        assertEquals(5, manager.getCapacity());
        assertTrue(manager.getRecentItems().isEmpty());
    }

    @Test
    @DisplayName("RHIS-02: Inserção e Promoção MRU ao Topo")
    void testMruInsertionAndPromotion() {
        manager.record("Cliente", 101L, "Supermercado Recheio", "NUIT: 400123456", "clientes", "fas-user-tie");
        manager.record("Produto", 202L, "Cimento 50kg", "SKU: CIM-50", "stock", "fas-box");

        List<RecentItem> items = manager.getRecentItems();
        assertEquals(2, items.size());
        assertEquals("Cimento 50kg", items.get(0).title(), "O item mais recente deve estar no topo (índice 0)");
        assertEquals("Supermercado Recheio", items.get(1).title());

        // Re-inserir o primeiro item deve promovê-lo de volta ao topo
        manager.record("Cliente", 101L, "Supermercado Recheio", "NUIT: 400123456", "clientes", "fas-user-tie");
        items = manager.getRecentItems();
        assertEquals(2, items.size(), "Não deve haver duplicação");
        assertEquals("Supermercado Recheio", items.get(0).title(), "Item re-inserido deve ser promovido ao topo");
    }

    @Test
    @DisplayName("RHIS-03: Limite de Capacidade e Evicção LRU da Cauda")
    void testLruCapacityEviction() {
        // Capacidade definida em 5
        for (int i = 1; i <= 7; i++) {
            manager.record("Produto", (long) i, "Produto " + i, "Sub " + i, "stock", "fas-box");
        }

        List<RecentItem> items = manager.getRecentItems();
        assertEquals(5, items.size(), "A lista deve conter no máximo a capacidade configurada (5)");
        assertEquals("Produto 7", items.get(0).title(), "Item 7 é o mais recente");
        assertEquals("Produto 6", items.get(1).title());
        assertEquals("Produto 5", items.get(2).title());
        assertEquals("Produto 4", items.get(3).title());
        assertEquals("Produto 3", items.get(4).title(), "Item 3 é o mais antigo remanescente");
        // Itens 1 e 2 devem ter sido descartados pela regra LRU
        assertFalse(items.stream().anyMatch(it -> "Produto 1".equals(it.title())));
        assertFalse(items.stream().anyMatch(it -> "Produto 2".equals(it.title())));
    }

    @Test
    @DisplayName("RHIS-04: Imutabilidade e Proteção da Lista Exposta")
    void testListImmutability() {
        manager.record("Fatura", 501L, "FAT-2026/001", "Total: 15.000 MT", "comercial", "fas-file-invoice");
        List<RecentItem> items = manager.getRecentItems();

        assertThrows(UnsupportedOperationException.class, () ->
                items.add(new RecentItem("FAKE", "Fatura", "Fake", "", "comercial", 999L, "fas-box", System.currentTimeMillis()))
        );
    }

    @Test
    @DisplayName("RHIS-05: Formatação de Tempo Relativo Decorrido")
    void testTimeAgoFormatting() {
        long baseTime = 1_000_000_000L;
        RecentItem itemNow = new RecentItem("T1", "Geral", "Agora", "", "view", 1L, "fas-circle", baseTime);

        assertEquals("Agora mesmo", itemNow.formatTimeAgo(baseTime + 15 * 1000));
        assertEquals("há 5 min", itemNow.formatTimeAgo(baseTime + 5 * 60 * 1000));
        assertEquals("há 2 h", itemNow.formatTimeAgo(baseTime + 2 * 3600 * 1000));
        assertEquals("há 3 d", itemNow.formatTimeAgo(baseTime + 3 * 24 * 3600 * 1000));
    }

    @Test
    @DisplayName("RHIS-06: Persistência Atómica e Recarregamento Local em JSON")
    void testJsonPersistence() {
        manager.record("Cliente", 10L, "Empresa Alpha", "Matola", "clientes", "fas-building");
        manager.record("Fatura", 20L, "FAT-002", "Maputo", "comercial", "fas-file-invoice");

        assertTrue(tempStorageFile.exists(), "Ficheiro JSON de histórico deve ter sido criado");

        // Cria uma nova instância apontando para o mesmo ficheiro
        RecentItemsHistoryManager secondManager = new RecentItemsHistoryManager(tempStorageFile, 5);
        List<RecentItem> reloaded = secondManager.getRecentItems();

        assertEquals(2, reloaded.size());
        assertEquals("FAT-002", reloaded.get(0).title());
        assertEquals("Empresa Alpha", reloaded.get(1).title());
        assertEquals("clientes", reloaded.get(1).targetView());
        assertEquals(10L, reloaded.get(1).recordId());
    }

    @Test
    @DisplayName("RHIS-07: Operação de Limpeza (clear)")
    void testClearOperation() {
        manager.record("Módulo", null, "Stock & Armazéns", "Módulo", "stock", "fas-box");
        assertFalse(manager.getRecentItems().isEmpty());

        manager.clear();
        assertTrue(manager.getRecentItems().isEmpty());

        // Nova instância também deve estar vazia após carregar o ficheiro limpo
        RecentItemsHistoryManager freshManager = new RecentItemsHistoryManager(tempStorageFile, 5);
        assertTrue(freshManager.getRecentItems().isEmpty());
    }

    @Test
    @DisplayName("RHIS-08: Limite Estrito de Linhas de Código (<= 1000)")
    void testCodeLinesConstraint() throws Exception {
        List<String> filesToCheck = List.of(
                "desktop/src/main/java/mz/multicore/erp/gui/components/RecentItem.java",
                "desktop/src/main/java/mz/multicore/erp/gui/components/RecentItemsHistoryManager.java",
                "desktop/src/main/java/mz/multicore/erp/gui/components/RecentItemsDialog.java",
                "desktop/src/main/java/mz/multicore/erp/gui/MainFrame.java"
        );

        for (String relPath : filesToCheck) {
            File f = new File(relPath);
            if (!f.exists()) {
                f = new File("../" + relPath);
            }
            assertTrue(f.exists(), "Ficheiro deve existir: " + relPath);
            long lineCount = Files.lines(f.toPath()).count();
            assertTrue(lineCount <= 1000,
                    "A classe " + f.getName() + " ultrapassou o limite estrito de 1000 linhas! Atual: " + lineCount);
        }
    }
}
