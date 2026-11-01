package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.session.DesktopSession;
import mz.multicore.erp.desktop.session.DesktopSessionStore;
import mz.multicore.erp.desktop.session.SignedInUser;
import mz.multicore.erp.desktop.client.ApprovalApiClient;
import mz.multicore.erp.desktop.client.FiscalApiClient;
import mz.multicore.erp.desktop.client.PlatformApiClient;
import mz.multicore.erp.desktop.client.UserApiClient;
import mz.multicore.erp.desktop.client.AuditApiClient;
import mz.multicore.erp.desktop.client.BackupApiClient;
import mz.multicore.erp.desktop.client.DocumentConfigApiClient;
import mz.multicore.erp.desktop.client.SupportApiClient;
import mz.multicore.erp.desktop.client.MySubscriptionApiClient;
import mz.multicore.erp.desktop.client.CRMApiClient;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.CreditNoteApiClient;
import mz.multicore.erp.desktop.client.DebitNoteApiClient;
import mz.multicore.erp.desktop.client.MovimentosApiClient;
import mz.multicore.erp.desktop.client.FinanceApiClient;
import mz.multicore.erp.desktop.client.InventoryApiClient;
import mz.multicore.erp.desktop.client.InventoryCountApiClient;
import mz.multicore.erp.desktop.client.ProductCategoryApiClient;
import mz.multicore.erp.desktop.client.POSApiClient;
import mz.multicore.erp.desktop.client.PromotionApiClient;
import mz.multicore.erp.desktop.client.PurchaseApiClient;
import mz.multicore.erp.desktop.client.StockTransferApiClient;
import mz.multicore.erp.gui.components.Theme;
import mz.multicore.erp.gui.components.TopNavBar;
import mz.multicore.erp.gui.components.CollapsibleSidebar;
import mz.multicore.erp.gui.components.GlobalSearchDialog;
import mz.multicore.erp.gui.components.ShortcutHelpDialog;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.RecentItemsDialog;
import mz.multicore.erp.gui.components.RecentItemsHistoryManager;
import mz.multicore.erp.desktop.client.HRApiClient;
import mz.multicore.erp.desktop.client.AiAssistantApiClient;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.math.BigDecimal;
import java.util.List;

