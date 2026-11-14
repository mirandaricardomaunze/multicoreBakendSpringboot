package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.UIHelper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Auditoria de todos os ecrãs (docs/ECRA_CAMINHO_E_MENU_SPEC.md, ECR-11): constrói cada módulo com clientes
 * simulados, percorre todos os separadores (também os interiores) a 1270 px (menu recolhido num ecrã de
 * 1366) e a 1100 px (menu aberto) e mede cada cabeçalho de tabela: título e botões na linha dos campos,
 * filtros sem esmagar, nada sobreposto nem cortado.
 */
class AllScreensHeaderAuditTest {

    private static final int[] WIDTHS = {1270, 1100};
    private static final int HEIGHT = 720;
    private static final int TOLERANCE = 2;

    /** Os módulos da janela principal e os ecrãs de sector. */
    private static final List<String> MAIN_SCREENS = List.of(
            "ComercialPanel", "ComprasPanel", "StockPanel", "FinanceiroPanel", "HRPanel", "CRMPanel",
            "ClientesPanel", "FiscalPanel", "accounting.AccountingPanel", "AssetsPanel", "ManufacturingPanel",
            "FleetPanel", "RecurringContractsPanel", "QualityPanel", "ApprovalsPanel", "ConfigPanel",
            "PerformancePanel", "ForensicAuditPanel", "PlataformaPanel");

    @Test
    @DisplayName("ECR-11: em todos os ecrãs os cabeçalhos de tabela estão alinhados e nada é esmagado ou cortado")
    void ecr11_everyHeaderInEveryScreenIsAligned() throws Exception {
        List<String> screens = new ArrayList<>(MAIN_SCREENS);
        screens.addAll(sectorScreens());
        Map<String, String> notBuilt = new LinkedHashMap<>();
        List<String> problems = new ArrayList<>();
        List<String> audited = new ArrayList<>();
        int[] headerCount = {0};

        for (String name : screens) {
            SwingUtilities.invokeAndWait(() -> {
                JComponent screen;
                try {
                    screen = build("mz.multicore.erp.gui." + name);
                } catch (Throwable e) {
                    notBuilt.put(name, rootMessage(e));
                    return;
                }
                try {
                    for (int width : WIDTHS) {
                        Set<Container> seen = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
                        visitAllTabs(screen, width, () -> {
                            for (Container header : headers(screen)) {
                                if (!seen.add(header) || !reallyVisible(header, screen)) continue;
                                headerCount[0]++;
                                problems.addAll(audit(name, width, header));
                            }
                        });
                    }
                } catch (Throwable e) {
                    notBuilt.put(name + " (a percorrer)", rootMessage(e));
                    return;
                }
                audited.add(name);
                if (name.equals("StockPanel") || name.equals("ComercialPanel")) snapshot(screen, name);
            });
        }

        String report = "Ecrãs auditados (" + audited.size() + "): " + audited
                + "\nCabeçalhos medidos: " + headerCount[0]
                + "\nNão construídos (" + notBuilt.size() + "): " + notBuilt
                + "\nProblemas (" + problems.size() + "):\n  " + String.join("\n  ", problems);
        Files.writeString(Path.of("target", "all-screens-header-audit.txt"), report);
        System.out.println(report);

        assertThat(audited).as("ecrãs auditados").hasSizeGreaterThanOrEqualTo(MAIN_SCREENS.size() - 2);
        assertThat(problems).as(report).isEmpty();
    }

    // ── auditoria de um cabeçalho ────────────────────────────────────────────────────────────

