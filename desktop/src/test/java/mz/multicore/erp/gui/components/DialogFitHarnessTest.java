package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Point;

import static org.assertj.core.api.Assertions.assertThat;

/** Nenhum diálogo maior do que o ecrã (docs/TABELAS_CONSISTENCIA_SPEC.md, regra 13, TC-15). */
class DialogFitHarnessTest {

    @Test
    @DisplayName("TC-15: o diálogo que não cabe encolhe ao abrir; o que cabe mantém tamanho e lugar")
    void tc15_dialogsNeverOutgrowTheScreen() throws Exception {
        UiThemeAndWindowSupport.installModalContainment();
        JDialog[] big = new JDialog[1];
        SwingUtilities.invokeAndWait(() -> {
            big[0] = new JDialog();
            big[0].setSize(5000, 5000); // como o Fecho de Caixa (720x760) num ecrã de 1366x720, mas em qualquer ecrã
            big[0].setVisible(true);
        });
        for (int i = 0; i < 5; i++) SwingUtilities.invokeAndWait(() -> {});
        try {
            SwingUtilities.invokeAndWait(() -> {
                Dimension max = UiThemeAndWindowSupport.maxModalSize(big[0]);
                assertThat(big[0].getWidth()).isLessThanOrEqualTo(max.width);
                assertThat(big[0].getHeight()).as("os botões do rodapé ficam no ecrã").isLessThanOrEqualTo(max.height);

                JDialog small = new JDialog();
                small.setSize(300, 200);
                small.setLocation(40, 50);
                UiThemeAndWindowSupport.fitOnScreen(small);
                assertThat(small.getSize()).isEqualTo(new Dimension(300, 200));
                assertThat(small.getLocation()).as("o que cabe não é mexido").isEqualTo(new Point(40, 50));
                small.dispose();
            });
        } finally {
            SwingUtilities.invokeAndWait(() -> big[0].dispose());
        }
        assertThat(java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/mz/multicore/erp/gui/components/UiThemeAndWindowSupport.java")))
                .as("instalado no arranque, antes do login e da licença")
                .contains("installModalContainment(); // at startup");
    }
}
