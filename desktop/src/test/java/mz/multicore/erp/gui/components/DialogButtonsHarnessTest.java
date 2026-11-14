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

    @Test
    @DisplayName("TC-12: em todos os diálogos cada botão tem papel: principal (verde), secundário ou destrutivo")
    void tc12_everyDialogButtonHasARole() throws IOException {
        Path gui = Path.of("src/main/java/mz/multicore/erp/gui");
        // Excepções nomeadas: confirmação de acção destrutiva (vermelho cheio de propósito), o selector de
        // filtros dos alertas e o botão de cabeçalho de tabela do BonusTab (tratado pelo cabeçalho comum).
        List<String> allowed = List.of(
                "? UIHelper.createDangerButton(\"Confirmar\")",
                "UIHelper.createSecondaryButton(\"Todos (\"",
                "UIHelper.createSecondaryButton(\"Críticos (\"",
                "UIHelper.createSecondaryButton(\"Atenção (\"",
                "UIHelper.createSuccessButton(\"Aprovar Prémio\")");
        java.util.regex.Pattern raw = java.util.regex.Pattern.compile("UIHelper\\.create(Primary|Danger|Warning|Secondary|Success)Button\\(");
        List<String> offenders = new java.util.ArrayList<>();
        try (Stream<Path> files = Files.walk(gui)) {
            for (Path f : files.filter(x -> x.toString().endsWith(".java")).toList()) {
                String name = f.toString().replace('\\', '/');
                if (name.contains("/tv/") || name.endsWith("ExecutiveDetailDialog.java")) continue; // tema próprio do Painel Executivo
                String src = read(f);
                if (!src.contains("extends JDialog") && !src.contains("new JDialog(")) continue;
                for (String line : src.split("\n")) {
                    if (!raw.matcher(line).find()) continue;
                    if (line.matches(".*ButtonStyles\\.(main|secondary|destructive)\\(UIHelper\\.create.*")) continue;
                    if (allowed.stream().anyMatch(line::contains)) continue;
                    offenders.add(f.getFileName() + ": " + line.trim());
                }
            }
        }
        assertThat(offenders).as("botões de diálogo sem papel").isEmpty();
    }

    @Test
    @DisplayName("TC-13: Enter carrega no botão verde e Esc fecha; teclas próprias e excepções respeitadas")
    void tc13_enterAndEscapeInEveryDialog() throws Exception {
        javax.swing.SwingUtilities.invokeAndWait(() -> {
            javax.swing.JDialog dialog = new javax.swing.JDialog();
            ModernButton close = ButtonStyles.secondary(UIHelper.createSecondaryButton("Fechar"));
            ModernButton approve = ButtonStyles.main(UIHelper.createPrimaryButton("Aprovar"));
            javax.swing.JPanel footer = new javax.swing.JPanel();
            footer.add(close);
            footer.add(approve);
            dialog.add(footer);
            boolean[] closing = {false};
            dialog.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override public void windowClosing(java.awt.event.WindowEvent e) { closing[0] = true; }
            });
            DialogKeys.apply(dialog);
            assertThat(dialog.getRootPane().getDefaultButton()).as("Enter = botão verde").isSameAs(approve);
            javax.swing.KeyStroke esc = javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0);
            Object key = dialog.getRootPane().getInputMap(javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW).get(esc);
            dialog.getRootPane().getActionMap().get(key).actionPerformed(null);
            assertThat(closing[0]).as("Esc fecha como o X da janela").isTrue();
            dialog.dispose();

            javax.swing.JDialog two = new javax.swing.JDialog();
            javax.swing.JPanel p = new javax.swing.JPanel();
            p.add(ButtonStyles.main(UIHelper.createPrimaryButton("A")));
            p.add(ButtonStyles.main(UIHelper.createPrimaryButton("B")));
            two.add(p);
            DialogKeys.apply(two);
            assertThat(two.getRootPane().getDefaultButton()).as("dois verdes: sem Enter").isNull();
            two.dispose();

            javax.swing.JDialog licence = new javax.swing.JDialog();
            ModernButton accept = ButtonStyles.main(UIHelper.createPrimaryButton("Concordar"));
            accept.putClientProperty(DialogKeys.NO_ENTER, Boolean.TRUE);
            licence.add(accept);
            DialogKeys.apply(licence);
            assertThat(licence.getRootPane().getDefaultButton()).isNull();
            licence.dispose();

            assertThat(ButtonStyles.isMain(ButtonStyles.secondary(ButtonStyles.main(UIHelper.createPrimaryButton("X")))))
                    .as("mudar de papel tira a marca de principal").isFalse();
        });
        Path gui = Path.of("src/main/java/mz/multicore/erp/gui");
        assertThat(read(gui.resolve("components/UiThemeAndWindowSupport.java"))).contains("DialogKeys.install();");
        assertThat(read(gui.resolve("components/ModernMessageDialog.java")))
                .contains("setDefaultButton(type == FeedbackType.ERROR ? cancel : confirmButton)");
        assertThat(read(gui.resolve("LicenseAcceptanceDialog.java"))).contains("DialogKeys.NO_ENTER");
    }

    private static String read(Path p) {
        try {
            return Files.readString(p);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
