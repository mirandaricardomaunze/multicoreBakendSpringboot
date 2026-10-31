package mz.multicore.erp.gui.components;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntConsumer;

public class UIHelper {

    public static Color BG_DARK = Theme.LIGHT.bg;
    public static Color BG_CARD = Theme.LIGHT.card;
    public static Color TEXT_LIGHT = Theme.LIGHT.textPrimary;
    public static Color TEXT_MUTED = Theme.LIGHT.textMuted;
    public static Color GRID = Theme.LIGHT.grid;
    public static Color TABLE_HEADER_BG = Theme.LIGHT.tableHeaderBg;
    public static Color ROW_ALT = Theme.LIGHT.rowAlt;
    public static Color FIELD_BG = Theme.LIGHT.fieldBg;
    public static Color BORDER = Theme.LIGHT.border;
    public static Color SELECTION_BG = Theme.LIGHT.selectionBg;

    public static final Color ACCENT = new Color(139, 92, 246);
    public static final Color ACCENT_HOVER = new Color(124, 58, 237);
    public static final Color ACCENT_BLUE = new Color(59, 130, 246);
    public static final Color ACCENT_BLUE_HOVER = new Color(37, 99, 235);
    public static final Color APPROVED_GREEN = new Color(16, 185, 129);
    public static final Color APPROVED_GREEN_HOVER = new Color(5, 150, 105);
    public static final Color REJECTED_RED = new Color(239, 68, 68);
    public static final Color REJECTED_RED_HOVER = new Color(220, 38, 38);
    public static final Color PENDING_YELLOW = new Color(245, 158, 11);
    public static final Color ACCENT_CYAN = new Color(6, 182, 212);
    public static final Color ACCENT_ORANGE = new Color(249, 115, 22);
    public static final Color ACCENT_ORANGE_HOVER = new Color(234, 88, 12);
    public static final Color ACCENT_PINK = new Color(236, 72, 153);
    public static final Color ACCENT_SKY = new Color(14, 165, 233);
    public static final Color KPI_INFO_SOFT = new Color(224, 242, 254);
    public static final Color KPI_PURPLE_SOFT = new Color(243, 232, 255);
    public static final Color KPI_WARNING_SOFT = new Color(254, 243, 199);
    public static final Color KPI_NEUTRAL_SOFT = new Color(209, 213, 219);
    public static final Color KPI_SUCCESS_SOFT = new Color(204, 251, 241);
    public static final Color KPI_DANGER_SOFT = new Color(254, 226, 226);
    public static final Color KPI_ORANGE_SOFT = new Color(255, 237, 213);
    public static final Color KPI_INFO_DARK = new Color(9, 79, 172);
    public static final Color KPI_INFO_END = new Color(13, 148, 136);
    public static final Color KPI_PURPLE_DARK = new Color(109, 40, 217);
    public static final Color KPI_PURPLE_END = new Color(147, 51, 234);
    public static final Color KPI_WARNING_DARK = new Color(180, 83, 9);
    public static final Color KPI_WARNING_END = new Color(217, 119, 6);
    public static final Color KPI_NEUTRAL_DARK = new Color(15, 23, 42);
    public static final Color KPI_NEUTRAL_END = new Color(30, 41, 59);
    public static final Color KPI_DANGER_DARK = new Color(220, 38, 38);
    public static final Color KPI_DANGER_END = new Color(185, 28, 28);
    public static final Color KPI_ORANGE_DARK = new Color(194, 65, 12);
    public static final Color KPI_ORANGE_END = new Color(234, 88, 12);
    public static final Color SECONDARY = new Color(3, 105, 161);
    public static final Color SECONDARY_HOVER = new Color(7, 89, 133);
    public static final Color BUTTON_NEUTRAL = new Color(99, 102, 241);
    public static final Color BUTTON_NEUTRAL_HOVER = new Color(79, 70, 229);

