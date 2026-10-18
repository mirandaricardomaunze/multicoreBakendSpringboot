package mz.multicore.erp.gui.tv;

import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.SimplePieChart;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.comercial.dto.InvoiceLineDTO;
import mz.multicore.erp.modules.purchases.dto.BusinessUnitDTO;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Painel Executivo para Sala de Reuniões e TV de Administração (War Room / Full Screen TV Dashboard).
 * Projetado para exibição contínua em projetores e televisões em alta resolução.
 * Suporta Tema Escuro (Dark Slate) e Tema Claro (Executive Light), auto-refresh e tela cheia (F11).
 */
public class ExecutiveTvDashboardDialog extends JDialog {

    public enum Period {
        HOJE("Hoje"),
        ESTA_SEMANA("Esta Semana"),
        ESTE_MES("Mês Atual"),
        ESTE_ANO("Este Ano");

        private final String label;
        Period(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    private final ComercialApiClient comercialApiClient;
    private final Long companyId;

    // Estado do Tema (Alternável pelo utilizador)
    private boolean lightMode = false;

    // Componentes de Topo
    private JPanel headerContainer;
    private JLabel clockLabel;
    private Period currentPeriod = Period.ESTE_MES;
    private final Map<Period, ModernButton> periodButtons = new HashMap<>();
    private JComboBox<String> businessUnitCombo;
    private ModernButton autoSlideBtn;
    private ModernButton fullScreenBtn;
    private ModernButton themeToggleBtn;
    private JLabel statusHint;

    // Timers
    private Timer clockTimer;
    private Timer autoRefreshTimer;
    private Timer slideshowTimer;
    private boolean isSlideshowActive = false;
    private int slideshowIndex = 0; // 0 = consolidado, 1..N = unidades individuais
    private int secondsToNextSlide = 15;
    private static final int SLIDESHOW_DURATION_SECONDS = 15;

    // Estado dos Dados
    private List<BusinessUnitDTO> businessUnits = new ArrayList<>();
    private List<InvoiceDTO> allInvoices = new ArrayList<>();

    // Área Central Dinâmica
    private JPanel mainContentContainer;
    private boolean isFullScreen = false;

    public ExecutiveTvDashboardDialog(Window owner, ComercialApiClient comercialApiClient, Long companyId) {
        super(owner, "Painel Executivo de Negócios • Sala de Reuniões", ModalityType.MODELESS);
        this.comercialApiClient = comercialApiClient;
        this.companyId = companyId != null ? companyId : 1L;

        initComponents();
        setupKeyBindings();
        startTimers();
        loadData();
    }

    // Paleta de Cores Dinâmica (Suporta Tema Escuro e Claro)
    private Color getBgColor() {
        return lightMode ? new Color(241, 245, 249) : new Color(15, 23, 42); // Slate 100 vs Slate 900
    }

    private Color getCardBgColor() {
        return lightMode ? Color.WHITE : new Color(30, 41, 59); // White vs Slate 800
    }

    private Color getCardBorderColor() {
        return lightMode ? new Color(226, 232, 240) : new Color(51, 65, 85); // Slate 200 vs Slate 700
    }

    private Color getTextLightColor() {
        return lightMode ? new Color(15, 23, 42) : new Color(248, 250, 252); // Slate 900 vs Slate 50
    }

    private Color getTextMutedColor() {
        return lightMode ? new Color(100, 116, 139) : new Color(148, 163, 184); // Slate 500 vs Slate 400
    }

    private Color getHeaderBgColor() {
        return lightMode ? new Color(255, 255, 255, 245) : new Color(15, 23, 42, 245);
    }

    private void initComponents() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(getBgColor());
        setLayout(new BorderLayout());

        // Cabeçalho Estruturado em 2 Linhas (Zero sobreposição)
        headerContainer = buildExecutiveHeader();
        add(headerContainer, BorderLayout.NORTH);

        // Área Central
        mainContentContainer = new JPanel(new BorderLayout());
        mainContentContainer.setOpaque(false);
        mainContentContainer.setBorder(new EmptyBorder(12, 16, 16, 16));
        add(mainContentContainer, BorderLayout.CENTER);

        // Rodapé com atalhos
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(6, 16, 8, 16));

        JLabel shortcutsHint = new JLabel("Atalhos da Sala: [F11] Alternar Tela Cheia  •  [Espaço] Pausar/Continuar Rodízio  •  [ESC] Fechar Janela");
        shortcutsHint.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        shortcutsHint.setForeground(getTextMutedColor());
        footer.add(shortcutsHint, BorderLayout.WEST);

        statusHint = new JLabel("Atualização Automática: Activa (a cada 45s)");
        statusHint.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        statusHint.setForeground(new Color(16, 185, 129));
        footer.add(statusHint, BorderLayout.EAST);

        add(footer, BorderLayout.SOUTH);

