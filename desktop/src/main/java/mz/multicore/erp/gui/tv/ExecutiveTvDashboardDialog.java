package mz.multicore.erp.gui.tv;

import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.gui.components.Theme;
import mz.multicore.erp.gui.components.ThemeChangeListeners;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.purchases.dto.BusinessUnitDTO;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Painel Executivo para a sala de reuniões e a TV da administração, pensado para ecrã inteiro.
 * Coordena dados, tema e temporizadores; o desenho fica nos builders do pacote.
 */
public class ExecutiveTvDashboardDialog extends JDialog implements ExecutiveTvHeaderBuilder.HeaderActions, ExecutiveTvTimerCoordinator.TimerListener {

    public enum Period {
        HOJE("Hoje", "Hoje"),
        ESTA_SEMANA("Últimos 7 dias", "7 dias"),
        ESTE_MES("Este mês", "Mês"),
        ESTE_ANO("Este ano", "Ano");

        private final String label, shortLabel;
        Period(String label, String shortLabel) { this.label = label; this.shortLabel = shortLabel; }
        public String getLabel() { return label; }
        public String getShortLabel() { return shortLabel; }
    }

    private static final DateTimeFormatter UPDATED_AT = DateTimeFormatter.ofPattern("HH:mm");
    private final ComercialApiClient comercialApiClient;
    private final Long companyId;
    private final ExecutiveTvHeaderBuilder headerBuilder;
    private final ExecutiveTvTimerCoordinator timerCoordinator;
    private final Window anchor; // só para posicionar: o painel não é filho da janela principal
    private final Consumer<Theme> appThemeListener = theme -> applyPanelTheme(theme == Theme.LIGHT); // alto contraste fica "Slate"
    private boolean lightMode = UIHelper.isLight(), isFullScreen, loading;
    private Period currentPeriod = Period.ESTE_MES;
    private int slideshowIndex = 0;
    private List<BusinessUnitDTO> businessUnits = new ArrayList<>();
    private List<InvoiceDTO> allInvoices = new ArrayList<>();
    private JPanel mainContentContainer, footer;
    private String syncText = "A carregar dados...";
    private boolean syncOk = true; // falso quando a última leitura ao servidor falhou ou não há cliente

    public ExecutiveTvDashboardDialog(Window owner, ComercialApiClient comercialApiClient, Long companyId) {
        super((Window) null, "Painel Executivo", ModalityType.MODELESS); // sem dono: a troca de tema descarta a janela principal
        this.anchor = owner;
        this.comercialApiClient = comercialApiClient;
        this.companyId = companyId != null ? companyId : 1L;
        this.headerBuilder = new ExecutiveTvHeaderBuilder(this);
        this.timerCoordinator = new ExecutiveTvTimerCoordinator(this);
        initComponents();
        setupKeyBindings();
        timerCoordinator.start();
        loadData();
        ThemeChangeListeners.add(appThemeListener);
    }

    @Override // Esc, botão ou X: pára os temporizadores e deixa de ouvir o tema
    public void dispose() {
        timerCoordinator.stop();
        ThemeChangeListeners.remove(appThemeListener);
        super.dispose();
    }

    private void initComponents() {
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        getContentPane().setBackground(ExecutiveTvTheme.getBgColor(lightMode));
        setLayout(new BorderLayout());
        add(headerBuilder.build(lightMode, currentPeriod), BorderLayout.NORTH);

        mainContentContainer = ExecutiveTvComponents.transparent(new BorderLayout());
        mainContentContainer.setBorder(new EmptyBorder(16, 20, 20, 20)); // 20 px em baixo: os cartões não colam ao rodapé
        add(ExecutiveTvComponents.responsiveScroll(mainContentContainer, ExecutiveTvComponents.MIN_CONTENT_HEIGHT), BorderLayout.CENTER);
        rebuildFooter();
        applyWindowedBounds();
    }

    private void rebuildFooter() {
        if (footer != null) remove(footer);
        add(footer = ExecutiveTvFooterBuilder.build(lightMode, syncText, syncOk), BorderLayout.SOUTH);
        footer.revalidate();
        footer.repaint();
    }

