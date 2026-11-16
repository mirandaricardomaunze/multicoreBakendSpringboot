package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** Harness of the inline field validation (docs/VALIDACAO_CAMPOS_SPEC.md). */
class FieldValidationHarnessTest {

    private static final Path GUI = Path.of("src/main/java/mz/multicore/erp/gui");

    @Test
    @DisplayName("VC-01: campo solto fica com borda vermelha e a mensagem por baixo; editar limpa os dois")
    void vc01_plainFieldMarkedAndCleared() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTextField name = new JTextField(20);
            JDialog dialog = host(name);

            boolean handled = FieldErrors.showIfField(FieldValidationException.of(name, "O nome é obrigatório."));

            assertThat(handled).isTrue();
            assertThat(name.getClientProperty("validationError")).isEqualTo("O nome é obrigatório.");
            assertThat(FieldErrors.caption(name)).isNotNull();
            assertThat(FieldErrors.caption(name).getText()).isEqualTo("O nome é obrigatório.");
            assertThat(FieldErrors.caption(name).getY()).as("por baixo do campo")
                    .isGreaterThanOrEqualTo(SwingUtilities.convertPoint(name, 0, name.getHeight(),
                            dialog.getRootPane().getLayeredPane()).y);

            name.setText("Ana");

            assertThat(name.getClientProperty("validationError")).isNull();
            assertThat(FieldErrors.caption(name)).isNull();
            dialog.dispose();
        });
    }

    @Test
    @DisplayName("VC-02: dentro de um FormField a mensagem vai para a linha de erro do próprio FormField")
    void vc02_formFieldUsesItsErrorLine() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTextField code = new JTextField(10);
            FormField form = new FormField("Código", code, true, null);
            JDialog dialog = host(form);

            FieldErrors.show(code, "O código é obrigatório.");

            assertThat(form.errorLabel().isVisible()).isTrue();
            assertThat(form.errorLabel().getText()).isEqualTo("O código é obrigatório.");
            assertThat(FieldErrors.caption(code)).as("sem legenda flutuante duplicada").isNull();

            code.setText("A1");

            assertThat(form.errorLabel().isVisible()).isFalse();
            dialog.dispose();
        });
    }

    @Test
    @DisplayName("VC-03: num spinner o erro é assinalado no editor de texto onde se escreve")
    void vc03_spinnerMarksItsEditor() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0, 10, 1));
            JDialog dialog = host(spinner);

            FieldErrors.show(spinner, "Indique uma quantidade.");

            JTextField editor = ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
            assertThat(editor.getClientProperty("validationError")).isEqualTo("Indique uma quantidade.");
            dialog.dispose();
        });
    }

    @Test
    @DisplayName("VC-04: um erro que não é de campo não marca nada")
    void vc04_otherErrorsAreNotFieldErrors() {
        assertThat(FieldErrors.showIfField(new IllegalArgumentException("Falha geral"))).isFalse();
        assertThat(FieldErrors.showIfField(new RuntimeException(new IllegalStateException("x")))).isFalse();
    }

    @Test
    @DisplayName("VC-05: o ModernFormDialog envia os erros de campo para o próprio campo")
    void vc05_dialogRoutesFieldErrors() {
        assertThat(read(GUI.resolve("components/ModernFormDialog.java")))
                .contains("FieldErrors.showIfField(error)")
                .contains("Corrija o campo assinalado");
    }

    @Test
    @DisplayName("VC-06: ninguém foca um campo e lança IllegalArgumentException à mão: usa FieldValidationException")
    void vc06_noHandRolledFocusAndThrow() {
        Pattern handRolled = Pattern.compile("\\.requestFocus(?:InWindow)?\\(\\);\\s*throw new IllegalArgumentException\\(");
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(GUI)) {
            files.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                Matcher m = handRolled.matcher(read(p));
                while (m.find()) offenders.add(GUI.relativize(p).toString().replace('\\', '/'));
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        assertThat(offenders).isEmpty();
    }

    @Test
    @DisplayName("VC-07: os formulários principais assinalam o campo (activos, viaturas, contratos, transferências)")
    void vc07_mainFormsUseFieldErrors() {
        for (String form : List.of("AssetFormDialog.java", "VehicleFormDialog.java", "RecurringContractFormDialog.java",
                "StockTransferEditorForm.java", "BomFormDialog.java", "users/UserEditorDialog.java")) {
            assertThat(read(GUI.resolve(form))).as(form).contains("FieldValidationException.of(");
        }
    }

    private static JDialog host(java.awt.Component field) {
        JDialog dialog = new JDialog();
        JPanel panel = new JPanel();
        panel.add(field);
        dialog.add(panel);
        dialog.pack();
        dialog.getRootPane().getLayeredPane().setSize(dialog.getRootPane().getSize());
        return dialog;
    }

    private static String read(Path p) {
        try {
            return Files.readString(p);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
