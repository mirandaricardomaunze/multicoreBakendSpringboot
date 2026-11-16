package mz.multicore.erp.gui;

import javax.swing.JComponent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.mockito.Mockito.mock;

/**
 * Builds every screen (and hand-made dialog) with simulated API clients, for the all-screens audit (ECR-11) and the
 * visual gallery. One place, so both look at exactly the same screens.
 */
final class ScreenFactory {

    /** The modules of the main window. Sector screens are found by {@link #sectorScreens()}. */
    static final List<String> MAIN_SCREENS = List.of(
            "ComercialPanel", "ComprasPanel", "StockPanel", "FinanceiroPanel", "HRPanel", "CRMPanel",
            "ClientesPanel", "FiscalPanel", "accounting.AccountingPanel", "AssetsPanel", "ManufacturingPanel",
            "FleetPanel", "RecurringContractsPanel", "QualityPanel", "ApprovalsPanel", "ConfigPanel",
            "PerformancePanel", "ForensicAuditPanel", "PlataformaPanel");

    private ScreenFactory() {}

    /** Builds a class of the gui package through its widest non-private constructor, faking every argument. */
    static Object build(String className) throws Exception {
        Class<?> type = Class.forName(className);
        // The widest non-private constructor; a dialog opened only through a static show() has a private one.
        Constructor<?> constructor = Arrays.stream(type.getDeclaredConstructors())
                .filter(c -> !Modifier.isPrivate(c.getModifiers()))
                .max(Comparator.comparingInt(Constructor::getParameterCount))
                .orElseGet(() -> Arrays.stream(type.getDeclaredConstructors())
                        .max(Comparator.comparingInt(Constructor::getParameterCount)).orElseThrow());
        constructor.setAccessible(true);
        Object[] args = Arrays.stream(constructor.getParameterTypes()).map(ScreenFactory::fake).toArray();
        return constructor.newInstance(args);
    }

    static JComponent buildScreen(String className) throws Exception {
        return (JComponent) build(className);
    }

    static Object fake(Class<?> type) {
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
        if (Window.class.isAssignableFrom(type) || Frame.class.isAssignableFrom(type) || Dialog.class.isAssignableFrom(type)) {
            return null; // a dialog without owner
        }
        try {
            return mock(type);
        } catch (RuntimeException e) {
            return null;
        }
    }

    static List<String> sectorScreens() throws IOException {
        Path dir = Path.of("src/main/java/mz/multicore/erp/gui/sector");
        try (Stream<Path> files = Files.list(dir)) {
            return files.filter(f -> f.toString().endsWith("Panel.java"))
                    .filter(f -> read(f).contains("ResponsiveSupport.cardTop(") && read(f).contains("extends JPanel"))
                    .map(f -> "sector." + f.getFileName().toString().replace(".java", ""))
                    .sorted()
                    .toList();
        }
    }

    /** Two passes: the table header decides one or two rows only once it knows its width. */
    static void layoutTwice(Container root) {
        layoutTree(root);
        layoutTree(root);
    }

    private static void layoutTree(Container root) {
        root.doLayout();
        for (Component child : root.getComponents()) {
            if (child instanceof Container c && c.isVisible()) layoutTree(c);
        }
    }

    static String read(Path p) {
        try {
            return Files.readString(p);
        } catch (IOException e) {
            return "";
        }
    }

    static String rootMessage(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null) t = t.getCause();
        return t.getClass().getSimpleName() + (t.getMessage() == null ? "" : ": " + t.getMessage().lines().findFirst().orElse(""));
    }
}