    public static final Color PAPER = Color.WHITE;
    public static final Color PAPER_SHADOW = new Color(0, 0, 0, 70);
    public static final Color PAPER_INK = new Color(31, 41, 55);

    public static final Color MODULE_DASHBOARD  = ACCENT_BLUE;
    public static final Color MODULE_POS        = new Color(236, 72, 153);
    public static final Color MODULE_COMERCIAL  = ACCENT;
    public static final Color MODULE_COMPRAS    = PENDING_YELLOW;
    public static final Color MODULE_STOCK      = APPROVED_GREEN;
    public static final Color MODULE_FINANCEIRO = APPROVED_GREEN;
    public static final Color MODULE_HR         = ACCENT;
    public static final Color MODULE_CRM        = ACCENT_BLUE;
    public static final Color MODULE_CLIENTES   = new Color(14, 165, 233);
    public static final Color MODULE_FISCAL     = new Color(202, 138, 4);
    public static final Color MODULE_ACCOUNTING = new Color(124, 58, 237);
    public static final Color MODULE_APPROVALS  = PENDING_YELLOW;
    public static final Color MODULE_CONFIG     = new Color(107, 114, 128);

    public static final String FONT = "Segoe UI";

    public static final int ICON_XS = 12;
    public static final int ICON_SM = 14;
    public static final int ICON_MD = 16;
    public static final int ICON_LG = 20;
    public static final int ICON_XL = 24;
    public static final int ICON_HERO = 48;

    public static final int RADIUS_SM = 8;
    public static final int RADIUS_MD = 12;
    public static final int RADIUS_LG = 16;

    public static final int FORM_CONTROL_HEIGHT = 38;
    public static final int DIALOG_FORM_MIN_WIDTH = 560;
    public static final int CONTROL_ARC = 10;

    public static final Locale LOCALE_MZ = new Locale("pt", "MZ");
    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    public static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public static Runnable onThemeChanged;
    public static Runnable onForcedLogout;
    public static Window mainWindow;

    public static String formatMzn(BigDecimal amount) {
        return TableCellRenderers.format(amount, 2, " MT");
    }

    public static String formatMzn(Number amount) {
        return amount == null ? "—" : formatMzn(BigDecimal.valueOf(amount.doubleValue()));
    }

    public static String formatQty(BigDecimal qty) {
        return TableCellRenderers.format(qty, 2, "");
    }

    public static String formatDate(TemporalAccessor date) {
        return date == null ? "—" : DATE_FMT.format(date);
    }

    public static String formatDateTime(TemporalAccessor dateTime) {
        return dateTime == null ? "—" : DATETIME_FMT.format(dateTime);
    }

