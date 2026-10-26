package mz.multicore.erp.gui.pos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Teste de homologação estrutural da decomposição de MobilePaymentModal (Fase 77).
 * Valida limites estritos de linhas, ausência de JOptionPane e delegações canónicas.
 */
class MobilePaymentModalDecompositionHarnessTest {

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
    @DisplayName("MPM-01: Limites físicos estritos de linhas de código")
    void mpm01_limitesFisicosLinhasCodigo() {
        long modalLines = countLines("src/main/java/mz/multicore/erp/gui/MobilePaymentModal.java");
        long initLines = countLines("src/main/java/mz/multicore/erp/gui/MobilePaymentInitiationPanel.java");
        long waitLines = countLines("src/main/java/mz/multicore/erp/gui/MobilePaymentWaitingPanel.java");
        long coordLines = countLines("src/main/java/mz/multicore/erp/gui/MobilePaymentWorkflowCoordinator.java");

        assertThat(modalLines)
                .as("MobilePaymentModal.java deve ter <= 125 linhas (reduzido de 468)")
                .isLessThanOrEqualTo(125);

        assertThat(initLines)
                .as("MobilePaymentInitiationPanel.java deve ter <= 145 linhas")
                .isLessThanOrEqualTo(145);

        assertThat(waitLines)
                .as("MobilePaymentWaitingPanel.java deve ter <= 130 linhas")
                .isLessThanOrEqualTo(130);

        assertThat(coordLines)
                .as("MobilePaymentWorkflowCoordinator.java deve ter <= 135 linhas")
                .isLessThanOrEqualTo(135);
    }

    @Test
    @DisplayName("MPM-02: Zero diálogos legados JOptionPane")
    void mpm02_zeroLegadoJOptionPane() {
        for (String file : List.of(
                "src/main/java/mz/multicore/erp/gui/MobilePaymentModal.java",
                "src/main/java/mz/multicore/erp/gui/MobilePaymentInitiationPanel.java",
                "src/main/java/mz/multicore/erp/gui/MobilePaymentWaitingPanel.java",
                "src/main/java/mz/multicore/erp/gui/MobilePaymentWorkflowCoordinator.java"
        )) {
            String src = readSource(file);
            assertThat(src)
                    .as(file + " não deve conter chamadas a JOptionPane")
                    .doesNotContain("JOptionPane.");
        }
    }

    @Test
    @DisplayName("MPM-03: Preservação de contrato público do diálogo")
    void mpm03_contratoPublicoPreservado() {
        String modal = readSource("src/main/java/mz/multicore/erp/gui/MobilePaymentModal.java");

        assertThat(modal)
                .as("MobilePaymentModal deve manter método de entrada público estático show(...)")
                .contains("public static String show(Component parent, MobilePaymentProvider initialProvider,")
                .contains("new MobilePaymentWorkflowCoordinator(")
                .contains("new MobilePaymentInitiationPanel(")
                .contains("new MobilePaymentWaitingPanel(");
    }

    @Test
    @DisplayName("MPM-04: Integração de áudio POS e feedback profissional")
    void mpm04_integracaoAudioEFeedback() {
        String coord = readSource("src/main/java/mz/multicore/erp/gui/MobilePaymentWorkflowCoordinator.java");
        String modal = readSource("src/main/java/mz/multicore/erp/gui/MobilePaymentModal.java");

        assertThat(coord)
                .contains("PosAudioFeedbackEngine.getInstance().playAsync(PosAudioFeedbackEngine.SoundEvent.SUCCESS)")
                .contains("PosAudioFeedbackEngine.getInstance().playAsync(PosAudioFeedbackEngine.SoundEvent.ERROR)");

        assertThat(modal)
                .contains("ToastManager.show(");
    }
}
