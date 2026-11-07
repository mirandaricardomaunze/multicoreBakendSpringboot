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
import javax.swing.SwingWorker;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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
    private boolean lightMode, isFullScreen, loading;
    private Period currentPeriod = Period.ESTE_MES;
    private int slideshowIndex = 0;
    private List<BusinessUnitDTO> businessUnits = new ArrayList<>();
    private List<InvoiceDTO> allInvoices = new ArrayList<>();
    private JPanel mainContentContainer, footer;
    private String syncText = "A carregar dados...";
    private boolean syncOk = true; // falso quando a última leitura ao servidor falhou ou não há cliente

    public ExecutiveTvDashboardDialog(Window owner, ComercialApiClient comercialApiClient, Long companyId) {
        super(owner, "Painel Executivo", ModalityType.MODELESS);
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

        mainContentContainer = ExecutiveTvComponents.transparent(new BorderLayout());
        mainContentContainer.setBorder(new EmptyBorder(16, 20, 8, 20));
        add(mainContentContainer, BorderLayout.CENTER);
        rebuildFooter();
        applyWindowedBounds();
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { timerCoordinator.stop(); }
        });
    }

    /** Rodapé curto: atalhos à esquerda, estado da sincronização à direita. */
    private void rebuildFooter() {
        if (footer != null) remove(footer);
        footer = ExecutiveTvComponents.transparent(new BorderLayout());
        footer.setBorder(new EmptyBorder(4, 22, 10, 22));
        footer.add(ExecutiveTvComponents.label("F11  ecrã inteiro     Espaço  rodízio     Esc  fechar", Font.PLAIN, 12,
                ExecutiveTvTheme.getTextFaintColor(lightMode)), BorderLayout.WEST);
        JLabel sync = ExecutiveTvComponents.label(syncText, Font.PLAIN, 12, ExecutiveTvTheme.getTextMutedColor(lightMode));
        sync.setIcon(UIHelper.icon("fas-circle", 8, syncOk ? UIHelper.APPROVED_GREEN : UIHelper.PENDING_YELLOW));
        sync.setIconTextGap(8);
        footer.add(sync, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
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
    public void onToggleTheme() {
        lightMode = !lightMode;
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
            applyWindowedBounds();
        }
        headerBuilder.setFullScreenState(isFullScreen);
        setVisible(true);
    }

    private void applyWindowedBounds() {
        Dimension screen = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setSize(Math.min(screen.width - 40, 1366), Math.min(screen.height - 60, 800));
        setLocationRelativeTo(getOwner());
    }

    @Override public void onClose() { timerCoordinator.stop(); dispose(); }
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
