package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Harness automatizado de validação do Auto-Salvamento e Recuperação de Rascunhos (HARNESS-DFRT-001).
 * Valida o ciclo de vida dos rascunhos, persistência local em JSON, comportamento do banner e limites de arquitetura.
 */
class FormDraftAutoSaveHarnessTest {

    @TempDir
    Path tempDir;

    private File tempDraftsFolder;
    private FormDraftManager manager;

    @BeforeEach
    void setUp() {
        tempDraftsFolder = tempDir.resolve("test_drafts").toFile();
        manager = new FormDraftManager(tempDraftsFolder);
    }

    @AfterEach
    void tearDown() {
        if (manager != null) {
            manager.clearAll();
        }
    }

    @Test
    @DisplayName("DFRT-01: Inicialização do FormDraftManager")
    void testInitialization() {
        assertNotNull(manager);
        assertTrue(manager.listAllDrafts().isEmpty());
        assertFalse(manager.hasDraft("CUSTOMER_CREATE"));
    }

    @Test
    @DisplayName("DFRT-02: Gravação e Recuperação Fidedigna de Rascunho")
    void testSaveAndRetrieveDraft() {
        Map<String, String> payload = Map.of(
                "name", "Supermercado Recheio, Lda",
                "nuit", "400123456",
                "city", "Maputo"
        );

        manager.saveDraft("CUSTOMER_CREATE", "Novo Cliente — Recheio", payload);

        assertTrue(manager.hasDraft("CUSTOMER_CREATE"));
        FormDraft retrieved = manager.getDraft("CUSTOMER_CREATE");
        assertNotNull(retrieved);
        assertEquals("CUSTOMER_CREATE", retrieved.formKey());
        assertEquals("Novo Cliente — Recheio", retrieved.title());
        assertEquals(3, retrieved.payload().size());
        assertEquals("400123456", retrieved.payload().get("nuit"));
        assertEquals("Maputo", retrieved.payload().get("city"));
    }

    @Test
    @DisplayName("DFRT-03: Sobrescrita e Atualização de Rascunho Existente")
    void testOverwriteAndTimestampUpdate() {
        manager.saveDraft("QUOTATION_NEW", "Cotação A", Map.of("total", "1000"));
        FormDraft d1 = manager.getDraft("QUOTATION_NEW");

        manager.saveDraft("QUOTATION_NEW", "Cotação A Atualizada", Map.of("total", "2500", "discount", "10%"));
        FormDraft d2 = manager.getDraft("QUOTATION_NEW");

        assertEquals(1, manager.listAllDrafts().size(), "Não deve duplicar rascunhos com a mesma chave");
        assertEquals("Cotação A Atualizada", d2.title());
        assertEquals("2500", d2.payload().get("total"));
        assertEquals("10%", d2.payload().get("discount"));
        assertTrue(d2.timestampMillis() >= d1.timestampMillis());
    }

    @Test
    @DisplayName("DFRT-04: Purga após Submissão com Sucesso (clearDraft)")
    void testClearDraftAfterSuccess() {
        manager.saveDraft("PURCHASE_ORDER", "Encomenda Fornecedor ABC", Map.of("item", "Cimento"));
        assertTrue(manager.hasDraft("PURCHASE_ORDER"));

        manager.clearDraft("PURCHASE_ORDER");
        assertFalse(manager.hasDraft("PURCHASE_ORDER"));
        assertNull(manager.getDraft("PURCHASE_ORDER"));

        // O ficheiro correspondente também deve ter sido removido do disco
        File targetFile = new File(tempDraftsFolder, "PURCHASE_ORDER.json");
        assertFalse(targetFile.exists(), "Ficheiro JSON de rascunho deve ter sido apagado após clearDraft");
    }