    // ── Tema e contenção de janelas (delega para UiThemeAndWindowSupport) ─────────────────────
    public static void requestLogout(Component parent) { UiThemeAndWindowSupport.requestLogout(parent); }
    public static boolean confirm(Component parent, String message, String title) { return UiThemeAndWindowSupport.confirm(parent, message, title); }
    public static Rectangle getUsableScreenBounds(Component ref) { return UiThemeAndWindowSupport.getUsableScreenBounds(ref); }
    public static void applyStandardModalBounds(Window dialog, int preferredWidth, int preferredHeight) { UiThemeAndWindowSupport.applyStandardModalBounds(dialog, preferredWidth, preferredHeight); }
    public static void containWithinMain(Window dialog) { UiThemeAndWindowSupport.containWithinMain(dialog); }
    public static Rectangle mainArea() { return UiThemeAndWindowSupport.mainArea(); }
    public static void registerMainWindow(Window window) { UiThemeAndWindowSupport.registerMainWindow(window); }
    public static void clampInsideMain(Window window) { UiThemeAndWindowSupport.clampInsideMain(window); }
    public static Theme currentTheme() { return UiThemeAndWindowSupport.currentTheme(); }
    public static boolean isLight() { return UiThemeAndWindowSupport.isLight(); }
    public static boolean isHighContrast() { return UiThemeAndWindowSupport.isHighContrast(); }
    public static void loadAndApplySavedTheme() { UiThemeAndWindowSupport.loadAndApplySavedTheme(); }
    public static void applyTheme(Theme theme) { UiThemeAndWindowSupport.applyTheme(theme); }
    public static void setTheme(Theme theme) { UiThemeAndWindowSupport.setTheme(theme); }
    public static Theme cycleTheme() { return UiThemeAndWindowSupport.cycleTheme(); }
    public static Color readableTextOn(Color background) { return UiThemeAndWindowSupport.readableTextOn(background); }
    public static double contrastRatio(Color a, Color b) { return UiThemeAndWindowSupport.contrastRatio(a, b); }
    public static boolean meetsWcagAaa(Color a, Color b) { return UiThemeAndWindowSupport.meetsWcagAaa(a, b); }
    public static Color blendColors(Color c1, Color c2, float ratio) { return UiThemeAndWindowSupport.blendColors(c1, c2, ratio); }
    public static Color withAlpha(Color color, int alpha) { return UiThemeAndWindowSupport.withAlpha(color, alpha); }
    public static void initGlobalTheme() { UiThemeAndWindowSupport.initGlobalTheme(); }
    // ── Botões e controlos de formulário (delega para UiFormControlAndFormatSupport) ──────────
    public static ModernButton createPrimaryButton(String text) { return new ModernButton(text, ACCENT_BLUE, ACCENT_BLUE_HOVER); }
    public static ModernButton createSuccessButton(String text) { return new ModernButton(text, APPROVED_GREEN, APPROVED_GREEN_HOVER); }
    public static ModernButton createDangerButton(String text) { return new ModernButton(text, REJECTED_RED, REJECTED_RED_HOVER); }
    public static ModernButton createWarningButton(String text) { return new ModernButton(text, PENDING_YELLOW, KPI_WARNING_DARK); }
    public static ModernButton createSecondaryButton(String text) { return new ModernButton(text, SECONDARY, SECONDARY_HOVER); }
    public static ModernButton createButton(String text, Icon icon, Color baseColor, ActionListener listener) { return UiFormControlAndFormatSupport.createButton(text, icon, baseColor, listener); }
    public static ModernButton createRefreshButton(Runnable refreshAction) { return UiFormControlAndFormatSupport.createRefreshButton(refreshAction); }
    public static ModernButton createExportButton(JTable table, String defaultBaseName) { return UiFormControlAndFormatSupport.createExportButton(table, defaultBaseName); }
    public static JPanel actionsBar(JComponent... components) { return TableFilter.toolbar(null, components); }
    public static JPanel tableCardTop(String title, JComponent filters, JComponent... actions) { return UiFormControlAndFormatSupport.tableCardTop(title, filters, actions); }
    public static JPanel filterBar(JComponent[] filters, JComponent[] actions) { return TableFilter.toolbar(filters, actions); }
    public static TableQuickFilterBar attachQuickFilter(JTable table) { return TableQuickFilterBar.attach(table); }
    public static JPanel wrapTableWithQuickFilter(JScrollPane scrollPane, JTable table) { return TableQuickFilterBar.wrapWithFilter(scrollPane, table); }
    public static ActionMenuButton createActionMenuButton(String text) {
        ActionMenuButton button = new ActionMenuButton(text);
        button.setColors(SECONDARY, SECONDARY_HOVER);
        return button;
    }
    public static ModernButton createIconButton(String accessibleName, String iconCode) { return UiFormControlAndFormatSupport.createIconButton(accessibleName, iconCode); }
    public static JButton createIconButton(String code, int size, String tooltip, Runnable action) { return UiFormControlAndFormatSupport.createIconButton(code, size, tooltip, action); }
    public static ModernButton createAddLineButton() { return UiFormControlAndFormatSupport.createAddLineButton(); }
    public static ModernButton createProductLookupButton(Runnable onLookup) { return UiFormControlAndFormatSupport.createProductLookupButton(onLookup); }
    public static JPanel createFilterGroup(String labelText, JComponent component) { return UiFormControlAndFormatSupport.createFilterGroup(labelText, component); }
    public static JComponent makeDialogScrollable(JPanel content) { return makeDialogScrollable(content, (int) (mainArea().height * 0.80)); }
    public static JComponent makeDialogScrollable(JPanel content, int maxHeight) { return UiFormControlAndFormatSupport.makeDialogScrollable(content, maxHeight); }
    public static void styleTextField(JTextField field) { UiFormControlAndFormatSupport.styleTextField(field); }
    public static void markFieldInvalid(JComponent component, String message) { UiFormControlAndFormatSupport.markFieldInvalid(component, message); }
    public static void clearFieldInvalid(JComponent component) { UiFormControlAndFormatSupport.clearFieldInvalid(component); }
    public static void setReadOnly(JTextComponent component, boolean readOnly) { UiFormControlAndFormatSupport.setReadOnly(component, readOnly); }
    public static Border fieldBorder(Color line, int thickness) { return UiFormControlAndFormatSupport.fieldBorder(line, thickness); }
    public static void onTextChange(JTextField field, Runnable onChange) { UiFormControlAndFormatSupport.onTextChange(field, onChange); }
    public static String promptRequiredText(String title, String iconCode, String subtitle, String label) { return UiFormControlAndFormatSupport.promptRequiredText(title, iconCode, subtitle, label); }
    public static BigDecimal promptAmount(String title, String iconCode, String subtitle, String label, BigDecimal min) { return UiFormControlAndFormatSupport.promptAmount(title, iconCode, subtitle, label, min); }
    public static String humanRole(String role) { return UiFormControlAndFormatSupport.humanRole(role); }
    public static void humanizeRoleCombo(JComboBox<String> combo) { UiFormControlAndFormatSupport.humanizeRoleCombo(combo); }
    public static void stylePasswordField(JPasswordField field) { UiFormControlAndFormatSupport.stylePasswordField(field); }
    public static void styleTextArea(JTextArea area) { UiFormControlAndFormatSupport.styleTextArea(area); }
    public static <T> ListCellRenderer<T> labelRenderer(Function<T, String> label) { return UiFormControlAndFormatSupport.labelRenderer(label); }
    public static void styleComboBox(JComboBox<?> combo) { UiFormControlAndFormatSupport.styleComboBox(combo); }
    public static <T> JComboBox<T> createComboBox(T[] items, Function<T, String> labelExtractor) { return UiFormControlAndFormatSupport.createComboBox(items, labelExtractor); }
    public static void styleSpinner(JSpinner spinner) { UiFormControlAndFormatSupport.styleSpinner(spinner); }
    public static JLabel createWarningBanner(String message) { return UiFormControlAndFormatSupport.createWarningBanner(message); }
    public static JSpinner createYearSpinner(int year) { return UiFormControlAndFormatSupport.createYearSpinner(year); }
    public static String humanStatus(String status) { return UiFormControlAndFormatSupport.humanStatus(status); }
    public static JPanel createDialogForm(Object... labelsAndComponents) { return createDialogForm(2, labelsAndComponents); }
    public static JPanel createDialogForm(int columns, Object... labelsAndComponents) { return UiFormControlAndFormatSupport.createDialogForm(columns, labelsAndComponents); }
    public static void installDigitsOnlyFilter(JTextComponent comp, int maxLength) { UiFormControlAndFormatSupport.installDigitsOnlyFilter(comp, maxLength); }
    public static void installUppercaseFilter(JTextComponent comp, int maxLength) { UiFormControlAndFormatSupport.installUppercaseFilter(comp, maxLength); }

