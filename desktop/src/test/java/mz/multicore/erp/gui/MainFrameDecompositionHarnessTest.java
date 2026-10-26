package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Harness de validação estrutural da decomposição de MainFrame (Fase 91).
 * Valida limites estritos de linhas, ausência de JOptionPane, e delegação para helpers especializados.
 */
class MainFrameDecompositionHarnessTest {

    private static String readSource(String relativePath) {
        try {
            return Files.readString(Path.of(relativePath));
        } catch (IOException e) {
            throw new RuntimeException("Falha ao ler " + relativePath, e);
        }
    }

    private static long countLines(String relativePath) {
        try {
            return Files.lines(Path.of(relativePath)).count();
        } catch (IOException e) {
            throw new RuntimeException("Falha ao contar linhas em " + relativePath, e);
        }
    }

    @Test
    @DisplayName("MFD-01: Limites físicos estritos de linhas de código")
    void mfd01_limitesFisicosLinhasCodigo() {
        long mainFrameLines = countLines("src/main/java/mz/multicore/erp/gui/MainFrame.java");
        long notificationLines = countLines("src/main/java/mz/multicore/erp/gui/MainFrameNotificationCoordinator.java");
        long topBarLines = countLines("src/main/java/mz/multicore/erp/gui/MainFrameTopBarBuilder.java");

        assertThat(mainFrameLines)
                .as("MainFrame.java deve ter <= 750 linhas (reduzido de 991 linhas)")
                .isLessThanOrEqualTo(750);

        assertThat(notificationLines)
                .as("MainFrameNotificationCoordinator.java deve ter <= 180 linhas")
                .isLessThanOrEqualTo(180);

        assertThat(topBarLines)
                .as("MainFrameTopBarBuilder.java deve ter <= 130 linhas")
                .isLessThanOrEqualTo(130);
    }

    @Test
    @DisplayName("MFD-02: Zero diálogos legados JOptionPane")
    void mfd02_zeroLegadoJOptionPane() {
        for (String file : List.of(
                "src/main/java/mz/multicore/erp/gui/MainFrame.java",
                "src/main/java/mz/multicore/erp/gui/MainFrameNotificationCoordinator.java",
                "src/main/java/mz/multicore/erp/gui/MainFrameTopBarBuilder.java"
        )) {
            String src = readSource(file);
            assertThat(src)
                    .as(file + " não deve conter chamadas a JOptionPane")
                    .doesNotContain("JOptionPane.");
        }
    }

    @Test
    @DisplayName("MFD-03: Delegação canónica para helpers especializados")
    void mfd03_delegacaoCanonicaParaHelpers() {
        String mainFrame = readSource("src/main/java/mz/multicore/erp/gui/MainFrame.java");

        assertThat(mainFrame)
                .as("MainFrame deve delegar construção da barra superior para MainFrameTopBarBuilder")
                .contains("topBarBuilder.build(");

        assertThat(mainFrame)
                .as("MainFrame deve delegar perfis de utilizador para MainFrameTopBarBuilder")
                .contains("topBarBuilder.updateUserProfile(");

        assertThat(mainFrame)
                .as("MainFrame deve instanciar MainFrameNotificationCoordinator")
                .contains("new MainFrameNotificationCoordinator(");

        assertThat(mainFrame)
                .as("MainFrame deve delegar atualização de badge para MainFrameNotificationCoordinator")
                .contains("notificationCoordinator.updateNotificationBadge(");
    }

    @Test
    @DisplayName("MFD-04: Contratos e atalhos essenciais preservados")
    void mfd04_contratosPreservados() {
        String mainFrame = readSource("src/main/java/mz/multicore/erp/gui/MainFrame.java");

        assertThat(mainFrame)
                .contains("KeyEvent.VK_K")
                .contains("KeyEvent.VK_B")
                .contains("KeyEvent.VK_H")
                .contains("KeyEvent.VK_F1")
                .contains("KeyEvent.VK_F11")
                .contains("KeyEvent.VK_F12")
                .contains("openGlobalSearch()")
                .contains("buildSearchIndex()")
                .contains("buildSellerSearchIndex()");
    }
}
