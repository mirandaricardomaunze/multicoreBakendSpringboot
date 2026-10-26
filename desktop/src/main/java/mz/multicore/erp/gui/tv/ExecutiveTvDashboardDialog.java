package mz.multicore.erp.gui.tv;

import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.purchases.dto.BusinessUnitDTO;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Painel Executivo para Sala de Reuniões e TV de Administração (War Room / Full Screen TV Dashboard).
 * Projetado para exibição contínua em projetores e televisões em alta resolução.
 */
public class ExecutiveTvDashboardDialog extends JDialog implements ExecutiveTvHeaderBuilder.HeaderActions, ExecutiveTvTimerCoordinator.TimerListener {

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
    private final ExecutiveTvHeaderBuilder headerBuilder;
    private final ExecutiveTvTimerCoordinator timerCoordinator;

    private boolean lightMode = false;
    private boolean isFullScreen = false;
    private Period currentPeriod = Period.ESTE_MES;
    private int slideshowIndex = 0;
    private List<BusinessUnitDTO> businessUnits = new ArrayList<>();
    private List<InvoiceDTO> allInvoices = new ArrayList<>();
    private JPanel mainContentContainer;
    private JLabel shortcutsHint;

    public ExecutiveTvDashboardDialog(Window owner, ComercialApiClient comercialApiClient, Long companyId) {
        super(owner, "Painel Executivo de Negócios • Sala de Reuniões", ModalityType.MODELESS);
        this.comercialApiClient = comercialApiClient;
        this.companyId = companyId != null ? companyId : 1L;
        this.headerBuilder = new ExecutiveTvHeaderBuilder(this);
        this.timerCoordinator = new ExecutiveTvTimerCoordinator(this);

        initComponents();
        setupKeyBindings();
        timerCoordinator.start();
        loadData();
    }

    private void initComponents() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(ExecutiveTvTheme.getBgColor(lightMode));
        setLayout(new BorderLayout());
        add(headerBuilder.build(lightMode, currentPeriod), BorderLayout.NORTH);

        mainContentContainer = new JPanel(new BorderLayout());
        mainContentContainer.setOpaque(false);
        mainContentContainer.setBorder(new EmptyBorder(12, 16, 16, 16));
        add(mainContentContainer, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(6, 16, 8, 16));
        shortcutsHint = new JLabel("Atalhos da Sala: [F11] Alternar Tela Cheia  •  [Espaço] Pausar/Continuar Rodízio  •  [ESC] Fechar Janela");
        shortcutsHint.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        shortcutsHint.setForeground(ExecutiveTvTheme.getTextMutedColor(lightMode));
        footer.add(shortcutsHint, BorderLayout.WEST);

