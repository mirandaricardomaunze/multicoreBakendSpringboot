package mz.multicore.erp.gui.components;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.prefs.Preferences;

final class UiThemeAndWindowSupport {

    private static Theme activeTheme = Theme.LIGHT;
    private static final Preferences PREFS = Preferences.userRoot().node("mz/multicore/erp/ui");
    private static boolean modalContainmentInstalled = false;
    private static boolean clampingModal = false;

    private UiThemeAndWindowSupport() {}

    static void requestLogout(Component parent) {
        int opt = JOptionPane.showConfirmDialog(
                parent, "Deseja realmente terminar a sessão?", "Terminar Sessão",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (opt == JOptionPane.YES_OPTION) {
            if (UIHelper.onForcedLogout != null) {
                UIHelper.onForcedLogout.run();
            } else {
                Window win = parent instanceof Window w ? w : SwingUtilities.getWindowAncestor(parent);
                if (win != null) win.dispose();
            }
        }
    }

    static boolean confirm(Component parent, String message, String title) {
        int opt = JOptionPane.showConfirmDialog(
                parent, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        return opt == JOptionPane.YES_OPTION;
    }

    static Rectangle getUsableScreenBounds(Component ref) {
        try {
            GraphicsConfiguration gc = ref != null ? ref.getGraphicsConfiguration() : null;
            if (gc == null && UIHelper.mainWindow != null) {
                gc = UIHelper.mainWindow.getGraphicsConfiguration();
            }
            if (gc == null) {
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                if (!ge.isHeadlessInstance()) {
                    gc = ge.getDefaultScreenDevice().getDefaultConfiguration();
                }
            }
            if (gc != null) {
                Rectangle bounds = gc.getBounds();
                Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
                return new Rectangle(
                        bounds.x + insets.left, bounds.y + insets.top,
                        bounds.width - insets.left - insets.right,
                        bounds.height - insets.top - insets.bottom);
            }
        } catch (Exception ignored) {}
        try {
            Dimension sz = Toolkit.getDefaultToolkit().getScreenSize();
            return new Rectangle(0, 0, sz.width, Math.max(600, sz.height - 48));
        } catch (Exception ignored) {
            return new Rectangle(0, 0, 1280, 800);
        }
    }

    static void applyStandardModalBounds(Window dialog, int preferredWidth, int preferredHeight) {
        if (dialog == null) return;
        Rectangle usable = getUsableScreenBounds(dialog);
        int targetW = Math.min(preferredWidth, Math.max(600, usable.width - 32));
        int targetH = Math.min(preferredHeight, Math.max(480, usable.height - 40));
        dialog.setSize(new Dimension(targetW, targetH));
        containWithinMain(dialog);
    }

    static void containWithinMain(Window dialog) {
        if (dialog == null) return;
        Rectangle usable = getUsableScreenBounds(dialog);
        Rectangle area = mainArea();
        int maxW = Math.min((int) (area.width * 0.94), usable.width - 24);
        int maxH = Math.min((int) (area.height * 0.94), usable.height - 36);
        int w = Math.min(dialog.getWidth(), maxW);
        int h = Math.min(dialog.getHeight(), maxH);
        if (w > 0 && h > 0) dialog.setSize(w, h);
        int x = area.x + Math.max(0, (area.width - dialog.getWidth()) / 2);
        int y = area.y + Math.max(0, (area.height - dialog.getHeight()) / 2);
        x = Math.max(usable.x + 8, Math.min(x, usable.x + usable.width - dialog.getWidth() - 8));
        y = Math.max(usable.y + 8, Math.min(y, usable.y + usable.height - dialog.getHeight() - 8));
        dialog.setLocation(x, y);
    }

    static Rectangle mainArea() {
        if (UIHelper.mainWindow != null && UIHelper.mainWindow.isShowing() && UIHelper.mainWindow.getWidth() > 0) {
            return UIHelper.mainWindow.getBounds();
        }
        return getUsableScreenBounds(null);
    }

    static void registerMainWindow(Window window) {
        UIHelper.mainWindow = window;
        installModalContainment();
    }

    private static void installModalContainment() {
        if (modalContainmentInstalled) return;
        modalContainmentInstalled = true;
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (event.getID() != java.awt.event.ComponentEvent.COMPONENT_MOVED) return;
            Object source = event.getSource();
            if (source instanceof Dialog dialog && dialog != UIHelper.mainWindow) {
                clampInsideMain(dialog);
            }
        }, AWTEvent.COMPONENT_EVENT_MASK);
    }