    public static JLabel createHeading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(FONT, Font.BOLD, 22));
        label.setForeground(TEXT_LIGHT);
        return label;
    }

    public static JLabel createSubheading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(FONT, Font.BOLD, 15));
        label.setForeground(ACCENT);
        return label;
    }

    // ── Abas e tabelas (delega para UiTabbedAndTableStyler) ───────────────────────────────────
    public static void styleTabbedPane(JTabbedPane tabbedPane) { UiTabbedAndTableStyler.styleTabbedPane(tabbedPane); }
    public static void styleTabbedPaneMulticore(JTabbedPane tabbedPane) { UiTabbedAndTableStyler.styleTabbedPaneMulticore(tabbedPane); }
    public static void styleTable(JTable table) { UiTabbedAndTableStyler.styleTable(table); }
    public static void installDoubleClick(JTable table, Runnable onDoubleClick) { UiTabbedAndTableStyler.installDoubleClick(table, onDoubleClick); }
    public static void installRowDoubleClickHandler(JTable table, IntConsumer onRowDoubleClicked) { UiTabbedAndTableStyler.installRowDoubleClickHandler(table, onRowDoubleClicked); }
    public static void installRowContextMenu(JTable table, Function<Integer, JPopupMenu> popupProvider) { UiTabbedAndTableStyler.installRowContextMenu(table, popupProvider); }
    public static TableQuickPeekController installQuickPeek(JTable table, JPanel container) { return TableQuickPeekController.install(table, container); }
    public static void installDocumentGridShortcuts(JTable table, Runnable onAdd, Runnable onRemove, Runnable onSave, Runnable onLookup) { UiTabbedAndTableStyler.installDocumentGridShortcuts(table, onAdd, onRemove, onSave, onLookup); }
    public static void installDocumentGridShortcuts(JTable table, Runnable onAdd, Runnable onRemove, Runnable onSave) { installDocumentGridShortcuts(table, onAdd, onRemove, onSave, null); }
    public static void installCellNavigationKeys(JTable table, Runnable onAddLine) { UiTabbedAndTableStyler.installCellNavigationKeys(table, onAddLine); }
    public static int findNextEditableColumn(JTable table, int row, int startCol) { return UiTabbedAndTableStyler.findNextEditableColumn(table, row, startCol); }
    public static int findPrevEditableColumn(JTable table, int row, int startCol) { return UiTabbedAndTableStyler.findPrevEditableColumn(table, row, startCol); }
    static boolean looksNumeric(String raw) { return UiTabbedAndTableStyler.looksNumeric(raw); }
    static boolean isNumericColumn(JTable table, int viewCol) { return UiTabbedAndTableStyler.isNumericColumn(table, viewCol); }
    public static void showRowDetailsDialog(JTable table) { RowDetailsInspector.open(table); }
    public static void styleScrollPane(JScrollPane scroll) { UiTabbedAndTableStyler.styleScrollPane(scroll); }
    public static void styleEmbeddedTableScrollPane(JScrollPane scroll, JTable table, int visibleRows) { UiTabbedAndTableStyler.styleEmbeddedTableScrollPane(scroll, table, visibleRows); }
    public static void ensureHeadersFit(JTable table) { UiTabbedAndTableStyler.ensureHeadersFit(table); }
    public static void ensureColumnFits(JTable table, int column, String widestValue) { UiTabbedAndTableStyler.ensureColumnFits(table, column, widestValue); }

    // ── Ícones, imagens e cabeçalhos premium (delega para UiIconAndImageSupport) ──────────────
    public static Icon icon(String code, int size, Color color) { return UiIconAndImageSupport.icon(code, size, color); }
    public static Icon icon(String code, int size) { return icon(code, size, Color.WHITE); }
    public static Icon badgedIcon(String code, int size, Color color, int badgeCount, Color badgeBg) { return new BadgedIcon(icon(code, size, color), badgeCount, badgeBg); }
    public static Color semanticColorFor(String iconCode) { return UiIconAndImageSupport.semanticColorFor(iconCode); }
    public static Icon semanticIcon(String iconCode, int size) { return icon(iconCode, size, semanticColorFor(iconCode)); }
    public static void previewOrPrintPdf(Component parent, byte[] pdfBytes, String title) {
        if (pdfBytes == null || pdfBytes.length == 0) return;
        Window owner = SwingUtilities.getWindowAncestor(parent);
        PrintPreviewDialog.show(owner, pdfBytes, title);
    }
    public static Image iconImage(String code, int size, Color color) { return UiIconAndImageSupport.iconImage(code, size, color); }
    public static List<Image> getAppIcons() { return UiIconAndImageSupport.getAppIcons(); }
    public static Image getAppIcon(int size) { return UiIconAndImageSupport.getAppIcon(size); }
    public static byte[] readScaledImage(File file, int maxDim) { return UiIconAndImageSupport.readScaledImage(file, maxDim); }
    public static ImageIcon imageIconFromBytes(byte[] data, int w, int h) { return UiIconAndImageSupport.imageIconFromBytes(data, w, h); }
    public static String iconForTitle(String title) { return UiIconAndImageSupport.iconForTitle(title); }
    public static JComponent buildPremiumHeader(String iconCode, String title, String subtitle) { return UiIconAndImageSupport.buildPremiumHeader(iconCode, title, subtitle); }
    public static JComponent buildPremiumHeader(String title, String subtitle, String iconCode, Color accentColor) { return buildPremiumHeader(iconCode, title, subtitle); }

    // ── Tarefas assíncronas e progresso (delega para UiAsyncTaskSupport) ──────────────────────
    static void beginLoading(Window window) { UiAsyncTaskSupport.beginLoading(window); }
    static void endLoading(Window window) { UiAsyncTaskSupport.endLoading(window); }
    static int activeLoads(Window window) { return UiAsyncTaskSupport.activeLoads(window); }
    public static <T> SwingWorker<T, Void> loadAsync(JComponent scope, Callable<T> fetch, Consumer<T> onDone) {
        return loadAsync(scope, fetch, onDone, error -> {
            if (scope != null) {
                String message = error == null || error.getMessage() == null
                        ? "Não foi possível carregar os dados." : error.getMessage();
                scope.putClientProperty("loadError", message);
                scope.getAccessibleContext().setAccessibleDescription(message);
            }
        });
    }
    public static <T> SwingWorker<T, Void> loadAsync(JComponent scope, Callable<T> fetch, Consumer<T> onDone, Consumer<Throwable> onError) {
        return UiAsyncTaskSupport.loadAsync(scope, fetch, onDone, onError);
    }
    public static <T> SwingWorker<T, Void> submitAsync(AbstractButton button, Callable<T> task, Consumer<T> onDone, Consumer<Throwable> onError) {
        return UiAsyncTaskSupport.submitAsync(button, task, onDone, onError);
    }
    public static JProgressBar createBusyBar() { return UiAsyncTaskSupport.createBusyBar(); }
    public static <T> void runWithProgress(Component owner, String message, Callable<T> task, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        UiAsyncTaskSupport.runWithProgress(owner, message, task, onSuccess, onError);
    }

    /**
     * Contentor composto para o rodapé SOUTH de tabelas com paginação e rodapé adicional (ex.: totais, acções).
     */
    public static class PaginationSouthComposite extends JPanel {
        private JComponent pagination;
        private JComponent extraFooter;

        public PaginationSouthComposite(JComponent pagination) {
            super();
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(false);
            setPagination(pagination);
        }

        public void setPagination(JComponent pagination) {
            if (this.pagination != null) remove(this.pagination);
            this.pagination = pagination;
            if (pagination != null) add(pagination, 0);
            revalidate();
            repaint();
        }

        public void setExtraFooter(JComponent footer) {
            if (this.extraFooter == footer) return;
            if (this.extraFooter != null) remove(this.extraFooter);
            this.extraFooter = footer;
            if (footer != null) add(footer);
            revalidate();
            repaint();
        }

        public JComponent getPagination() { return pagination; }
        public JComponent getExtraFooter() { return extraFooter; }
    }
}
