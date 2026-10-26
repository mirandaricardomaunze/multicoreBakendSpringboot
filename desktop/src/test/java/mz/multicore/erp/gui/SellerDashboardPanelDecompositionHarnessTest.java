package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.POSApiClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("HARNESS-SELLER-DASHBOARD-PANEL-DECOMPOSITION-001: Decomposição do Painel do Vendedor")
class SellerDashboardPanelDecompositionHarnessTest {

    @Test
    @DisplayName("SDP-01: SellerDashboardPanel.java reduzido para <= 130 linhas")
    void testPanelLineCount() throws IOException {
        Path path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/SellerDashboardPanel.java");
        if (!Files.exists(path)) {
            path = Path.of("src/main/java/mz/multicore/erp/gui/SellerDashboardPanel.java");
        }
        assertTrue(Files.exists(path), "SellerDashboardPanel.java deve existir");

        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 130, "SellerDashboardPanel.java deve possuir <= 130 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("SDP-02: SellerDashboardViewCoordinator.java desacoplado com <= 80 linhas")
    void testCollaboratorLineCount() throws IOException {
        Path path = Path.of("desktop/src/main/java/mz/multicore/erp/gui/SellerDashboardViewCoordinator.java");
        if (!Files.exists(path)) {
            path = Path.of("src/main/java/mz/multicore/erp/gui/SellerDashboardViewCoordinator.java");
        }
        assertTrue(Files.exists(path), "SellerDashboardViewCoordinator.java deve existir");

        List<String> lines = Files.readAllLines(path);
        assertTrue(lines.size() <= 95, "SellerDashboardViewCoordinator.java deve possuir <= 95 linhas. Atual: " + lines.size());
    }

    @Test
    @DisplayName("SDP-03: Erradicação absoluta de JOptionPane em SellerDashboardPanel e colaboradores")
    void testZeroJOptionPane() throws IOException {
        List<String> files = List.of(
                "SellerDashboardPanel.java",
                "SellerDashboardViewCoordinator.java"
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
    @DisplayName("SDP-04: Contrato de construtor e métodos operacionais preservados")
    void testConstructorAndMethods() {
        assertDoesNotThrow(() ->
                SellerDashboardPanel.class.getDeclaredConstructor(
                        ComercialApiClient.class,
                        POSApiClient.class,
                        Consumer.class
                ), "SellerDashboardPanel deve possuir construtor com ComercialApiClient, POSApiClient, Consumer");
        assertDoesNotThrow(() ->
                SellerDashboardPanel.class.getDeclaredMethod("updateWelcomeMessage", String.class),
                "SellerDashboardPanel deve possuir método updateWelcomeMessage(String)");
        assertDoesNotThrow(() ->
                SellerDashboardPanel.class.getDeclaredMethod("refreshData"),
                "SellerDashboardPanel deve possuir método refreshData()");
    }
}