    static void clampInsideMain(Window window) {
        if (clampingModal || window == null || UIHelper.mainWindow == null || !UIHelper.mainWindow.isShowing()) {
            return;
        }
        Rectangle area = mainArea();
        Point loc = window.getLocation();
        int maxX = area.x + Math.max(0, area.width - window.getWidth());
        int maxY = area.y + Math.max(0, area.height - window.getHeight());
        int x = Math.max(area.x, Math.min(loc.x, maxX));
        int y = Math.max(area.y, Math.min(loc.y, maxY));
        if (x != loc.x || y != loc.y) {
            clampingModal = true;
            try {
                window.setLocation(x, y);
            } finally {
                clampingModal = false;
            }
        }
    }

    static Theme currentTheme() { return activeTheme; }

    static boolean isLight() { return activeTheme == Theme.LIGHT; }

    static boolean isHighContrast() { return activeTheme != null && activeTheme.isHighContrast(); }

    static Color withAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }

    static void loadAndApplySavedTheme() {
        String saved = PREFS.get("theme", "light");
        if ("dark".equalsIgnoreCase(saved)) {
            saved = "light";
            PREFS.put("theme", "light");
        }
        applyTheme(Theme.byId(saved));
    }

    static void applyTheme(Theme theme) {
        activeTheme = theme;
        UIHelper.BG_DARK = theme.bg;
        UIHelper.BG_CARD = theme.card;
        UIHelper.TEXT_LIGHT = theme.textPrimary;
        UIHelper.TEXT_MUTED = theme.textMuted;
        UIHelper.GRID = theme.grid;
        UIHelper.TABLE_HEADER_BG = theme.tableHeaderBg;
        UIHelper.ROW_ALT = theme.rowAlt;
        UIHelper.FIELD_BG = theme.fieldBg;
        UIHelper.BORDER = theme.border;
        UIHelper.SELECTION_BG = theme.selectionBg;
        initGlobalTheme();
    }

    static void setTheme(Theme theme) {
        Theme previous = activeTheme;
        if (theme == previous) return;
        PREFS.put("theme", theme.id);
        applyTheme(theme);
        try { FlatLaf.updateUI(); } catch (Exception ignored) {}
        if (UIHelper.onThemeChanged != null) {
            UIHelper.onThemeChanged.run();
        } else {
            restyleAllWindows(previous, theme);
        }
    }

    static Theme cycleTheme() {
        Theme next = (activeTheme == Theme.DARK) ? Theme.LIGHT
                : (activeTheme == Theme.LIGHT) ? Theme.HIGH_CONTRAST : Theme.DARK;
        setTheme(next);
        return next;
    }

    private static void restyleAllWindows(Theme from, Theme to) {
        Color[] oldP = from.palette();
        Color[] newP = to.palette();
        for (Window window : Window.getWindows()) {
            restyleTree(window, oldP, newP);
            window.invalidate();
            window.validate();
            window.repaint();
        }
    }

    static Color readableTextOn(Color background) {
        if (background == null) return Color.WHITE;
        return relativeLuminance(background) > 0.5 ? new Color(17, 24, 39) : Color.WHITE;
    }

    static double contrastRatio(Color a, Color b) {
        double la = relativeLuminance(a);
        double lb = relativeLuminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    static boolean meetsWcagAaa(Color a, Color b) {
        return contrastRatio(a, b) >= 7.0;
    }

    static Color blendColors(Color c1, Color c2, float ratio) {
        if (c1 == null && c2 == null) return Color.BLACK;
        if (c1 == null) return c2;
        if (c2 == null) return c1;
        float r = Math.max(0f, Math.min(1f, ratio));
        float ir = 1.0f - r;
        return new Color(
                Math.round(c1.getRed() * ir + c2.getRed() * r),
                Math.round(c1.getGreen() * ir + c2.getGreen() * r),
                Math.round(c1.getBlue() * ir + c2.getBlue() * r),
                Math.round(c1.getAlpha() * ir + c2.getAlpha() * r));
    }

    private static double relativeLuminance(Color c) {
        return 0.2126 * toLinear(c.getRed()) + 0.7152 * toLinear(c.getGreen()) + 0.0722 * toLinear(c.getBlue());
    }

    private static double toLinear(int channel) {
        double s = channel / 255.0;
        return s <= 0.03928 ? s / 12.92 : Math.pow((s + 0.055) / 1.055, 2.4);
    }

    private static void restyleTree(Component c, Color[] oldP, Color[] newP) {
        Color bg = mapColor(c.getBackground(), oldP, newP);
        if (bg != null) c.setBackground(bg);
        Color fg = mapColor(c.getForeground(), oldP, newP);
        if (fg != null) c.setForeground(fg);
        if (c instanceof JTable table) UIHelper.styleTable(table);
        if (c instanceof Container container) {
            for (Component child : container.getComponents()) restyleTree(child, oldP, newP);
        }
    }

    private static Color mapColor(Color color, Color[] oldP, Color[] newP) {
        if (color == null) return null;
        for (int i = 0; i < oldP.length; i++) {
            if (oldP[i].getRGB() == color.getRGB()) return newP[i];
        }
        return null;
    }

    static void initGlobalTheme() {
        try {
            if (activeTheme == Theme.LIGHT) FlatLightLaf.setup();
            else FlatDarkLaf.setup();

            UIManager.put("Component.arc", UIHelper.CONTROL_ARC);
            UIManager.put("Button.arc", UIHelper.CONTROL_ARC);
            UIManager.put("TextComponent.arc", UIHelper.CONTROL_ARC);
            UIManager.put("ComboBox.arc", UIHelper.CONTROL_ARC);
            UIManager.put("Spinner.arc", UIHelper.CONTROL_ARC);
            UIManager.put("ProgressBar.arc", 8);

            UIManager.put("Component.borderColor", UIHelper.BORDER);
            UIManager.put("Component.focusedBorderColor", UIHelper.ACCENT);
            UIManager.put("Component.focusWidth", 1);
            UIManager.put("Component.innerFocusWidth", 0);
            UIManager.put("TextComponent.borderColor", UIHelper.BORDER);
            UIManager.put("TextComponent.focusedBorderColor", UIHelper.ACCENT);
            UIManager.put("ComboBox.borderColor", UIHelper.BORDER);
            UIManager.put("ComboBox.focusedBorderColor", UIHelper.ACCENT);
            UIManager.put("ComboBox.buttonStyle", "none");
            UIManager.put("ComboBox.arrowType", "chevron");
            UIManager.put("Spinner.borderColor", UIHelper.BORDER);
            UIManager.put("Spinner.focusedBorderColor", UIHelper.ACCENT);

            UIManager.put("ScrollBar.showButtons", false);
            UIManager.put("ScrollBar.width", 10);
            UIManager.put("ScrollBar.thumbArc", 10);
            UIManager.put("TabbedPane.showTabSeparators", true);
            UIManager.put("TabbedPane.hasFullBorder", false);
            UIManager.put("TitlePane.unifiedBackground", true);
            UIManager.put("TitlePane.menuBarEmbedded", true);
            UIManager.put("TitlePane.centerTitle", false);

            UIManager.put("Table.alternateRowColor", UIHelper.ROW_ALT);
            UIManager.put("Table.showHorizontalLines", true);
            UIManager.put("Table.showVerticalLines", false);
            UIManager.put("Table.intercellSpacing", new Dimension(0, 1));
            UIManager.put("Table.selectionArc", 8);
            UIManager.put("Table.rowHeight", 36);
            UIManager.put("TextField.showClearButton", true);

            UIManager.put("Panel.background", UIHelper.BG_DARK);
            UIManager.put("OptionPane.background", UIHelper.BG_DARK);
            UIManager.put("OptionPane.messageForeground", UIHelper.TEXT_LIGHT);
            UIManager.put("OptionPane.messageFont", new Font(UIHelper.FONT, Font.BOLD, 13));
            UIManager.put("OptionPane.buttonFont", new Font(UIHelper.FONT, Font.BOLD, 12));

            UIManager.put("Button.background", UIHelper.BG_CARD);
            UIManager.put("Button.foreground", readableTextOn(UIHelper.BG_CARD));
            UIManager.put("Button.select", UIHelper.SELECTION_BG);
            UIManager.put("Button.focus", new Color(0, 0, 0, 0));
            UIManager.put("Button.gradient", Arrays.asList(1f, 0f, UIHelper.BG_CARD, UIHelper.BG_CARD, UIHelper.BG_CARD));
            UIManager.put("Button.disabledText", readableTextOn(UIHelper.BG_CARD).equals(Color.WHITE)
                    ? new Color(255, 255, 255, 140) : new Color(107, 114, 128, 180));

            UIManager.put("MenuItem.foreground", UIHelper.TEXT_LIGHT);
            UIManager.put("MenuItem.selectionForeground", UIHelper.TEXT_LIGHT);
            UIManager.put("Menu.foreground", UIHelper.TEXT_LIGHT);
            UIManager.put("PopupMenu.foreground", UIHelper.TEXT_LIGHT);

            UIManager.put("TabbedPane.background", UIHelper.BG_CARD);
            UIManager.put("TabbedPane.foreground", Color.WHITE);
            UIManager.put("TabbedPane.selected", UIHelper.ACCENT);
            UIManager.put("TabbedPane.selectedForeground", Color.WHITE);
            UIManager.put("TabbedPane.focusInputMap", new UIDefaults.LazyInputMap(new Object[]{}));
            UIManager.put("TabbedPane.shadow", UIHelper.BG_DARK);
            UIManager.put("TabbedPane.darkShadow", UIHelper.BG_DARK);
            UIManager.put("TabbedPane.light", UIHelper.BG_CARD);
            UIManager.put("TabbedPane.highlight", UIHelper.BG_CARD);
            UIManager.put("TabbedPane.focus", new Color(0, 0, 0, 0));

            UIManager.put("Label.foreground", UIHelper.TEXT_LIGHT);
            UIManager.put("Label.font", new Font(UIHelper.FONT, Font.PLAIN, 13));

            UIManager.put("ComboBox.background", UIHelper.FIELD_BG);
            UIManager.put("ComboBox.foreground", UIHelper.TEXT_LIGHT);
            UIManager.put("ComboBox.selectionBackground", UIHelper.SELECTION_BG);
            UIManager.put("ComboBox.selectionForeground", UIHelper.TEXT_LIGHT);
            UIManager.put("TextField.background", UIHelper.FIELD_BG);
            UIManager.put("TextField.foreground", UIHelper.TEXT_LIGHT);
            UIManager.put("TextField.caretForeground", UIHelper.TEXT_LIGHT);
            UIManager.put("PasswordField.background", UIHelper.FIELD_BG);
            UIManager.put("PasswordField.foreground", UIHelper.TEXT_LIGHT);
            UIManager.put("PasswordField.caretForeground", UIHelper.TEXT_LIGHT);
            UIManager.put("TextArea.background", UIHelper.BG_DARK);
            UIManager.put("TextArea.foreground", UIHelper.TEXT_LIGHT);
            UIManager.put("TextArea.caretForeground", UIHelper.TEXT_LIGHT);

            UIManager.put("ToolTip.background", UIHelper.BG_CARD);
            UIManager.put("ToolTip.foreground", UIHelper.TEXT_LIGHT);
            UIManager.put("ToolTip.font", new Font(UIHelper.FONT, Font.PLAIN, 12));
            UIManager.put("ToolTip.border", BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UIHelper.BORDER, 1, true),
                    BorderFactory.createEmptyBorder(4, 8, 4, 8)));
            ToolTipManager.sharedInstance().setInitialDelay(600);
            ToolTipManager.sharedInstance().setDismissDelay(8000);
        } catch (Exception ignored) {}
    }
}
