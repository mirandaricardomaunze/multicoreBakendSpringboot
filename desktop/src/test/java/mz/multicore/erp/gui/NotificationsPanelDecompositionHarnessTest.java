package mz.multicore.erp.gui;

import mz.multicore.erp.gui.NotificationFeed.NotificationItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HARNESS-NOTIFICATIONS-PANEL-DECOMPOSITION-001: Decomposição do Painel de Notificações")
class NotificationsPanelDecompositionHarnessTest {

    @Test
    @DisplayName("NP-01: NotificationsPanel.java reduzido para <= 180 linhas")
    void testPanelLineCount() throws IOException {
        Path path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/NotificationsPanel.java");
        if (!Files.exists(path)) {
            path = Path.of("src/main/java/mz/multicore/erp/gui/NotificationsPanel.java");
        }
        assertTrue(Files.exists(path), "NotificationsPanel.java deve existir");

        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 180, "NotificationsPanel.java deve possuir <= 180 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("NP-02: NotificationsViewCoordinator.java desacoplado com <= 110 linhas")
    void testCollaboratorLineCount() throws IOException {
        Path path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/NotificationsViewCoordinator.java");
        if (!Files.exists(path)) {
            path = Path.of("src/main/java/mz/multicore/erp/gui/NotificationsViewCoordinator.java");
        }
        assertTrue(Files.exists(path), "NotificationsViewCoordinator.java deve existir");

        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 110, "NotificationsViewCoordinator.java deve possuir <= 110 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("NP-03: Erradicação absoluta de JOptionPane em NotificationsPanel e colaboradores")
    void testZeroJOptionPane() throws IOException {
        List<String> files = List.of(
                "NotificationsPanel.java",
                "NotificationsViewCoordinator.java"
        );

        Path baseDir = Path.of("desktop/src/main/java/mz/multicore/erp/gui");
        if (!Files.exists(baseDir)) {
            baseDir = Path.of("src/main/java/mz/multicore/erp/gui");
        }

        for (String file : files) {
            Path filePath = baseDir.resolve(file);
            assertTrue(Files.exists(filePath), file + " deve existir");
            List<String> lines = Files.readAllLines(filePath);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                assertFalse(line.contains("JOptionPane"),
                        "Uso proibido de JOptionPane em " + file + " na linha " + (i + 1) + ": " + line);
            }
        }
    }

    @Test
    @DisplayName("NP-04: Contrato de construtor canónico e integridade da UI preservados")
    void testConstructorAndUiLayout() {
        assertDoesNotThrow(() ->
                NotificationsPanel.class.getDeclaredConstructor(
                        NotificationFeed.class,
                        NotificationReadStore.class,
                        Consumer.class,
                        IntConsumer.class
                ), "NotificationsPanel deve expor construtor com NotificationFeed, NotificationReadStore, Consumer, IntConsumer");
    }

    @Test
    @DisplayName("NP-05: Integridade funcional pura de rowFor e summaryText delegadas ao coordenador")
    void testPureFormattingDelegation() {
        NotificationItem item = new NotificationItem("Stock", "Alerta", "Detalhe teste",
                "Hoje", "stock", 1);

        Object[] unreadRow = NotificationsPanel.rowFor(item, false);
        assertEquals(5, unreadRow.length);
        assertEquals(NotificationsViewCoordinator.UNREAD_LABEL, unreadRow[4]);

        Object[] readRow = NotificationsPanel.rowFor(item, true);
        assertEquals(5, readRow.length);
        assertEquals(NotificationsViewCoordinator.READ_LABEL, readRow[4]);

        assertEquals("Não há notificações pendentes.", NotificationsPanel.summaryText(0, 0));
        assertEquals("1 notificação, já lida.", NotificationsPanel.summaryText(1, 0));
        assertEquals("2 notificações, todas lidas.", NotificationsPanel.summaryText(2, 0));
        assertEquals("1 por ler de 1 notificação.", NotificationsPanel.summaryText(1, 1));
        assertEquals("3 por ler de 5 notificações.", NotificationsPanel.summaryText(5, 3));
    }
}