    private static List<String> audit(String screen, int width, Container header) {
        List<String> out = new ArrayList<>();
        BorderLayout layout = (BorderLayout) header.getLayout();
        Component title = visible(layout.getLayoutComponent(BorderLayout.WEST));
        Component filters = visible(layout.getLayoutComponent(BorderLayout.CENTER));
        Component actions = visible(layout.getLayoutComponent(BorderLayout.EAST));
        String where = screen + " @" + width + " \"" + titleText(title, header) + "\": ";
        Rectangle box = new Rectangle(0, 0, header.getWidth(), header.getHeight());

        for (Component part : new Component[]{title, filters, actions}) {
            if (part == null) continue;
            if (!box.contains(part.getBounds())) out.add(where + kind(part, title, filters) + " cortado " + part.getBounds() + " em " + box);
        }
        if (overlaps(title, filters)) out.add(where + "título sobre os filtros");
        if (overlaps(title, actions)) out.add(where + "título sobre os botões");
        if (overlaps(filters, actions)) out.add(where + "filtros sobre os botões");
        if (actions instanceof Container bar) {
            Rectangle room = new Rectangle(0, 0, bar.getWidth(), bar.getHeight());
            for (Component button : bar.getComponents()) {
                if (button.isVisible() && (!room.contains(button.getBounds()) || button.getWidth() < button.getPreferredSize().width - TOLERANCE)) {
                    out.add(where + "botão cortado: " + (button instanceof javax.swing.AbstractButton b ? b.getText() : button) + " " + button.getBounds() + " em " + room);
                }
            }
        }
        if (filters != null && filters.getWidth() < filters.getPreferredSize().width - TOLERANCE
                && header.getWidth() >= filters.getPreferredSize().width) {
            out.add(where + "filtros esmagados (" + filters.getWidth() + " de " + filters.getPreferredSize().width + " px)");
        }
        boolean oneRow = filters != null && actions != null && filters.getY() < actions.getY() + actions.getHeight()
                && actions.getY() < filters.getY() + filters.getHeight();
        if (oneRow) {
            int line = filters.getY() + Math.min(filters.getHeight(), UIHelper.FORM_CONTROL_HEIGHT) / 2;
            int centre = actions.getY() + actions.getHeight() / 2;
            if (Math.abs(centre - line) > TOLERANCE) out.add(where + "botões fora da linha dos campos (" + (centre - line) + " px)");
            if (title != null && Math.abs(title.getY() + title.getHeight() / 2 - line) > TOLERANCE) {
                out.add(where + "título fora da linha dos campos (" + (title.getY() + title.getHeight() / 2 - line) + " px)");
            }
        }
        return out;
    }

    private static String kind(Component part, Component title, Component filters) {
        return part == title ? "título" : part == filters ? "filtros" : "botões";
    }

    private static boolean overlaps(Component a, Component b) {
        return a != null && b != null && a.getWidth() > 0 && b.getWidth() > 0 && a.getBounds().intersects(b.getBounds());
    }

    private static Component visible(Component c) {
        return c != null && c.isVisible() ? c : null;
    }

    private static String titleText(Component title, Container header) {
        Component west = ((BorderLayout) header.getLayout()).getLayoutComponent(BorderLayout.WEST);
        return west instanceof javax.swing.JLabel label ? label.getText() : "?";
    }

    // ── percorrer o ecrã ─────────────────────────────────────────────────────────────────────

    private static List<Container> headers(Container root) {
        List<Container> found = new ArrayList<>();
        collect(root, found);
        return found;
    }

    private static void collect(Container root, List<Container> found) {
        for (Component child : root.getComponents()) {
            if (child instanceof Container c) {
                if (c.getLayout() != null && c.getLayout().getClass().getName().endsWith("TableCardTop$RowLayout")) found.add(c);
                collect(c, found);
            }
        }
    }

    private static boolean reallyVisible(Component c, Component root) {
        for (Component p = c; p != null; p = p.getParent()) {
            if (!p.isVisible() || p.getWidth() <= 0) return false;
            if (p == root) return true;
        }
        return false;
    }

    /** Lays the screen out at the width, then selects every tab of every tab pane, one level at a time. */
    private static void visitAllTabs(JComponent screen, int width, Runnable onEachState) {
        screen.setSize(width, HEIGHT);
        layoutTwice(screen);
        onEachState.run();
        visitTabsBelow(screen, screen, onEachState);
    }

