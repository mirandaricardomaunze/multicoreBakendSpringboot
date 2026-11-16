package mz.multicore.erp.gui.components;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/** A moldura única dos diálogos (docs/TABELAS_CONSISTENCIA_SPEC.md, regra 14, TC-16 e TC-17). */
class DialogChromeHarnessTest {

    private static final Path GUI = Path.of("src/main/java/mz/multicore/erp/gui");

    /**
     * Fora da regra, de propósito: o Painel Executivo (tema próprio), a pesquisa global (paleta de comandos, sem
     * cabeçalho nem rodapé), as fichas e mensagens que já desenham a mesma moldura, e ficheiros que só citam JDialog.
     */
    private static final Set<String> OWN_FRAME = Set.of("GlobalSearchDialog.java", "ModernFormDialog.java",
            "ModernMessageDialog.java", "UiAsyncTaskSupport.java", "StockProductDetailHeroBuilder.java");

    /** Os botões do Pagamento Móvel estão nos painéis de cada passo (iniciar / à espera), não num rodapé. */
    private static final Set<String> NO_FOOTER = Set.of("MobilePaymentModal.java");

    @Test
    @DisplayName("TC-16: moldura comum — cabeçalho comum, margem XL, rodapé à direita sob uma linha")
    void tc16_everyDialogUsesTheSharedFrame() throws IOException {
        JButton cancel = new JButton("Cancelar");
        JButton ok = new JButton("Gravar");
        JLabel note = new JLabel("2 páginas");
        JPanel footer = DialogChrome.footerWithNote(note, cancel, ok);
        assertThat(footer.getBorder()).isInstanceOf(CompoundBorder.class);
        JPanel actions = (JPanel) ((BorderLayout) footer.getLayout()).getLayoutComponent(BorderLayout.EAST);
        assertThat(((FlowLayout) actions.getLayout()).getAlignment()).as("botões à direita").isEqualTo(FlowLayout.RIGHT);
        assertThat(actions.getComponent(actions.getComponentCount() - 1)).as("o principal no fim").isSameAs(ok);
        assertThat(((BorderLayout) footer.getLayout()).getLayoutComponent(BorderLayout.CENTER)).isSameAs(note);

        JPanel frame = DialogChrome.frame("fas-check", "Título", "Subtítulo", new JPanel(), footer);
        assertThat(frame.getBorder()).isInstanceOf(EmptyBorder.class);
        assertThat(((EmptyBorder) frame.getBorder()).getBorderInsets().left).isEqualTo(Spacing.XL);
        assertThat(((BorderLayout) frame.getLayout()).getLayoutComponent(BorderLayout.NORTH)).isInstanceOf(JComponent.class);
        assertThat(((BorderLayout) frame.getLayout()).getLayoutComponent(BorderLayout.SOUTH)).isSameAs(footer);

        List<String> offenders = new ArrayList<>();
        for (Path f : dialogs()) {
            String name = f.getFileName().toString();
            if (OWN_FRAME.contains(name)) continue;
            String src = read(f);
            boolean header = src.contains("DialogChrome.frame(") || src.contains("buildPremiumHeader(");
            boolean foot = NO_FOOTER.contains(name) || usesSharedFooter(src) || footerFromBuilder(f, src);
            if (!header) offenders.add(name + ": cabeçalho próprio");
            if (!foot) offenders.add(name + ": rodapé próprio");
        }
        assertThat(offenders).as("diálogos fora da moldura comum").isEmpty();
    }

    @Test
    @DisplayName("TC-17: nenhum botão com contorno com texto ou ícone branco (fica invisível no fundo claro)")
    void tc17_noWhiteOnOutlineButtons() throws IOException {
        Pattern outline = Pattern.compile("(\\w+)\\s*=\\s*ButtonStyles\\.(?:secondary|cancelButton|destructive)\\(");
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(GUI)) {
            for (Path f : files.filter(x -> x.toString().endsWith(".java")).toList()) {
                if (f.toString().replace('\\', '/').contains("/tv/")) continue;
                String src = read(f);
                Set<String> names = new HashSet<>();
                Matcher m = outline.matcher(src);
                while (m.find()) names.add(m.group(1));
                for (String line : src.split("\n")) {
                    for (String v : names) {
                        if (line.matches(".*\\b" + v + "\\.set(Icon|Foreground)\\(.*Color\\.WHITE.*")) {
                            offenders.add(f.getFileName() + ": " + line.trim());
                        }
                    }
                }
            }
        }
        assertThat(offenders).isEmpty();
    }

    private static boolean usesSharedFooter(String src) {
        return src.contains("DialogChrome.footer(") || src.contains("DialogChrome.footerWithNote(");
    }

    /** A footer built by a view builder next to the dialog ("XxxViewBuilder.buildFooter(") counts when that builder uses the frame. */
    private static boolean footerFromBuilder(Path dialog, String src) {
        Matcher m = Pattern.compile("(\\w+ViewBuilder)\\.buildFooter\\(").matcher(src);
        while (m.find()) {
            Path builder = dialog.resolveSibling(m.group(1) + ".java");
            if (Files.exists(builder) && usesSharedFooter(read(builder))) return true;
        }
        return false;
    }

    private static List<Path> dialogs() throws IOException {
        try (Stream<Path> files = Files.walk(GUI)) {
            return files.filter(f -> f.toString().endsWith(".java"))
                    .filter(f -> !f.toString().replace('\\', '/').contains("/tv/"))
                    .filter(f -> !f.getFileName().toString().equals("ExecutiveDetailDialog.java"))
                    .filter(f -> {
                        String s = read(f);
                        return s.contains("extends JDialog") || s.contains("new JDialog(");
                    })
                    .toList();
        }
    }

    private static String read(Path p) {
        try {
            return Files.readString(p);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
