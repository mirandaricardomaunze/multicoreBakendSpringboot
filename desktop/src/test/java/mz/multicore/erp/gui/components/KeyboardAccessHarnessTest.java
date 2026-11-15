package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Harness of keyboard and screen-reader access in dialogs (docs/TECLADO_ACESSIBILIDADE_SPEC.md). */
class KeyboardAccessHarnessTest {

    @Test
    @DisplayName("TA-01: cada botão com texto ganha Alt+letra, sem repetir letras no mesmo diálogo")
    void ta01_uniqueMnemonics() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            List<JButton> buttons = List.of(new JButton("Gravar"), new JButton("Cancelar"),
                    new JButton("Guardar rascunho"), new JButton("Imprimir"), new JButton("Ir para"));
            JDialog dialog = dialog(buttons.toArray(new JButton[0]));

            KeyboardAccess.apply(dialog);

            Set<Integer> letters = new HashSet<>();
            for (JButton b : buttons) {
                assertThat(b.getMnemonic()).as(b.getText()).isNotZero();
                assertThat(letters.add(b.getMnemonic())).as("letra repetida em " + b.getText()).isTrue();
            }
            assertThat((char) buttons.get(0).getMnemonic()).isEqualTo('G');
            assertThat((char) buttons.get(1).getMnemonic()).isEqualTo('C');
            assertThat((char) buttons.get(2).getMnemonic()).as("G já usado: 1.ª letra livre de outra palavra").isEqualTo('R');
            dialog.dispose();
        });
    }

    @Test
    @DisplayName("TA-02: a tecla escolhida pelo ecrã mantém-se e não é reutilizada")
    void ta02_keepsScreenMnemonics() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JButton approve = new JButton("Aprovar");
            approve.setMnemonic('P');
            JButton print = new JButton("Pagar");
            JDialog dialog = dialog(approve, print);

            KeyboardAccess.apply(dialog);

            assertThat((char) approve.getMnemonic()).isEqualTo('P');
            assertThat((char) print.getMnemonic()).isNotEqualTo('P');
            dialog.dispose();
        });
    }

    @Test
    @DisplayName("TA-03: letras acentuadas contam pela letra base (É → E, Ã → A)")
    void ta03_accents() {
        assertThat(KeyboardAccess.pick("Época", new HashSet<>())).isEqualTo('E');
        assertThat(KeyboardAccess.pick("Ânimo", new HashSet<>())).isEqualTo('A');
        assertThat(KeyboardAccess.pick("123", new HashSet<>())).isEqualTo((char) 0);
    }

    @Test
    @DisplayName("TA-04: botão só com ícone fica com o tooltip como nome acessível")
    void ta04_iconButtonGetsAccessibleName() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JButton icon = new JButton(UIHelper.icon("fas-print", 12));
            icon.setToolTipText("Imprimir documento");

            KeyboardAccess.nameFromTooltip(icon);

            assertThat(icon.getAccessibleContext().getAccessibleName()).isEqualTo("Imprimir documento");
        });
    }

    @Test
    @DisplayName("TA-05: aplicado a todos os diálogos pelo DialogKeys, ao abrir")
    void ta05_wiring() throws IOException {
        assertThat(Files.readString(Path.of("src/main/java/mz/multicore/erp/gui/components/DialogKeys.java")))
                .contains("KeyboardAccess.apply(dialog);");
    }

    private static JDialog dialog(JButton... buttons) {
        JDialog dialog = new JDialog();
        JPanel panel = new JPanel();
        for (JButton b : buttons) panel.add(b);
        dialog.add(panel);
        return dialog;
    }
}