    private static void visitTabsBelow(JComponent screen, Container root, Runnable onEachState) {
        for (JTabbedPane tabs : directTabPanes(root)) {
            int original = tabs.getSelectedIndex();
            for (int i = 0; i < tabs.getTabCount(); i++) {
                if (!tabs.isEnabledAt(i)) continue;
                try {
                    tabs.setSelectedIndex(i);
                } catch (RuntimeException ignored) {
                    // A listener reacted with simulated clients (no session); the tab is still selected.
                }
                if (tabs.getSelectedIndex() != i) continue;
                layoutTwice(screen);
                onEachState.run();
                if (tabs.getComponentAt(i) instanceof Container page) visitTabsBelow(screen, page, onEachState);
            }
            try {
                if (original >= 0) tabs.setSelectedIndex(original);
            } catch (RuntimeException ignored) {
                // see above
            }
        }
    }

    /** Tab panes inside root that are not inside another tab pane below root. */
    private static List<JTabbedPane> directTabPanes(Container root) {
        List<JTabbedPane> out = new ArrayList<>();
        for (Component child : root.getComponents()) {
            if (child instanceof JTabbedPane tabs) out.add(tabs);
            else if (child instanceof Container c) out.addAll(directTabPanes(c));
        }
        return out;
    }

    /** Two passes: the table header decides one or two rows only once it knows its width. */
    private static void layoutTwice(Container root) {
        layoutTree(root);
        layoutTree(root);
    }

    private static void layoutTree(Container root) {
        root.doLayout();
        for (Component child : root.getComponents()) {
            if (child instanceof Container c && c.isVisible()) layoutTree(c);
        }
    }

    // ── construir ecrãs com clientes simulados ───────────────────────────────────────────────

    private static JComponent build(String className) throws Exception {
        Class<?> type = Class.forName(className);
        Constructor<?> constructor = Arrays.stream(type.getDeclaredConstructors())
                .filter(c -> !Modifier.isPrivate(c.getModifiers()))
                .max(Comparator.comparingInt(Constructor::getParameterCount))
                .orElseThrow();
        constructor.setAccessible(true);
        Object[] args = Arrays.stream(constructor.getParameterTypes()).map(AllScreensHeaderAuditTest::fake).toArray();
        return (JComponent) constructor.newInstance(args);
    }

    private static Object fake(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0d;
        if (type == String.class) return "";
        if (type == Runnable.class) return (Runnable) () -> { };
        if (type == java.util.function.Consumer.class) return (java.util.function.Consumer<Object>) x -> { };
        if (type == Supplier.class) return (Supplier<Object>) () -> null;
        if (type == BooleanSupplier.class) return (BooleanSupplier) () -> false;
        if (type == IntSupplier.class) return (IntSupplier) () -> 0;
        if (type == List.class) return List.of();
        try {
            return mock(type);
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static List<String> sectorScreens() throws IOException {
        Path dir = Path.of("src/main/java/mz/multicore/erp/gui/sector");
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(f -> f.toString().endsWith("Panel.java"))
                    .filter(f -> read(f).contains("ResponsiveSupport.cardTop(") && read(f).contains("extends JPanel"))
                    .map(f -> "sector." + f.getFileName().toString().replace(".java", ""))
                    .sorted()
                    .toList();
        }
    }

    private static String read(Path p) {
        try {
            return Files.readString(p);
        } catch (IOException e) {
            return "";
        }
    }

    private static String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null) t = t.getCause();
        return t.getClass().getSimpleName() + (t.getMessage() == null ? "" : ": " + t.getMessage().lines().findFirst().orElse(""));
    }

    private static void snapshot(JComponent screen, String name) {
        try {
            screen.setSize(1270, HEIGHT);
            layoutTwice(screen);
            BufferedImage image = new BufferedImage(1270, HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            screen.paint(g);
            g.dispose();
            ImageIO.write(image, "png", Path.of("target", "audit-" + name + ".png").toFile());
        } catch (Exception ignored) {
            // A imagem é só para inspecção visual; a auditoria não depende dela.
        }
    }
}
