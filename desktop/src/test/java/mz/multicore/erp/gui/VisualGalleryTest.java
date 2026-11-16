package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.Theme;
import mz.multicore.erp.gui.components.UIHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Visual gallery (docs/GALERIA_VISUAL_SPEC.md): an image of every screen, every first-level tab and every
 * hand-made dialog, in the three themes, at 1270x720 (a 1366x720 screen with the menu collapsed). It only runs on
 * request, because it takes a few minutes:
 * <pre>mvn -o -pl desktop -am test -Dtest=VisualGalleryTest -Dgallery=true</pre>
 * Output: desktop/target/gallery/&lt;theme&gt;/*.png and manifest.json (theme, group, name, tab, file).
 */
class VisualGalleryTest {

    private static final int WIDTH = 1270;
    private static final int HEIGHT = 720;
    private static final Path OUT = Path.of("target", "gallery");

    @Test
    @DisplayName("Galeria: imagem de cada ecrã, separador e diálogo nos três temas")
    void gallery() throws Exception {
        assumeTrue(Boolean.getBoolean("gallery"), "só corre com -Dgallery=true");
        // Como a app (DesktopApplication): números em pt-MZ, "1.234,50 MT".
        java.util.Locale.setDefault(java.util.Locale.Category.FORMAT, mz.multicore.erp.architecture.format.DisplayLocale.NUMBERS);
        List<Map<String, String>> manifest = new ArrayList<>();
        Map<String, String> failures = new LinkedHashMap<>();
        List<String> screens = new ArrayList<>(ScreenFactory.MAIN_SCREENS);
        screens.addAll(ScreenFactory.sectorScreens());
        List<String> dialogs = dialogClasses();

        for (Theme theme : List.of(Theme.LIGHT, Theme.DARK, Theme.HIGH_CONTRAST)) {
            SwingUtilities.invokeAndWait(() -> UIHelper.applyTheme(theme));
            Path dir = OUT.resolve(theme.id);
            Files.createDirectories(dir);
            for (String name : screens) {
                runGuarded(() -> captureScreen(theme, dir, name, manifest), theme.id + " " + name, failures);
            }
            for (String name : dialogs) {
                runGuarded(() -> captureDialog(theme, dir, name, manifest), theme.id + " " + name, failures);
            }
        }
        StringBuilder json = new StringBuilder("[\n");
        for (int i = 0; i < manifest.size(); i++) {
            Map<String, String> e = manifest.get(i);
            json.append("  {");
            int k = 0;
            for (Map.Entry<String, String> f : e.entrySet()) {
                json.append(k++ > 0 ? ", " : "").append('"').append(f.getKey()).append("\": \"")
                        .append(f.getValue().replace("\\", "\\\\").replace("\"", "\\\"")).append('"');
            }
            json.append(i < manifest.size() - 1 ? "},\n" : "}\n");
        }
        Files.writeString(OUT.resolve("manifest.json"), json.append("]\n").toString());
        Files.writeString(OUT.resolve("failures.txt"), String.join("\n", failures.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue()).toList()));
        System.out.println("Galeria: " + manifest.size() + " imagens, " + failures.size() + " por construir");
        assertThat(manifest).isNotEmpty();
    }

    private interface Capture { void run() throws Exception; }

    /** Runs on the EDT; a constructor that opens a modal window is unblocked after 10 s by closing every window. */
    private static void runGuarded(Capture capture, String label, Map<String, String> failures) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        SwingUtilities.invokeLater(() -> {
            try {
                capture.run();
            } catch (Throwable e) {
                failures.put(label, ScreenFactory.rootMessage(e));
            } finally {
                done.countDown();
            }
        });
        if (!done.await(10, TimeUnit.SECONDS)) {
            failures.put(label, "abriu uma janela modal ao construir");
            SwingUtilities.invokeLater(() -> { for (Window w : Window.getWindows()) w.dispose(); });
            done.await(10, TimeUnit.SECONDS);
        }
    }

    private static void captureScreen(Theme theme, Path dir, String name,
                                      List<Map<String, String>> manifest) throws Exception {
        JComponent screen = ScreenFactory.buildScreen("mz.multicore.erp.gui." + name);
        screen.setSize(WIDTH, HEIGHT);
        ScreenFactory.layoutTwice(screen);
        String group = name.startsWith("sector.") ? "Sector" : "Ecrãs";
        String shortName = name.substring(name.lastIndexOf('.') + 1);
        JTabbedPane tabs = firstTabs(screen);
        if (tabs == null) {
            save(screen, dir, shortName, theme, group, shortName, "", manifest);
            return;
        }
        for (int i = 0; i < tabs.getTabCount(); i++) {
            if (!tabs.isEnabledAt(i)) continue;
            try {
                tabs.setSelectedIndex(i);
            } catch (RuntimeException ignored) {
                // a listener reacted with simulated clients; the tab is still selected
            }
            if (tabs.getSelectedIndex() != i) continue;
            ScreenFactory.layoutTwice(screen);
            String tab = tabs.getTitleAt(i);
            save(screen, dir, shortName + "__" + String.format("%02d", i), theme, group, shortName, tab, manifest);
        }
    }

    private static void captureDialog(Theme theme, Path dir, String name,
                                      List<Map<String, String>> manifest) throws Exception {
        Object built = ScreenFactory.build(name);
        if (!(built instanceof JDialog dialog)) return;
        try {
            Dimension size = dialog.getSize();
            if (size.width <= 0 || size.height <= 0) {
                dialog.pack();
                size = dialog.getSize();
            }
            // As on a 1366x720 screen: a dialog never larger than 94% of the window (fitOnScreen).
            int w = Math.min(Math.max(size.width, 320), (int) (1366 * 0.94));
            int h = Math.min(Math.max(size.height, 200), (int) (HEIGHT * 0.94));
            Container content = dialog.getContentPane();
            content.setSize(w, h);
            ScreenFactory.layoutTwice(content);
            String shortName = name.substring(name.lastIndexOf('.') + 1);
            save((JComponent) content, dir, "dialog__" + shortName, theme, "Diálogos", shortName, dialog.getTitle(), manifest);
        } finally {
            dialog.dispose();
        }
    }

    private static void save(JComponent component, Path dir, String file, Theme theme, String group,
                             String name, String tab, List<Map<String, String>> manifest) throws Exception {
        BufferedImage image = new BufferedImage(component.getWidth(), component.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(UIHelper.BG_DARK);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        component.paint(g);
        g.dispose();
        Path png = dir.resolve(file + ".png");
        ImageIO.write(image, "png", png.toFile());
        writeWebCopy(image, OUT.resolve("web").resolve(theme.id).resolve(file + ".jpg"));
        Map<String, String> entry = new LinkedHashMap<>();
        entry.put("theme", theme.id);
        entry.put("group", group);
        entry.put("name", name);
        entry.put("tab", tab == null ? "" : tab);
        entry.put("file", theme.id + "/" + png.getFileName());
        entry.put("size", image.getWidth() + "x" + image.getHeight());
        manifest.add(entry);
    }

    /** A lighter copy for the review page: 75% of the size, JPEG. */
    private static void writeWebCopy(BufferedImage image, Path target) throws Exception {
        Files.createDirectories(target.getParent());
        int w = (int) Math.round(image.getWidth() * 0.75);
        int h = (int) Math.round(image.getHeight() * 0.75);
        BufferedImage small = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = small.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(image, 0, 0, w, h, null);
        g.dispose();
        javax.imageio.ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.82f);
        try (javax.imageio.stream.ImageOutputStream out = ImageIO.createImageOutputStream(target.toFile())) {
            writer.setOutput(out);
            writer.write(null, new javax.imageio.IIOImage(small, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    private static JTabbedPane firstTabs(Container root) {
        for (Component child : root.getComponents()) {
            if (child instanceof JTabbedPane tabs) return tabs;
            if (child instanceof Container c) {
                JTabbedPane found = firstTabs(c);
                if (found != null) return found;
            }
        }
        return null;
    }

    /** Hand-made dialogs (classes that extend JDialog), outside the Painel Executivo. */
    private static List<String> dialogClasses() throws Exception {
        Path gui = Path.of("src/main/java/mz/multicore/erp/gui");
        try (Stream<Path> files = Files.walk(gui)) {
            return files.filter(f -> f.toString().endsWith(".java"))
                    .filter(f -> !f.toString().replace('\\', '/').contains("/tv/"))
                    .filter(f -> ScreenFactory.read(f).contains("extends JDialog"))
                    .map(f -> gui.relativize(f).toString().replace('\\', '/').replace(".java", "").replace('/', '.'))
                    .map(rel -> "mz.multicore.erp.gui." + rel)
                    .sorted()
                    .toList();
        }
    }
}