    private void setupKeyBindings() {
        getRootPane().registerKeyboardAction(e -> onToggleFullScreen(), KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(e -> onClose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(e -> onToggleSlideshow(), KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
    }

    /** Lê unidades e faturas fora do EDT; a TV continua a responder durante a leitura. */
    private void loadData() {
        if (loading) return;
        if (comercialApiClient == null) { syncOk = false; syncText = "Sem ligação ao servidor"; rebuildFooter(); renderCurrentView(); return; }
        loading = true;
        new SwingWorker<Object[], Void>() {
            @Override protected Object[] doInBackground() {
                return new Object[]{comercialApiClient.getActiveBusinessUnits(companyId), comercialApiClient.getAllInvoices()};
            }
            @Override @SuppressWarnings("unchecked") protected void done() {
                loading = false;
                try {
                    Object[] data = get();
                    if (data[0] != null) { businessUnits = (List<BusinessUnitDTO>) data[0]; headerBuilder.populateBusinessUnits(businessUnits); }
                    if (data[1] != null) allInvoices = (List<InvoiceDTO>) data[1];
                    syncOk = true;
                    syncText = "Actualizado às " + LocalDateTime.now().format(UPDATED_AT);
                } catch (Exception ex) {
                    syncOk = false;
                    syncText = "Sem ligação ao servidor · a mostrar os últimos dados";
                }
                rebuildFooter();
                renderCurrentView();
            }
        }.execute();
    }

    private void renderCurrentView() {
        mainContentContainer.removeAll();
        List<InvoiceDTO> filtered = ExecutiveTvCalculationHelper.filterInvoices(allInvoices, currentPeriod);
        int selectedIdx = headerBuilder.getBusinessUnitCombo().getSelectedIndex();
        if (selectedIdx <= 0 || selectedIdx > businessUnits.size()) {
            mainContentContainer.add(ExecutiveTvConsolidatedViewBuilder.build(businessUnits, filtered, currentPeriod,
                    lightMode, this::onOpenGoals), BorderLayout.CENTER);
        } else {
            BusinessUnitDTO unit = businessUnits.get(selectedIdx - 1);
            mainContentContainer.add(ExecutiveTvUnitViewBuilder.build(unit, filtered, currentPeriod, lightMode,
                    this::onOpenGoals), BorderLayout.CENTER);
        }
        headerBuilder.refreshContext();
        mainContentContainer.revalidate();
        mainContentContainer.repaint();
    }

    @Override
    public void onToggleTheme() { applyPanelTheme(!lightMode); } // os modais lêem lightMode ao abrir: seguem a troca

    private void applyPanelTheme(boolean light) {
        if (!isDisplayable()) return;
        lightMode = light;
        getContentPane().setBackground(ExecutiveTvTheme.getBgColor(lightMode));
        headerBuilder.applyTheme(lightMode);
        rebuildFooter();
        renderCurrentView();
    }

    @Override
    public void onOpenGoals() {
        if ((businessUnits == null || businessUnits.isEmpty()) && comercialApiClient != null) {
            try { List<BusinessUnitDTO> loaded = comercialApiClient.getActiveBusinessUnits(companyId); if (loaded != null && !loaded.isEmpty()) businessUnits = loaded; } catch (Exception ignored) {}
        }
        BusinessUnitGoalManager.showGoalsDialog(this, businessUnits, this::renderCurrentView, lightMode);
    }

    @Override
    public void onOpenSellers() {
        List<InvoiceDTO> filtered = ExecutiveTvCalculationHelper.filterInvoices(allInvoices, currentPeriod);
        SellerPerformanceDialog.showDialog(this, filtered, currentPeriod.getLabel(), lightMode);
    }

    @Override
    public void onToggleFullScreen() {
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        super.dispose(); // só para trocar a moldura: os temporizadores continuam
        isFullScreen = !isFullScreen;
        setUndecorated(isFullScreen);
        if (isFullScreen) {
            try {
                if (gd.isFullScreenSupported()) gd.setFullScreenWindow(this);
                else setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds());
            } catch (Exception ex) { setBounds(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds()); }
        } else {
            if (gd.getFullScreenWindow() == this) gd.setFullScreenWindow(null);
            applyWindowedBounds();
        }
        headerBuilder.setFullScreenState(isFullScreen);
        setVisible(true);
    }

    private void applyWindowedBounds() {
        Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(screen.width - 40, 1366), Math.min(screen.height - 60, 800));
        setLocationRelativeTo(anchor);
    }

    @Override public void onClose() { dispose(); }
    @Override public void onPeriodSelected(Period p) { if (p != null) { currentPeriod = p; headerBuilder.updatePeriodStyles(p, lightMode); renderCurrentView(); } }
    @Override public void onBusinessUnitChanged(int idx) { if (!timerCoordinator.isSlideshowActive()) renderCurrentView(); }
    @Override public void onToggleSlideshow() { boolean act = timerCoordinator.toggleSlideshow(); headerBuilder.updateSlideshowButton(act, timerCoordinator.getSecondsToNextSlide()); }
    @Override public void onClockTick(LocalDateTime now) { headerBuilder.setClock(now); }
    @Override public void onAutoRefresh() { loadData(); }
    @Override public void onCountdownTick(int s) { headerBuilder.updateSlideshowButton(true, s); }
    @Override public void onSlideAdvance() {
        int total = headerBuilder.getBusinessUnitCombo().getItemCount();
        if (total > 1) { slideshowIndex = (slideshowIndex + 1) % total; headerBuilder.getBusinessUnitCombo().setSelectedIndex(slideshowIndex); renderCurrentView(); }
    }
}
