package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** Harness dos botões dos diálogos (docs/TABELAS_CONSISTENCIA_SPEC.md, TC-10 e TC-11). */
class DialogButtonsHarnessTest {

    @Test
    @DisplayName("TC-10: \"Cancelar\" com contorno e nunca vermelho; botões extra com contorno, vermelho só no texto")
    void tc10_dialogButtonHierarchy() {
        ModernButton cancel = ButtonStyles.cancelButton();
        assertThat(cancel.getText()).isEqualTo("Cancelar");
        assertThat(ButtonStyles.isOutline(cancel)).isTrue();
        assertThat(cancel.getForeground()).isNotEqualTo(UIHelper.REJECTED_RED_HOVER);

        ModernButton reject = ButtonStyles.dialogExtra(UIHelper.createDangerButton("Rejeitar"));
        assertThat(ButtonStyles.isOutline(reject)).isTrue();
        assertThat(reject.getForeground()).isEqualTo(UIHelper.REJECTED_RED_HOVER);

        ModernButton previous = ButtonStyles.dialogExtra(UIHelper.createPrimaryButton("Anterior"));
        assertThat(ButtonStyles.isOutline(previous)).as("só a confirmação fica com cor cheia").isTrue();
    }

    @Test
    @DisplayName("TC-11: os diálogos usam o \"Cancelar\" comum e o ModernFormDialog aplica a regra")
    void tc11_everyDialogUsesTheSharedCancel() throws IOException {
        Path gui = Path.of("src/main/java/mz/multicore/erp/gui");
        try (Stream<Path> files = Files.walk(gui)) {
            List<String> offenders = files.filter(f -> f.toString().endsWith(".java"))
                    .filter(f -> !f.getFileName().toString().equals("ButtonStyles.java"))
                    .filter(f -> read(f).matches("(?s).*UIHelper\\.create(Secondary|Danger|Primary)Button\\(\"Cancelar\"\\).*"))
                    .map(f -> f.getFileName().toString())
                    .toList();
            assertThat(offenders).as("\"Cancelar\" feito à mão").isEmpty();
        }
        String dialog = read(gui.resolve("components/ModernFormDialog.java"));
        assertThat(dialog).contains("cancelBtn = ButtonStyles.cancelButton();");
        assertThat(dialog.split("ButtonStyles\\.dialogExtra\\(btn\\)", -1)).as("nos dois addActionButton").hasSize(3);
    }

    private static String read(Path p) {
        try {
            return Files.readString(p);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
