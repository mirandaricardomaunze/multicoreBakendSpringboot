package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Harness automatizado de validação de Densidade de Interface & Escala (HARNESS-DENS-001).
 * Verifica integridade dos modos de densidade, transição cíclica, UIManager, tabelas e restrições de código.
 */
class UiDensityZoomHarnessTest {

    private UiDensity originalDensity;

    @BeforeEach
    void setUp() {
        originalDensity = UiDensityManager.getInstance().getDensity();
    }

    @AfterEach
    void tearDown() {
        UiDensityManager.getInstance().setDensity(originalDensity != null ? originalDensity : UiDensity.STANDARD);
    }

    @Test
    @DisplayName("DENS-01: Integridade dos Níveis de Densidade")
    void testDensityIntegrity() {
        assertEquals(28, UiDensity.COMPACT.getTableRowHeight());
        assertEquals(32, UiDensity.COMPACT.getFormControlHeight());
        assertEquals(0.90f, UiDensity.COMPACT.getFontScale(), 0.001f);

        assertEquals(36, UiDensity.STANDARD.getTableRowHeight());
        assertEquals(38, UiDensity.STANDARD.getFormControlHeight());
        assertEquals(1.00f, UiDensity.STANDARD.getFontScale(), 0.001f);

        assertEquals(44, UiDensity.COMFORTABLE.getTableRowHeight());
        assertEquals(44, UiDensity.COMFORTABLE.getFormControlHeight());
        assertEquals(1.15f, UiDensity.COMFORTABLE.getFontScale(), 0.001f);
    }

    @Test
    @DisplayName("DENS-02: Resolução por Identificadores em UiDensity.byId")
    void testByIdResolution() {
        assertSame(UiDensity.COMPACT, UiDensity.byId("compact"));
        assertSame(UiDensity.COMPACT, UiDensity.byId("compacto"));
        assertSame(UiDensity.COMPACT, UiDensity.byId("dense"));

        assertSame(UiDensity.STANDARD, UiDensity.byId("standard"));
        assertSame(UiDensity.STANDARD, UiDensity.byId("padrao"));
        assertSame(UiDensity.STANDARD, UiDensity.byId(null));
        assertSame(UiDensity.STANDARD, UiDensity.byId("unknown"));

        assertSame(UiDensity.COMFORTABLE, UiDensity.byId("comfortable"));
        assertSame(UiDensity.COMFORTABLE, UiDensity.byId("confortavel"));
        assertSame(UiDensity.COMFORTABLE, UiDensity.byId("amplo"));
        assertSame(UiDensity.COMFORTABLE, UiDensity.byId("large"));
    }

    @Test
    @DisplayName("DENS-03: Ciclo Circular de Densidade (STANDARD -> COMPACT -> COMFORTABLE -> STANDARD)")
    void testDensityCycling() {
        UiDensityManager manager = UiDensityManager.getInstance();
        manager.setDensity(UiDensity.STANDARD);
        assertEquals(UiDensity.STANDARD, manager.getDensity());

        UiDensity d1 = manager.cycleDensity();
        assertSame(UiDensity.COMPACT, d1);
        assertSame(UiDensity.COMPACT, manager.getDensity());

        UiDensity d2 = manager.cycleDensity();
        assertSame(UiDensity.COMFORTABLE, d2);
        assertSame(UiDensity.COMFORTABLE, manager.getDensity());

        UiDensity d3 = manager.cycleDensity();
        assertSame(UiDensity.STANDARD, d3);
        assertSame(UiDensity.STANDARD, manager.getDensity());
    }

    @Test
    @DisplayName("DENS-04: Atualização das Propriedades de UIManager")
    void testUIManagerUpdate() {
        UiDensityManager manager = UiDensityManager.getInstance();

        manager.setDensity(UiDensity.COMPACT);
        assertEquals(28, UIManager.getInt("Table.rowHeight"));

        manager.setDensity(UiDensity.COMFORTABLE);
        assertEquals(44, UIManager.getInt("Table.rowHeight"));

        manager.setDensity(UiDensity.STANDARD);
        assertEquals(36, UIManager.getInt("Table.rowHeight"));
    }

    @Test
    @DisplayName("DENS-05: Aplicação em Tabelas via styleTable")
    void testStyleTableAdoptsActiveDensity() {
        UiDensityManager manager = UiDensityManager.getInstance();

        manager.setDensity(UiDensity.COMPACT);
        JTable table = new JTable(new DefaultTableModel(new Object[][]{{"A"}}, new Object[]{"Col"}));
        UIHelper.styleTable(table);
        assertEquals(28, table.getRowHeight(), "A tabela deve adotar a altura de linha da densidade compacta (28 px)");

        manager.setDensity(UiDensity.COMFORTABLE);
        JTable tableLarge = new JTable(new DefaultTableModel(new Object[][]{{"A"}}, new Object[]{"Col"}));
        UIHelper.styleTable(tableLarge);
        assertEquals(44, tableLarge.getRowHeight(), "A tabela deve adotar a altura de linha da densidade confortável (44 px)");
    }

    @Test
    @DisplayName("DENS-06: Persistência e Restauração de Preferências")
    void testPersistenceAndRestoration() {
        UiDensityManager manager = UiDensityManager.getInstance();
        manager.setDensity(UiDensity.COMPACT);

        // Força recarregamento a partir das preferências gravadas
        manager.loadAndApplySavedDensity();
        assertEquals(UiDensity.COMPACT, manager.getDensity());
    }

    @Test
    @DisplayName("DENS-07: Notificação de Ouvintes ao Mudar Densidade")
    void testListenersNotification() {
        UiDensityManager manager = UiDensityManager.getInstance();
        manager.setDensity(UiDensity.STANDARD);

        AtomicBoolean notified = new AtomicBoolean(false);
        Runnable listener = () -> notified.set(true);

        manager.addChangeListener(listener);
        manager.setDensity(UiDensity.COMPACT);
        assertTrue(notified.get(), "O ouvinte registado deve ter sido notificado");

        // Remove listener e garante que não volta a ser notificado
        notified.set(false);
        manager.removeChangeListener(listener);
        manager.setDensity(UiDensity.COMFORTABLE);
        assertFalse(notified.get(), "O ouvinte removido não pode receber notificações adicionais");
    }

    @Test
    @DisplayName("DENS-08: Limite Estrito de Linhas de Código (<= 1000)")
    void testCodeLinesConstraint() throws Exception {
        List<String> filesToCheck = List.of(
                "desktop/src/main/java/mz/multicore/erp/gui/components/UiDensity.java",
                "desktop/src/main/java/mz/multicore/erp/gui/components/UiDensityManager.java",
                "desktop/src/main/java/mz/multicore/erp/gui/ConfigPanel.java",
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