        // Tamanho inicial grande adaptado ao ecrã
        Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(screen.width - 40, 1366), Math.min(screen.height - 60, 768));
        setLocationRelativeTo(getOwner());

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stopTimers();
            }
        });
    }

    /**
     * Constrói o cabeçalho executivo dividido em duas linhas perfeitamente organizadas:
     * - Linha 1: Título / Marca, Relógio Centralizado e Ações de Janela (Tema, Metas, Tela Cheia, Fechar).
     * - Linha 2: Barra de Filtros de Período e Seletor de Negócio com Rodízio.
     */
    private JPanel buildExecutiveHeader() {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setOpaque(true);
        container.setBackground(getHeaderBgColor());
        container.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, getCardBorderColor()),
                new EmptyBorder(8, 16, 8, 16)
        ));

        // ════════════════════════════════════════════════════════════════════════
        // LINHA 1: MARCA + RELÓGIO + BOTÕES DE SISTEMA
        // ════════════════════════════════════════════════════════════════════════
        JPanel row1 = new JPanel(new BorderLayout(12, 0));
        row1.setOpaque(false);

        // Esquerda: Marca e Título
        JPanel brandBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        brandBox.setOpaque(false);

        JLabel iconLbl = new JLabel(UIHelper.icon("fas-tv", 16, UIHelper.ACCENT_BLUE));
        brandBox.add(iconLbl);

        JLabel brandLbl = new JLabel("MULTICORE ERP");
        brandLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 15));
        brandLbl.setForeground(getTextLightColor());
        brandBox.add(brandLbl);

        JLabel badgeLbl = new JLabel("  SALA DE REUNIÕES  ");
        badgeLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 10));
        badgeLbl.setForeground(UIHelper.PENDING_YELLOW);
        badgeLbl.setOpaque(true);
        badgeLbl.setBackground(new Color(245, 158, 11, 25));
        badgeLbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(245, 158, 11, 80), 1, true),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)
        ));
        brandBox.add(badgeLbl);
        row1.add(brandBox, BorderLayout.WEST);

        // Centro: Relógio Digital em tempo real
        clockLabel = new JLabel("—");
        clockLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        clockLabel.setForeground(getTextLightColor());
        clockLabel.setHorizontalAlignment(JLabel.CENTER);
        row1.add(clockLabel, BorderLayout.CENTER);

        // Direita: Controles de Janela e Tema
        JPanel sysControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        sysControls.setOpaque(false);

        // Botão de Alternar Tema Claro / Escuro
        themeToggleBtn = UIHelper.createSecondaryButton(lightMode ? "Tema Escuro" : "Tema Claro");
        themeToggleBtn.setIcon(UIHelper.icon(lightMode ? "fas-moon" : "fas-sun", 12, UIHelper.PENDING_YELLOW));
        themeToggleBtn.setToolTipText("Alternar entre Tema Claro e Tema Escuro");
        themeToggleBtn.setPreferredSize(new Dimension(115, 28));
        themeToggleBtn.addActionListener(e -> toggleTheme());
        sysControls.add(themeToggleBtn);

        // Botão de Metas
        ModernButton goalsBtn = UIHelper.createSecondaryButton("Metas");
        goalsBtn.setIcon(UIHelper.icon("fas-bullseye", 12, UIHelper.PENDING_YELLOW));
        goalsBtn.setToolTipText("Configurar metas mensais da empresa e selecionar módulos de negócio");
        goalsBtn.setPreferredSize(new Dimension(85, 28));
        goalsBtn.addActionListener(e -> {
            if ((businessUnits == null || businessUnits.isEmpty()) && comercialApiClient != null) {
                try {
                    List<BusinessUnitDTO> loaded = comercialApiClient.getActiveBusinessUnits(companyId);
                    if (loaded != null && !loaded.isEmpty()) {
                        businessUnits = loaded;
                    }
                } catch (Exception ignored) {}
            }
            BusinessUnitGoalManager.showGoalsDialog(this, businessUnits, this::renderCurrentView);
        });
        sysControls.add(goalsBtn);

        // Botão de Desempenho dos Vendedores
        ModernButton sellersBtn = UIHelper.createSecondaryButton("Vendedores");
        sellersBtn.setIcon(UIHelper.icon("fas-users", 12, UIHelper.ACCENT_CYAN));
        sellersBtn.setToolTipText("Consultar desempenho e ranking individual da força de vendas");
        sellersBtn.setPreferredSize(new Dimension(105, 28));
        sellersBtn.addActionListener(e -> SellerPerformanceDialog.showDialog(this, getFilteredInvoices(), currentPeriod.getLabel(), lightMode));
        sysControls.add(sellersBtn);

        // Botão Tela Cheia
        fullScreenBtn = UIHelper.createSecondaryButton(isFullScreen ? "Janela" : "Tela Cheia");
        fullScreenBtn.setIcon(UIHelper.icon(isFullScreen ? "fas-compress" : "fas-expand", 12, UIHelper.ACCENT_BLUE));
        fullScreenBtn.setPreferredSize(new Dimension(105, 28));
        fullScreenBtn.addActionListener(e -> toggleFullScreen());
        sysControls.add(fullScreenBtn);

        // Botão Fechar
        ModernButton closeBtn = UIHelper.createSecondaryButton("Sair");
        closeBtn.setIcon(UIHelper.icon("fas-times", 12, UIHelper.REJECTED_RED));
        closeBtn.setPreferredSize(new Dimension(75, 28));
        closeBtn.addActionListener(e -> dispose());
        sysControls.add(closeBtn);

        row1.add(sysControls, BorderLayout.EAST);
        container.add(row1);
        container.add(Box.createVerticalStrut(8));

        // ════════════════════════════════════════════════════════════════════════
        // LINHA 2: FILTROS DE PERÍODO + SELEÇÃO DE NEGÓCIO + RODÍZIO
        // ════════════════════════════════════════════════════════════════════════
        JPanel row2 = new JPanel(new BorderLayout(12, 0));
        row2.setOpaque(false);

        // Esquerda: Período
        JPanel periodBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        periodBox.setOpaque(false);

        JLabel perLbl = new JLabel("Período: ");
        perLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        perLbl.setForeground(getTextMutedColor());
        periodBox.add(perLbl);

        for (Period p : Period.values()) {
            ModernButton btn = UIHelper.createSecondaryButton(p.getLabel());
            btn.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
            btn.setPreferredSize(new Dimension(btn.getPreferredSize().width + 10, 26));
            btn.addActionListener(e -> setPeriod(p));
            periodButtons.put(p, btn);
            periodBox.add(btn);
        }
        updatePeriodStyles();
        row2.add(periodBox, BorderLayout.WEST);

        // Direita: Seletor de Negócio e Botão de Rodízio TV
        JPanel filterControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filterControls.setOpaque(false);

        JLabel buLbl = new JLabel("Unidade de Negócio: ");
        buLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        buLbl.setForeground(getTextMutedColor());
        filterControls.add(buLbl);

        businessUnitCombo = new JComboBox<>(new String[]{"Todas as Unidades (Visão Consolidada)"});
        businessUnitCombo.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
        businessUnitCombo.setPreferredSize(new Dimension(240, 28));
        UIHelper.styleComboBox(businessUnitCombo);
        businessUnitCombo.addActionListener(e -> {
            if (!isSlideshowActive) {
                renderCurrentView();
            }
        });
        filterControls.add(businessUnitCombo);

        autoSlideBtn = UIHelper.createSecondaryButton("Rodízio TV (15s)");
        autoSlideBtn.setIcon(UIHelper.icon("fas-play", 11, UIHelper.APPROVED_GREEN));
        autoSlideBtn.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        autoSlideBtn.setToolTipText("Inicia apresentação automática rotativa a cada 15 segundos");
        autoSlideBtn.setPreferredSize(new Dimension(135, 28));
        autoSlideBtn.addActionListener(e -> toggleSlideshow());
        filterControls.add(autoSlideBtn);

        row2.add(filterControls, BorderLayout.EAST);
        container.add(row2);

        return container;
    }

    private void toggleTheme() {
        lightMode = !lightMode;
        getContentPane().setBackground(getBgColor());

        if (headerContainer != null) {
            headerContainer.setBackground(getHeaderBgColor());
            headerContainer.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, getCardBorderColor()),
                    new EmptyBorder(8, 16, 8, 16)
            ));
        }

        themeToggleBtn.setText(lightMode ? "Tema Escuro" : "Tema Claro");
        themeToggleBtn.setIcon(UIHelper.icon(lightMode ? "fas-moon" : "fas-sun", 12, UIHelper.PENDING_YELLOW));

        updatePeriodStyles();
        renderCurrentView();
    }

    private void setupKeyBindings() {
        JComponent root = getRootPane();

        // F11: Alternar Tela Cheia
        root.registerKeyboardAction(
                e -> toggleFullScreen(),
                KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        // ESC: Fechar
        root.registerKeyboardAction(
                e -> dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        // Espaço: Pausar/Continuar Rodízio
        root.registerKeyboardAction(
                e -> toggleSlideshow(),
                KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    private void startTimers() {
        DateTimeFormatter clockFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy  •  HH:mm:ss");
        clockTimer = new Timer(1000, e -> clockLabel.setText(LocalDateTime.now().format(clockFmt)));
        clockTimer.start();
        clockLabel.setText(LocalDateTime.now().format(clockFmt));

        autoRefreshTimer = new Timer(45000, e -> loadData());
        autoRefreshTimer.start();

        slideshowTimer = new Timer(1000, e -> {
            if (!isSlideshowActive) return;
            secondsToNextSlide--;
            if (secondsToNextSlide <= 0) {
                secondsToNextSlide = SLIDESHOW_DURATION_SECONDS;
                advanceSlideshow();
            }
            autoSlideBtn.setText(String.format("Rodízio (%ds)", secondsToNextSlide));
        });
    }

    private void stopTimers() {
        if (clockTimer != null) clockTimer.stop();
        if (autoRefreshTimer != null) autoRefreshTimer.stop();
        if (slideshowTimer != null) slideshowTimer.stop();
    }

    private void toggleSlideshow() {
        isSlideshowActive = !isSlideshowActive;
        if (isSlideshowActive) {
            secondsToNextSlide = SLIDESHOW_DURATION_SECONDS;
            autoSlideBtn.setIcon(UIHelper.icon("fas-pause", 11, UIHelper.PENDING_YELLOW));
            autoSlideBtn.setText(String.format("Rodízio (%ds)", secondsToNextSlide));
            slideshowTimer.start();
        } else {
            autoSlideBtn.setIcon(UIHelper.icon("fas-play", 11, UIHelper.APPROVED_GREEN));
            autoSlideBtn.setText("Rodízio TV (15s)");
            slideshowTimer.stop();
        }
    }

    private void advanceSlideshow() {
        int totalOptions = businessUnitCombo.getItemCount();
        if (totalOptions <= 1) return;
        slideshowIndex = (slideshowIndex + 1) % totalOptions;
        businessUnitCombo.setSelectedIndex(slideshowIndex);
        renderCurrentView();
    }

    private void toggleFullScreen() {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();

        dispose();
        if (!isFullScreen) {
            setUndecorated(true);
            try {
                if (gd.isFullScreenSupported()) {
                    gd.setFullScreenWindow(this);
                } else {
                    setBounds(ge.getMaximumWindowBounds());
                }
            } catch (Exception ex) {
                setBounds(ge.getMaximumWindowBounds());
            }
            fullScreenBtn.setText("Janela");
            fullScreenBtn.setIcon(UIHelper.icon("fas-compress", 12, UIHelper.ACCENT_BLUE));
            isFullScreen = true;
        } else {
            if (gd.getFullScreenWindow() == this) {
                gd.setFullScreenWindow(null);
            }
            setUndecorated(false);
            Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
            setSize(Math.min(screen.width - 40, 1366), Math.min(screen.height - 60, 768));
            setLocationRelativeTo(getOwner());
            fullScreenBtn.setText("Tela Cheia");
            fullScreenBtn.setIcon(UIHelper.icon("fas-expand", 12, UIHelper.ACCENT_BLUE));
            isFullScreen = false;
        }
        setVisible(true);
    }

    private void setPeriod(Period p) {
        if (p == null) return;
        this.currentPeriod = p;
        updatePeriodStyles();
        renderCurrentView();
    }

    private void updatePeriodStyles() {
        for (Map.Entry<Period, ModernButton> entry : periodButtons.entrySet()) {
            boolean active = entry.getKey() == currentPeriod;
            ModernButton btn = entry.getValue();
            if (active) {
                btn.setColors(UIHelper.ACCENT_BLUE, UIHelper.ACCENT_BLUE_HOVER);
                btn.setForeground(Color.WHITE);
                btn.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
            } else {
                Color idleBg = lightMode ? Color.WHITE : new Color(30, 41, 59);
                Color idleHover = lightMode ? new Color(241, 245, 249) : new Color(51, 65, 85);
                btn.setColors(idleBg, idleHover);
                btn.setForeground(getTextLightColor());
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(getCardBorderColor(), 1, true),
                        BorderFactory.createEmptyBorder(3, 9, 3, 9)
                ));
            }
        }
    }

    private void loadData() {
        SwingUtilities.invokeLater(() -> {
            try {
                if (comercialApiClient != null) {
                    List<BusinessUnitDTO> loadedUnits = comercialApiClient.getActiveBusinessUnits(companyId);
                    if (loadedUnits != null) {
                        businessUnits = loadedUnits;
                        String selected = (String) businessUnitCombo.getSelectedItem();
                        businessUnitCombo.removeAllItems();
                        businessUnitCombo.addItem("Todas as Unidades (Visão Consolidada)");
                        for (BusinessUnitDTO bu : businessUnits) {
                            businessUnitCombo.addItem(bu.name());
                        }
                        if (selected != null) {
                            businessUnitCombo.setSelectedItem(selected);
                        }
                    }

                    List<InvoiceDTO> loadedInvoices = comercialApiClient.getAllInvoices();
                    if (loadedInvoices != null) {
                        allInvoices = loadedInvoices;
                    }
                }
            } catch (Exception ex) {
                // Silencioso
            }
            renderCurrentView();
        });
    }

    private void renderCurrentView() {
        mainContentContainer.removeAll();

        int selectedIdx = businessUnitCombo.getSelectedIndex();
        if (selectedIdx <= 0 || businessUnits.isEmpty()) {
            mainContentContainer.add(buildConsolidatedView(), BorderLayout.CENTER);
        } else {
            BusinessUnitDTO unit = businessUnits.get(selectedIdx - 1);
            mainContentContainer.add(buildUnitFocusedView(unit), BorderLayout.CENTER);
        }

        mainContentContainer.revalidate();
        mainContentContainer.repaint();
    }

    // ════════════════════════════════════════════════════════════════════════════
    // VISÃO 1: CONSOLIDADA (TODAS AS UNIDADES DE NEGÓCIO LADO A LADO)
    // ════════════════════════════════════════════════════════════════════════════
    private JPanel buildConsolidatedView() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);

        BigDecimal globalGoal = BusinessUnitGoalManager.getGlobalGoal();
        BigDecimal globalBilled = calculateTotalBilled(null);
        BigDecimal difference = globalBilled.subtract(globalGoal);
        double globalPct = globalGoal.compareTo(BigDecimal.ZERO) > 0
                ? globalBilled.multiply(BigDecimal.valueOf(100)).divide(globalGoal, 1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        JPanel masterCard = new ModernPanel(14, getCardBgColor(), getCardBgColor());
        masterCard.setLayout(new BorderLayout(0, 10));
        masterCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(getCardBorderColor(), 1, true),
                new EmptyBorder(14, 18, 14, 18)
        ));

        // Título do Master Card
        JPanel masterTitleRow = new JPanel(new BorderLayout());
        masterTitleRow.setOpaque(false);

        JLabel masterTitle = new JLabel("META GLOBAL DA EMPRESA • " + currentPeriod.getLabel().toUpperCase());
        masterTitle.setFont(new Font(UIHelper.FONT, Font.BOLD, 15));
        masterTitle.setForeground(getTextLightColor());
        masterTitleRow.add(masterTitle, BorderLayout.WEST);

        JLabel statusBadge = buildStatusBadge(globalPct);
        masterTitleRow.add(statusBadge, BorderLayout.EAST);
        masterCard.add(masterTitleRow, BorderLayout.NORTH);

        // Super Barra de Progresso
        GoalProgressBar masterBar = new GoalProgressBar(32);
        masterBar.setLightMode(lightMode);
        masterBar.setValues(globalBilled, globalGoal);
        masterCard.add(masterBar, BorderLayout.CENTER);

        // 4 KPIs em linha
        JPanel kpiRow = new JPanel(new GridLayout(1, 4, 12, 0));
        kpiRow.setOpaque(false);
        kpiRow.setBorder(new EmptyBorder(8, 0, 0, 0));

        kpiRow.add(buildTvKpi("META ESTIPULADA", String.format("%,.2f MT", globalGoal), "Valor orçamentado", UIHelper.ACCENT_CYAN));
        kpiRow.add(buildTvKpi("TOTAL FATURADO", String.format("%,.2f MT", globalBilled), String.format("%.1f%% da meta atingida", globalPct),
                globalPct >= 100.0 ? UIHelper.APPROVED_GREEN : UIHelper.ACCENT_BLUE));

        String diffLabel = difference.compareTo(BigDecimal.ZERO) >= 0 ? "SUPERÁVIT" : "FALTAM PARA A META";
        Color diffColor = difference.compareTo(BigDecimal.ZERO) >= 0 ? UIHelper.APPROVED_GREEN : UIHelper.PENDING_YELLOW;
        kpiRow.add(buildTvKpi(diffLabel, String.format("%,.2f MT", difference.abs()),
                difference.compareTo(BigDecimal.ZERO) >= 0 ? "Meta superada com êxito" : "Necessário acelerar vendas", diffColor));

        // Ritmo Diário (Run-rate)
        int daysInMonth = YearMonth.now().lengthOfMonth();
        int dayOfMonth = Math.max(1, LocalDate.now().getDayOfMonth());
        BigDecimal currentDailyRate = globalBilled.divide(BigDecimal.valueOf(dayOfMonth), 2, RoundingMode.HALF_UP);
        int daysRemaining = Math.max(1, daysInMonth - dayOfMonth);
        BigDecimal requiredDailyRate = difference.compareTo(BigDecimal.ZERO) < 0
                ? difference.abs().divide(BigDecimal.valueOf(daysRemaining), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        String runRateText = difference.compareTo(BigDecimal.ZERO) >= 0
                ? String.format("Média: %,.2f MT/dia", currentDailyRate)
                : String.format("Meta dia: %,.2f MT", requiredDailyRate);

        kpiRow.add(buildTvKpi("RITMO DIÁRIO", String.format("%,.2f MT/dia", currentDailyRate), runRateText, UIHelper.KPI_PURPLE_SOFT));

        masterCard.add(kpiRow, BorderLayout.SOUTH);
        panel.add(masterCard, BorderLayout.NORTH);

        // Centro: Grid de Negócios + Gráfico de Share & Top Produtos
        JPanel centerSplit = new JPanel(new BorderLayout(14, 0));
        centerSplit.setOpaque(false);

        JPanel unitsGridContainer = new JPanel(new BorderLayout());
        unitsGridContainer.setOpaque(false);

        JLabel unitsTitle = new JLabel("PROGRESSO POR LINHA DE DISTRIBUIÇÃO / UNIDADE DE NEGÓCIO");
        unitsTitle.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        unitsTitle.setForeground(getTextLightColor());
        unitsTitle.setBorder(new EmptyBorder(0, 4, 8, 0));
        unitsGridContainer.add(unitsTitle, BorderLayout.NORTH);

        int cols = businessUnits.size() > 4 ? 3 : 2;
        JPanel unitsCards = new JPanel(new GridLayout(0, cols, 10, 10));
        unitsCards.setOpaque(false);

        for (BusinessUnitDTO bu : businessUnits) {
            unitsCards.add(buildBusinessUnitCard(bu));
        }

        JScrollPane scrollUnits = new JScrollPane(unitsCards);
        scrollUnits.setOpaque(false);
        scrollUnits.getViewport().setOpaque(false);
        scrollUnits.setBorder(BorderFactory.createEmptyBorder());
        unitsGridContainer.add(scrollUnits, BorderLayout.CENTER);

        centerSplit.add(unitsGridContainer, BorderLayout.CENTER);

        // Lado Direito: Donut de Share + Top Produtos
        JPanel rightWidgets = new JPanel();
        rightWidgets.setLayout(new BoxLayout(rightWidgets, BoxLayout.Y_AXIS));
        rightWidgets.setOpaque(false);
        rightWidgets.setPreferredSize(new Dimension(380, 0));

        SimplePieChart shareChart = buildShareChart();
        ModernPanel shareCard = new ModernPanel(14, getCardBgColor(), getCardBgColor());
        shareCard.setLayout(new BorderLayout());
        shareCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(getCardBorderColor(), 1, true),
                new EmptyBorder(8, 8, 8, 8)
        ));
        shareCard.add(shareChart, BorderLayout.CENTER);
        rightWidgets.add(shareCard);
        rightWidgets.add(Box.createVerticalStrut(10));

        JPanel topProductsCard = buildTopProductsTvWidget(null);
        rightWidgets.add(topProductsCard);

        centerSplit.add(rightWidgets, BorderLayout.EAST);
        panel.add(centerSplit, BorderLayout.CENTER);

        return panel;
    }

    private JPanel buildBusinessUnitCard(BusinessUnitDTO bu) {
        ModernPanel card = new ModernPanel(12, getCardBgColor(), getCardBgColor());
        card.setLayout(new BorderLayout(0, 8));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(getCardBorderColor(), 1, true),
                new EmptyBorder(12, 14, 12, 14)
        ));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JLabel nameLbl = new JLabel(bu.name());
        nameLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 14));
        nameLbl.setForeground(getTextLightColor());
        top.add(nameLbl, BorderLayout.WEST);

        boolean isIncluded = BusinessUnitGoalManager.isUnitIncluded(bu.id());
        BigDecimal billed = calculateTotalBilled(bu.id());
        BigDecimal goal = BusinessUnitGoalManager.getGoalForUnit(bu.id());
        double pct = goal.compareTo(BigDecimal.ZERO) > 0
                ? billed.multiply(BigDecimal.valueOf(100)).divide(goal, 1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        JLabel badge;
        if (!isIncluded) {
            badge = new JLabel("  FORA DA META  ");
            badge.setFont(new Font(UIHelper.FONT, Font.BOLD, 10));
            badge.setForeground(getTextMutedColor());
            badge.setOpaque(true);
            badge.setBackground(lightMode ? new Color(241, 245, 249) : new Color(51, 65, 85, 40));
            badge.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(getCardBorderColor(), 1, true),
                    new EmptyBorder(2, 6, 2, 6)
            ));
        } else {
            badge = buildStatusBadge(pct);
        }
        top.add(badge, BorderLayout.EAST);
        card.add(top, BorderLayout.NORTH);

        GoalProgressBar bar = new GoalProgressBar(22);
        bar.setLightMode(lightMode);
        bar.setValues(billed, isIncluded ? goal : BigDecimal.ZERO);
        card.add(bar, BorderLayout.CENTER);

        JPanel btm = new JPanel(new BorderLayout());
        btm.setOpaque(false);

        String supplier = bu.supplierName() != null && !bu.supplierName().isBlank() ? bu.supplierName() : "Multi-fornecedor";
        JLabel supLbl = new JLabel(supplier);
        supLbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        supLbl.setForeground(getTextMutedColor());
        btm.add(supLbl, BorderLayout.WEST);

        String targetStr = isIncluded ? String.format("Meta: %,.2f MT", goal) : "Não orçamentado";
        JLabel targetLbl = new JLabel(targetStr);
        targetLbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        targetLbl.setForeground(getTextMutedColor());
        btm.add(targetLbl, BorderLayout.EAST);

        card.add(btm, BorderLayout.SOUTH);
        return card;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // VISÃO 2: FOCADA NUMA UNIDADE DE NEGÓCIO ESPECÍFICA (EX: UNILEVER)
    // ════════════════════════════════════════════════════════════════════════════
    private JPanel buildUnitFocusedView(BusinessUnitDTO bu) {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setOpaque(false);

        BigDecimal unitGoal = BusinessUnitGoalManager.getGoalForUnit(bu.id());
        BigDecimal unitBilled = calculateTotalBilled(bu.id());
        BigDecimal difference = unitBilled.subtract(unitGoal);
        double pct = unitGoal.compareTo(BigDecimal.ZERO) > 0
                ? unitBilled.multiply(BigDecimal.valueOf(100)).divide(unitGoal, 1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        JPanel focusCard = new ModernPanel(14, getCardBgColor(), getCardBgColor());
        focusCard.setLayout(new BorderLayout(0, 12));
        focusCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(getCardBorderColor(), 1, true),
                new EmptyBorder(16, 20, 16, 20)
        ));

        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);

        String supInfo = bu.supplierName() != null ? "  •  Fornecedor: " + bu.supplierName() : "";
        JLabel titleLbl = new JLabel(bu.name().toUpperCase() + supInfo + "  [" + currentPeriod.getLabel().toUpperCase() + "]");
        titleLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 16));
        titleLbl.setForeground(getTextLightColor());
        titleRow.add(titleLbl, BorderLayout.WEST);

        titleRow.add(buildStatusBadge(pct), BorderLayout.EAST);
        focusCard.add(titleRow, BorderLayout.NORTH);

        GoalProgressBar bar = new GoalProgressBar(34);
        bar.setLightMode(lightMode);
        bar.setValues(unitBilled, unitGoal);
        focusCard.add(bar, BorderLayout.CENTER);

        JPanel kpiRow = new JPanel(new GridLayout(1, 4, 12, 0));
        kpiRow.setOpaque(false);

        kpiRow.add(buildTvKpi("META DA UNIDADE", String.format("%,.2f MT", unitGoal), "Meta orçamentada", UIHelper.ACCENT_CYAN));
        kpiRow.add(buildTvKpi("FATURADO REALIZADO", String.format("%,.2f MT", unitBilled), String.format("%.1f%% alcançado", pct),
                pct >= 100.0 ? UIHelper.APPROVED_GREEN : UIHelper.ACCENT_BLUE));

        String diffLbl = difference.compareTo(BigDecimal.ZERO) >= 0 ? "SUPERÁVIT" : "A ATINGIR";
        Color diffCol = difference.compareTo(BigDecimal.ZERO) >= 0 ? UIHelper.APPROVED_GREEN : UIHelper.PENDING_YELLOW;
        kpiRow.add(buildTvKpi(diffLbl, String.format("%,.2f MT", difference.abs()),
                difference.compareTo(BigDecimal.ZERO) >= 0 ? "Meta ultrapassada" : "Falta faturar", diffCol));

        long invoiceCount = getFilteredInvoices().stream()
                .filter(inv -> bu.id().equals(inv.businessUnitId()))
                .count();
        kpiRow.add(buildTvKpi("FATURAS EMITIDAS", String.valueOf(invoiceCount), "Documentos faturados no período", UIHelper.KPI_PURPLE_SOFT));

        focusCard.add(kpiRow, BorderLayout.SOUTH);
        panel.add(focusCard, BorderLayout.NORTH);

        JPanel centerGrid = new JPanel(new GridLayout(1, 2, 14, 0));
        centerGrid.setOpaque(false);

        centerGrid.add(buildTopProductsTvWidget(bu.id()));
        centerGrid.add(buildRecentInvoicesTvWidget(bu.id()));

        panel.add(centerGrid, BorderLayout.CENTER);
        return panel;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // WIDGETS E COMPONENTES AUXILIARES
    // ════════════════════════════════════════════════════════════════════════════
    private JPanel buildTvKpi(String title, String value, String subtitle, Color accentColor) {
        Color kpiBg = lightMode ? new Color(248, 250, 252) : new Color(15, 23, 42, 180);
        Color kpiBorder = lightMode ? new Color(226, 232, 240) : new Color(51, 65, 85, 120);

        ModernPanel panel = new ModernPanel(10, kpiBg, kpiBg);
        panel.setLayout(new BorderLayout(0, 4));
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(kpiBorder, 1, true),
                new EmptyBorder(8, 12, 8, 12)
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 10));
        titleLbl.setForeground(getTextMutedColor());
        panel.add(titleLbl, BorderLayout.NORTH);

        JLabel valLbl = new JLabel(value);
        valLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 18));
        valLbl.setForeground(accentColor);
        panel.add(valLbl, BorderLayout.CENTER);

        JLabel subLbl = new JLabel(subtitle);
        subLbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
        subLbl.setForeground(getTextMutedColor());
        panel.add(subLbl, BorderLayout.SOUTH);

        return panel;
    }

    /**
     * Badge de status sem caracteres Unicode especiais (sem emojis que viram retângulos no Windows).
     */
    private JLabel buildStatusBadge(double pct) {
        JLabel badge = new JLabel();
        badge.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        badge.setOpaque(true);

        if (pct >= 100.0) {
            badge.setText("  META SUPERADA  ");
            badge.setForeground(new Color(16, 185, 129));
            badge.setBackground(new Color(16, 185, 129, 30));
            badge.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(16, 185, 129, 90), 1, true),
                    new EmptyBorder(3, 8, 3, 8)
            ));
        } else if (pct >= 80.0) {
            badge.setText("  NO RITMO  ");
            badge.setForeground(new Color(59, 130, 246));
            badge.setBackground(new Color(59, 130, 246, 30));
            badge.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(59, 130, 246, 90), 1, true),
                    new EmptyBorder(3, 8, 3, 8)
            ));
        } else if (pct >= 50.0) {
            badge.setText("  ATENÇÃO  ");
            badge.setForeground(new Color(245, 158, 11));
            badge.setBackground(new Color(245, 158, 11, 30));
            badge.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(245, 158, 11, 90), 1, true),
                    new EmptyBorder(3, 8, 3, 8)
            ));
        } else {
            badge.setText("  ALERTA  ");
            badge.setForeground(new Color(239, 68, 68));
            badge.setBackground(new Color(239, 68, 68, 30));
            badge.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(239, 68, 68, 90), 1, true),
                    new EmptyBorder(3, 8, 3, 8)
            ));
        }
        return badge;
    }

    private SimplePieChart buildShareChart() {
        SimplePieChart chart = new SimplePieChart("Participação nas Vendas (%)", true);
        List<String> labels = new ArrayList<>();
        List<BigDecimal> values = new ArrayList<>();

        for (BusinessUnitDTO bu : businessUnits) {
            BigDecimal b = calculateTotalBilled(bu.id());
            if (b.compareTo(BigDecimal.ZERO) > 0) {
                labels.add(bu.name());
                values.add(b);
            }
        }

        if (values.isEmpty()) {
            labels.add("Sem Vendas");
            values.add(BigDecimal.ONE);
        }

        Color[] palette = new Color[]{
                new Color(59, 130, 246),
                new Color(16, 185, 129),
                new Color(245, 158, 11),
                new Color(139, 92, 246),
                new Color(236, 72, 153),
                new Color(20, 184, 166),
                new Color(249, 115, 22)
        };
        Color[] sliceColors = new Color[labels.size()];
        for (int i = 0; i < sliceColors.length; i++) {
            sliceColors[i] = palette[i % palette.length];
        }

        chart.setData(labels.toArray(new String[0]), values.toArray(new BigDecimal[0]), sliceColors);
        return chart;
    }

    private JPanel buildTopProductsTvWidget(Long businessUnitId) {
        ModernPanel card = new ModernPanel(14, getCardBgColor(), getCardBgColor());
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(getCardBorderColor(), 1, true),
                new EmptyBorder(12, 14, 12, 14)
        ));

        String title = businessUnitId == null ? "Top Artigos Campeões (Global)" : "Top Artigos da Unidade";
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        titleLbl.setForeground(getTextLightColor());
        card.add(titleLbl, BorderLayout.NORTH);

        Map<String, BigDecimal> productRevenue = new HashMap<>();
        Map<String, BigDecimal> productQty = new HashMap<>();

        for (InvoiceDTO inv : getFilteredInvoices()) {
            if (businessUnitId != null && !businessUnitId.equals(inv.businessUnitId())) continue;
            if (inv.lines() != null) {
                for (InvoiceLineDTO line : inv.lines()) {
                    String name = line.productName() != null ? line.productName() : "Artigo";
                    BigDecimal rev = line.lineTotal() != null ? line.lineTotal() : BigDecimal.ZERO;
                    BigDecimal q = line.quantity() != null ? line.quantity() : BigDecimal.ZERO;

                    productRevenue.put(name, productRevenue.getOrDefault(name, BigDecimal.ZERO).add(rev));
                    productQty.put(name, productQty.getOrDefault(name, BigDecimal.ZERO).add(q));
                }
            }
        }

        List<Map.Entry<String, BigDecimal>> sorted = new ArrayList<>(productRevenue.entrySet());
        sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);

        BigDecimal maxRev = sorted.isEmpty() ? BigDecimal.ONE : sorted.get(0).getValue();
        int rank = 1;

        for (Map.Entry<String, BigDecimal> entry : sorted) {
            if (rank > 5) break;
            String name = entry.getKey();
            BigDecimal rev = entry.getValue();
            BigDecimal q = productQty.getOrDefault(name, BigDecimal.ZERO);
            double pct = maxRev.compareTo(BigDecimal.ZERO) > 0
                    ? rev.multiply(BigDecimal.valueOf(100)).divide(maxRev, 1, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;

            list.add(createTopProductRow(rank, name, rev, q.intValue(), pct));
            list.add(Box.createVerticalStrut(6));
            rank++;
        }

        if (sorted.isEmpty()) {
            JLabel emptyLbl = new JLabel("Nenhuma venda no período seleccionado.");
            emptyLbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
            emptyLbl.setForeground(getTextMutedColor());
            list.add(emptyLbl);
        }

        card.add(list, BorderLayout.CENTER);
        return card;
    }

    private JPanel createTopProductRow(int rank, String name, BigDecimal revenue, int qty, double pctOfMax) {
        JPanel row = new JPanel(new BorderLayout(6, 3));
        row.setOpaque(false);

        JPanel textRow = new JPanel(new BorderLayout());
        textRow.setOpaque(false);

        JLabel nameLbl = new JLabel(rank + ". " + name);
        nameLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        nameLbl.setForeground(getTextLightColor());

        String revStr = String.format("%,.2f MT (%d un)", revenue, qty);
        JLabel revLbl = new JLabel(revStr);
        revLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        revLbl.setForeground(rank == 1 ? UIHelper.PENDING_YELLOW : UIHelper.APPROVED_GREEN);

        textRow.add(nameLbl, BorderLayout.WEST);
        textRow.add(revLbl, BorderLayout.EAST);
        row.add(textRow, BorderLayout.NORTH);

        JPanel bar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                g2.setColor(lightMode ? new Color(226, 232, 240) : new Color(51, 65, 85));
                g2.fillRoundRect(0, 0, w, h, 4, 4);

                int filled = (int) Math.round((pctOfMax / 100.0) * w);
                if (filled > 0) {
                    g2.setColor(rank == 1 ? UIHelper.PENDING_YELLOW : UIHelper.ACCENT_BLUE);
                    g2.fillRoundRect(0, 0, filled, h, 4, 4);
                }
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(100, 5));
        row.add(bar, BorderLayout.SOUTH);

        return row;
    }

    private JPanel buildRecentInvoicesTvWidget(Long businessUnitId) {
        ModernPanel card = new ModernPanel(14, getCardBgColor(), getCardBgColor());
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(getCardBorderColor(), 1, true),
                new EmptyBorder(12, 14, 12, 14)
        ));

        JLabel titleLbl = new JLabel("Últimas Faturas Emitidas da Linha");
        titleLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 13));
        titleLbl.setForeground(getTextLightColor());
        card.add(titleLbl, BorderLayout.NORTH);

        List<InvoiceDTO> unitInvoices = getFilteredInvoices().stream()
                .filter(inv -> businessUnitId.equals(inv.businessUnitId()))
                .limit(6)
                .toList();

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);

        for (InvoiceDTO inv : unitInvoices) {
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setOpaque(false);
            row.setBorder(new EmptyBorder(4, 0, 4, 0));

            String client = inv.clientName() != null ? inv.clientName() : "Cliente Diversos";
            JLabel invLbl = new JLabel(inv.invoiceNumber() + " • " + client);
            invLbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
            invLbl.setForeground(getTextLightColor());

            String amtStr = String.format("%,.2f MT", inv.totalAmount());
            JLabel amtLbl = new JLabel(amtStr);
            amtLbl.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
            amtLbl.setForeground(UIHelper.APPROVED_GREEN);

            row.add(invLbl, BorderLayout.WEST);
            row.add(amtLbl, BorderLayout.EAST);
            list.add(row);
        }

        if (unitInvoices.isEmpty()) {
            JLabel emptyLbl = new JLabel("Nenhuma fatura registada nesta unidade para o período.");
            emptyLbl.setFont(new Font(UIHelper.FONT, Font.PLAIN, 12));
            emptyLbl.setForeground(getTextMutedColor());
            list.add(emptyLbl);
        }

        card.add(list, BorderLayout.CENTER);
        return card;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // CÁLCULOS E FILTRAGEM
    // ════════════════════════════════════════════════════════════════════════════
    private BigDecimal calculateTotalBilled(Long businessUnitId) {
        BigDecimal total = BigDecimal.ZERO;
        for (InvoiceDTO inv : getFilteredInvoices()) {
            if (businessUnitId == null) {
                // No consolidado da meta global, inclui faturas de unidades ativas no plano de metas (ou sem unidade associada)
                if (inv.businessUnitId() == null || BusinessUnitGoalManager.isUnitIncluded(inv.businessUnitId())) {
                    if (inv.totalAmount() != null) {
                        total = total.add(inv.totalAmount());
                    }
                }
            } else if (businessUnitId.equals(inv.businessUnitId())) {
                if (inv.totalAmount() != null) {
                    total = total.add(inv.totalAmount());
                }
            }
        }
        return total;
    }

    private List<InvoiceDTO> getFilteredInvoices() {
        LocalDate now = LocalDate.now();
        return allInvoices.stream()
                .filter(inv -> {
                    if (inv.createdAt() == null) return false;
                    LocalDate d = inv.createdAt().toLocalDate();
                    return switch (currentPeriod) {
                        case HOJE -> d.isEqual(now);
                        case ESTA_SEMANA -> d.isAfter(now.minusDays(7)) || d.isEqual(now.minusDays(7));
                        case ESTE_MES -> d.getYear() == now.getYear() && d.getMonth() == now.getMonth();
                        case ESTE_ANO -> d.getYear() == now.getYear();
                    };
                })
                .toList();
    }
}
