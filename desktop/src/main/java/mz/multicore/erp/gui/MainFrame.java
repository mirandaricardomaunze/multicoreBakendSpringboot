package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.session.DesktopSession;
import mz.multicore.erp.desktop.session.DesktopSessionStore;
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
    private final ComercialPanel comercialPanel;
    private final FinanceiroPanel financeiroPanel;
    private final HRPanel hrPanel;
    private final CRMPanel crmPanel;
    private final ClientesPanel clientesPanel;
    private final FiscalPanel fiscalPanel;
    private final mz.multicore.erp.gui.accounting.AccountingPanel accountingPanel;
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

    private final DesktopSessionStore desktopSessionStore;
    private final mz.multicore.erp.desktop.client.VersionApiClient versionApiClient;
    private final MySubscriptionApiClient mySubscriptionApiClient;
    private final BackupApiClient backupApiClient;
    private final boolean superAdmin;
    private TopNavBar topBar;
    private CollapsibleSidebar sidebar;
    private mz.multicore.erp.gui.components.StatusBar statusBar;
    private String sessionDisplayName;
    private JLabel notificationBadgeLabel;
    private int notificationBadgeLoadVersion;

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
            mz.multicore.erp.desktop.client.SystemMonitoringApiClient systemMonitoringApiClient
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
            dashboardPanel = null; comercialPanel = null; financeiroPanel = null; hrPanel = null;
            crmPanel = null; clientesPanel = null; fiscalPanel = null; approvalsPanel = null;
            accountingPanel = null; performancePanel = null; forensicAuditPanel = null;
            posPanel = null; stockPanel = null; comprasPanel = null; configPanel = null;
            notificationFeed = null; notificationsPanel = null; notificationReadStore = null;
            plataformaPanel = new PlataformaPanel(platformApiClient, systemMonitoringApiClient);
            contentPanel.add(plataformaPanel, "plataforma");
        } else {
            dashboardPanel  = new DashboardPanel(
                    comercialApiClient, financeApiClient, approvalApiClient, crmApiClient, purchaseApiClient, inventoryApiClient,
                    forensicAuditApiClient, cashFlowForecastApiClient, creditRiskApiClient, performanceApiClient, this::navigate);
            stockPanel      = new StockPanel(inventoryApiClient, comercialApiClient, stockTransferApiClient, inventoryCountApiClient, productCategoryApiClient, printApiClient, stockWasteApiClient, inventoryPhysicalCountingApiClient);
            comercialPanel  = new ComercialPanel(comercialApiClient, inventoryApiClient, financeApiClient, creditNoteApiClient, debitNoteApiClient, posApiClient, movimentosApiClient, promotionApiClient, printApiClient,
                    () -> {
                        navigate("stock");
                        if (topBar != null) topBar.setActive("Stock & Armazéns");
                        if (sidebar != null) sidebar.setActive("Stock & Armazéns");
                        stockPanel.showWarehouseTransfers();
                    });
            financeiroPanel = new FinanceiroPanel(financeApiClient, comercialApiClient, bankReconciliationApiClient, cashFlowForecastApiClient);
            hrPanel         = new HRPanel(hrApiClient, printApiClient);
            crmPanel        = new CRMPanel(crmApiClient, comercialApiClient);
            clientesPanel   = new ClientesPanel(comercialApiClient, printApiClient, creditRiskApiClient, accountStatementApiClient);
            fiscalPanel     = new FiscalPanel(fiscalApiClient);
            accountingPanel = new mz.multicore.erp.gui.accounting.AccountingPanel(accountingApiClient);
            approvalsPanel  = new ApprovalsPanel(approvalApiClient);
            posPanel        = new POSPanel(posApiClient, comercialApiClient, inventoryApiClient, financeApiClient, promotionApiClient, scaleBarcodeParser);
            comprasPanel    = new ComprasPanel(purchaseApiClient, inventoryApiClient, comercialApiClient, financeApiClient, accountStatementApiClient);
            configPanel     = new ConfigPanel(userApiClient, auditApiClient, backupApiClient, documentConfigApiClient, supportApiClient, mySubscriptionApiClient, systemMonitoringApiClient);
            performancePanel = new PerformancePanel(performanceApiClient, desktopSessionStore);
            forensicAuditPanel = new ForensicAuditPanel(forensicAuditApiClient);
            notificationFeed = new NotificationFeed(approvalApiClient, inventoryApiClient,
                    mySubscriptionApiClient, hrApiClient, performanceApiClient, creditRiskApiClient, stockWasteApiClient, systemMonitoringApiClient);
            notificationReadStore = new NotificationReadStore();
            notificationsPanel = new NotificationsPanel(notificationFeed, notificationReadStore,
                    this::navigateFromNotification, this::updateNotificationBadge);
            plataformaPanel = null;
            contentPanel.add(dashboardPanel, "dashboard"); contentPanel.add(posPanel, "pos");
            contentPanel.add(comercialPanel, "comercial"); contentPanel.add(comprasPanel, "compras");
            contentPanel.add(stockPanel, "stock"); contentPanel.add(financeiroPanel, "financeiro");
            contentPanel.add(hrPanel, "hr"); contentPanel.add(performancePanel, "desempenho");
            contentPanel.add(crmPanel, "crm"); contentPanel.add(clientesPanel, "clientes");
            contentPanel.add(fiscalPanel, "fiscal"); contentPanel.add(accountingPanel, "contabilidade");
            contentPanel.add(approvalsPanel, "approvals"); contentPanel.add(configPanel, "config");
            contentPanel.add(forensicAuditPanel, "auditoria_forense");
            contentPanel.add(notificationsPanel, "notifications");
        }

        setLayout(new BorderLayout());
        sidebar = buildSidebar();
        topBar = buildTopBar();
        statusBar = new mz.multicore.erp.gui.components.StatusBar();

        JPanel centerContainer = new JPanel(new BorderLayout());
        centerContainer.setOpaque(false);
        centerContainer.add(topBar, BorderLayout.NORTH);
        centerContainer.add(contentPanel, BorderLayout.CENTER);
        centerContainer.add(statusBar, BorderLayout.SOUTH);

        add(sidebar, BorderLayout.WEST);
        add(centerContainer, BorderLayout.CENTER);

        // Atalhos de Teclado Rápidos (Ctrl+B, Ctrl+K, F1, F11)
        int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        getRootPane().registerKeyboardAction(e -> { if (sidebar != null) sidebar.toggle(); }, KeyStroke.getKeyStroke(KeyEvent.VK_B, mask), JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(e -> openGlobalSearch(), KeyStroke.getKeyStroke(KeyEvent.VK_K, mask), JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(e -> openRecentItemsHistory(), KeyStroke.getKeyStroke(KeyEvent.VK_H, mask), JComponent.WHEN_IN_FOCUSED_WINDOW);
        getRootPane().registerKeyboardAction(e -> openShortcutHelp(), KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
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

    /** Called by the application bootstrap once the user is authenticated. */
    public void applyAuthenticatedUser(String displayName, String role) {
        sessionDisplayName = displayName;
        String activeRole = CurrentUserContext.getRole();
        if (dashboardPanel != null) dashboardPanel.updateWelcomeMessage(displayName, activeRole);
        if (sessionUserLabel != null) sessionUserLabel.setText(displayName);
        if (sessionRoleLabel != null) sessionRoleLabel.setText(UIHelper.humanRole(activeRole));
        if (sidebar != null) sidebar.setUserProfile(displayName, UIHelper.humanRole(activeRole));
        if (statusBar != null) {
            DesktopSession s = desktopSessionStore.requireSession();
            String company = s.companies().isEmpty() ? "" : s.companies().get(0).name();
            statusBar.setContext("Painel Inicial", -1, company, displayName);
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

    private javax.swing.Icon navIcon(String code) {
        return UIHelper.icon(code, 20, topBarIconTint());
    }

    /** Tinta dos ícones da barra de topo — escura sobre barra clara, clara sobre barra escura. */
    private static Color topBarIconTint() {
        return UIHelper.TEXT_MUTED;
    }

    private TopNavBar buildTopBar() {
        if (superAdmin) {
            return buildSuperAdminTopBar();
        }

        TopNavBar bar = new TopNavBar("Painel Inicial", "");
        bar.setSearchAction(this::openGlobalSearch);

        // Área direita: utilitários + seletor de empresa + chip de utilizador.
        JComboBox<DesktopSession.CompanyAccess> companyCombo = buildCompanyCombo();
        UIHelper.styleComboBox(companyCombo);
        companyCombo.setToolTipText("Empresa ativa");
        companyCombo.setPreferredSize(new Dimension(210, 32));
        // Reaplicar o renderer DEPOIS de styleComboBox (senão mostraria CompanyAccess[...]).
        applyCompanyRenderer(companyCombo);
        bar.addTrailing(buildHistoryButton());
        bar.addTrailing(buildThemeToggle());
        bar.addTrailing(buildNotificationBell());
        javax.swing.JComponent subChip = buildSubscriptionChip();
        if (subChip != null) bar.addTrailing(subChip);
        // Seletor de empresa só quando há mais de uma — com uma só é redundante (a barra lateral já mostra
        // o nome). O combo é sempre construído (selecciona a empresa activa no contexto), mas só se
        // mostra quando há escolha a fazer.
        if (desktopSessionStore.requireSession().companies().size() > 1) {
            bar.addTrailing(companyCombo);
        }
        bar.addTrailing(buildUserChip());

        return bar;
    }

    /** Barra do superadmin: cabeçalho limpo com utilitários e chip de utilizador. */
    private TopNavBar buildSuperAdminTopBar() {
        TopNavBar bar = new TopNavBar("Consola da Plataforma", "");
        bar.setSearchAction(this::openGlobalSearch);
        bar.addTrailing(buildThemeToggle());
        bar.addTrailing(buildUserChip());
        return bar;
    }

    public CollapsibleSidebar getSidebar() {
        return sidebar;
    }

    private CollapsibleSidebar buildSidebar() {
        DesktopSession initialSession = desktopSessionStore.requireSession();
        String subBrand = initialSession.companies().isEmpty() ? "ERP Profissional" : initialSession.companies().get(0).name();
        CollapsibleSidebar sb = new CollapsibleSidebar("MULTICORE", subBrand);

        if (superAdmin) {
            sb.addSection("Plataforma");
            sb.addItem(UIHelper.icon("fas-server", 16, UIHelper.ACCENT_BLUE), "Plataforma", UIHelper.ACCENT_BLUE, () -> navigate("plataforma"));
            return sb;
        }

        // 1. OPERAÇÕES
        sb.addSection("Operações");
        sb.addItem(UIHelper.icon("fas-th-large", 16, UIHelper.MODULE_DASHBOARD), "Painel Inicial", UIHelper.MODULE_DASHBOARD, () -> navigate("dashboard"));
        sb.addItem(UIHelper.icon("fas-cash-register", 16, UIHelper.MODULE_POS), "POS — Caixa", UIHelper.MODULE_POS, () -> navigate("pos"));
        sb.addItem(UIHelper.icon("fas-clipboard-list", 16, UIHelper.MODULE_COMERCIAL), "Pedidos", UIHelper.MODULE_COMERCIAL, () -> {
            navigate("comercial");
            comercialPanel.showCustomerOrders();
        });
        sb.addItem(UIHelper.icon("fas-shopping-cart", 16, UIHelper.MODULE_COMPRAS), "Compras", UIHelper.MODULE_COMPRAS, () -> navigate("compras"));
        sb.addItem(UIHelper.icon("fas-boxes", 16, UIHelper.MODULE_STOCK), "Stock & Armazéns", UIHelper.MODULE_STOCK, () -> navigate("stock"));

        // 2. GESTÃO & CRM
        sb.addSection("Gestão & CRM");
        sb.addItem(UIHelper.icon("fas-coins", 16, UIHelper.MODULE_FINANCEIRO), "Tesouraria", UIHelper.MODULE_FINANCEIRO, () -> navigate("financeiro"));
        sb.addItem(UIHelper.icon("fas-users", 16, UIHelper.MODULE_HR), "Recursos Humanos", UIHelper.MODULE_HR, () -> navigate("hr"));
        sb.addItem(UIHelper.icon("fas-trophy", 16, UIHelper.MODULE_COMERCIAL), "Desempenho", UIHelper.MODULE_COMERCIAL, () -> navigate("desempenho"));
        sb.addItem(UIHelper.icon("fas-headset", 16, UIHelper.MODULE_CRM), "CRM & Assistência", UIHelper.MODULE_CRM, () -> navigate("crm"));
        sb.addItem(UIHelper.icon("fas-address-book", 16, UIHelper.MODULE_CLIENTES), "Clientes", UIHelper.MODULE_CLIENTES, () -> navigate("clientes"));

        // 3. FISCAL & AUDITORIA
        sb.addSection("Fiscal & Auditoria");
        sb.addItem(UIHelper.icon("fas-percent", 16, UIHelper.MODULE_FISCAL), "Área Fiscal", UIHelper.MODULE_FISCAL, () -> navigate("fiscal"));
        sb.addItem(UIHelper.icon("fas-calculator", 16, UIHelper.MODULE_ACCOUNTING), "Contabilidade", UIHelper.MODULE_ACCOUNTING, () -> navigate("contabilidade"));
        sb.addItem(UIHelper.icon("fas-check-double", 16, UIHelper.MODULE_APPROVALS), "Aprovações", UIHelper.MODULE_APPROVALS, () -> navigate("approvals"));
        sb.addItem(UIHelper.icon("fas-shield-alt", 16, UIHelper.REJECTED_RED), "Auditoria Forense", UIHelper.REJECTED_RED, () -> navigate("auditoria_forense"));

        // 4. SISTEMA
        sb.addSection("Sistema");
        sb.addItem(UIHelper.icon("fas-bell", 16, UIHelper.ACCENT_CYAN), "Notificações", UIHelper.ACCENT_CYAN, () -> navigate("notifications"));
        sb.addItem(UIHelper.icon("fas-cog", 16, UIHelper.ACCENT_BLUE), "Configurações", UIHelper.ACCENT_BLUE, () -> navigate("config"));

        return sb;
    }

    /** Botão de histórico recente na barra de menu (Ctrl+H). */
    private javax.swing.JComponent buildHistoryButton() {
        JLabel btn = new JLabel(UIHelper.icon("fas-history", 18, topBarIconTint()));
        btn.setToolTipText("Histórico de Itens Recentes (Ctrl+H)");
        btn.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        btn.setBorder(new javax.swing.border.EmptyBorder(0, 6, 0, 6));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent e) { openRecentItemsHistory(); }
        });
        return btn;
    }

    public void openRecentItemsHistory() {
        RecentItemsDialog.show(this, item -> {
            if (item != null && item.targetView() != null && !item.targetView().isBlank()) navigate(item.targetView());
        });
    }

    /** Botão de tema na barra de menu (sol/lua/ajuste). Trocar reconstrói a janela no tema escolhido. */
    private javax.swing.JComponent buildThemeToggle() {
        String code = UIHelper.isHighContrast() ? "fas-adjust" : (UIHelper.isLight() ? "fas-moon" : "fas-sun");
        String tip = UIHelper.isHighContrast()
                ? "Mudar para Tema Escuro (Atual: Alto Contraste)"
                : (UIHelper.isLight() ? "Mudar para Alto Contraste (Atual: Claro)" : "Mudar para Tema Claro (Atual: Escuro)");
        JLabel toggle = new JLabel(UIHelper.icon(code, 18, topBarIconTint()));
        toggle.setToolTipText(tip);
        toggle.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        toggle.setBorder(new javax.swing.border.EmptyBorder(0, 6, 0, 6));
        toggle.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                UIHelper.cycleTheme();
            }
        });
        return toggle;
    }

    /** Bell da barra superior: prévia curta e acesso à página completa por "Ver todas". */
    private javax.swing.JComponent buildNotificationBell() {
        JPanel bell = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 3, 5));
        bell.setOpaque(false);
        bell.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        bell.setToolTipText("Notificações");

        JLabel icon = new JLabel(UIHelper.icon("fas-bell", 19, topBarIconTint()));
        notificationBadgeLabel = new JLabel("0");
        notificationBadgeLabel.setOpaque(true);
        notificationBadgeLabel.setBackground(UIHelper.REJECTED_RED);
        notificationBadgeLabel.setForeground(Color.WHITE);
        notificationBadgeLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 9));
        notificationBadgeLabel.setHorizontalAlignment(JLabel.CENTER);
        notificationBadgeLabel.setBorder(BorderFactory.createEmptyBorder(1, 4, 1, 4));
        notificationBadgeLabel.setVisible(false);
        bell.add(icon);
        bell.add(notificationBadgeLabel);

        bell.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mousePressed(java.awt.event.MouseEvent e) {
                showNotificationPreview(bell);
            }
        });
        javax.swing.SwingUtilities.invokeLater(this::refreshNotificationBadgeAsync);
        return bell;
    }

    private void showNotificationPreview(javax.swing.JComponent anchor) {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null) return; // superadmin: sem empresa activa, sem notificações de tenant
        javax.swing.JPopupMenu popup = new javax.swing.JPopupMenu();
        popup.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIHelper.BORDER),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        javax.swing.JMenuItem loading = new javax.swing.JMenuItem("A carregar notificações…");
        loading.setEnabled(false);
        popup.add(loading);
        popup.show(anchor, -220, anchor.getHeight());

        new javax.swing.SwingWorker<List<NotificationFeed.NotificationItem>, Void>() {
            @Override protected List<NotificationFeed.NotificationItem> doInBackground() {
                return notificationFeed.load(companyId);
            }

            @Override protected void done() {
                try {
                    List<NotificationFeed.NotificationItem> items = get();
                    List<NotificationFeed.NotificationItem> unread = notificationReadStore.unread(items);
                    updateNotificationBadge(unread.size());
                    popup.removeAll();
                    if (unread.isEmpty()) {
                        javax.swing.JMenuItem empty = new javax.swing.JMenuItem("Não há notificações por ler.");
                        empty.setEnabled(false);
                        popup.add(empty);
                    } else {
                        unread.stream().limit(5).forEach(item -> {
                            javax.swing.JMenu itemMenu = new javax.swing.JMenu(
                                    item.type() + " · " + shorten(item.title(), 44));
                            itemMenu.setToolTipText(item.detail());
                            javax.swing.JMenuItem open = new javax.swing.JMenuItem(
                                    "Abrir módulo", UIHelper.icon("fas-external-link-alt", 12));
                            open.addActionListener(e -> navigateFromNotification(item.moduleCard()));
                            javax.swing.JMenuItem markRead = new javax.swing.JMenuItem(
                                    "Marcar como lida", UIHelper.icon("fas-check", 12));
                            markRead.addActionListener(e -> {
                                notificationReadStore.markRead(item);
                                updateNotificationBadge(notificationReadStore.unreadCount(items));
                                popup.setVisible(false);
                            });
                            itemMenu.add(open);
                            itemMenu.add(markRead);
                            popup.add(itemMenu);
                        });
                    }
                    popup.addSeparator();
                    javax.swing.JMenuItem markAll = new javax.swing.JMenuItem(
                            "Marcar todas como lidas", UIHelper.icon("fas-check-double", 13));
                    markAll.setEnabled(!unread.isEmpty());
                    markAll.addActionListener(e -> {
                        notificationReadStore.markAllRead(items);
                        updateNotificationBadge(0);
                        popup.setVisible(false);
                    });
                    popup.add(markAll);
                    javax.swing.JMenuItem viewAll = new javax.swing.JMenuItem("Ver todas");
                    viewAll.setIcon(UIHelper.icon("fas-list", 13));
                    viewAll.addActionListener(e -> {
                        topBar.setActive("__notifications__");
                        navigate("notifications");
                    });
                    popup.add(viewAll);
                    popup.setVisible(false);
                    popup.show(anchor, -220, anchor.getHeight());
                } catch (Exception ex) {
                    popup.removeAll();
                    javax.swing.JMenuItem error = new javax.swing.JMenuItem("Não foi possível carregar as notificações.");
                    error.setEnabled(false);
                    popup.add(error);
                    popup.setVisible(false);
                    popup.show(anchor, -260, anchor.getHeight());
                }
            }
        }.execute();
    }

    private void refreshNotificationBadgeAsync() {
        if (notificationFeed == null) return;
        // Sem empresa activa (superadmin) não há notificações de tenant para contar. Antes o contexto
        // assumia a empresa 1 e o sino mostrava alertas de uma empresa que não é a do utilizador.
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null) return;
        int version = ++notificationBadgeLoadVersion;
        new javax.swing.SwingWorker<Integer, Void>() {
            @Override protected Integer doInBackground() {
                return notificationReadStore.unreadCount(notificationFeed.load(companyId));
            }

            @Override protected void done() {
                if (version != notificationBadgeLoadVersion) return;
                try {
                    updateNotificationBadge(get());
                } catch (Exception ignored) {
                    updateNotificationBadge(0);
                }
            }
        }.execute();
    }

    private void updateNotificationBadge(int count) {
        if (notificationBadgeLabel != null) {
            notificationBadgeLabel.setText(count > 99 ? "99+" : String.valueOf(count));
            notificationBadgeLabel.setVisible(count > 0);
            notificationBadgeLabel.setToolTipText(count + " notificação" + (count == 1 ? " pendente" : " pendentes"));
        }
        if (sidebar != null) {
            sidebar.setBadge("Notificações", count);
        }
    }

    private static String shorten(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value == null ? "" : value;
        return value.substring(0, maxLength - 1) + "…";
    }

    private JComboBox<DesktopSession.CompanyAccess> buildCompanyCombo() {
        DesktopSession session = desktopSessionStore.requireSession();
        List<DesktopSession.CompanyAccess> companies = session.companies();
        JComboBox<DesktopSession.CompanyAccess> combo =
                new JComboBox<>(companies.toArray(new DesktopSession.CompanyAccess[0]));

        if (!companies.isEmpty()) {
            selectDesktopCompany(companies.get(0));
        }

        combo.addActionListener(e -> {
            DesktopSession.CompanyAccess selected = (DesktopSession.CompanyAccess) combo.getSelectedItem();
            if (selected != null) {
                selectDesktopCompany(selected);
                if (topBar != null) topBar.setSubBrand(selected.name());
                if (sidebar != null) sidebar.setSubBrand(selected.name());
                updateSessionRole();
                refreshActivePanel();
                refreshNotificationBadgeAsync();
            }
        });
        return combo;
    }

    /**
     * Renderer do combo de empresas: nome em destaque e perfil traduzido para PT em tom
     * discreto. Tem de ser aplicado depois de {@code UIHelper.styleComboBox}, que substitui
     * o renderer e mostraria o {@code toString()} do record ({@code CompanyAccess[...]}).
     */
    private void applyCompanyRenderer(JComboBox<DesktopSession.CompanyAccess> combo) {
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBackground(isSelected ? UIHelper.GRID : UIHelper.BG_CARD);
                setForeground(UIHelper.TEXT_LIGHT);
                setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
                if (value instanceof DesktopSession.CompanyAccess company) {
                    String roleColor = isSelected ? "#E5E7EB" : "#9CA3AF";
                    setText("<html>" + company.name()
                            + " <font color='" + roleColor + "'>· " + UIHelper.humanRole(company.role())
                            + "</font></html>");
                }
                return this;
            }
        });
    }

    private void selectDesktopCompany(DesktopSession.CompanyAccess company) {
        DesktopSession session = desktopSessionStore.requireSession();
        session.selectCompany(company.id());
        CurrentUserContext.setCurrentUser(session.username(), company.role());
        CurrentUserContext.setCurrentCompanyId(company.id());
    }

    private JLabel sessionUserLabel;
    private JLabel sessionRoleLabel;

    /** Compact horizontal user chip for the top bar: avatar + name/role stacked. */
    private JPanel buildUserChip() {
        JPanel chip = new JPanel(new BorderLayout(8, 0));
        chip.setOpaque(false);

        JLabel avatar = new JLabel(UIHelper.icon("fas-user-circle", 28, UIHelper.ACCENT));
        chip.add(avatar, BorderLayout.WEST);

        JPanel textStack = new JPanel();
        textStack.setOpaque(false);
        textStack.setLayout(new BoxLayout(textStack, BoxLayout.Y_AXIS));
        sessionUserLabel = new JLabel("—");
        sessionUserLabel.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        sessionUserLabel.setForeground(UIHelper.TEXT_LIGHT);
        sessionRoleLabel = new JLabel("—");
        sessionRoleLabel.setFont(new Font(UIHelper.FONT, Font.PLAIN, 10));
        sessionRoleLabel.setForeground(UIHelper.TEXT_MUTED);
        textStack.add(sessionUserLabel);
        textStack.add(sessionRoleLabel);
        chip.add(textStack, BorderLayout.CENTER);

        JButton logoutBtn = new JButton(UIHelper.icon("fas-sign-out-alt", 15, UIHelper.REJECTED_RED));
        logoutBtn.setToolTipText("Terminar Sessão (Logout)");
        logoutBtn.setFocusPainted(false);
        logoutBtn.setBorderPainted(false);
        logoutBtn.setContentAreaFilled(false);
        logoutBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoutBtn.addActionListener(e -> UIHelper.requestLogout(this));
        chip.add(logoutBtn, BorderLayout.EAST);

        return chip;
    }

    /** Assinatura da empresa activa (null para superadmin ou se falhar). Nunca deixa a UI rebentar. */
    private mz.multicore.erp.modules.subscription.dto.MySubscriptionDTO mySubscriptionSafe() {
        if (superAdmin) return null;
        try {
            return mySubscriptionApiClient.getMySubscription();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /** -1 = expirada/suspensa (vermelho); 0 = a expirar em ≤7 dias (amarelo); 1 = ok (sem chip). */
    private static int subscriptionSeverity(mz.multicore.erp.modules.subscription.dto.MySubscriptionDTO s) {
        if (s == null || !s.hasSubscription()) return 1;
        if ("EXPIRED".equals(s.status()) || "SUSPENDED".equals(s.status())) return -1;
        Long d = s.daysRemaining();
        if (d != null && d >= 0 && d <= SUB_ALERT_DAYS) return 0;
        return 1;
    }

    /** Chip na barra de topo — só aparece quando a assinatura está a expirar ou já expirou. */
    private javax.swing.JComponent buildSubscriptionChip() {
        mz.multicore.erp.modules.subscription.dto.MySubscriptionDTO s = mySubscriptionSafe();
        int sev = subscriptionSeverity(s);
        if (sev == 1) return null;

        boolean expired = sev == -1;
        Color color = expired ? UIHelper.REJECTED_RED : UIHelper.PENDING_YELLOW;
        String text = expired
                ? "Assinatura " + s.statusLabel().toLowerCase()
                : (s.daysRemaining() == 0 ? "Assinatura expira hoje" : "Assinatura: " + s.daysRemaining() + " dia(s)");

        JLabel chip = new JLabel(text, UIHelper.icon("fas-exclamation-triangle", 14, color), JLabel.LEFT);
        chip.setFont(new Font(UIHelper.FONT, Font.BOLD, 12));
        chip.setForeground(color);
        chip.setToolTipText("Veja Configurações → A Minha Assinatura, ou contacte o suporte.");
        chip.setBorder(new javax.swing.border.EmptyBorder(0, 6, 0, 6));
        return chip;
    }

    /** Aviso único no arranque quando a assinatura está a expirar/expirada. Chamado após a janela abrir. */
    public void checkSubscriptionOnStartup() {
        mz.multicore.erp.modules.subscription.dto.MySubscriptionDTO s = mySubscriptionSafe();
        int sev = subscriptionSeverity(s);
        if (sev == 1) return;
        if (sev == -1) { enforceExpiredSubscription(s); return; }
        // sev == 0: aviso ≤7 dias — só informa, não bloqueia.
        String msg = "A assinatura da sua empresa expira "
                + (s.daysRemaining() == 0 ? "hoje" : "em " + s.daysRemaining() + " dia(s)")
                + " (" + s.validUntil() + ").\nContacte o suporte da plataforma para renovar a tempo.";
        ToastManager.show(this, FeedbackType.WARNING, msg);
    }

    /**
     * Vigia periódica (6h) da assinatura com a app aberta. Ao detetar expiração/suspensão, avisa e
     * força o logout para o ecrã de login — onde o re-login fica bloqueado ({@code allowsLogin}) até
     * renovar. Superadmin não tem assinatura, por isso não é vigiado.
     */
    private void startSubscriptionWatch() {
        if (superAdmin) return;
        subscriptionWatch = new javax.swing.Timer(SUB_WATCH_INTERVAL_MS, e -> {
            mz.multicore.erp.modules.subscription.dto.MySubscriptionDTO s = mySubscriptionSafe();
            if (subscriptionSeverity(s) == -1) enforceExpiredSubscription(s);
        });
        // O arranque já é coberto por checkSubscriptionOnStartup; a vigia trata da app já aberta.
        subscriptionWatch.setInitialDelay(SUB_WATCH_INTERVAL_MS);
        subscriptionWatch.start();
    }

    /** Aviso de expiração + logout forçado. Idempotente — dispara uma única vez. */
    private void enforceExpiredSubscription(mz.multicore.erp.modules.subscription.dto.MySubscriptionDTO s) {
        if (subscriptionEnforced) return;
        subscriptionEnforced = true;
        if (subscriptionWatch != null) subscriptionWatch.stop();
        String estado = (s != null && s.statusLabel() != null) ? s.statusLabel().toLowerCase() : "expirada";
        javax.swing.JOptionPane.showMessageDialog(this,
                "A assinatura da sua empresa está " + estado + ".\n"
                        + "O acesso vai ser suspenso. Contacte o suporte da plataforma para regularizar.",
                "Assinatura", javax.swing.JOptionPane.ERROR_MESSAGE);
        if (UIHelper.onForcedLogout != null) {
            UIHelper.onForcedLogout.run();
        }
    }

    @Override
    public void dispose() {
        if (subscriptionWatch != null) subscriptionWatch.stop();
        super.dispose();
    }

    private void navigate(String cardName) {
        if ("risco_credito".equals(cardName)) {
            cardName = "clientes";
            if (clientesPanel != null) clientesPanel.selectCreditRiskTab();
        } else if ("stock_waste".equals(cardName)) {
            cardName = "stock";
            if (stockPanel != null) stockPanel.showWasteManagement();
        } else if ("previsao_tesouraria".equals(cardName)) {
            cardName = "financeiro";
            if (financeiroPanel != null) financeiroPanel.showForecastTab();
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
            case "approvals"  -> "Aprovações";
            case "auditoria_forense" -> "Auditoria Forense";
            case "config"     -> "Configurações";
            case "notifications" -> "Notificações";
            case "plataforma" -> "Plataforma";
            default           -> cardName;
        };
        if (statusBar != null) statusBar.setModule(modName);
        if (topBar != null) topBar.setActive(modName);
        if (sidebar != null) sidebar.setActive(modName);
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
            default -> null;
        };
        topBar.setActive(navLabel);
        navigate(cardName);
    }

    private void refreshActivePanel() {
        for (Component comp : contentPanel.getComponents()) {
            if (!comp.isVisible()) continue;
            if (comp instanceof DashboardPanel p)  p.refreshData();
            else if (comp instanceof ComercialPanel p)  p.onPanelSelected();
            else if (comp instanceof FinanceiroPanel p) p.onPanelSelected();
            else if (comp instanceof HRPanel p)         p.onPanelSelected();
            else if (comp instanceof PerformancePanel p) p.onPanelSelected();
            else if (comp instanceof CRMPanel p)        p.onPanelSelected();
            else if (comp instanceof ClientesPanel p)   p.onPanelSelected();
            else if (comp instanceof FiscalPanel p)     p.onPanelSelected();
            else if (comp instanceof mz.multicore.erp.gui.accounting.AccountingPanel p) p.onPanelSelected();
            else if (comp instanceof ApprovalsPanel p)  p.onPanelSelected();
            else if (comp instanceof POSPanel p)        p.onPanelSelected();
            else if (comp instanceof StockPanel p)      p.onPanelSelected();
            else if (comp instanceof ComprasPanel p)    p.onPanelSelected();
            else if (comp instanceof ConfigPanel p)     p.onPanelSelected();
            else if (comp instanceof NotificationsPanel p) p.onPanelSelected();
            else if (comp instanceof PlataformaPanel p) p.onPanelSelected();
        }
    }

    private void updateSessionRole() {
        String activeRole = CurrentUserContext.getRole();
        if (sessionRoleLabel != null) {
            sessionRoleLabel.setText(UIHelper.humanRole(activeRole));
        }
        if (sessionDisplayName != null && dashboardPanel != null) {
            dashboardPanel.updateWelcomeMessage(sessionDisplayName, activeRole);
        }
    }

    public void openGlobalSearch() {
        GlobalSearchDialog.show(this, buildSearchIndex());
    }

    public void openShortcutHelp() {
        ShortcutHelpDialog.show(this);
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
        items.add(new GlobalSearchDialog.SearchItem(
                "mod_credit_risk", "Risco de Crédito & Cobrança (Aging)", "Módulos", null,
                UIHelper.icon("fas-file-invoice-dollar", 16, UIHelper.MODULE_COMERCIAL), UIHelper.MODULE_COMERCIAL,
                () -> {
                    navigate("clientes");
                    if (clientesPanel != null) clientesPanel.selectCreditRiskTab();
                },
                List.of("aging", "cobranca", "risco", "credito", "mora", "devedores", "bloqueio")
        ));
        items.add(new GlobalSearchDialog.SearchItem(
                "mod_fiscal", "Área Fiscal & IVA", "Módulos", null,
                UIHelper.icon("fas-percent", 16, UIHelper.MODULE_FISCAL), UIHelper.MODULE_FISCAL,
                () -> navigate("fiscal"), List.of("imposto", "declaracao", "mapa", "retencoes")
        ));
        items.add(new GlobalSearchDialog.SearchItem("mod_contabilidade", "Contabilidade & Razão", "Módulos", null, UIHelper.icon("fas-calculator", 16, UIHelper.MODULE_ACCOUNTING), UIHelper.MODULE_ACCOUNTING, () -> navigate("contabilidade"), List.of("lancamentos", "diario", "balancete", "contas")));
        items.add(new GlobalSearchDialog.SearchItem("mod_approvals", "Aprovações Pendentes", "Módulos", null, UIHelper.icon("fas-check-double", 16, UIHelper.MODULE_APPROVALS), UIHelper.MODULE_APPROVALS, () -> navigate("approvals"), List.of("autorizar", "pendencias", "requisicoes")));
        items.add(new GlobalSearchDialog.SearchItem("mod_notifications", "Notificações & Avisos", "Módulos", null, UIHelper.icon("fas-bell", 16, UIHelper.ACCENT_CYAN), UIHelper.ACCENT_CYAN, () -> navigate("notifications"), List.of("alertas", "sino", "avisos", "validade")));
        items.add(new GlobalSearchDialog.SearchItem("mod_config", "Configurações do Sistema", "Módulos", null, UIHelper.icon("fas-cog", 16, UIHelper.ACCENT_BLUE), UIHelper.ACCENT_BLUE, () -> navigate("config"), List.of("utilizadores", "empresa", "auditoria", "backup", "licenca")));

        // 2. Ações Rápidas & Ferramentas
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
}
