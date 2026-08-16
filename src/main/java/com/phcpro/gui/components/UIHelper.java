package com.phcpro.gui.components;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.border.Border;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableModel;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class UIHelper {

    // ── Slots de cor sensíveis ao tema (claro/escuro). Os nomes são históricos mas representam
    //    papéis semânticos: BG_DARK = fundo de página, TEXT_LIGHT = texto principal, etc.
    //    Não são final — são reatribuídos por applyTheme(...). Ver Theme.java. ────────────────
    public static Color BG_DARK = Theme.DARK.bg;
    public static Color BG_CARD = Theme.DARK.card;
    public static Color TEXT_LIGHT = Theme.DARK.textPrimary;
    public static Color TEXT_MUTED = Theme.DARK.textMuted;
    public static Color GRID = Theme.DARK.grid;
    public static Color TABLE_HEADER_BG = Theme.DARK.tableHeaderBg;
    public static Color ROW_ALT = Theme.DARK.rowAlt;
    public static Color FIELD_BG = Theme.DARK.fieldBg;
    public static Color BORDER = Theme.DARK.border;
    public static Color SELECTION_BG = Theme.DARK.selectionBg;

    // ── Cores de acento — partilhadas entre temas (não mudam com claro/escuro). Cada acento tem o
    //    seu tom "hover" curado (shade -600 do Tailwind), em vez de .brighter()/.darker() do AWT
    //    (que produz tons lamacentos). ─────────────────────────────────────────────────────────
    public static final Color ACCENT = new Color(139, 92, 246);      // Violet-500 (#8B5CF6)
    public static final Color ACCENT_HOVER = new Color(124, 58, 237); // Violet-600 (#7C3AED)
    public static final Color ACCENT_BLUE = new Color(59, 130, 246); // Blue-500 (#3B82F6)
    public static final Color ACCENT_BLUE_HOVER = new Color(37, 99, 235); // Blue-600 (#2563EB)
    public static final Color APPROVED_GREEN = new Color(16, 185, 129); // Emerald-500 (#10B981)
    public static final Color APPROVED_GREEN_HOVER = new Color(5, 150, 105); // Emerald-600 (#059669)
    public static final Color REJECTED_RED = new Color(239, 68, 68);    // Red-500 (#EF4444)
    public static final Color REJECTED_RED_HOVER = new Color(220, 38, 38); // Red-600 (#DC2626)
    public static final Color PENDING_YELLOW = new Color(245, 158, 11);  // Amber-500 (#F59E0B)
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
    private static final Color SECONDARY = new Color(75, 85, 99);       // Gray-600 (#4B5563)
    private static final Color SECONDARY_HOVER = new Color(107, 114, 128); // Gray-500 (#6B7280)
    public static final Color BUTTON_NEUTRAL = new Color(51, 65, 85);       // Slate-700
    public static final Color BUTTON_NEUTRAL_HOVER = new Color(71, 85, 105); // Slate-600

    // ── Cores de acento por módulo (partilhadas entre temas). Uma só fonte de verdade — a barra de
    //    topo e os painéis referenciam estas em vez de literais Color soltos. ────────────────────
    public static final Color MODULE_DASHBOARD  = ACCENT_BLUE;
    public static final Color MODULE_POS        = new Color(236, 72, 153); // Pink-500
    public static final Color MODULE_COMERCIAL  = ACCENT;
    public static final Color MODULE_COMPRAS    = PENDING_YELLOW;          // Amber-500
    public static final Color MODULE_STOCK      = APPROVED_GREEN;          // Emerald-500
    public static final Color MODULE_FINANCEIRO = APPROVED_GREEN;
    public static final Color MODULE_HR         = ACCENT;
    public static final Color MODULE_CRM        = ACCENT_BLUE;
    public static final Color MODULE_CLIENTES   = new Color(14, 165, 233); // Sky-500
    public static final Color MODULE_FISCAL     = new Color(202, 138, 4);  // Yellow-600
    public static final Color MODULE_ACCOUNTING = new Color(124, 58, 237); // Violet-600
    public static final Color MODULE_APPROVALS  = PENDING_YELLOW;
    public static final Color MODULE_CONFIG     = new Color(107, 114, 128); // Gray-500

    // ── Tipografia: família base num só sítio (evita "Segoe UI" repetido; troca/fallback central). ──
    public static final String FONT = "Segoe UI";

    // ── Escala de raios de canto (px): uma linguagem única em vez de valores soltos (8/10/14/16/20). ──
    public static final int RADIUS_SM = 8;   // realces de nav, tabs, chips
    public static final int RADIUS_MD = 12;  // botões, cards, badges
    public static final int RADIUS_LG = 16;  // superfícies grandes

    public static final int FORM_CONTROL_HEIGHT = 38;
    public static final int DIALOG_FORM_MIN_WIDTH = 560;

    private static Theme activeTheme = Theme.DARK;
    private static final java.util.prefs.Preferences PREFS =
            java.util.prefs.Preferences.userRoot().node("com/phcpro/ui");

    /**
     * Hook opcional para reconstruir a janela principal quando o tema muda. O desktop regista aqui
     * uma rotina que recria o MainFrame já com a paleta nova — garante que componentes pintados e
     * ícones (que fixam a cor na criação) também ficam coerentes. Sem hook (ex.: testes), recorre-se
     * à re-pintura por mapeamento de cores.
     */
    public static Runnable onThemeChanged;

    /**
     * Rotina de <b>logout forçado → ecrã de login</b>. Registada pelo {@code DesktopLauncher}. É
     * invocada quando a assinatura da empresa expira/é suspensa com a app aberta: mostra o aviso e
     * volta ao login (onde o re-login fica bloqueado por {@code allowsLogin} até renovar). Sem hook
     * (ex.: testes/backend), não há efeito.
     */
    public static Runnable onForcedLogout;

    /**
     * Janela principal da aplicação. Registada pelo {@code MainFrame} no arranque para que os modais
     * possam ser limitados e centrados dentro dela — nunca a "sair" para fora da janela principal.
     */
    public static Window mainWindow;

    /**
     * Garante que um diálogo modal fica totalmente contido na janela principal: limita o tamanho a
     * ~94% da janela e centra-o sobre ela (sem ultrapassar as margens). Se a janela principal ainda
     * não estiver registada/visível, usa o ecrã como referência.
     */
    public static void containWithinMain(Window dialog) {
        if (dialog == null) {
            return;
        }
        Rectangle area = mainArea();
        int maxW = (int) (area.width * 0.94);
        int maxH = (int) (area.height * 0.94);
        int w = Math.min(dialog.getWidth(), maxW);
        int h = Math.min(dialog.getHeight(), maxH);
        if (w > 0 && h > 0) {
            dialog.setSize(w, h);
        }
        int x = area.x + Math.max(0, (area.width - dialog.getWidth()) / 2);
        int y = area.y + Math.max(0, (area.height - dialog.getHeight()) / 2);
        dialog.setLocation(x, y);
    }

    /** Limites de referência para modais: a janela principal se registada e visível, senão o ecrã. */
    public static Rectangle mainArea() {
        if (mainWindow != null && mainWindow.isShowing() && mainWindow.getWidth() > 0) {
            return mainWindow.getBounds();
        }
        return new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
    }

    private static boolean modalContainmentInstalled = false;
    private static boolean clampingModal = false;

    /**
     * Regista a janela principal e instala a contenção global de modais: a partir daqui, qualquer
     * diálogo (modal) é mantido **dentro** da janela principal mesmo quando o utilizador o arrasta —
     * não consegue sair para fora dela.
     */
    public static void registerMainWindow(Window window) {
        mainWindow = window;
        installModalContainment();
    }

    private static void installModalContainment() {
        if (modalContainmentInstalled) {
            return;
        }
        modalContainmentInstalled = true;
        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (event.getID() != java.awt.event.ComponentEvent.COMPONENT_MOVED) {
                return;
            }
            Object source = event.getSource();
            // Só diálogos (modais); a própria janela principal e janelas top-level ficam livres.
            if (source instanceof Dialog dialog && dialog != mainWindow) {
                clampInsideMain(dialog);
            }
        }, AWTEvent.COMPONENT_EVENT_MASK);
    }

    /** Reposiciona a janela para ficar totalmente dentro da janela principal, se a ultrapassar. */
    public static void clampInsideMain(Window window) {
        if (clampingModal || window == null || mainWindow == null || !mainWindow.isShowing()) {
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

    public static Theme currentTheme() {
        return activeTheme;
    }

    public static boolean isLight() {
        return activeTheme == Theme.LIGHT;
    }

    /** Lê o tema guardado (por defeito escuro) e aplica os slots + UIManager. Chamar no arranque. */
    public static void loadAndApplySavedTheme() {
        applyTheme(Theme.byId(PREFS.get("theme", "dark")));
    }

    /** Reatribui os slots de cor e os defaults do Swing à paleta do tema indicado (sem persistir). */
    public static void applyTheme(Theme theme) {
        activeTheme = theme;
        BG_DARK = theme.bg;
        BG_CARD = theme.card;
        TEXT_LIGHT = theme.textPrimary;
        TEXT_MUTED = theme.textMuted;
        GRID = theme.grid;
        TABLE_HEADER_BG = theme.tableHeaderBg;
        ROW_ALT = theme.rowAlt;
        FIELD_BG = theme.fieldBg;
        BORDER = theme.border;
        SELECTION_BG = theme.selectionBg;
        initGlobalTheme();
    }

    /**
     * Troca o tema em tempo real: persiste a escolha, reaplica a paleta e re-pinta todas as janelas
     * abertas mapeando, cor a cor, a paleta antiga para a nova.
     */
    public static void setTheme(Theme theme) {
        Theme previous = activeTheme;
        if (theme == previous) {
            return;
        }
        PREFS.put("theme", theme.id);
        applyTheme(theme);
        if (onThemeChanged != null) {
            onThemeChanged.run(); // desktop: reconstrói a janela já com a paleta nova
        } else {
            restyleAllWindows(previous, theme);
        }
    }

    private static void restyleAllWindows(Theme from, Theme to) {
        Color[] oldP = from.palette();
        Color[] newP = to.palette();
        // Nota: NÃO usar updateComponentTreeUI — reinstalaria os UIs default do Look&Feel e
        // destruiria a estilização custom (ex.: tabs). O mapeamento de cores + repaint chega,
        // porque os componentes pintados lêem os slots de cor em tempo de pintura.
        for (Window window : Window.getWindows()) {
            restyleTree(window, oldP, newP);
            window.invalidate();
            window.validate();
            window.repaint();
        }
    }

    /**
     * Cor de texto legível sobre {@code background} — branco ou cinzento-escuro, o que
     * tiver melhor razão de contraste (WCAG 2.1).
     *
     * <p>Existe por causa dos botões que trocam de fundo em runtime (segmented controls como
     * <i>Venda POS | Histórico</i>): o estado inactivo usa {@link #BG_CARD}, que no tema
     * <b>claro</b> é branco puro. Com o texto fixo a branco, o botão ficava invisível até o
     * rato lhe passar por cima. No tema escuro o problema não aparece (card é cinzento).
     */
    public static Color readableTextOn(Color background) {
        if (background == null) return Color.WHITE;
        // Limiar, não "máximo contraste": sobre o azul das acções o preto até contrastaria
        // mais, mas o sistema quer branco. Só fundos claros é que trocam para texto escuro.
        return relativeLuminance(background) > 0.5 ? new Color(17, 24, 39) : Color.WHITE;
    }

    /** Razão de contraste WCAG entre duas cores (1:1 a 21:1). */
    static double contrastRatio(Color a, Color b) {
        double la = relativeLuminance(a);
        double lb = relativeLuminance(b);
        double hi = Math.max(la, lb);
        double lo = Math.min(la, lb);
        return (hi + 0.05) / (lo + 0.05);
    }

    private static double relativeLuminance(Color c) {
        return 0.2126 * toLinear(c.getRed())
             + 0.7152 * toLinear(c.getGreen())
             + 0.0722 * toLinear(c.getBlue());
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

        if (c instanceof JTable table) {
            styleTable(table); // reaplica grid/cabeçalho/seleção a partir dos slots actuais
        }
        if (c instanceof Container container) {
            for (Component child : container.getComponents()) {
                restyleTree(child, oldP, newP);
            }
        }
    }

    /** Devolve a cor equivalente no novo tema se {@code color} pertencer à paleta antiga; senão null. */
    private static Color mapColor(Color color, Color[] oldP, Color[] newP) {
        if (color == null) return null;
        for (int i = 0; i < oldP.length; i++) {
            if (oldP[i].getRGB() == color.getRGB()) {
                return newP[i];
            }
        }
        return null;
    }

    public static ModernButton createPrimaryButton(String text) {
        return new ModernButton(text, ACCENT_BLUE, ACCENT_BLUE_HOVER);
    }

    public static ModernButton createSuccessButton(String text) {
        return new ModernButton(text, APPROVED_GREEN, APPROVED_GREEN_HOVER);
    }

    public static ModernButton createDangerButton(String text) {
        return new ModernButton(text, REJECTED_RED, REJECTED_RED_HOVER);
    }

    /** Acção que exige atenção mas não é destrutiva (ex.: movimento manual de caixa). */
    public static ModernButton createWarningButton(String text) {
        return new ModernButton(text, PENDING_YELLOW, KPI_WARNING_DARK);
    }

    public static ModernButton createSecondaryButton(String text) {
        return new ModernButton(text, SECONDARY, SECONDARY_HOVER);
    }

    public static ActionMenuButton createActionMenuButton(String text) {
        ActionMenuButton button = new ActionMenuButton(text);
        button.setColors(SECONDARY, SECONDARY_HOVER);
        return button;
    }

    /** Botão apenas com ícone, sempre com tooltip e nome acessível. */
    public static ModernButton createIconButton(String accessibleName, String iconCode) {
        if (accessibleName == null || accessibleName.isBlank()) {
            throw new IllegalArgumentException("O botão com ícone deve ter um nome acessível.");
        }
        ModernButton button = createSecondaryButton("");
        button.setIcon(icon(iconCode, 14));
        button.setToolTipText(accessibleName);
        button.getAccessibleContext().setAccessibleName(accessibleName);
        button.setPreferredSize(new Dimension(FORM_CONTROL_HEIGHT, FORM_CONTROL_HEIGHT));
        button.setMinimumSize(new Dimension(FORM_CONTROL_HEIGHT, FORM_CONTROL_HEIGHT));
        return button;
    }

    public static ModernButton createAddLineButton() {
        ModernButton button = createPrimaryButton("Adicionar Linha");
        button.setIcon(icon("fas-plus", 14));
        button.setPreferredSize(new Dimension(180, FORM_CONTROL_HEIGHT));
        button.setMinimumSize(new Dimension(180, FORM_CONTROL_HEIGHT));
        return button;
    }

    public static void initGlobalTheme() {
        try {
            // Style OptionPane and dialogs for Dark Theme
            UIManager.put("Panel.background", BG_DARK);
            UIManager.put("OptionPane.background", BG_DARK);
            UIManager.put("OptionPane.messageForeground", TEXT_LIGHT);
            UIManager.put("OptionPane.messageFont", new Font(FONT, Font.BOLD, 13));
            UIManager.put("OptionPane.buttonFont", new Font(FONT, Font.BOLD, 12));

            // Buttons inside dialogs
            UIManager.put("Button.background", BG_CARD);
            UIManager.put("Button.foreground", TEXT_LIGHT);
            UIManager.put("Button.select", SELECTION_BG);
            UIManager.put("Button.focus", new Color(0, 0, 0, 0));
            // O L&F Metal/Ocean pinta o botão com um gradiente claro próprio e ignora o
            // Button.background — o texto do tema ficaria claro sobre fundo claro (ilegível).
            // Achatar o gradiente para a cor sólida do tema repõe o contraste do texto.
            UIManager.put("Button.gradient",
                    java.util.Arrays.asList(1f, 0f, BG_CARD, BG_CARD, BG_CARD));
            UIManager.put("Button.disabledText", TEXT_MUTED);

            // TabbedPane dark theme consistency
            UIManager.put("TabbedPane.background", BG_CARD);
            UIManager.put("TabbedPane.foreground", TEXT_LIGHT);
            UIManager.put("TabbedPane.selected", ACCENT);
            UIManager.put("TabbedPane.selectedForeground", Color.WHITE);
            UIManager.put("TabbedPane.focusInputMap", new UIDefaults.LazyInputMap(new Object[]{}));
            UIManager.put("TabbedPane.shadow", BG_DARK);
            UIManager.put("TabbedPane.darkShadow", BG_DARK);
            UIManager.put("TabbedPane.light", BG_CARD);
            UIManager.put("TabbedPane.highlight", BG_CARD);
            UIManager.put("TabbedPane.focus", new Color(0, 0, 0, 0));

            // Labels
            UIManager.put("Label.foreground", TEXT_LIGHT);
            UIManager.put("Label.font", new Font(FONT, Font.PLAIN, 13));

            // ComboBox and TextField
            UIManager.put("ComboBox.background", FIELD_BG);
            UIManager.put("ComboBox.foreground", TEXT_LIGHT);
            UIManager.put("ComboBox.selectionBackground", SELECTION_BG);
            UIManager.put("ComboBox.selectionForeground", TEXT_LIGHT);
            UIManager.put("TextField.background", FIELD_BG);
            UIManager.put("TextField.foreground", TEXT_LIGHT);
            UIManager.put("TextField.caretForeground", TEXT_LIGHT);

            // PasswordField and TextArea
            UIManager.put("PasswordField.background", FIELD_BG);
            UIManager.put("PasswordField.foreground", TEXT_LIGHT);
            UIManager.put("PasswordField.caretForeground", TEXT_LIGHT);
            UIManager.put("TextArea.background", BG_DARK);
            UIManager.put("TextArea.foreground", TEXT_LIGHT);
            UIManager.put("TextArea.caretForeground", TEXT_LIGHT);

            // ── Tooltips premium — fundo do tema, borda subtil, fonte Segoe UI 12 ──────────────
            // O ToolTipManager.sharedInstance() controla o atraso; o UIManager controla o visual.
            UIManager.put("ToolTip.background", BG_CARD);
            UIManager.put("ToolTip.foreground", TEXT_LIGHT);
            UIManager.put("ToolTip.font", new Font(FONT, Font.PLAIN, 12));
            UIManager.put("ToolTip.border",
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(BORDER, 1, true),
                            BorderFactory.createEmptyBorder(4, 8, 4, 8)));
            ToolTipManager.sharedInstance().setInitialDelay(600);
            ToolTipManager.sharedInstance().setDismissDelay(8000);
        } catch (Exception ignored) {}
    }

    public static void styleTabbedPane(JTabbedPane tabbedPane) {
        tabbedPane.setOpaque(false);
        tabbedPane.setBackground(BG_DARK);
        tabbedPane.setForeground(TEXT_LIGHT);
        tabbedPane.setFont(new Font(FONT, Font.BOLD, 13));
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.setUI(new BasicTabbedPaneUI() {
            @Override
            protected void installDefaults() {
                super.installDefaults();
                tabAreaInsets = new Insets(0, 0, 0, 0);
                contentBorderInsets = new Insets(0, 0, 0, 0);
                tabInsets = new Insets(9, 16, 9, 16);
                selectedTabPadInsets = new Insets(0, 0, 0, 0);
            }

            @Override
            protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
                                              int x, int y, int w, int h, boolean isSelected) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isSelected ? ACCENT : BG_CARD);
                g2.fillRoundRect(x + 2, y + 2, w - 4, h - 4, RADIUS_SM, RADIUS_SM);
                g2.dispose();
            }

            @Override
            protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics,
                                     int tabIndex, String title, Rectangle textRect, boolean isSelected) {
                g.setFont(font);
                g.setColor(isSelected ? Color.WHITE : TEXT_MUTED);
                int mnemonicIndex = tabPane.getDisplayedMnemonicIndexAt(tabIndex);
                BasicGraphicsUtils.drawStringUnderlineCharAt(g, title, mnemonicIndex,
                        textRect.x, textRect.y + metrics.getAscent());
            }

            @Override
            protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex,
                                          int x, int y, int w, int h, boolean isSelected) {
                if (!isSelected) {
                    return;
                }
                g.setColor(ACCENT_BLUE);
                g.drawLine(x + 8, y + h - 2, x + w - 8, y + h - 2);
            }

            @Override
            protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
                // The surrounding panels already provide the app frame.
            }

            @Override
            protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects,
                                               int tabIndex, Rectangle iconRect, Rectangle textRect,
                                               boolean isSelected) {
                // Avoid low-contrast focus rectangles from the platform Look & Feel.
            }
        });
    }

    /**
     * Estilo de tab PHC — linha de acento (3 px, ACCENT) na base da tab activa; sem fundo cheio.
     * Texto activo: TEXT_LIGHT. Inactivo: TEXT_MUTED. Linha separadora subtil abaixo das tabs.
     * Usar nos paineis internos (ComercialPanel, StockPanel, ...).
     */
    public static void styleTabbedPanePHC(JTabbedPane tabbedPane) {
        tabbedPane.setOpaque(false);
        tabbedPane.setBackground(BG_DARK);
        tabbedPane.setForeground(TEXT_LIGHT);
        tabbedPane.setFont(new Font(FONT, Font.BOLD, 13));
        // SCROLL_TAB_LAYOUT: com muitas tabs (≥5), o WRAP_TAB_LAYOUT padrão cria uma 2ª
        // linha de tabs que fica "atrás" do conteúdo, tornando-a inacessível a cliques.
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPane.setUI(new BasicTabbedPaneUI() {
            @Override
            protected void installDefaults() {
                super.installDefaults();
                tabAreaInsets = new Insets(0, 0, 0, 0);
                contentBorderInsets = new Insets(4, 0, 0, 0);
                tabInsets = new Insets(8, 16, 8, 16);
                selectedTabPadInsets = new Insets(0, 0, 0, 0);
            }

            @Override
            protected void paintTabBackground(Graphics g, int tabPlacement, int tabIndex,
                                              int x, int y, int w, int h, boolean isSelected) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (isSelected) {
                    // Fundo subtil: blend entre BG_DARK e BG_CARD
                    g2.setColor(new Color(
                            (BG_DARK.getRed()   + BG_CARD.getRed())   / 2,
                            (BG_DARK.getGreen() + BG_CARD.getGreen()) / 2,
                            (BG_DARK.getBlue()  + BG_CARD.getBlue())  / 2));
                } else {
                    g2.setColor(BG_DARK);
                }
                g2.fillRoundRect(x + 1, y + 1, w - 2, h - 2, RADIUS_SM, RADIUS_SM);
                g2.dispose();
            }

            @Override
            protected void paintTabBorder(Graphics g, int tabPlacement, int tabIndex,
                                          int x, int y, int w, int h, boolean isSelected) {
                if (!isSelected) return;
                // Linha de acento PHC -- 3 px na base
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ACCENT);
                g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x + 6, y + h - 2, x + w - 6, y + h - 2);
                g2.dispose();
            }

            @Override
            protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics,
                                     int tabIndex, String title, Rectangle textRect, boolean isSelected) {
                g.setFont(font);
                g.setColor(isSelected ? TEXT_LIGHT : TEXT_MUTED);
                int mnemonicIndex = tabPane.getDisplayedMnemonicIndexAt(tabIndex);
                BasicGraphicsUtils.drawStringUnderlineCharAt(g, title, mnemonicIndex,
                        textRect.x, textRect.y + metrics.getAscent());
            }

            @Override
            protected void paintContentBorder(Graphics g, int tabPlacement, int selectedIndex) {
                // Linha separadora subtil entre tabs e conteudo
                int tabH = calculateTabAreaHeight(tabPlacement, runCount, maxTabHeight);
                g.setColor(GRID);
                g.fillRect(0, tabH - 1, tabPane.getWidth(), 1);
            }

            @Override
            protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects,
                                               int tabIndex, Rectangle iconRect, Rectangle textRect,
                                               boolean isSelected) {
                // Sem rectangulo de foco (baixo contraste)
            }
        });
    }

    /**
     * Cor semântica de um valor de estado (verde/vermelho/amarelo) ou {@code null} se não for um
     * estado reconhecido. Usada para pintar o texto da célula e, quando existe coluna "Estado", o
     * tom subtil da linha inteira. Vocabulário PT/EN de retalho.
     */
    private static Color statusColorFor(String upper) {
        if (upper == null) return null;
        if (eqAny(upper, "APPROVED", "APROVADO", "APROVADA", "RESOLVED", "PAID", "PAGO",
                "RECEIVED", "RECEBIDO", "RECEBIDA", "ATIVO", "ACTIVO", "ATIVA", "ACTIVA", "OK", "EM STOCK")) {
            return APPROVED_GREEN;
        }
        if (eqAny(upper, "REJECTED", "REJEITADO", "CANCELLED", "CANCELADO", "CANCELADA",
                "ANULADO", "ANULADA", "ESGOTADO", "SEM STOCK", "INATIVO", "INACTIVO") || upper.startsWith("VENCIDO")) {
            return REJECTED_RED;
        }
        if (upper.contains("PENDING") || upper.contains("PENDENTE") || upper.startsWith("VENCE")
                || upper.equals("BAIXO") || upper.equals("STOCK BAIXO")
                || upper.contains("DÍVIDA") || upper.contains("DIVIDA")
                || upper.contains("PARCIAL") || upper.contains("PARTIALLY")) {
            return PENDING_YELLOW;
        }
        return null;
    }

    private static boolean eqAny(String s, String... opts) {
        for (String o : opts) if (o.equals(s)) return true;
        return false;
    }

    /** Índice (na vista) da coluna de estado da tabela, ou -1 se não houver. */
    private static int statusColumnIndex(JTable t) {
        for (int i = 0; i < t.getColumnCount(); i++) {
            String n = t.getColumnName(i);
            if (n == null) continue;
            String u = n.trim().toLowerCase();
            if (u.equals("estado") || u.equals("status") || u.equals("situação") || u.equals("situacao")) {
                return i;
            }
        }
        return -1;
    }

    /** Cor de estado da linha (a partir da coluna "Estado"), ou {@code null}. */
    private static Color rowStatusColor(JTable t, int viewRow) {
        int col = statusColumnIndex(t);
        if (col < 0) return null;
        Object v = t.getValueAt(viewRow, col);
        return v == null ? null : statusColorFor(v.toString().toUpperCase());
    }

    /** Mistura {@code accent} sobre {@code base} na proporção {@code ratio} (0..1). Adapta-se ao tema. */
    private static Color blend(Color base, Color accent, float ratio) {
        float r = 1f - ratio;
        return new Color(
                Math.round(base.getRed() * r + accent.getRed() * ratio),
                Math.round(base.getGreen() * r + accent.getGreen() * ratio),
                Math.round(base.getBlue() * r + accent.getBlue() * ratio));
    }

    public static void styleTable(JTable table) {
        table.setBackground(BG_CARD);
        table.setForeground(TEXT_LIGHT);
        table.setGridColor(GRID);
        table.setFont(new Font(FONT, Font.PLAIN, 13));
        table.setRowHeight(35);
        table.setSelectionBackground(SELECTION_BG);
        table.setSelectionForeground(TEXT_LIGHT);
        // Grelha completa estilo PHC (linhas verticais + horizontais)
        table.setShowGrid(true);
        table.setShowVerticalLines(true);
        table.setShowHorizontalLines(true);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setBorder(BorderFactory.createLineBorder(GRID));

        // ── Cabeçalho: fundo, fonte, separadores de coluna e alinhamento coerente com a coluna ──
        JTableHeader header = table.getTableHeader();
        header.setBackground(TABLE_HEADER_BG);
        header.setForeground(TEXT_LIGHT);
        header.setFont(new Font(FONT, Font.BOLD, 13));
        header.setPreferredSize(new Dimension(100, 38));
        header.setReorderingAllowed(false);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, GRID));
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                boolean numeric = isNumericColumn(t, col);
                setHorizontalAlignment(numeric ? SwingConstants.RIGHT : SwingConstants.LEFT);
                setBackground(TABLE_HEADER_BG);
                setForeground(TEXT_LIGHT);
                setFont(new Font(FONT, Font.BOLD, 13));
                // separador vertical entre colunas + linha de base (estilo grelha PHC)
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 2, 1, GRID),
                        new EmptyBorder(0, numeric ? 8 : 12, 0, numeric ? 14 : 8)));
                return this;
            }
        };
        // Renderer por omissão do cabeçalho: aplica-se também às colunas recriadas ao definir o modelo.
        header.setDefaultRenderer(headerRenderer);

        // ── Células: zebra, alinhamento numérico à direita, padding e tinta de estado ──
        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);

                boolean numeric = (value instanceof Number) || (value != null && looksNumeric(value.toString()));
                setHorizontalAlignment(numeric ? SwingConstants.RIGHT : SwingConstants.LEFT);
                // mais respiro à direita nos números (alinhamento contabilístico)
                setBorder(new EmptyBorder(0, numeric ? 10 : 12, 0, numeric ? 14 : 10));

                if (isSelected) {
                    setBackground(t.getSelectionBackground());
                } else {
                    // Zebra + tom subtil de estado na LINHA inteira (lido da coluna "Estado", se existir).
                    Color base = row % 2 == 0 ? BG_CARD : ROW_ALT;
                    Color status = rowStatusColor(t, row);
                    setBackground(status == null ? base : blend(base, status, 0.18f));
                }

                if (value != null) {
                    String valStr = value.toString();
                    if (valStr.length() > 30) {
                        setToolTipText("<html><body style='width: 250px; font-family: Segoe UI; font-size: 11px; padding: 4px;'>"
                                       + valStr.replace("\n", "<br>") + "</body></html>");
                    } else {
                        setToolTipText(valStr);
                    }

                    // Numa linha seleccionada o texto fica neutro (selecção limpa, sem competir com a tinta de estado).
                    if (isSelected) {
                        setForeground(TEXT_LIGHT);
                        setFont(getFont().deriveFont(Font.PLAIN));
                    } else {
                        Color statusText = statusColorFor(valStr.toUpperCase());
                        if (statusText != null) {
                            setForeground(statusText);
                            setFont(getFont().deriveFont(Font.BOLD));
                        } else {
                            setForeground(TEXT_LIGHT);
                            setFont(getFont().deriveFont(Font.PLAIN));
                        }
                    }
                } else {
                    setToolTipText(null);
                }
                return this;
            }
        };
        table.setDefaultRenderer(Object.class, cellRenderer);
        table.setDefaultRenderer(Number.class, cellRenderer);
        table.setDefaultRenderer(String.class, cellRenderer);

        // Duplo-clique abre o inspector de detalhes — salvo se a tabela optar por um modal próprio
        // (client property "noRowInspector", ex.: a fila de aprovações usa o seu modal de decisão).
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() != -1
                        && !Boolean.TRUE.equals(table.getClientProperty("noRowInspector"))) {
                    showRowDetailsDialog(table);
                }
            }
        });

        installRowSelector(table);
        installListingFooter(table);
    }

    /**
     * Instala automaticamente um {@link TableFooter} (contagem + soma) por baixo das <b>tabelas de
     * listagem</b> — as que têm um {@code RowSorter} (filtro), o que as distingue das tabelas de
     * formulário/carrinho (sem sorter). Só o faz quando o card que contém a tabela usa
     * {@code BorderLayout} e tem o SOUTH livre. Para excluir uma tabela específica:
     * {@code table.putClientProperty("noTableFooter", Boolean.TRUE)}.
     */
    private static void installListingFooter(JTable table) {
        if (Boolean.TRUE.equals(table.getClientProperty("listingFooterWired"))) return;
        table.putClientProperty("listingFooterWired", Boolean.TRUE);
        table.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) == 0) return;
            maybeAddListingFooter(table);
        });
        // O sorter pode ser instalado (TableFilter) depois de a tabela já estar na árvore.
        table.addPropertyChangeListener("rowSorter", e -> maybeAddListingFooter(table));
    }

    private static void maybeAddListingFooter(JTable table) {
        if (Boolean.TRUE.equals(table.getClientProperty("noTableFooter"))) return;
        if (Boolean.TRUE.equals(table.getClientProperty(ClientTablePagination.DISABLED))) return;
        if (table.getRowSorter() == null) return; // só tabelas de listagem (com filtro)
        JScrollPane sp = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, table);
        if (sp == null || !(sp.getParent() instanceof JComponent parent)) return;
        if (!(parent.getLayout() instanceof BorderLayout bl)) return;
        if (bl.getLayoutComponent(BorderLayout.SOUTH) != null) return;    // SOUTH ocupado (rodapé próprio)
        if (Boolean.TRUE.equals(parent.getClientProperty("tableFooterAdded"))) return;
        parent.putClientProperty("tableFooterAdded", Boolean.TRUE);
        parent.add(ClientTablePagination.install(table), BorderLayout.SOUTH);
        parent.revalidate();
        parent.repaint();
    }

    /**
     * Instala a "calha de selecção" estilo PHC: uma margem estreita do lado esquerdo da grelha
     * (dentro da moldura da tabela, alinhada às linhas) que mostra o marcador ▸ na linha activa.
     * É colocada como {@code rowHeaderView} do {@link JScrollPane} que contém a tabela — fica fixa
     * à esquerda (não rola na horizontal) e <b>não toca no modelo de colunas</b>, pelo que não
     * interfere com a lógica de esconder a coluna de ID por índice nos painéis. A instalação é
     * diferida para quando a tabela entra num {@code JScrollPane}.
     */
    private static void installRowSelector(JTable table) {
        if (Boolean.TRUE.equals(table.getClientProperty("rowSelectorWired"))) return;
        table.putClientProperty("rowSelectorWired", Boolean.TRUE);
        table.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) == 0) return;
            JScrollPane sp = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, table);
            if (sp == null || Boolean.TRUE.equals(sp.getClientProperty("rowSelectorGutter"))) return;
            sp.putClientProperty("rowSelectorGutter", Boolean.TRUE);
            sp.setRowHeaderView(buildRowSelectorGutter(table));
            JPanel corner = new JPanel();
            corner.setBackground(TABLE_HEADER_BG);
            corner.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 1, GRID));
            sp.setCorner(JScrollPane.UPPER_LEFT_CORNER, corner);
        });
    }

    private static JComponent buildRowSelectorGutter(JTable table) {
        final int width = 24;
        JComponent gutter = new JComponent() {
            @Override
            public Dimension getPreferredSize() {
                int h = table.getHeight() > 0 ? table.getHeight() : table.getPreferredSize().height;
                return new Dimension(width, h);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(TABLE_HEADER_BG);
                g2.fillRect(0, 0, getWidth(), getHeight());

                int rows = table.getRowCount();
                g2.setColor(GRID);
                for (int r = 0; r < rows; r++) {
                    Rectangle cr = table.getCellRect(r, 0, true);
                    g2.drawLine(0, cr.y + cr.height - 1, getWidth(), cr.y + cr.height - 1);
                }
                g2.drawLine(getWidth() - 1, 0, getWidth() - 1, getHeight()); // separador junto aos dados

                int sel = table.getSelectedRow();
                if (sel >= 0 && sel < rows) {
                    Rectangle cr = table.getCellRect(sel, 0, true);
                    g2.setColor(SELECTION_BG);
                    g2.fillRect(0, cr.y, getWidth() - 1, cr.height);
                    int cy = cr.y + cr.height / 2;
                    int x = 8;
                    Polygon tri = new Polygon(new int[]{x, x + 7, x}, new int[]{cy - 5, cy, cy + 5}, 3);
                    g2.setColor(ACCENT);
                    g2.fill(tri);
                }
                g2.dispose();
            }
        };
        gutter.setOpaque(true);

        Runnable sync = () -> { gutter.revalidate(); gutter.repaint(); };
        table.getSelectionModel().addListSelectionListener(e -> gutter.repaint());
        table.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { sync.run(); }
        });
        TableModelListener tml = e -> sync.run();
        table.getModel().addTableModelListener(tml);
        table.addPropertyChangeListener("model", evt -> {
            if (evt.getNewValue() instanceof TableModel m) m.addTableModelListener(tml);
            sync.run();
        });
        return gutter;
    }

    /**
     * Decide se um texto representa um valor numérico (quantidade, preço, IVA, total), aceitando
     * separadores de milhar/decimal pt-MZ, sinal negativo e sufixos de moeda/percentagem comuns
     * ("123", "1.234,56", "43,00 MT", "16%", "-5,00 €"). Usado para alinhar números à direita.
     */
    static boolean looksNumeric(String raw) {
        if (raw == null) return false;
        String s = raw.trim();
        if (s.isEmpty()) return false;
        // remove sufixos/símbolos de moeda e percentagem e espaços de agrupamento
        String cleaned = s.replaceAll("(?i)\\s*(MT|MZN|MTn|€|\\$|%)\\s*$", "").trim();
        if (cleaned.isEmpty()) return false;
        return cleaned.matches("-?\\d{1,3}([.\\s]\\d{3})*(,\\d+)?")   // 1.234.567,89
            || cleaned.matches("-?\\d{1,3}(,\\d{3})*(\\.\\d+)?")      // 1,234,567.89
            || cleaned.matches("-?\\d+([.,]\\d+)?");                  // 1234 ou 1234,56
    }

    /**
     * Coluna é considerada numérica se a sua classe declarada o for, ou (para modelos que devolvem
     * tudo como {@code Object}/{@code String}) se as primeiras linhas com valor parecerem números.
     * Permite alinhar o cabeçalho à direita de forma coerente com as células.
     */
    static boolean isNumericColumn(JTable table, int viewCol) {
        int modelCol = table.convertColumnIndexToModel(viewCol);
        Class<?> declared = table.getModel().getColumnClass(modelCol);
        if (Number.class.isAssignableFrom(declared)) return true;
        if (declared != Object.class && declared != String.class) return false;
        int rows = Math.min(table.getRowCount(), 25);
        boolean sawValue = false;
        // Defensivo: um paint pode ocorrer com o modelo a meio de uma mutação (linhas já removidas mas
        // o sorter/ vista ainda com a contagem antiga) → getValueAt lançaria. Nesse caso desiste sem rebentar.
        try {
            for (int r = 0; r < rows && r < table.getRowCount(); r++) {
                Object v = table.getValueAt(r, viewCol);
                if (v == null || v.toString().trim().isEmpty()) continue;
                sawValue = true;
                if (v instanceof Number) continue;
                if (!looksNumeric(v.toString())) return false;
            }
        } catch (RuntimeException ex) {
            return false;
        }
        return sawValue;
    }

    /**
     * Inspector de detalhes de uma linha — modal profissional só-leitura ({@link ModernFormDialog}
     * com cabeçalho premium, contido na janela principal e com scroll responsivo). Cada coluna
     * visível vira um par etiqueta → valor (campo só-leitura, copiável); valores longos ganham uma
     * área com quebra de linha.
     */
    public static void showRowDetailsDialog(JTable table) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) return;

        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.NORTH;

        int rowCount = 0;
        for (int col = 0; col < table.getColumnCount(); col++) {
            // Salta colunas escondidas (ex.: coluna de ID com largura 0)
            if (table.getColumnModel().getColumn(col).getWidth() == 0 &&
                table.getColumnModel().getColumn(col).getMaxWidth() == 0) {
                continue;
            }

            String colName = table.getColumnName(col);
            Object colVal = table.getValueAt(selectedRow, col);
            String valStr = (colVal != null) ? colVal.toString() : "";

            gbc.gridx = 0;
            gbc.gridy = rowCount;
            gbc.weightx = 0.32;
            JLabel nameLabel = new JLabel(colName);
            nameLabel.setFont(new Font(FONT, Font.BOLD, 12));
            nameLabel.setForeground(ACCENT);
            fields.add(nameLabel, gbc);

            gbc.gridx = 1;
            gbc.weightx = 0.68;
            if (valStr.length() > 50 || valStr.contains("\n")) {
                JTextArea valArea = new JTextArea(valStr);
                valArea.setEditable(false);
                valArea.setLineWrap(true);
                valArea.setWrapStyleWord(true);
                styleTextArea(valArea);
                JScrollPane scrollArea = new JScrollPane(valArea);
                scrollArea.setPreferredSize(new Dimension(360, 80));
                scrollArea.setBorder(fieldBorder(BORDER, 1));
                fields.add(scrollArea, gbc);
            } else {
                JTextField valField = new JTextField(valStr);
                valField.setEditable(false);
                styleTextField(valField);
                fields.add(valField, gbc);
            }
            rowCount++;
        }

        new ModernFormDialog(mainWindow, "Detalhes do Registo", "fas-info-circle",
                "Inspeção do registo seleccionado", fields)
                .asReadOnly("Fechar").showDialog();
    }

    /**
     * Vector icon helper backed by Ikonli + FontAwesome 5.
     * Use FontAwesome icon codes like "fas-users", "fas-print", "fas-file-pdf".
     * See https://fontawesome.com/v5/search?o=r&m=free for the catalogue.
     */
    public static javax.swing.Icon icon(String code, int size, Color color) {
        org.kordamp.ikonli.swing.FontIcon fi = org.kordamp.ikonli.swing.FontIcon.of(
                org.kordamp.ikonli.fontawesome5.FontAwesomeSolid.valueOf(toFaEnum(code)));
        fi.setIconSize(size);
        fi.setIconColor(color);
        return fi;
    }

    public static javax.swing.Icon icon(String code, int size) {
        return icon(code, size, Color.WHITE);
    }

    /** Maps "fas-cart-plus" → "CART_PLUS" for the FontAwesomeSolid enum. */
    private static String toFaEnum(String code) {
        String s = code.startsWith("fas-") ? code.substring(4) : code;
        return s.replace('-', '_').toUpperCase();
    }

    /**
     * Renders a FontAwesome icon as a square AWT {@link java.awt.Image} suitable for
     * {@code JFrame.setIconImage(...)} / {@code JDialog.setIconImage(...)}.
     */
    public static java.awt.Image iconImage(String code, int size, Color color) {
        javax.swing.Icon ic = icon(code, size, color);
        java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(
                size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = img.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        ic.paintIcon(null, g, 0, 0);
        g.dispose();
        return img;
    }

    /**
     * Lê um ficheiro de imagem e devolve-o como PNG reduzido (lado máximo {@code maxDim}px,
     * mantendo proporção). Para guardar thumbnails de produto na BD sem inchar.
     */
    public static byte[] readScaledImage(java.io.File file, int maxDim) {
        try {
            java.awt.image.BufferedImage src = javax.imageio.ImageIO.read(file);
            if (src == null) return null;
            int w = src.getWidth(), h = src.getHeight();
            double scale = Math.min(1.0, (double) maxDim / Math.max(w, h));
            int nw = Math.max(1, (int) Math.round(w * scale));
            int nh = Math.max(1, (int) Math.round(h * scale));
            java.awt.image.BufferedImage dst = new java.awt.image.BufferedImage(nw, nh, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = dst.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(src, 0, 0, nw, nh, null);
            g.dispose();
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(dst, "png", out);
            return out.toByteArray();
        } catch (java.io.IOException ex) {
            return null;
        }
    }

    /** Constrói um {@link ImageIcon} a partir de bytes, escalado para {@code w}×{@code h}. Null se inválido. */
    public static ImageIcon imageIconFromBytes(byte[] data, int w, int h) {
        if (data == null || data.length == 0) return null;
        try {
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(data));
            if (img == null) return null;
            return new ImageIcon(img.getScaledInstance(w, h, java.awt.Image.SCALE_SMOOTH));
        } catch (java.io.IOException ex) {
            return null;
        }
    }

    /**
     * Estiliza um {@link JScrollPane} para o tema activo:
     * borda da cor BORDER do tema, viewport transparente, scroll bars finas ({@link SlimScrollBarUI}).
     */
    public static void styleScrollPane(JScrollPane scroll) {
        scroll.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);
        scroll.getVerticalScrollBar().setUI(new SlimScrollBarUI());
        scroll.getVerticalScrollBar().setOpaque(false);
        scroll.getHorizontalScrollBar().setUI(new SlimScrollBarUI());
        scroll.getHorizontalScrollBar().setOpaque(false);
        // UX transversal das tabelas — só quando o conteúdo é uma JTable (cada instalador guarda-se
        // a si próprio). Central: cobre todas as listagens. Ver UI_TABELAS_UX_SPEC.
        TableNavigator.install(scroll);   // barra lateral (fora da tabela) + auto-hide + teclado
        TableEmptyState.install(scroll);  // "Sem registos." quando vazio
        TableContextMenu.install(scroll); // botão direito: copiar/ir topo-fundo
    }

    /**
     * Carrega dados fora do EDT (evita "congelar" a UI nas chamadas HTTP do cliente-fino) e aplica o
     * resultado no EDT quando chega, com cursor de espera na janela. {@code fetch} corre em segundo
     * plano; {@code onDone} corre no EDT. Ver UI_TABELAS_UX_SPEC §4.
     */
    public static <T> SwingWorker<T, Void> loadAsync(JComponent scope,
                                                      java.util.concurrent.Callable<T> fetch,
                                                      java.util.function.Consumer<T> onDone) {
        return loadAsync(scope, fetch, onDone, error -> {
            if (scope != null) {
                String message = error == null || error.getMessage() == null
                        ? "Não foi possível carregar os dados." : error.getMessage();
                scope.putClientProperty("loadError", message);
                scope.getAccessibleContext().setAccessibleDescription(message);
            }
        });
    }

    /**
     * Carregamentos em curso por janela. O cursor de espera é <b>contado</b>, não guardado:
     * guardar o cursor anterior falha quando dois carregamentos se sobrepõem (um painel que
     * carrega várias tabelas), porque o segundo guarda o cursor de espera do primeiro e
     * "repõe-no" no fim — a janela ficava com o cursor de loading preso para sempre.
     *
     * <p>Só tocado no EDT (o {@code loadAsync} arranca no EDT e o {@code done()} corre no EDT),
     * por isso não precisa de sincronização. {@link java.util.WeakHashMap} para não segurar
     * janelas fechadas.
     */
    private static final java.util.Map<java.awt.Window, Integer> ACTIVE_LOADS = new java.util.WeakHashMap<>();

    /** Marca mais um carregamento em curso; põe o cursor de espera no primeiro. */
    static void beginLoading(java.awt.Window window) {
        if (window == null) return;
        int active = ACTIVE_LOADS.merge(window, 1, Integer::sum);
        if (active == 1) {
            window.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
        }
    }

    /** Fecha um carregamento; devolve o cursor normal só quando o último termina. */
    static void endLoading(java.awt.Window window) {
        if (window == null) return;
        Integer active = ACTIVE_LOADS.get(window);
        if (active == null) return;
        if (active <= 1) {
            ACTIVE_LOADS.remove(window);
            window.setCursor(java.awt.Cursor.getDefaultCursor());
        } else {
            ACTIVE_LOADS.put(window, active - 1);
        }
    }

    /** Quantos carregamentos estão em curso nesta janela (para testes). */
    static int activeLoads(java.awt.Window window) {
        return window == null ? 0 : ACTIVE_LOADS.getOrDefault(window, 0);
    }

    /**
     * Variante completa de loading assíncrono: propaga contexto, entrega erro no EDT e ignora
     * resposta de um tenant que deixou de estar activo enquanto o pedido estava em curso.
     */
    public static <T> SwingWorker<T, Void> loadAsync(JComponent scope,
                                                      java.util.concurrent.Callable<T> fetch,
                                                      java.util.function.Consumer<T> onDone,
                                                      java.util.function.Consumer<Throwable> onError) {
        java.awt.Window window = scope == null ? null : SwingUtilities.getWindowAncestor(scope);
        beginLoading(window);
        if (scope != null) {
            scope.putClientProperty("loading", Boolean.TRUE);
            scope.putClientProperty("loadError", null);
        }
        com.phcpro.architecture.security.CurrentUserContext.UserSession capturedUser =
                com.phcpro.architecture.security.CurrentUserContext.findCurrentUser();
        Long capturedCompany = com.phcpro.architecture.security.CurrentUserContext.findCurrentCompanyId();

        SwingWorker<T, Void> worker = new javax.swing.SwingWorker<>() {
            @Override protected T doInBackground() throws Exception {
                if (capturedUser != null) {
                    com.phcpro.architecture.security.CurrentUserContext.setCurrentUser(
                            capturedUser.username(), capturedUser.role());
                }
                if (capturedCompany != null) {
                    com.phcpro.architecture.security.CurrentUserContext.setCurrentCompanyId(capturedCompany);
                }
                try {
                    return fetch.call();
                } finally {
                    com.phcpro.architecture.security.CurrentUserContext.clear();
                }
            }

            @Override protected void done() {
                try {
                    T result = get();
                    Long activeCompany = com.phcpro.architecture.security.CurrentUserContext.findCurrentCompanyId();
                    if (java.util.Objects.equals(capturedCompany, activeCompany) && onDone != null) {
                        onDone.accept(result);
                    }
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if (onError != null) onError.accept(cause);
                } finally {
                    if (scope != null) scope.putClientProperty("loading", Boolean.FALSE);
                    endLoading(window);
                }
            }
        };
        worker.execute();
        return worker;
    }

    /** Submissão remota protegida contra duplo clique; reabilita o botão em sucesso ou erro. */
    public static <T> SwingWorker<T, Void> submitAsync(AbstractButton button,
                                                        java.util.concurrent.Callable<T> task,
                                                        java.util.function.Consumer<T> onDone,
                                                        java.util.function.Consumer<Throwable> onError) {
        if (Boolean.TRUE.equals(button.getClientProperty("submitting"))) return null;
        button.putClientProperty("submitting", Boolean.TRUE);
        button.setEnabled(false);
        return loadAsync(button, task, result -> {
            button.putClientProperty("submitting", Boolean.FALSE);
            button.setEnabled(true);
            if (onDone != null) onDone.accept(result);
        }, error -> {
            button.putClientProperty("submitting", Boolean.FALSE);
            button.setEnabled(true);
            if (onError != null) onError.accept(error);
        });
    }

    /**
     * Wraps tall modal-dialog content in a vertical scroll pane capped to ~78% of
     * screen height so the OK/Cancel buttons stay visible on small displays.
     * If the content already fits, it is returned unchanged.
     */
    public static JComponent makeDialogScrollable(JPanel content) {
        // Limita à altura da janela principal (não do ecrã) para o modal não sair fora dela.
        return makeDialogScrollable(content, (int) (mainArea().height * 0.80));
    }

    /** Como acima, mas com altura máxima explícita (para reservar espaço a cabeçalho/botões). */
    public static JComponent makeDialogScrollable(JPanel content, int maxHeight) {
        Dimension contentSize = content.getPreferredSize();
        if (contentSize.height <= maxHeight) {
            return content;
        }
        int width = Math.max(contentSize.width, DIALOG_FORM_MIN_WIDTH) + 24;
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setPreferredSize(new Dimension(width, maxHeight));
        return scroll;
    }

    // ── Cabeçalho premium de modais (badge com ícone + título + subtítulo + divisória) ──────────
    // Usado tanto pelo ModernFormDialog como pelos modais legados baseados em JOptionPane, para que
    // TODOS os formulários tenham o mesmo topo profissional (ícone no topo, padrão pedido).

    /**
     * Deduz um código de ícone FontAwesome a partir do título do modal (domínio &gt; verbo).
     * Vocabulário alinhado com a skill {@code phc-icons}.
     */
    public static String iconForTitle(String title) {
        String t = title == null ? "" : title.toLowerCase();
        // 1) Domínio (ganha sobre o verbo: "Editar Fornecedor" → camião, não lápis)
        if (t.contains("transfer"))                            return "fas-exchange-alt";
        if (t.contains("fatura"))                              return "fas-file-invoice";
        if (t.contains("recibo") || t.contains("recebimento")) return "fas-receipt";
        if (t.contains("nota de crédito") || t.contains("nota de credito")) return "fas-file-invoice-dollar";
        if (t.contains("nota de débito") || t.contains("nota de debito"))   return "fas-file-invoice-dollar";
        if (t.contains("fornecedor"))                          return "fas-truck";
        if (t.contains("encomenda"))                           return "fas-file-signature";
        if (t.contains("compra") || t.contains("entrada"))     return "fas-download";
        if (t.contains("ajuste") || t.contains("contagem") || t.contains("invent")) return "fas-clipboard-list";
        if (t.contains("categoria"))                           return "fas-tags";
        if (t.contains("lote") || t.contains("validade"))      return "fas-boxes";
        if (t.contains("cliente"))                             return "fas-address-book";
        if (t.contains("produto"))                             return "fas-boxes";
        if (t.contains("armazém") || t.contains("armazem"))    return "fas-warehouse";
        if (t.contains("promo"))                               return "fas-percent";
        if (t.contains("funcionário") || t.contains("funcionario") || t.contains("colaborador")) return "fas-users";
        if (t.contains("salário") || t.contains("salario") || t.contains("recibo de"))            return "fas-file-invoice-dollar";
        if (t.contains("falta"))                               return "fas-user-times";
        if (t.contains("férias") || t.contains("ferias"))      return "fas-umbrella-beach";
        if (t.contains("despesa"))                             return "fas-receipt";
        if (t.contains("imposto") || t.contains("iva") || t.contains("taxa") || t.contains("fiscal")) return "fas-percent";
        if (t.contains("utilizador") || t.contains("user"))    return "fas-user-plus";
        // 2) Verbo genérico, quando o domínio não é reconhecido
        if (t.contains("editar"))                              return "fas-edit";
        if (t.contains("pagar") || t.contains("pagamento"))    return "fas-money-bill-wave";
        if (t.contains("novo") || t.contains("nova") || t.contains("cadastrar")
                || t.contains("registar") || t.contains("adicionar") || t.contains("criar"))
            return "fas-plus";
        return "fas-file-alt";
    }

    /** Badge quadrado de cantos arredondados com o ícone ao centro (acento sólido, ícone branco). */
    private static JComponent iconBadge(String iconCode, int badgeSize, int iconSize) {
        final javax.swing.Icon ic = icon(iconCode, iconSize, Color.WHITE);
        JComponent badge = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ACCENT_BLUE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), RADIUS_MD, RADIUS_MD);
                int ix = (getWidth() - ic.getIconWidth()) / 2;
                int iy = (getHeight() - ic.getIconHeight()) / 2;
                ic.paintIcon(this, g2, ix, iy);
                g2.dispose();
            }
        };
        badge.setOpaque(false);
        badge.setPreferredSize(new Dimension(badgeSize, badgeSize));
        badge.setMinimumSize(new Dimension(badgeSize, badgeSize));
        badge.setMaximumSize(new Dimension(badgeSize, badgeSize));
        return badge;
    }

    /**
     * Cabeçalho premium: badge com ícone à esquerda, título e subtítulo à direita, divisória em
     * baixo. Se {@code iconCode} for null, é deduzido do título; subtítulo opcional (null = sem linha).
     */
    public static JComponent buildPremiumHeader(String iconCode, String title, String subtitle) {
        String code = iconCode != null ? iconCode : iconForTitle(title);

        JPanel texts = new JPanel();
        texts.setOpaque(false);
        texts.setLayout(new BoxLayout(texts, BoxLayout.Y_AXIS));
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font(FONT, Font.BOLD, 19));
        titleLbl.setForeground(TEXT_LIGHT);
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        texts.add(titleLbl);
        if (subtitle != null && !subtitle.isBlank()) {
            JLabel subLbl = new JLabel(subtitle);
            subLbl.setFont(new Font(FONT, Font.PLAIN, 12));
            subLbl.setForeground(TEXT_MUTED);
            subLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            subLbl.setBorder(new EmptyBorder(2, 0, 0, 0));
            texts.add(subLbl);
        }

        JPanel row = new JPanel(new BorderLayout(14, 0));
        row.setOpaque(false);
        JComponent badge = iconBadge(code, 46, 22);
        JPanel badgeWrap = new JPanel(new GridBagLayout()); // centra o badge verticalmente
        badgeWrap.setOpaque(false);
        badgeWrap.add(badge);
        row.add(badgeWrap, BorderLayout.WEST);
        row.add(texts, BorderLayout.CENTER);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(row, BorderLayout.CENTER);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, GRID),
                new EmptyBorder(0, 0, 14, 0)));
        return header;
    }

    public static void styleEmbeddedTableScrollPane(JScrollPane scroll, JTable table, int visibleRows) {
        styleScrollPane(scroll);
        table.setFillsViewportHeight(true);
        table.setPreferredScrollableViewportSize(new Dimension(420, table.getRowHeight() * visibleRows));
        int headerHeight = table.getTableHeader() != null
                ? table.getTableHeader().getPreferredSize().height
                : 0;
        int height = headerHeight + (table.getRowHeight() * visibleRows) + 26;
        scroll.setPreferredSize(new Dimension(420, height));
        scroll.setMinimumSize(new Dimension(280, height));
        scroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
    }

    public static void styleTextField(JTextField field) {
        field.setBackground(FIELD_BG);
        field.setForeground(TEXT_LIGHT);
        field.setCaretColor(TEXT_LIGHT);
        field.setFont(new Font(FONT, Font.PLAIN, 13));
        installFocusBorder(field);
        applyFormControlHeight(field);
    }

    /** Marca um controlo inválido sem abrir modal; a mensagem fica disponível à acessibilidade. */
    public static void markFieldInvalid(JComponent component, String message) {
        component.putClientProperty("validationError", message);
        component.setBorder(fieldBorder(REJECTED_RED, 2));
        component.getAccessibleContext().setAccessibleDescription(message);
    }

    /** Remove o estado inválido e repõe a borda canónica do controlo. */
    public static void clearFieldInvalid(JComponent component) {
        component.putClientProperty("validationError", null);
        component.setBorder(fieldBorder(BORDER, 1));
        component.getAccessibleContext().setAccessibleDescription(null);
    }

    /** Aplica o estado visual e semântico de campo somente leitura. */
    public static void setReadOnly(JTextComponent component, boolean readOnly) {
        component.setEditable(!readOnly);
        component.putClientProperty("readOnly", readOnly);
        component.setBackground(readOnly ? BG_CARD : FIELD_BG);
        component.setForeground(readOnly ? TEXT_MUTED : TEXT_LIGHT);
        component.setFocusable(!readOnly);
    }

    /** Borda arredondada de campo com a cor de linha indicada + padding interno uniforme. */
    private static Border fieldBorder(Color line, int thickness) {
        return BorderFactory.createCompoundBorder(
                new LineBorder(line, thickness, true),
                new EmptyBorder(6 - (thickness - 1), 10, 6 - (thickness - 1), 10));
    }

    /**
     * Instala a borda de campo profissional com **realce de foco**: borda normal ({@code BORDER})
     * que acende na cor de acento ao ganhar foco e volta ao normal ao perder. Fonte única para
     * todos os campos (text field, password, text area, combo) — DRY. Protegido contra re-skins
     * (não empilha listeners).
     */
    private static void installFocusBorder(JComponent component) {
        component.setBorder(fieldBorder(BORDER, 1));
        if (Boolean.TRUE.equals(component.getClientProperty("focusBorderWired"))) return;
        component.putClientProperty("focusBorderWired", Boolean.TRUE);
        component.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                if (Boolean.TRUE.equals(component.getClientProperty("noFocusBorder"))) return;
                component.setBorder(fieldBorder(ACCENT, 2));
            }
            @Override public void focusLost(FocusEvent e) {
                if (Boolean.TRUE.equals(component.getClientProperty("noFocusBorder"))) return;
                component.setBorder(fieldBorder(BORDER, 1));
            }
        });
    }

    /**
     * Liga um {@link Runnable} às alterações de texto de um campo (inserção, remoção, edição).
     * Útil para pesquisa incremental — corre o callback sempre que o utilizador escreve.
     */
    public static void onTextChange(JTextField field, Runnable onChange) {
        field.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { onChange.run(); }
        });
    }

    /**
     * Modal profissional para introduzir **uma linha de texto obrigatória** (ex.: motivo de anulação/
     * rejeição). Devolve o texto (sem espaços nas pontas) ou {@code null} se cancelado. Vazio mantém
     * o modal aberto com mensagem. Substitui os {@code JOptionPane.showInputDialog} de motivo.
     */
    public static String promptRequiredText(String title, String iconCode, String subtitle, String label) {
        JTextArea field = new JTextArea(3, 24);
        field.setLineWrap(true);
        field.setWrapStyleWord(true);
        JPanel form = createDialogForm(label, field);
        String[] holder = new String[1];
        ModernFormDialog dlg = new ModernFormDialog(mainWindow, title, iconCode, subtitle, form)
                .setConfirmButton("Confirmar", "fas-check");
        dlg.setOnSave(() -> {
            String v = field.getText().trim();
            if (v.isEmpty()) throw new IllegalArgumentException("Este campo é obrigatório.");
            holder[0] = v;
        });
        return dlg.showDialog() ? holder[0] : null;
    }

    /**
     * Modal profissional para introduzir **um valor monetário** (aceita vírgula ou ponto decimal).
     * Devolve o {@link java.math.BigDecimal} ou {@code null} se cancelado. Valor não numérico ou
     * abaixo de {@code min} mantém o modal aberto com mensagem.
     */
    public static java.math.BigDecimal promptAmount(String title, String iconCode, String subtitle,
                                                    String label, java.math.BigDecimal min) {
        JTextField field = new JTextField();
        JPanel form = createDialogForm(label, field);
        java.math.BigDecimal[] holder = new java.math.BigDecimal[1];
        ModernFormDialog dlg = new ModernFormDialog(mainWindow, title, iconCode, subtitle, form)
                .setConfirmButton("Confirmar", "fas-check");
        dlg.setOnSave(() -> {
            java.math.BigDecimal v;
            try {
                v = new java.math.BigDecimal(field.getText().trim().replace(',', '.'));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Introduza um valor numérico válido.");
            }
            if (min != null && v.compareTo(min) < 0) {
                throw new IllegalArgumentException("O valor deve ser maior ou igual a " + min + ".");
            }
            holder[0] = v;
        });
        return dlg.showDialog() ? holder[0] : null;
    }

    /** Traduz o código de perfil (role) para uma etiqueta profissional em PT-MZ. */
    public static String humanRole(String role) {
        if (role == null || role.isBlank()) return "—";
        return switch (role.trim().toUpperCase()) {
            case "ADMIN" -> "Administrador";
            case "MANAGER" -> "Gestor";
            case "EMPLOYEE" -> "Funcionário";
            case "SUPERADMIN" -> "Administrador da Plataforma";
            default -> role;
        };
    }

    /** Mantém o código técnico como valor do select, apresentando apenas a etiqueta humana. */
    public static void humanizeRoleCombo(JComboBox<String> combo) {
        @SuppressWarnings("unchecked")
        ListCellRenderer<? super String> original = (ListCellRenderer<? super String>) combo.getRenderer();
        combo.setRenderer((list, value, index, selected, focus) -> {
            Component rendered = original.getListCellRendererComponent(list, value, index, selected, focus);
            if (rendered instanceof JLabel label) label.setText(humanRole(value));
            return rendered;
        });
    }

    public static void stylePasswordField(JPasswordField field) {
        field.setBackground(FIELD_BG);
        field.setForeground(TEXT_LIGHT);
        field.setCaretColor(TEXT_LIGHT);
        field.setFont(new Font(FONT, Font.PLAIN, 13));
        installFocusBorder(field);
        applyFormControlHeight(field);
    }

    public static void styleTextArea(JTextArea area) {
        area.setBackground(FIELD_BG);
        area.setForeground(TEXT_LIGHT);
        area.setCaretColor(TEXT_LIGHT);
        area.setFont(new Font(FONT, Font.PLAIN, 13));
        installFocusBorder(area);
    }

    public static void styleComboBox(JComboBox<?> combo) {
        ListCellRenderer<?> originalRenderer = combo.getRenderer();
        combo.setBackground(FIELD_BG);
        combo.setForeground(TEXT_LIGHT);
        combo.setFont(new Font(FONT, Font.PLAIN, 13));
        installFocusBorder(combo);
        applyFormControlHeight(combo);
        flattenComboArrow(combo);
        // O fundo da lista do popup segue o tema (SELECTION_BG/FIELD_BG), senão em tema claro
        // a opção destacada ficava com fundo escuro e texto escuro = ilegível.
        combo.setRenderer(new ListCellRenderer<Object>() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                @SuppressWarnings("unchecked")
                ListCellRenderer<Object> delegate = (ListCellRenderer<Object>) originalRenderer;
                Component rendered = delegate == null
                        ? new DefaultListCellRenderer().getListCellRendererComponent(
                                (JList<Object>) list, value, index, isSelected, cellHasFocus)
                        : delegate.getListCellRendererComponent(
                                (JList<Object>) list, value, index, isSelected, cellHasFocus);
                rendered.setBackground(isSelected ? SELECTION_BG : FIELD_BG);
                rendered.setForeground(TEXT_LIGHT);
                if (rendered instanceof JComponent component) {
                    component.setBorder(new EmptyBorder(5, 8, 5, 8));
                    component.setOpaque(true);
                }
                return rendered;
            }
        });
    }

    /** Tradução central de estados frequentes; valores desconhecidos ficam legíveis. */
    public static String humanStatus(String status) {
        if (status == null || status.isBlank()) return "—";
        return switch (status.trim().toUpperCase()) {
            case "ACTIVE" -> "Activo";
            case "INACTIVE" -> "Inactivo";
            case "ACTIVA" -> "Activa";
            case "INACTIVA" -> "Inactiva";
            case "OPEN" -> "Aberto";
            case "CLOSED" -> "Fechado";
            case "PENDING", "PENDING_APPROVAL" -> "Pendente";
            case "APPROVED" -> "Aprovado";
            case "REJECTED" -> "Rejeitado";
            case "CANCELLED" -> "Anulado";
            case "PAID" -> "Paga";
            case "PARTIALLY_PAID" -> "Parcialmente paga";
            case "OVERDUE" -> "Em atraso";
            default -> status.trim().replace('_', ' ');
        };
    }

    /** Achata o botão de seta do combo (sem bevel 3D do Metal) para o combo parecer uma peça só. */
    private static void flattenComboArrow(JComboBox<?> combo) {
        for (Component child : combo.getComponents()) {
            if (child instanceof AbstractButton arrow) {
                arrow.setBorder(new EmptyBorder(0, 6, 0, 6));
                arrow.setBorderPainted(false);
                arrow.setContentAreaFilled(false);
                arrow.setFocusable(false);
                arrow.setOpaque(false);
            }
        }
    }

    private static void applyFormControlHeight(JComponent component) {
        Dimension preferred = component.getPreferredSize();
        int width = Math.max(preferred.width, component.getMinimumSize().width);
        Dimension uniform = new Dimension(width, FORM_CONTROL_HEIGHT);
        component.setPreferredSize(uniform);
        component.setMinimumSize(new Dimension(0, FORM_CONTROL_HEIGHT));
    }

    /**
     * Barra de progresso indeterminada, fina e na cor de acento — para estados de "a carregar"
     * profissionais (login, checkout, geração de PDF, …). Começa escondida; mostrar com
     * {@code setVisible(true)} enquanto a tarefa corre num {@code SwingWorker}.
     */
    public static JProgressBar createBusyBar() {
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        bar.setBorderPainted(false);
        bar.setBackground(BG_CARD);
        bar.setForeground(ACCENT);
        bar.setPreferredSize(new Dimension(0, 6));
        return bar;
    }

    /**
     * Corre uma tarefa demorada (rede/BD/PDF) fora do EDT com feedback profissional: mostra um
     * pequeno diálogo modal "a processar…" com barra indeterminada enquanto a tarefa corre num
     * {@link SwingWorker}, e devolve o resultado (ou o erro) já de volta no EDT. Não congela a UI.
     */
    public static <T> void runWithProgress(Component owner, String message,
                                           java.util.concurrent.Callable<T> task,
                                           java.util.function.Consumer<T> onSuccess,
                                           java.util.function.Consumer<Throwable> onError) {
        Window win = owner == null ? null : SwingUtilities.getWindowAncestor(owner);
        JDialog dialog = new JDialog(win, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);

        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setBackground(BG_CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true), new EmptyBorder(18, 26, 18, 26)));
        JLabel label = new JLabel(message);
        label.setForeground(TEXT_LIGHT);
        label.setFont(new Font(FONT, Font.BOLD, 13));
        panel.add(label, BorderLayout.NORTH);
        panel.add(createBusyBar(), BorderLayout.SOUTH);
        dialog.setContentPane(panel);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(260, dialog.getHeight()));
        dialog.setLocationRelativeTo(win);

        // O CurrentUserContext é ThreadLocal — captura no EDT e repõe na thread de fundo, senão
        // os Services veem empresa/utilizador vazios ("o documento pertence a outra empresa").
        // Usa as variantes find* (nullable): o superadmin não tem empresa e a captura não pode lançar.
        com.phcpro.architecture.security.CurrentUserContext.UserSession capturedUser =
                com.phcpro.architecture.security.CurrentUserContext.findCurrentUser();
        Long capturedCompany =
                com.phcpro.architecture.security.CurrentUserContext.findCurrentCompanyId();

        SwingWorker<T, Void> worker = new SwingWorker<>() {
            @Override protected T doInBackground() throws Exception {
                if (capturedUser != null) {
                    com.phcpro.architecture.security.CurrentUserContext.setCurrentUser(
                            capturedUser.username(), capturedUser.role());
                }
                if (capturedCompany != null) {
                    com.phcpro.architecture.security.CurrentUserContext.setCurrentCompanyId(capturedCompany);
                }
                try {
                    return task.call();
                } finally {
                    com.phcpro.architecture.security.CurrentUserContext.clear();
                }
            }
            @Override protected void done() {
                dialog.dispose();
                try {
                    T result = get();
                    if (onSuccess != null) onSuccess.accept(result);
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    if (onError != null) onError.accept(cause);
                }
            }
        };
        worker.execute();
        dialog.setVisible(true); // modal: bloqueia até done() fazer dispose (mas o EDT continua a despachar)
    }

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

    /** Formulário de diálogo em grelha de 2 colunas (label sobre campo em cada célula). */
    public static JPanel createDialogForm(Object... labelsAndComponents) {
        return createDialogForm(2, labelsAndComponents);
    }

    /**
     * Formulário de diálogo em grelha com {@code columns} colunas. Cada par (label, componente) ocupa
     * uma célula com o label (a negrito/acento) por cima do campo. Os componentes são estilizados
     * automaticamente. Layout em grelha = aspecto profissional e mais compacto (menos altura).
     */
    public static JPanel createDialogForm(int columns, Object... labelsAndComponents) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BG_DARK);
        panel.setBorder(new EmptyBorder(10, 14, 10, 14));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1.0 / columns;
        gbc.insets = new Insets(6, 7, 6, 7);

        int pair = 0;
        for (int i = 0; i + 1 < labelsAndComponents.length; i += 2, pair++) {
            Object labelObj = labelsAndComponents[i];
            Object compObj = labelsAndComponents[i + 1];

            if (compObj instanceof Component) {
                Component c = (Component) compObj;
                if (c instanceof JTextField) {
                    if (c instanceof JPasswordField) {
                        stylePasswordField((JPasswordField) c);
                    } else {
                        styleTextField((JTextField) c);
                    }
                } else if (c instanceof JComboBox) {
                    styleComboBox((JComboBox<?>) c);
                } else if (c instanceof JTextArea) {
                    styleTextArea((JTextArea) c);
                }
            }

            JPanel cell = new JPanel(new BorderLayout(0, 4));
            cell.setOpaque(false);
            if (labelObj instanceof String rawLabel && compObj instanceof JComponent input) {
                boolean required = rawLabel.contains("*");
                String cleanLabel = rawLabel.replace("*", "").trim();
                cell.add(new FormField(cleanLabel, input, required, null), BorderLayout.CENTER);
            } else {
                if (labelObj instanceof Component) cell.add((Component) labelObj, BorderLayout.NORTH);
                if (compObj instanceof Component) cell.add((Component) compObj, BorderLayout.CENTER);
            }

            gbc.gridx = pair % columns;
            gbc.gridy = pair / columns;
            panel.add(cell, gbc);
        }
        Dimension preferred = panel.getPreferredSize();
        panel.setPreferredSize(new Dimension(Math.max(DIALOG_FORM_MIN_WIDTH, preferred.width), preferred.height));
        panel.setMinimumSize(new Dimension(DIALOG_FORM_MIN_WIDTH, preferred.height));
        return panel;
    }
}