        JLabel statusHint = new JLabel("Atualização Automática: Activa (a cada 45s)");
        statusHint.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        statusHint.setForeground(new Color(16, 185, 129));
        footer.add(statusHint, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);

        Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(screen.width - 40, 1366), Math.min(screen.height - 60, 768));
        setLocationRelativeTo(getOwner());
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { timerCoordinator.stop(); }
        });
    }

    private void setupKeyBindings() {
        getRootPane().registerKeyboardAction(e -> onToggleFullScreen(), KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(e -> onClose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(e -> onToggleSlideshow(), KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    private void loadData() {
        SwingUtilities.invokeLater(() -> {
            try {
                if (comercialApiClient != null) {
                    List<BusinessUnitDTO> units = comercialApiClient.getActiveBusinessUnits(companyId);
                    if (units != null) {
                        businessUnits = units;
                        headerBuilder.populateBusinessUnits(businessUnits);
                    }
                    List<InvoiceDTO> invoices = comercialApiClient.getAllInvoices();
                    if (invoices != null) allInvoices = invoices;
                }
            } catch (Exception ignored) {}
            renderCurrentView();
        });
    }

    private void renderCurrentView() {
        mainContentContainer.removeAll();
        List<InvoiceDTO> filtered = ExecutiveTvCalculationHelper.filterInvoices(allInvoices, currentPeriod);
        int selectedIdx = headerBuilder.getBusinessUnitCombo().getSelectedIndex();
        if (selectedIdx <= 0 || businessUnits.isEmpty()) {
            mainContentContainer.add(ExecutiveTvConsolidatedViewBuilder.build(businessUnits, filtered, currentPeriod, lightMode), BorderLayout.CENTER);
        } else {
            BusinessUnitDTO unit = businessUnits.get(selectedIdx - 1);
            mainContentContainer.add(ExecutiveTvUnitViewBuilder.build(unit, filtered, currentPeriod, lightMode), BorderLayout.CENTER);
        }
        mainContentContainer.revalidate();
        mainContentContainer.repaint();
    }

    @Override
    public void onToggleTheme() {
        lightMode = !lightMode;
        getContentPane().setBackground(ExecutiveTvTheme.getBgColor(lightMode));
        headerBuilder.applyTheme(lightMode);
        headerBuilder.updatePeriodStyles(currentPeriod, lightMode);
        shortcutsHint.setForeground(ExecutiveTvTheme.getTextMutedColor(lightMode));
        renderCurrentView();
    }

    @Override
    public void onOpenGoals() {
        if ((businessUnits == null || businessUnits.isEmpty()) && comercialApiClient != null) {
            try { List<BusinessUnitDTO> loaded = comercialApiClient.getActiveBusinessUnits(companyId); if (loaded != null && !loaded.isEmpty()) businessUnits = loaded; } catch (Exception ignored) {}
        }
        BusinessUnitGoalManager.showGoalsDialog(this, businessUnits, this::renderCurrentView);
    }

    @Override
    public void onOpenSellers() {
        List<InvoiceDTO> filtered = ExecutiveTvCalculationHelper.filterInvoices(allInvoices, currentPeriod);
        SellerPerformanceDialog.showDialog(this, filtered, currentPeriod.getLabel(), lightMode);
    }

    @Override
    public void onToggleFullScreen() {
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        dispose();
        isFullScreen = !isFullScreen;
        setUndecorated(isFullScreen);
        if (isFullScreen) {
            try {
                if (gd.isFullScreenSupported()) gd.setFullScreenWindow(this);
                else setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
            } catch (Exception ex) { setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds()); }
        } else {
            if (gd.getFullScreenWindow() == this) gd.setFullScreenWindow(null);
            Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
            setSize(Math.min(screen.width - 40, 1366), Math.min(screen.height - 60, 768));
            setLocationRelativeTo(getOwner());
        }
        headerBuilder.setFullScreenState(isFullScreen);
        setVisible(true);
    }

    @Override public void onClose() { timerCoordinator.stop(); dispose(); }
    @Override public void onPeriodSelected(Period p) { if (p != null) { currentPeriod = p; headerBuilder.updatePeriodStyles(p, lightMode); renderCurrentView(); } }
    @Override public void onBusinessUnitChanged(int idx) { if (!timerCoordinator.isSlideshowActive()) renderCurrentView(); }
    @Override public void onToggleSlideshow() { boolean act = timerCoordinator.toggleSlideshow(); headerBuilder.updateSlideshowButton(act, timerCoordinator.getSecondsToNextSlide()); }
    @Override public void onClockTick(String t) { headerBuilder.setClockText(t); }
    @Override public void onAutoRefresh() { loadData(); }
    @Override public void onCountdownTick(int s) { headerBuilder.updateSlideshowButton(true, s); }
    @Override public void onSlideAdvance() {
        int total = headerBuilder.getBusinessUnitCombo().getItemCount();
        if (total > 1) { slideshowIndex = (slideshowIndex + 1) % total; headerBuilder.getBusinessUnitCombo().setSelectedIndex(slideshowIndex); renderCurrentView(); }
    }
}