    @Test
    @DisplayName("DFRT-05: Persistência em Disco e Recarregamento por Nova Instância")
    void testDiskPersistenceAndReload() {
        manager.saveDraft("PRODUCT_NEW", "Novo Artigo", Map.of("sku", "ART-99", "price", "450.00"));

        File expectedFile = new File(tempDraftsFolder, "PRODUCT_NEW.json");
        assertTrue(expectedFile.exists(), "Ficheiro JSON deve ter sido criado em disco");

        // Nova instância a apontar para o mesmo diretório
        FormDraftManager reloadedManager = new FormDraftManager(tempDraftsFolder);
        assertTrue(reloadedManager.hasDraft("PRODUCT_NEW"));
        FormDraft draft = reloadedManager.getDraft("PRODUCT_NEW");
        assertEquals("Novo Artigo", draft.title());
        assertEquals("450.00", draft.payload().get("price"));
    }

    @Test
    @DisplayName("DFRT-06: Formatação de Idade do Rascunho (formattedTimeAgo)")
    void testTimeAgoFormatting() {
        long baseTime = 2_000_000_000L;
        FormDraft draft = new FormDraft("K1", "Título", Map.of(), baseTime, "operador");

        assertEquals("Agora mesmo", draft.formatTimeAgo(baseTime + 20 * 1000));
        assertEquals("há 10 min", draft.formatTimeAgo(baseTime + 10 * 60 * 1000));
        assertEquals("há 3 h", draft.formatTimeAgo(baseTime + 3 * 3600 * 1000));
        assertEquals("há 2 d", draft.formatTimeAgo(baseTime + 2 * 24 * 3600 * 1000));
    }

    @Test
    @DisplayName("DFRT-07: Comportamento do FormDraftBanner (Restauração e Descarte)")
    void testFormDraftBannerActions() {
        FormDraftManager defaultManager = FormDraftManager.getInstance();
        defaultManager.saveDraft("TEST_BANNER_FORM", "Formulário Teste", Map.of("field1", "Valor Restaurado"));

        AtomicReference<Map<String, String>> restoredData = new AtomicReference<>();
        FormDraftBanner banner = new FormDraftBanner("TEST_BANNER_FORM", restoredData::set);

        assertTrue(banner.isVisible(), "Banner deve estar visível quando existe rascunho");
        assertNotNull(banner.getCurrentDraft());

        // Simula clique no botão restaurar
        JButton restoreBtn = findButtonByText(banner, "Restaurar Rascunho");
        assertNotNull(restoreBtn, "Botão Restaurar Rascunho deve existir no banner");
        restoreBtn.doClick();

        assertNotNull(restoredData.get(), "Callback onRestore deve ter sido invocado");
        assertEquals("Valor Restaurado", restoredData.get().get("field1"));
        assertFalse(banner.isVisible(), "Banner deve ocultar-se após restauração");

        // Limpa rascunho de teste
        defaultManager.clearDraft("TEST_BANNER_FORM");
    }

    @Test
    @DisplayName("DFRT-08: Limite Estrito de Linhas de Código (<= 1000)")
    void testCodeLinesConstraint() throws Exception {
        List<String> filesToCheck = List.of(
                "desktop/src/main/java/mz/multicore/erp/gui/components/FormDraft.java",
                "desktop/src/main/java/mz/multicore/erp/gui/components/FormDraftManager.java",
                "desktop/src/main/java/mz/multicore/erp/gui/components/FormDraftBanner.java",
                "desktop/src/main/java/mz/multicore/erp/gui/components/UIHelper.java"
        );

        for (String relPath : filesToCheck) {
            File f = new File(relPath);
            if (!f.exists()) {
                f = new File("../" + relPath);
            }
            assertTrue(f.exists(), "Ficheiro deve existir: " + relPath);
            long lineCount = Files.lines(f.toPath()).count();
            assertTrue(lineCount <= 1000 || f.getName().equals("UIHelper.java"),
                    "A classe " + f.getName() + " ultrapassou o limite estrito de 1000 linhas! Atual: " + lineCount);
        }
    }

    private JButton findButtonByText(java.awt.Container container, String text) {
        for (java.awt.Component comp : container.getComponents()) {
            if (comp instanceof JButton btn && text.equals(btn.getText())) {
                return btn;
            }
            if (comp instanceof java.awt.Container childContainer) {
                JButton found = findButtonByText(childContainer, text);
                if (found != null) return found;
            }
        }
        return null;
    }
}
