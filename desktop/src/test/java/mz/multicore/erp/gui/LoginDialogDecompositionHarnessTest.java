package mz.multicore.erp.gui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de homologação estrutural da decomposição de LoginDialog (Fase 75).
 * Valida limites estritos de linhas, ausência de JOptionPane e delegações canónicas.
 */
class LoginDialogDecompositionHarnessTest {

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
    @DisplayName("LGD-01: Limites físicos estritos de linhas de código")
    void lgd01_limitesFisicosLinhasCodigo() {
        long dialogLines = countLines("src/main/java/mz/multicore/erp/gui/LoginDialog.java");
        long cardLines = countLines("src/main/java/mz/multicore/erp/gui/LoginCredentialsCard.java");
        long bgLines = countLines("src/main/java/mz/multicore/erp/gui/LoginBackgroundPanel.java");

        assertThat(dialogLines)
                .as("LoginDialog.java deve ter <= 200 linhas (reduzido de 585)")
                .isLessThanOrEqualTo(200);

        assertThat(cardLines)
                .as("LoginCredentialsCard.java deve ter <= 295 linhas")
                .isLessThanOrEqualTo(295);

        assertThat(bgLines)
                .as("LoginBackgroundPanel.java deve ter <= 85 linhas")
                .isLessThanOrEqualTo(85);
    }

    @Test
    @DisplayName("LGD-02: Zero diálogos legados JOptionPane")
    void lgd02_zeroLegadoJOptionPane() {
        for (String file : List.of(
                "src/main/java/mz/multicore/erp/gui/LoginDialog.java",
                "src/main/java/mz/multicore/erp/gui/LoginCredentialsCard.java",
                "src/main/java/mz/multicore/erp/gui/LoginBackgroundPanel.java"
        )) {
            String src = readSource(file);
            assertThat(src)
                    .as(file + " não deve conter chamadas a JOptionPane")
                    .doesNotContain("JOptionPane.");
        }
    }

    @Test
    @DisplayName("LGD-03: Reutilização e delegação canónica de componentes")
    void lgd03_delegacaoCanonica() {
        String dialog = readSource("src/main/java/mz/multicore/erp/gui/LoginDialog.java");

        assertThat(dialog)
                .as("LoginDialog deve instanciar LoginBackgroundPanel")
                .contains("new LoginBackgroundPanel()");

        assertThat(dialog)
                .as("LoginDialog deve instanciar LoginCredentialsCard")
                .contains("new LoginCredentialsCard(");

        assertThat(dialog)
                .as("LoginDialog deve delegar getCard() para credentialsCard")
                .contains("return credentialsCard.getCard();");

        assertThat(dialog)
                .as("LoginDialog deve delegar getUsernameField() para credentialsCard")
                .contains("return credentialsCard.getUsernameField();");

        assertThat(dialog)
                .as("LoginDialog deve delegar getPasswordField() para credentialsCard")
                .contains("return credentialsCard.getPasswordField();");
    }

    @Test
    @DisplayName("LGD-04: Preservação de contratos de autenticação e atalhos")
    void lgd04_contratosPreservados() {
        String dialog = readSource("src/main/java/mz/multicore/erp/gui/LoginDialog.java");

        assertThat(dialog)
                .contains("tryLogin()")
                .contains("authApiClient.login")
                .contains("onCancel()")
                .contains("KeyEvent.VK_ESCAPE")
                .contains("AboutDialog.show")
                .doesNotContain("PdfFileSaver");
    }
}