@org.springframework.stereotype.Component
@org.springframework.context.annotation.Profile("desktop")
@org.springframework.context.annotation.Lazy
@org.springframework.context.annotation.Scope("prototype")
public class MainFrame extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentPanel = new JPanel(cardLayout);

    private final DashboardPanel dashboardPanel;
    private final SellerDashboardPanel sellerDashboardPanel;
    private final ComercialPanel comercialPanel;
    private final FinanceiroPanel financeiroPanel;
    private final HRPanel hrPanel;
    private final CRMPanel crmPanel;
    private final ClientesPanel clientesPanel;
    private final FiscalPanel fiscalPanel;
    private final mz.multicore.erp.gui.accounting.AccountingPanel accountingPanel;
    private final AssetsPanel assetsPanel;
    private final ManufacturingPanel manufacturingPanel;
    private final FleetPanel fleetPanel;
    private final RecurringContractsPanel recurringContractsPanel;
    private final QualityPanel qualityPanel;
    private final ApprovalsPanel approvalsPanel;
    private final POSPanel posPanel;
    private final StockPanel stockPanel;
    private final ComprasPanel comprasPanel;
    private final ConfigPanel configPanel;
    private final PlataformaPanel plataformaPanel;
    private final PerformancePanel performancePanel;
    private final NotificationFeed notificationFeed;
    private final NotificationsPanel notificationsPanel;
    private final NotificationReadStore notificationReadStore;
    private final ForensicAuditPanel forensicAuditPanel;
    private final AiManagementAssistantPanel aiAssistantPanel;

    private final DesktopSessionStore desktopSessionStore;
    private final mz.multicore.erp.desktop.client.VersionApiClient versionApiClient;
    private final MySubscriptionApiClient mySubscriptionApiClient;
    private final BackupApiClient backupApiClient;
    private final boolean superAdmin;
    private final MainFrameTopBarBuilder topBarBuilder = new MainFrameTopBarBuilder();
    private final MainFrameNotificationCoordinator notificationCoordinator;
    private TopNavBar topBar;
    private CollapsibleSidebar sidebar;
    private mz.multicore.erp.gui.components.StatusBar statusBar;
    private String sessionDisplayName;
    private boolean sidebarCollapsedForPos;

    /** Antecedência (dias) a partir da qual se avisa o assinante que a assinatura vai expirar. */
    private static final long SUB_ALERT_DAYS = 7;
    /** Intervalo da vigia de assinatura enquanto a app está aberta (6 horas). */
    private static final int SUB_WATCH_INTERVAL_MS = 6 * 60 * 60 * 1000;
    private javax.swing.Timer subscriptionWatch;
    private boolean subscriptionEnforced;

    public MainFrame(
            ComercialApiClient comercialApiClient,
            CreditNoteApiClient creditNoteApiClient,
            DebitNoteApiClient debitNoteApiClient,
            MovimentosApiClient movimentosApiClient,
            ApprovalApiClient approvalApiClient,
            FinanceApiClient financeApiClient,
            InventoryApiClient inventoryApiClient,
            StockTransferApiClient stockTransferApiClient,
            InventoryCountApiClient inventoryCountApiClient,
            ProductCategoryApiClient productCategoryApiClient,
            PurchaseApiClient purchaseApiClient,
            CRMApiClient crmApiClient,
            HRApiClient hrApiClient,
            UserApiClient userApiClient,
            AuditApiClient auditApiClient,
            BackupApiClient backupApiClient,
            DocumentConfigApiClient documentConfigApiClient,
            SupportApiClient supportApiClient,
            MySubscriptionApiClient mySubscriptionApiClient,
            PromotionApiClient promotionApiClient,
            POSApiClient posApiClient,
            DesktopSessionStore desktopSessionStore,
            FiscalApiClient fiscalApiClient,
            PlatformApiClient platformApiClient,
            mz.multicore.erp.modules.pos.scale.ScaleBarcodeParser scaleBarcodeParser,
            mz.multicore.erp.desktop.client.AccountingApiClient accountingApiClient, mz.multicore.erp.desktop.client.VersionApiClient versionApiClient,
            mz.multicore.erp.desktop.client.PrintApiClient printApiClient, mz.multicore.erp.desktop.client.PerformanceApiClient performanceApiClient,
            mz.multicore.erp.desktop.client.StockWasteApiClient stockWasteApiClient, mz.multicore.erp.desktop.client.CreditRiskApiClient creditRiskApiClient,
            mz.multicore.erp.desktop.client.BankReconciliationApiClient bankReconciliationApiClient, mz.multicore.erp.desktop.client.AccountStatementApiClient accountStatementApiClient,
            mz.multicore.erp.desktop.client.CashFlowForecastApiClient cashFlowForecastApiClient,
            mz.multicore.erp.desktop.client.ForensicAuditApiClient forensicAuditApiClient,
            mz.multicore.erp.desktop.client.InventoryPhysicalCountingApiClient inventoryPhysicalCountingApiClient,
            mz.multicore.erp.desktop.client.SystemMonitoringApiClient systemMonitoringApiClient,
            mz.multicore.erp.desktop.client.AssetApiClient assetApiClient,
            mz.multicore.erp.desktop.client.ProductionApiClient productionApiClient,
            mz.multicore.erp.desktop.client.FleetApiClient fleetApiClient,
            mz.multicore.erp.desktop.client.RecurringContractApiClient recurringContractApiClient,
            mz.multicore.erp.desktop.client.DashboardApiClient dashboardApiClient,
            AiAssistantApiClient aiAssistantApiClient,
            mz.multicore.erp.desktop.client.QualityApiClient qualityApiClient
    ) {
        this.desktopSessionStore = desktopSessionStore;
        this.versionApiClient = versionApiClient;
        this.mySubscriptionApiClient = mySubscriptionApiClient;
        this.backupApiClient = backupApiClient;
        this.superAdmin = desktopSessionStore.requireSession().superAdmin();

        setTitle("MULTICORE — Gestão Profissional");
        setIconImages(UIHelper.getAppIcons());
        getRootPane().putClientProperty("JRootPane.titleBarShowIcon", true);
        getRootPane().putClientProperty("JRootPane.titleBarShowTitle", true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 820);
        setMinimumSize(new Dimension(1024, 700));
        setLocationRelativeTo(null);
        UIHelper.registerMainWindow(this); // contém modais dentro da janela principal (mesmo ao arrastar)
        getContentPane().setBackground(UIHelper.BG_DARK);

        if (superAdmin) {
            dashboardPanel = null; sellerDashboardPanel = null; comercialPanel = null; financeiroPanel = null; hrPanel = null;
            crmPanel = null; clientesPanel = null; fiscalPanel = null; approvalsPanel = null; qualityPanel = null;
            accountingPanel = null; assetsPanel = null; manufacturingPanel = null; fleetPanel = null; recurringContractsPanel = null; performancePanel = null; forensicAuditPanel = null; aiAssistantPanel = null;
            posPanel = null; stockPanel = null; comprasPanel = null; configPanel = null;
            notificationFeed = null; notificationsPanel = null; notificationReadStore = null;
            plataformaPanel = new PlataformaPanel(platformApiClient, systemMonitoringApiClient);
            contentPanel.add(plataformaPanel, "plataforma");
            notificationCoordinator = null;
        } else {
            boolean sellerSession = "SELLER".equalsIgnoreCase(desktopSessionStore.requireSession().activeRole());
            dashboardPanel = sellerSession ? null : new DashboardPanel(
                    comercialApiClient, financeApiClient, approvalApiClient, crmApiClient, purchaseApiClient, inventoryApiClient,
                    forensicAuditApiClient, cashFlowForecastApiClient, creditRiskApiClient, performanceApiClient,
                    dashboardApiClient, this::navigate);
            sellerDashboardPanel = sellerSession
                    ? new SellerDashboardPanel(comercialApiClient, posApiClient, this::handleSellerDashboardAction)
                    : null;
            stockPanel = sellerSession ? null : new StockPanel(inventoryApiClient, comercialApiClient, stockTransferApiClient, inventoryCountApiClient, productCategoryApiClient, printApiClient, stockWasteApiClient, inventoryPhysicalCountingApiClient);
            comercialPanel  = new ComercialPanel(comercialApiClient, inventoryApiClient, financeApiClient, creditNoteApiClient, debitNoteApiClient, posApiClient, movimentosApiClient, promotionApiClient, printApiClient,
                    productionApiClient, fleetApiClient,
                    () -> {
                        navigate("stock");
                        if (topBar != null) topBar.setActive("Stock & Armazéns");
                        if (sidebar != null) sidebar.setActive("Stock & Armazéns");
                        if (stockPanel != null) stockPanel.showWarehouseTransfers();
                    });
            financeiroPanel = sellerSession ? null : new FinanceiroPanel(financeApiClient, comercialApiClient, bankReconciliationApiClient, cashFlowForecastApiClient);
            hrPanel         = sellerSession ? null : new HRPanel(hrApiClient, printApiClient);
            crmPanel        = new CRMPanel(crmApiClient, comercialApiClient);
            clientesPanel   = new ClientesPanel(comercialApiClient, printApiClient, creditRiskApiClient, accountStatementApiClient);
            fiscalPanel     = sellerSession ? null : new FiscalPanel(fiscalApiClient);
            accountingPanel = sellerSession ? null : new mz.multicore.erp.gui.accounting.AccountingPanel(accountingApiClient);
            assetsPanel     = sellerSession ? null : new AssetsPanel(assetApiClient, printApiClient);
            manufacturingPanel = sellerSession ? null : new ManufacturingPanel(productionApiClient, comercialApiClient, inventoryApiClient);
            fleetPanel      = sellerSession ? null : new FleetPanel(fleetApiClient);
            recurringContractsPanel = sellerSession ? null : new RecurringContractsPanel(recurringContractApiClient);
            qualityPanel    = sellerSession ? null : new QualityPanel(qualityApiClient, hrApiClient, printApiClient);
            approvalsPanel  = sellerSession ? null : new ApprovalsPanel(approvalApiClient, stockTransferApiClient,
                    comercialApiClient, printApiClient);
            posPanel        = new POSPanel(posApiClient, comercialApiClient, inventoryApiClient, financeApiClient,
                    promotionApiClient, scaleBarcodeParser, printApiClient);
            comprasPanel    = sellerSession ? null : new ComprasPanel(purchaseApiClient, inventoryApiClient, comercialApiClient, financeApiClient, accountStatementApiClient);
            configPanel     = sellerSession ? null : new ConfigPanel(userApiClient, auditApiClient, backupApiClient, documentConfigApiClient, supportApiClient, mySubscriptionApiClient, systemMonitoringApiClient);
            performancePanel = sellerSession ? null : new PerformancePanel(performanceApiClient, desktopSessionStore);
            forensicAuditPanel = sellerSession ? null : new ForensicAuditPanel(forensicAuditApiClient);
            aiAssistantPanel = sellerSession ? null : new AiManagementAssistantPanel(aiAssistantApiClient, this::navigate);
            notificationFeed = new NotificationFeed(approvalApiClient, inventoryApiClient,
                    mySubscriptionApiClient, hrApiClient, performanceApiClient, creditRiskApiClient, stockWasteApiClient, systemMonitoringApiClient, stockTransferApiClient, qualityApiClient);
            notificationReadStore = new NotificationReadStore();
            notificationsPanel = new NotificationsPanel(notificationFeed, notificationReadStore,
                    this::navigateFromNotification, this::updateNotificationBadge);
            notificationCoordinator = new MainFrameNotificationCoordinator(notificationFeed, notificationReadStore, this::navigateFromNotification);
            plataformaPanel = null;
            addCard(sellerDashboardPanel != null ? sellerDashboardPanel : dashboardPanel, "dashboard");
            addCard(posPanel, "pos");
            addCard(comercialPanel, "comercial"); addCard(comprasPanel, "compras");
            addCard(stockPanel, "stock"); addCard(financeiroPanel, "financeiro");
            addCard(hrPanel, "hr"); addCard(performancePanel, "desempenho");
            addCard(crmPanel, "crm"); addCard(clientesPanel, "clientes");
            addCard(fiscalPanel, "fiscal"); addCard(accountingPanel, "contabilidade");
            addCard(assetsPanel, "assets");
            addCard(manufacturingPanel, "manufacturing");
            addCard(fleetPanel, "fleet");
            addCard(recurringContractsPanel, "recurring");
            addCard(qualityPanel, "quality");
            addCard(approvalsPanel, "approvals"); addCard(configPanel, "config");
            addCard(forensicAuditPanel, "auditoria_forense");
            addCard(aiAssistantPanel, "assistente_ia");
            addCard(notificationsPanel, "notifications");
        }

        setLayout(new BorderLayout());
        sidebar = buildSidebar();
        topBar = topBarBuilder.build(superAdmin, this, this::openGlobalSearch, this::openRecentItemsHistory,
                notificationCoordinator != null ? notificationCoordinator.buildNotificationBell() : null,
                buildSubscriptionChip(), buildCompanyCombo(),
                desktopSessionStore.requireSession().companies().size() > 1);
        if (notificationCoordinator != null) notificationCoordinator.attachNavComponents(topBar, sidebar);
        statusBar = new mz.multicore.erp.gui.components.StatusBar();

        JPanel centerContainer = new JPanel(new BorderLayout());
        centerContainer.setOpaque(false);
        centerContainer.add(topBar, BorderLayout.NORTH);
        centerContainer.add(contentPanel, BorderLayout.CENTER);
        centerContainer.add(statusBar, BorderLayout.SOUTH);

        add(sidebar, BorderLayout.WEST);
        add(centerContainer, BorderLayout.CENTER);

        // Atalhos de Teclado Rápidos (Ctrl+B, Ctrl+K, F1, F11, F12)
        int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        getRootPane().registerKeyboardAction(e -> { if (sidebar != null) sidebar.toggle(); }, KeyStroke.getKeyStroke(KeyEvent.VK_B, mask), JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(e -> openGlobalSearch(), KeyStroke.getKeyStroke(KeyEvent.VK_K, mask), JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(e -> openRecentItemsHistory(), KeyStroke.getKeyStroke(KeyEvent.VK_H, mask), JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(e -> openShortcutHelp(), KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(e -> openAboutDialog(), KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(e -> toggleFullScreen(), KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

        if (superAdmin) {
            navigate("plataforma");
            topBar.setActive("Plataforma");
            if (sidebar != null) sidebar.setActive("Plataforma");
        } else {
            topBar.setActive("Painel Inicial");
            if (sidebar != null) sidebar.setActive("Painel Inicial");
        }

        startSubscriptionWatch();
    }

    private void addCard(JComponent panel, String name) {
        if (panel != null) contentPanel.add(panel, name);
    }

    /** Called by the application bootstrap once the user is authenticated. */
    public void applyAuthenticatedUser(String displayName, String role) {
        sessionDisplayName = displayName;
        String activeRole = CurrentUserContext.getRole();
        if (posPanel != null) posPanel.setOperatorDisplayName(displayName);
        if (dashboardPanel != null) dashboardPanel.updateWelcomeMessage(displayName, activeRole);
        if (sellerDashboardPanel != null) sellerDashboardPanel.updateWelcomeMessage(displayName);
        topBarBuilder.updateUserProfile(displayName, UIHelper.humanRole(activeRole));
        if (sidebar != null) sidebar.setUserProfile(displayName, UIHelper.humanRole(activeRole));
        if (statusBar != null) {
            checkForNewerVersion();
        }
    }

    /**
     * Aviso discreto no rodapé quando o servidor já tem versão mais recente.
     *
     * <p>Fora do EDT (a chamada é HTTP) e <b>à prova de falha</b>: se o servidor não responder,
     * não há aviso nenhum e a loja continua a trabalhar. Um problema a verificar a versão nunca
     * pode impedir alguém de vender.
     */
    private void adjustSidebarForPos(boolean entering) {
        if (sidebar == null) return;
        if (entering && !sidebar.isCollapsedState() && PosLayout.shouldCollapseSidebar(getWidth())) {
            sidebar.setCollapsedState(true);
            sidebarCollapsedForPos = true;
        } else if (!entering && sidebarCollapsedForPos) {
            sidebar.setCollapsedState(false);
            sidebarCollapsedForPos = false;
        }
    }

    private void checkForNewerVersion() {
        if (versionApiClient == null) return;
        new javax.swing.SwingWorker<String, Void>() {
            @Override protected String doInBackground() {
                return versionApiClient.newerVersionAvailable();
            }
            @Override protected void done() {
                try {
                    statusBar.setUpdateAvailable(get());
                } catch (Exception ignored) {
                    // Sem aviso. Nunca incomodar o operador por causa disto.
                }
            }
        }.execute();
    }

    public CollapsibleSidebar getSidebar() {
        return sidebar;
    }

    private CollapsibleSidebar buildSidebar() {
        return MainFrameSidebarBuilder.build(
                desktopSessionStore.requireSession(),
                superAdmin,
                this::navigate,
                comercialPanel
        );
    }

    public void openRecentItemsHistory() {
        RecentItemsDialog.show(this, item -> {
            if (item != null && item.targetView() != null && !item.targetView().isBlank()) navigate(item.targetView());
        });
    }

    private void refreshNotificationBadgeAsync() {
        if (notificationCoordinator != null) notificationCoordinator.refreshNotificationBadgeAsync();
    }

    private void updateNotificationBadge(int count) {
        if (notificationCoordinator != null) notificationCoordinator.updateNotificationBadge(count);
    }

    private JComboBox<DesktopSession.CompanyAccess> buildCompanyCombo() {
        return MainFrameCompanySelector.buildCompanyCombo(desktopSessionStore, () -> {
            DesktopSession.CompanyAccess selected = desktopSessionStore.requireSession().activeCompany();
            if (selected != null) {
                if (topBar != null) topBar.setSubBrand(selected.name());
                if (sidebar != null) sidebar.setSubBrand(selected.name());
                updateSessionRole();
                refreshActivePanel();
                refreshNotificationBadgeAsync();
            }
        });
    }

    private void updateSessionRole() {
        String activeRole = CurrentUserContext.getRole();
        topBarBuilder.updateRole(UIHelper.humanRole(activeRole));
        if (sessionDisplayName != null && dashboardPanel != null) {
            dashboardPanel.updateWelcomeMessage(sessionDisplayName, activeRole);
        }
    }

    private javax.swing.JComponent buildSubscriptionChip() {
        return MainFrameSubscriptionCoordinator.buildSubscriptionChip(mySubscriptionApiClient, superAdmin);
    }

    public void checkSubscriptionOnStartup() {
        MainFrameSubscriptionCoordinator.checkSubscriptionOnStartup(this, mySubscriptionApiClient, superAdmin, UIHelper.onForcedLogout);
    }

    private void startSubscriptionWatch() {
        subscriptionWatch = MainFrameSubscriptionCoordinator.startSubscriptionWatch(this, mySubscriptionApiClient, superAdmin, UIHelper.onForcedLogout);
    }

    @Override
    public void dispose() {
        if (subscriptionWatch != null) subscriptionWatch.stop();
        super.dispose();
    }

    private void navigate(String cardName) {
        if (!SignedInUser.canNavigate(CurrentUserContext.getRole(), cardName)) {
            ToastManager.show(this, FeedbackType.WARNING,
                    "Esta área requer perfil de gestor ou administrador. Peça apoio ao responsável da loja.");
            return;
        }
        if ("risco_credito".equals(cardName)) {
            cardName = "clientes";
            if (clientesPanel != null) clientesPanel.selectCreditRiskTab();
        } else if ("stock_waste".equals(cardName)) {
            cardName = "stock";
            if (stockPanel != null) stockPanel.showWasteManagement();
        } else if ("previsao_tesouraria".equals(cardName)) {
            cardName = "financeiro";
            if (financeiroPanel != null) financeiroPanel.showForecastTab();
        } else if ("stock_transfers".equals(cardName)) {
            cardName = "stock";
            if (stockPanel != null) stockPanel.showWarehouseTransfers();
        }
        cardLayout.show(contentPanel, cardName);
        String modName = switch (cardName) {
            case "dashboard"  -> "Painel Inicial";
            case "pos"        -> "POS — Caixa";
            case "comercial"  -> "Vendas & Faturação";
            case "compras"    -> "Compras";
            case "stock"      -> "Stock & Armazéns";
            case "financeiro" -> "Tesouraria";
            case "hr"         -> "Recursos Humanos";
            case "desempenho" -> "Desempenho Comercial";
            case "crm"        -> "CRM & Assistência";
            case "clientes"   -> "Clientes";
            case "fiscal"     -> "Área Fiscal";
            case "contabilidade" -> "Contabilidade";
            case "assets"     -> "Activos & Património";
            case "manufacturing" -> "Produção & Fabrico";
            case "fleet"      -> "Gestão de Frotas";
            case "recurring"  -> "Avenças & Contratos";
            case "quality"    -> "Qualidade & HST";
            case "approvals"  -> "Aprovações";
            case "auditoria_forense" -> "Auditoria Forense";
            case "assistente_ia" -> "Assistente de Gestão";
            case "config"     -> "Configurações";
            case "notifications" -> "Notificações";
            case "plataforma" -> "Plataforma";
            default           -> cardName;
        };
        if (topBar != null) topBar.setActive(modName);
        if (sidebar != null) sidebar.setActive(modName);
        adjustSidebarForPos("pos".equals(cardName));
        RecentItemsHistoryManager.getInstance().record("Módulo", null, modName, "Acesso rápido", cardName, "fas-folder-open");
        refreshPanel(cardName);
    }

    private void refreshPanel(String cardName) {
        switch (cardName) {
            case "dashboard"  -> dashboardPanel.refreshData();
            case "comercial"  -> comercialPanel.onPanelSelected();
            case "financeiro" -> financeiroPanel.onPanelSelected();
            case "hr"         -> hrPanel.onPanelSelected();
            case "desempenho" -> performancePanel.onPanelSelected();
            case "crm"        -> crmPanel.onPanelSelected();
            case "clientes"   -> clientesPanel.onPanelSelected();
            case "fiscal"     -> fiscalPanel.onPanelSelected();
            case "contabilidade" -> accountingPanel.onPanelSelected();
            case "assets"     -> assetsPanel.onPanelSelected();
            case "manufacturing" -> manufacturingPanel.onPanelSelected();
            case "fleet"      -> fleetPanel.onPanelSelected();
            case "recurring"  -> recurringContractsPanel.onPanelSelected();
            case "quality"    -> qualityPanel.onPanelSelected();
            case "approvals"  -> approvalsPanel.onPanelSelected();
            case "auditoria_forense" -> forensicAuditPanel.loadData();
            case "pos"        -> posPanel.onPanelSelected();
            case "stock"      -> stockPanel.onPanelSelected();
            case "compras"    -> comprasPanel.onPanelSelected();
            case "config"     -> configPanel.onPanelSelected();
            case "notifications" -> notificationsPanel.onPanelSelected();
            case "plataforma" -> plataformaPanel.onPanelSelected();
        }
    }

    private void navigateFromNotification(String cardName) {
        String navLabel = switch (cardName) {
            case "approvals" -> "Aprovações";
            case "stock", "stock_waste" -> "Stock & Armazéns";
            case "config" -> "Configurações";
            case "clientes", "risco_credito" -> "Clientes";
            case "quality" -> "Qualidade & HST";
            default -> null;
        };
        topBar.setActive(navLabel);
        navigate(cardName);
    }

    private void refreshActivePanel() {
        for (Component comp : contentPanel.getComponents()) {
            if (!comp.isVisible()) continue;
            if (comp instanceof DashboardPanel p)  p.refreshData();
            else if (comp instanceof SellerDashboardPanel p) p.refreshData();
            else if (comp instanceof ComercialPanel p)  p.onPanelSelected();
            else if (comp instanceof FinanceiroPanel p) p.onPanelSelected();
            else if (comp instanceof HRPanel p)         p.onPanelSelected();
            else if (comp instanceof PerformancePanel p) p.onPanelSelected();
            else if (comp instanceof CRMPanel p)        p.onPanelSelected();
            else if (comp instanceof ClientesPanel p)   p.onPanelSelected();
            else if (comp instanceof FiscalPanel p)     p.onPanelSelected();
            else if (comp instanceof mz.multicore.erp.gui.accounting.AccountingPanel p) p.onPanelSelected();
            else if (comp instanceof AssetsPanel p)     p.onPanelSelected();
            else if (comp instanceof ManufacturingPanel p) p.onPanelSelected();
            else if (comp instanceof FleetPanel p)         p.onPanelSelected();
            else if (comp instanceof RecurringContractsPanel p) p.onPanelSelected();
            else if (comp instanceof QualityPanel p)    p.onPanelSelected();
            else if (comp instanceof ApprovalsPanel p)  p.onPanelSelected();
            else if (comp instanceof POSPanel p)        p.onPanelSelected();
            else if (comp instanceof StockPanel p)      p.onPanelSelected();
            else if (comp instanceof ComprasPanel p)    p.onPanelSelected();
            else if (comp instanceof ConfigPanel p)     p.onPanelSelected();
            else if (comp instanceof NotificationsPanel p) p.onPanelSelected();
            else if (comp instanceof PlataformaPanel p) p.onPanelSelected();
        }
    }

    public void openGlobalSearch() {
        GlobalSearchDialog.show(this, buildSearchIndex());
    }

    public void openShortcutHelp() {
        ShortcutHelpDialog.show(this);
    }

    public void openAboutDialog() {
        mz.multicore.erp.gui.components.AboutDialog.show(this);
    }

    public void toggleFullScreen() {
        int state = getExtendedState();
        if ((state & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH) {
            setExtendedState(JFrame.NORMAL);
        } else {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        }
    }

    public void openSmartAlerts() {
        if (notificationFeed != null) {
            Long companyId = CurrentUserContext.getCurrentCompanyId();
            List<mz.multicore.erp.gui.NotificationFeed.NotificationItem> notifs = notificationFeed.load(companyId);
            List<mz.multicore.erp.gui.components.SmartAlertsDialog.SmartAlert> alerts = notifs.stream()
                    .map(mz.multicore.erp.gui.components.SmartAlertsDialog.SmartAlert::fromNotification)
                    .toList();
            new mz.multicore.erp.gui.components.SmartAlertsDialog(this, alerts, this::navigate).setVisible(true);
        }
    }

    public void openBackupDialog() {
        new mz.multicore.erp.gui.components.DatabaseBackupDialog(this, backupApiClient).setVisible(true);
    }

    public void openCurrencyDialog() {
        new mz.multicore.erp.gui.components.CurrencyExchangeDialog(this, BigDecimal.valueOf(1000), res -> {
            ToastManager.success(this, "Câmbio: " + res.foreignAmount() + " " + res.currency().name() + " = " + res.mznEquivalent() + " MT");
        }).setVisible(true);
    }

    public List<GlobalSearchDialog.SearchItem> buildSearchIndex() {
        List<GlobalSearchDialog.SearchItem> items = new java.util.ArrayList<>();
        if (superAdmin) {
            items.add(new GlobalSearchDialog.SearchItem(
                    "mod_plataforma", "Consola da Plataforma", "Módulos", null,
                    UIHelper.icon("fas-server", 16, UIHelper.ACCENT_BLUE), UIHelper.ACCENT_BLUE,
                    () -> navigate("plataforma"), List.of("plataforma", "tenants", "empresas", "admin")
            ));
            return items;
        }

        String role = CurrentUserContext.getRole();
        if ("SELLER".equalsIgnoreCase(role)) return buildSellerSearchIndex();
        if (!"MANAGER".equalsIgnoreCase(role) && !"ADMIN".equalsIgnoreCase(role)) {
            items.add(new GlobalSearchDialog.SearchItem("mod_dashboard", "Painel Inicial", "Módulos", null,
                    UIHelper.icon("fas-th-large", 16, UIHelper.MODULE_DASHBOARD), UIHelper.MODULE_DASHBOARD,
                    () -> navigate("dashboard"), List.of("inicio", "tarefas")));
            items.add(new GlobalSearchDialog.SearchItem("mod_notifications", "Notificações & Avisos", "Módulos", null,
                    UIHelper.icon("fas-bell", 16, UIHelper.ACCENT_CYAN), UIHelper.ACCENT_CYAN,
                    () -> navigate("notifications"), List.of("alertas", "avisos")));
            return items;
        }

        // 1. Módulos Principais
        items.add(new GlobalSearchDialog.SearchItem(
                "mod_dashboard", "Painel Inicial", "Módulos", null,
                UIHelper.icon("fas-th-large", 16, UIHelper.MODULE_DASHBOARD), UIHelper.MODULE_DASHBOARD,
                () -> navigate("dashboard"), List.of("inicio", "home", "resumo", "kpi", "graficos")
        ));
        items.add(new GlobalSearchDialog.SearchItem(
                "mod_pos", "POS — Caixa e Balcão", "Módulos", "F9",
                UIHelper.icon("fas-cash-register", 16, UIHelper.MODULE_POS), UIHelper.MODULE_POS,
                () -> navigate("pos"), List.of("venda", "balcao", "caixa", "terminal", "pagamento")
        ));
        items.add(new GlobalSearchDialog.SearchItem(
                "mod_comercial", "Vendas & Faturação", "Módulos", null,
                UIHelper.icon("fas-file-invoice-dollar", 16, UIHelper.MODULE_COMERCIAL), UIHelper.MODULE_COMERCIAL,
                () -> navigate("comercial"), List.of("fatura", "cotacao", "guia", "proforma", "documentos")
        ));
        items.add(new GlobalSearchDialog.SearchItem(
                "mod_compras", "Compras & Fornecedores", "Módulos", null,
                UIHelper.icon("fas-shopping-cart", 16, UIHelper.MODULE_COMPRAS), UIHelper.MODULE_COMPRAS,
                () -> navigate("compras"), List.of("encomenda", "aquisicoes", "recepcao", "fatura de compra")
        ));
        items.add(new GlobalSearchDialog.SearchItem(
                "mod_stock", "Stock & Armazéns", "Módulos", null,
                UIHelper.icon("fas-boxes", 16, UIHelper.MODULE_STOCK), UIHelper.MODULE_STOCK,
                () -> navigate("stock"), List.of("artigos", "produtos", "inventario", "transferencias", "lotes")
        ));
        items.add(new GlobalSearchDialog.SearchItem(
                "mod_financeiro", "Tesouraria & Contas", "Módulos", null,
                UIHelper.icon("fas-coins", 16, UIHelper.MODULE_FINANCEIRO), UIHelper.MODULE_FINANCEIRO,
                () -> navigate("financeiro"), List.of("banco", "caixa", "saldo", "fluxo", "movimentos")
        ));
        items.add(new GlobalSearchDialog.SearchItem(
                "mod_hr", "Recursos Humanos & Salários", "Módulos", null,
                UIHelper.icon("fas-users", 16, UIHelper.MODULE_HR), UIHelper.MODULE_HR,
                () -> navigate("hr"), List.of("colaboradores", "folha", "salario", "recibos", "ferias", "faltas")
        ));
        items.add(new GlobalSearchDialog.SearchItem(
                "mod_desempenho", "Centro de Desempenho Comercial", "Módulos", null,
                UIHelper.icon("fas-trophy", 16, UIHelper.MODULE_COMERCIAL), UIHelper.MODULE_COMERCIAL,
                () -> navigate("desempenho"), List.of("metas", "ranking", "bonus", "premios", "comercial", "desempenho")
        ));
        items.add(new GlobalSearchDialog.SearchItem(
                "mod_crm", "CRM & Assistência Técnica", "Módulos", null,
                UIHelper.icon("fas-headset", 16, UIHelper.MODULE_CRM), UIHelper.MODULE_CRM,
                () -> navigate("crm"), List.of("pedidos", "suporte", "tickets", "folhas de obra", "tecnicos")
        ));
        items.add(new GlobalSearchDialog.SearchItem(
                "mod_clientes", "Gestão de Clientes", "Módulos", null,
                UIHelper.icon("fas-address-book", 16, UIHelper.MODULE_CLIENTES), UIHelper.MODULE_CLIENTES,
                () -> navigate("clientes"), List.of("contactos", "nuit", "saldo", "extrato")
        ));
        items.add(new GlobalSearchDialog.SearchItem("mod_credit_risk", "Risco de Crédito & Cobrança (Aging)", "Módulos", null, UIHelper.icon("fas-file-invoice-dollar", 16, UIHelper.MODULE_COMERCIAL), UIHelper.MODULE_COMERCIAL, () -> { navigate("clientes"); if (clientesPanel != null) clientesPanel.selectCreditRiskTab(); }, List.of("aging", "cobranca", "risco", "credito", "mora", "devedores", "bloqueio")));
        items.add(new GlobalSearchDialog.SearchItem("mod_fiscal", "Área Fiscal & IVA", "Módulos", null, UIHelper.icon("fas-percent", 16, UIHelper.MODULE_FISCAL), UIHelper.MODULE_FISCAL, () -> navigate("fiscal"), List.of("imposto", "declaracao", "mapa", "retencoes")));
        items.add(new GlobalSearchDialog.SearchItem("mod_contabilidade", "Contabilidade & Razão", "Módulos", null, UIHelper.icon("fas-calculator", 16, UIHelper.MODULE_ACCOUNTING), UIHelper.MODULE_ACCOUNTING, () -> navigate("contabilidade"), List.of("lancamentos", "diario", "balancete", "contas")));
        items.add(new GlobalSearchDialog.SearchItem("mod_assets", "Activos Fixos & Património", "Módulos", null, UIHelper.icon("fas-building", 16, UIHelper.ACCENT_BLUE), UIHelper.ACCENT_BLUE, () -> navigate("assets"), List.of("activos", "patrimonio", "imobilizado", "amortizacoes", "depreciacao", "vlc")));
        items.add(new GlobalSearchDialog.SearchItem("mod_manufacturing", "Produção & Transformação Industrial", "Módulos", null, UIHelper.icon("fas-industry", 16, UIHelper.ACCENT_BLUE), UIHelper.ACCENT_BLUE, () -> navigate("manufacturing"), List.of("producao", "fabrico", "bom", "ficha tecnica", "ordem de producao", "cip", "industria")));
        items.add(new GlobalSearchDialog.SearchItem("mod_fleet", "Gestão de Frotas & Viaturas", "Módulos", null, UIHelper.icon("fas-car", 16, UIHelper.ACCENT_CYAN), UIHelper.ACCENT_CYAN, () -> navigate("fleet"), List.of("frota", "viaturas", "carros", "combustivel", "abastecimento", "odometro", "km/l", "motoristas")));
        items.add(new GlobalSearchDialog.SearchItem("mod_recurring", "Avenças & Contratos Recorrentes", "Módulos", null, UIHelper.icon("fas-file-contract", 16, UIHelper.ACCENT_BLUE), UIHelper.ACCENT_BLUE, () -> navigate("recurring"), List.of("avencas", "contratos", "recorrente", "mensalidades", "subscricoes", "servicos", "batch", "faturacao")));
        items.add(new GlobalSearchDialog.SearchItem("mod_quality", "Qualidade & HST (Equipamentos)", "Módulos", null, UIHelper.icon("fas-shield-alt", 16, UIHelper.ACCENT_CYAN), UIHelper.ACCENT_CYAN, () -> navigate("quality"), List.of("qualidade", "hst", "epi", "equipamento", "seguranca", "trabalhador", "validade")));
        items.add(new GlobalSearchDialog.SearchItem("mod_approvals", "Aprovações Pendentes", "Módulos", null, UIHelper.icon("fas-check-double", 16, UIHelper.MODULE_APPROVALS), UIHelper.MODULE_APPROVALS, () -> navigate("approvals"), List.of("autorizar", "pendencias", "requisicoes")));
        items.add(new GlobalSearchDialog.SearchItem("mod_notifications", "Notificações & Avisos", "Módulos", null, UIHelper.icon("fas-bell", 16, UIHelper.ACCENT_CYAN), UIHelper.ACCENT_CYAN, () -> navigate("notifications"), List.of("alertas", "sino", "avisos", "validade")));
        items.add(new GlobalSearchDialog.SearchItem("mod_config", "Configurações do Sistema", "Módulos", null, UIHelper.icon("fas-cog", 16, UIHelper.ACCENT_BLUE), UIHelper.ACCENT_BLUE, () -> navigate("config"), List.of("utilizadores", "empresa", "auditoria", "backup", "licenca")));

        // 2. Ações Rápidas & Ferramentas
        items.add(new GlobalSearchDialog.SearchItem("act_about", "Sobre o Multicore ERP", "Sistema", "F12", UIHelper.icon("fas-info-circle", 16, UIHelper.ACCENT_BLUE), UIHelper.ACCENT_BLUE, this::openAboutDialog, List.of("sobre", "versao", "sistema", "licenca", "autor", "miranda", "maunze", "desenvolvedor", "arquiteto", "creditos")));
        items.add(new GlobalSearchDialog.SearchItem("act_logout", "Terminar Sessão (Logout)", "Sistema", null, UIHelper.icon("fas-sign-out-alt", 16, UIHelper.REJECTED_RED), UIHelper.REJECTED_RED, () -> UIHelper.requestLogout(this), List.of("logout", "sair", "encerrar", "trocar utilizador", "login", "desconectar")));
        items.add(new GlobalSearchDialog.SearchItem("act_help", "Guia de Atalhos de Teclado", "Ferramentas", "F1", UIHelper.icon("fas-keyboard", 16, UIHelper.ACCENT_BLUE), UIHelper.ACCENT_BLUE, this::openShortcutHelp, List.of("atalhos", "ajuda", "comandos", "teclado", "help")));
        items.add(new GlobalSearchDialog.SearchItem("act_fullscreen", "Alternar Modo Ecrã Completo", "Ferramentas", "F11", UIHelper.icon("fas-expand", 16, UIHelper.BUTTON_NEUTRAL), UIHelper.BUTTON_NEUTRAL, this::toggleFullScreen, List.of("fullscreen", "ecra inteiro", "maximizar", "quiosque")));
        items.add(new GlobalSearchDialog.SearchItem("act_sidebar", "Alternar Menu Lateral", "Ferramentas", "Ctrl+B", UIHelper.icon("fas-bars", 16, UIHelper.BUTTON_NEUTRAL), UIHelper.BUTTON_NEUTRAL, () -> { if (sidebar != null) sidebar.toggle(); }, List.of("menu", "sidebar", "ocultar", "expandir")));
        items.add(new GlobalSearchDialog.SearchItem("act_labels", "Gerador de Etiquetas de Prateleira", "Ferramentas", null, UIHelper.icon("fas-barcode", 16, UIHelper.MODULE_STOCK), UIHelper.MODULE_STOCK, () -> { navigate("stock"); if (stockPanel != null) stockPanel.openLabelDialog(); }, List.of("etiquetas", "barcode", "preco", "prateleira", "gondola", "rotulos")));
        items.add(new GlobalSearchDialog.SearchItem("act_waste", "Gestão de Quebras, Perdas & Desperdício", "Stock", null, UIHelper.icon("fas-trash-alt", 16, UIHelper.REJECTED_RED), UIHelper.REJECTED_RED, () -> { navigate("stock"); if (stockPanel != null) stockPanel.showWasteManagement(); }, List.of("quebras", "perdas", "desperdicio", "validade", "avarias", "furto", "waste", "radar")));
        items.add(new GlobalSearchDialog.SearchItem("act_reconciliation", "Centro de Reconciliação Bancária", "Financeiro", null, UIHelper.icon("fas-university", 16, UIHelper.MODULE_FINANCEIRO), UIHelper.MODULE_FINANCEIRO, () -> { navigate("financeiro"); if (financeiroPanel != null) financeiroPanel.showReconciliationTab(); }, List.of("reconciliacao", "bancaria", "extracto", "bancos", "bim", "bci", "conciliar", "extrato")));
        items.add(new GlobalSearchDialog.SearchItem("act_forensic_audit", "Central de Auditoria Forense & Controlo de Fraude", "Fiscal & Auditoria", null, UIHelper.icon("fas-shield-alt", 16, UIHelper.REJECTED_RED), UIHelper.REJECTED_RED, () -> navigate("auditoria_forense"), List.of("auditoria", "forense", "fraude", "desvios", "anomalias", "cancelamentos", "quebras", "risco")));
        items.add(new GlobalSearchDialog.SearchItem("act_alerts", "Alertas Inteligentes & Ações Proativas", "Ferramentas", null, UIHelper.icon("fas-bell", 16, UIHelper.PENDING_YELLOW), UIHelper.PENDING_YELLOW, this::openSmartAlerts, List.of("alertas", "acoes", "proativo", "notificacoes", "urgente")));
        items.add(new GlobalSearchDialog.SearchItem("act_backup", "Cópias de Segurança & Integridade", "Ferramentas", null, UIHelper.icon("fas-database", 16, UIHelper.ACCENT_BLUE), UIHelper.ACCENT_BLUE, this::openBackupDialog, List.of("backup", "copia", "restauro", "base de dados", "seguranca")));
        items.add(new GlobalSearchDialog.SearchItem("act_theme", "Alternar Tema (Escuro / Claro / Alto Contraste)", "Ferramentas", null, UIHelper.icon("fas-adjust", 16, UIHelper.PENDING_YELLOW), UIHelper.PENDING_YELLOW, () -> UIHelper.cycleTheme(), List.of("tema", "dark", "light", "alto contraste", "outdoor", "acessibilidade", "cores")));
        items.add(new GlobalSearchDialog.SearchItem("act_theme_hc", "Modo Alto Contraste / Operação Exterior", "Acessibilidade", null, UIHelper.icon("fas-low-vision", 16, UIHelper.ACCENT_CYAN), UIHelper.ACCENT_CYAN, () -> UIHelper.setTheme(Theme.HIGH_CONTRAST), List.of("alto contraste", "outdoor", "acessibilidade", "sol", "leitura", "visibilidade")));
        items.add(new GlobalSearchDialog.SearchItem("act_recent_history", "Histórico de Itens Recentes & Quick-Recall", "Ferramentas", "Ctrl+H", UIHelper.icon("fas-history", 16, UIHelper.ACCENT_CYAN), UIHelper.ACCENT_CYAN, this::openRecentItemsHistory, List.of("historico", "recentes", "ultimos", "recall", "clientes", "faturas", "atalho")));
        items.add(new GlobalSearchDialog.SearchItem("act_density", "Alternar Densidade da Interface (Compacto / Padrão / Confortável)", "Ferramentas", null, UIHelper.icon("fas-text-height", 16, UIHelper.ACCENT_BLUE), UIHelper.ACCENT_BLUE, () -> mz.multicore.erp.gui.components.UiDensityManager.getInstance().cycleDensity(), List.of("densidade", "zoom", "escala", "compacto", "amplo", "confortavel", "linhas")));

        // 3. Ações Operacionais de Documentos
        items.add(new GlobalSearchDialog.SearchItem("act_new_invoice", "Emitir Nova Factura Comercial", "Operações Comerciais", null, UIHelper.icon("fas-file-invoice", 16, UIHelper.APPROVED_GREEN), UIHelper.APPROVED_GREEN, () -> navigate("comercial"), List.of("nova fatura", "faturar", "venda", "ft", "emitir", "comercial")));
        items.add(new GlobalSearchDialog.SearchItem("act_new_order", "Criar Nova Encomenda de Cliente", "Operações Comerciais", null, UIHelper.icon("fas-clipboard-list", 16, UIHelper.ACCENT_BLUE), UIHelper.ACCENT_BLUE, () -> navigate("comercial"), List.of("nova encomenda", "pedido", "encomendar", "separacao")));
        items.add(new GlobalSearchDialog.SearchItem("act_new_quotation", "Elaborar Nova Cotação", "Operações Comerciais", null, UIHelper.icon("fas-file-alt", 16, UIHelper.PENDING_YELLOW), UIHelper.PENDING_YELLOW, () -> navigate("comercial"), List.of("nova cotacao", "proposta", "orcamento", "cotar")));
        items.add(new GlobalSearchDialog.SearchItem("act_new_po", "Criar Encomenda a Fornecedor", "Compras", null, UIHelper.icon("fas-cart-plus", 16, UIHelper.MODULE_COMPRAS), UIHelper.MODULE_COMPRAS, () -> navigate("compras"), List.of("comprar", "encomenda fornecedor", "compras", "nova compra")));
        items.add(new GlobalSearchDialog.SearchItem("act_receive_po", "Recepção e Conferência de Mercadorias", "Compras & Stock", null, UIHelper.icon("fas-truck-loading", 16, UIHelper.MODULE_STOCK), UIHelper.MODULE_STOCK, () -> navigate("compras"), List.of("receber mercadoria", "conferencia", "descarga", "entrada stock", "recepcao")));
        items.add(new GlobalSearchDialog.SearchItem("act_transfer", "Transferência entre Armazéns", "Stock & Logística", null, UIHelper.icon("fas-dolly", 16, UIHelper.ACCENT_BLUE), UIHelper.ACCENT_BLUE, () -> navigate("stock"), List.of("transferencia", "transferir stock", "guias", "armazens", "logistica")));
        items.add(new GlobalSearchDialog.SearchItem("act_pos_cash_move", "Movimento de Caixa (Sangria / Suprimento)", "POS & Caixa", "F9", UIHelper.icon("fas-money-bill-wave", 16, UIHelper.PENDING_YELLOW), UIHelper.PENDING_YELLOW, () -> navigate("pos"), List.of("sangria", "suprimento", "caixa", "troco", "retirada", "reforco", "dinheiro", "gaveta")));

        return items;
    }

    private List<GlobalSearchDialog.SearchItem> buildSellerSearchIndex() {
        List<GlobalSearchDialog.SearchItem> items = new java.util.ArrayList<>();
        items.add(new GlobalSearchDialog.SearchItem("seller_dashboard", "Painel do Vendedor", "Vendas", null,
                UIHelper.icon("fas-th-large", 16, UIHelper.MODULE_DASHBOARD), UIHelper.MODULE_DASHBOARD,
                () -> navigate("dashboard"), List.of("inicio", "resumo", "vendedor")));
        items.add(new GlobalSearchDialog.SearchItem("seller_pos", "Nova venda no POS", "Vendas", "F9",
                UIHelper.icon("fas-cash-register", 16, UIHelper.MODULE_POS), UIHelper.MODULE_POS,
                () -> navigate("pos"), List.of("venda", "caixa", "pagamento")));
        items.add(new GlobalSearchDialog.SearchItem("seller_order", "Criar nova encomenda", "Vendas", null,
                UIHelper.icon("fas-clipboard-list", 16, UIHelper.ACCENT_BLUE), UIHelper.ACCENT_BLUE,
                () -> handleSellerDashboardAction("new-order"), List.of("encomenda", "pedido")));
        items.add(new GlobalSearchDialog.SearchItem("seller_quotation", "Criar nova cotação", "Vendas", null,
                UIHelper.icon("fas-file-signature", 16, UIHelper.PENDING_YELLOW), UIHelper.PENDING_YELLOW,
                () -> handleSellerDashboardAction("new-quotation"), List.of("cotacao", "proposta", "orcamento")));
        items.add(new GlobalSearchDialog.SearchItem("seller_clients", "Pesquisar clientes", "Vendas", null,
                UIHelper.icon("fas-address-book", 16, UIHelper.MODULE_CLIENTES), UIHelper.MODULE_CLIENTES,
                () -> navigate("clientes"), List.of("cliente", "contacto", "nuit")));
        items.add(new GlobalSearchDialog.SearchItem("seller_products", "Consultar produtos", "Vendas", null,
                UIHelper.icon("fas-box-open", 16, UIHelper.ACCENT_CYAN), UIHelper.ACCENT_CYAN,
                () -> navigate("pos"), List.of("produto", "artigo", "preco", "stock")));
        items.add(new GlobalSearchDialog.SearchItem("seller_crm", "CRM & Assistência", "Vendas", null,
                UIHelper.icon("fas-headset", 16, UIHelper.MODULE_CRM), UIHelper.MODULE_CRM,
                () -> navigate("crm"), List.of("crm", "assistencia", "ticket")));
        items.add(new GlobalSearchDialog.SearchItem("seller_notifications", "Notificações", "Sistema", null,
                UIHelper.icon("fas-bell", 16, UIHelper.ACCENT_ORANGE), UIHelper.ACCENT_ORANGE,
                () -> navigate("notifications"), List.of("alertas", "avisos")));
        items.add(new GlobalSearchDialog.SearchItem("seller_logout", "Terminar sessão", "Sistema", null,
                UIHelper.icon("fas-sign-out-alt", 16, UIHelper.REJECTED_RED), UIHelper.REJECTED_RED,
                () -> UIHelper.requestLogout(this), List.of("logout", "sair")));
        return items;
    }

    private void handleSellerDashboardAction(String action) {
        switch (action) {
            case "new-order" -> {
                navigate("comercial");
                comercialPanel.startNewOrder();
            }
            case "new-quotation" -> {
                navigate("comercial");
                comercialPanel.startNewQuotation();
            }
            case "orders" -> {
                navigate("comercial");
                comercialPanel.showCustomerOrders();
            }
            case "quotations" -> {
                navigate("comercial");
                comercialPanel.showQuotations();
            }
            case "clients" -> navigate("clientes");
            case "notifications" -> navigate("notifications");
            case "products", "pos" -> navigate("pos");
            default -> navigate("dashboard");
        }
    }
}
