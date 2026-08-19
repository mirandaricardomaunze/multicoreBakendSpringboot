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
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.desktop.client.HRApiClient;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
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
    private final NotificationFeed notificationFeed;
    private final NotificationsPanel notificationsPanel;
    private final NotificationReadStore notificationReadStore;

    private final DesktopSessionStore desktopSessionStore;
    private final mz.multicore.erp.desktop.client.VersionApiClient versionApiClient;
    private final MySubscriptionApiClient mySubscriptionApiClient;
    private final boolean superAdmin;
    private TopNavBar topBar;
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
            mz.multicore.erp.desktop.client.AccountingApiClient accountingApiClient,
            mz.multicore.erp.desktop.client.VersionApiClient versionApiClient
    ) {
        this.desktopSessionStore = desktopSessionStore;
        this.versionApiClient = versionApiClient;
        this.mySubscriptionApiClient = mySubscriptionApiClient;
        this.superAdmin = desktopSessionStore.requireSession().superAdmin();

        setTitle("MULTICORE — Gestão Profissional");
        setIconImage(UIHelper.iconImage("fas-cube", 64, UIHelper.ACCENT));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 820);
        setMinimumSize(new Dimension(1024, 700));
        setLocationRelativeTo(null);
        UIHelper.registerMainWindow(this); // contém modais dentro da janela principal (mesmo ao arrastar)
        getContentPane().setBackground(UIHelper.BG_DARK);

        if (superAdmin) {
            // O superadmin só usa a consola da plataforma. Não construir os painéis de empresa evita
            // carregamentos e avisos ("nenhum armazém disponível", etc.) de módulos que exigem uma
            // empresa activa — que o superadmin não tem.
            dashboardPanel = null; comercialPanel = null; financeiroPanel = null; hrPanel = null;
            crmPanel = null; clientesPanel = null; fiscalPanel = null; approvalsPanel = null;
            accountingPanel = null;
            posPanel = null; stockPanel = null; comprasPanel = null; configPanel = null;
            notificationFeed = null; notificationsPanel = null; notificationReadStore = null;
            plataformaPanel = new PlataformaPanel(platformApiClient);
            contentPanel.add(plataformaPanel, "plataforma");
        } else {
            dashboardPanel  = new DashboardPanel(comercialApiClient, financeApiClient, approvalApiClient, crmApiClient, purchaseApiClient, inventoryApiClient);
            // O Stock nasce antes do Comercial porque o Comercial recebe um atalho para ele: a guia
            // de transferência vive no Stock (é lá que ela pertence), mas quem a procura procura-a
            // ao pé das Guias de Remessa.
            stockPanel      = new StockPanel(inventoryApiClient, comercialApiClient, stockTransferApiClient, inventoryCountApiClient, productCategoryApiClient);
            comercialPanel  = new ComercialPanel(comercialApiClient, inventoryApiClient, financeApiClient, creditNoteApiClient, debitNoteApiClient, posApiClient, movimentosApiClient, promotionApiClient,
                    () -> { navigate("stock"); topBar.setActive("Stock & Armazéns"); stockPanel.showWarehouseTransfers(); });
            financeiroPanel = new FinanceiroPanel(financeApiClient, comercialApiClient);
            hrPanel         = new HRPanel(hrApiClient);
            crmPanel        = new CRMPanel(crmApiClient);
            clientesPanel   = new ClientesPanel(comercialApiClient);
            fiscalPanel     = new FiscalPanel(fiscalApiClient);
            accountingPanel = new mz.multicore.erp.gui.accounting.AccountingPanel(accountingApiClient);
            approvalsPanel  = new ApprovalsPanel(approvalApiClient);
            posPanel        = new POSPanel(posApiClient, comercialApiClient, inventoryApiClient, financeApiClient, promotionApiClient, scaleBarcodeParser);
            comprasPanel    = new ComprasPanel(purchaseApiClient, inventoryApiClient, comercialApiClient, financeApiClient);
            configPanel     = new ConfigPanel(userApiClient, auditApiClient, backupApiClient, documentConfigApiClient, supportApiClient, mySubscriptionApiClient);
            notificationFeed = new NotificationFeed(approvalApiClient, inventoryApiClient, mySubscriptionApiClient);
            notificationReadStore = new NotificationReadStore();
            notificationsPanel = new NotificationsPanel(notificationFeed, notificationReadStore,
                    this::navigateFromNotification, this::updateNotificationBadge);
            plataformaPanel = null;

            contentPanel.add(dashboardPanel,  "dashboard");
            contentPanel.add(posPanel,        "pos");
            contentPanel.add(comercialPanel,  "comercial");
            contentPanel.add(comprasPanel,    "compras");
            contentPanel.add(stockPanel,      "stock");
            contentPanel.add(financeiroPanel, "financeiro");
            contentPanel.add(hrPanel,         "hr");
            contentPanel.add(crmPanel,        "crm");
            contentPanel.add(clientesPanel,   "clientes");
            contentPanel.add(fiscalPanel,     "fiscal");
            contentPanel.add(accountingPanel, "contabilidade");
            contentPanel.add(approvalsPanel,  "approvals");
            contentPanel.add(configPanel,     "config");
            contentPanel.add(notificationsPanel, "notifications");
        }

        setLayout(new BorderLayout());

        topBar = buildTopBar();
        statusBar = new mz.multicore.erp.gui.components.StatusBar();
        add(topBar, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
        add(statusBar, BorderLayout.SOUTH);

        if (superAdmin) {
            navigate("plataforma");
            topBar.setActive("Plataforma");
        } else {
            topBar.setActive("Painel Inicial");
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

        TopNavBar bar = new TopNavBar("MULTICORE", "ERP Profissional");

        // Navegação: ícones-only com tooltip = nome do módulo (CONVENTIONS: UIHelper.icon, sem emojis).
        bar.addItem(navIcon("fas-th-large"),            "Painel Inicial",     UIHelper.MODULE_DASHBOARD,  () -> navigate("dashboard"));
        bar.addItem(navIcon("fas-cash-register"),       "POS — Caixa",        UIHelper.MODULE_POS,        () -> navigate("pos"));
        bar.addItem(navIcon("fas-clipboard-list"),      "Pedidos",            UIHelper.MODULE_COMERCIAL,  () -> {
            navigate("comercial");
            comercialPanel.showCustomerOrders();
        });
        bar.addItem(navIcon("fas-shopping-cart"),       "Compras",            UIHelper.MODULE_COMPRAS,    () -> navigate("compras"));
        bar.addItem(navIcon("fas-boxes"),               "Stock & Armazéns",   UIHelper.MODULE_STOCK,      () -> navigate("stock"));
        bar.addItem(navIcon("fas-coins"),               "Tesouraria",         UIHelper.MODULE_FINANCEIRO, () -> navigate("financeiro"));
        bar.addItem(navIcon("fas-users"),               "Recursos Humanos",   UIHelper.MODULE_HR,         () -> navigate("hr"));
        bar.addItem(navIcon("fas-headset"),             "CRM & Assistência",  UIHelper.MODULE_CRM,        () -> navigate("crm"));
        bar.addItem(navIcon("fas-address-book"),        "Clientes",           UIHelper.MODULE_CLIENTES,   () -> navigate("clientes"));
        bar.addMenu(navIcon("fas-ellipsis-h"), "Mais", UIHelper.MODULE_CONFIG, List.of(
                new TopNavBar.MenuEntry(navIcon("fas-percent"), "Área Fiscal", () -> navigate("fiscal")),
                new TopNavBar.MenuEntry(navIcon("fas-book"), "Contabilidade", () -> navigate("contabilidade")),
                new TopNavBar.MenuEntry(navIcon("fas-check-double"), "Aprovações", () -> navigate("approvals")),
                new TopNavBar.MenuEntry(navIcon("fas-cog"), "Configurações", () -> navigate("config"))
        ));

        // Área direita: seletor de empresa + chip de utilizador.
        JComboBox<DesktopSession.CompanyAccess> companyCombo = buildCompanyCombo();
        UIHelper.styleComboBox(companyCombo);
        companyCombo.setToolTipText("Empresa ativa");
        companyCombo.setPreferredSize(new Dimension(210, 32));
        // Reaplicar o renderer DEPOIS de styleComboBox (senão mostraria CompanyAccess[...]).
        applyCompanyRenderer(companyCombo);
        bar.addTrailing(buildThemeToggle());
        bar.addTrailing(buildNotificationBell());
        javax.swing.JComponent subChip = buildSubscriptionChip();
        if (subChip != null) bar.addTrailing(subChip);
        // Seletor de empresa só quando há mais de uma — com uma só é redundante (a sub-marca já mostra
        // o nome). O combo é sempre construído (selecciona a empresa activa no contexto), mas só se
        // mostra quando há escolha a fazer.
        if (desktopSessionStore.requireSession().companies().size() > 1) {
            bar.addTrailing(companyCombo);
        }
        bar.addTrailing(buildUserChip());

        // Marca: MULTICORE no topo, nome da empresa activa por baixo.
        DesktopSession initialSession = desktopSessionStore.requireSession();
        if (!initialSession.companies().isEmpty()) {
            bar.setSubBrand(initialSession.companies().get(0).name());
        }

        return bar;
    }

    /** Barra do superadmin: só a consola da plataforma, sem seletor de empresa nem abas de tenant. */
    private TopNavBar buildSuperAdminTopBar() {
        TopNavBar bar = new TopNavBar("MULTICORE", "Consola da Plataforma");
        bar.addItem(navIcon("fas-server"), "Plataforma", UIHelper.MODULE_CONFIG, () -> navigate("plataforma"));
        bar.addTrailing(buildThemeToggle());
        bar.addTrailing(buildUserChip());
        bar.setSubBrand("Plataforma");
        return bar;
    }

    /** Botão de tema na barra de menu (ícone sol/lua). Trocar reconstrói a janela no tema escolhido. */
    private javax.swing.JComponent buildThemeToggle() {
        String code = UIHelper.isLight() ? "fas-moon" : "fas-sun";
        String tip = UIHelper.isLight() ? "Mudar para tema escuro" : "Mudar para tema claro";
        JLabel toggle = new JLabel(UIHelper.icon(code, 18, topBarIconTint()));
        toggle.setToolTipText(tip);
        toggle.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        toggle.setBorder(new javax.swing.border.EmptyBorder(0, 6, 0, 6));
        toggle.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                UIHelper.setTheme(UIHelper.isLight() ? Theme.DARK : Theme.LIGHT);
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
        if (notificationBadgeLabel == null) return;
        notificationBadgeLabel.setText(count > 99 ? "99+" : String.valueOf(count));
        notificationBadgeLabel.setVisible(count > 0);
        notificationBadgeLabel.setToolTipText(count + " notificação" + (count == 1 ? " pendente" : " pendentes"));
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
        javax.swing.JOptionPane.showMessageDialog(this, msg, "Assinatura",
                javax.swing.JOptionPane.WARNING_MESSAGE);
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
        cardLayout.show(contentPanel, cardName);
        if (statusBar != null) {
            String modName = switch (cardName) {
                case "dashboard"  -> "Painel Inicial";
                case "pos"        -> "POS \u2014 Caixa";
                case "comercial"  -> "Vendas & Fatura\u00e7\u00e3o";
                case "compras"    -> "Compras";
                case "stock"      -> "Stock & Armaz\u00e9ns";
                case "financeiro" -> "Tesouraria";
                case "hr"         -> "Recursos Humanos";
                case "crm"        -> "CRM & Assist\u00eancia";
                case "clientes"   -> "Clientes";
                case "fiscal"     -> "\u00c1rea Fiscal";
                case "contabilidade" -> "Contabilidade";
                case "approvals"  -> "Aprova\u00e7\u00f5es";
                case "config"     -> "Configura\u00e7\u00f5es";
                case "notifications" -> "Notifica\u00e7\u00f5es";
                case "plataforma" -> "Plataforma";
                default           -> cardName;
            };
            statusBar.setModule(modName);
        }
        refreshPanel(cardName);
    }

    private void refreshPanel(String cardName) {
        switch (cardName) {
            case "dashboard"  -> dashboardPanel.refreshData();
            case "comercial"  -> comercialPanel.onPanelSelected();
            case "financeiro" -> financeiroPanel.onPanelSelected();
            case "hr"         -> hrPanel.onPanelSelected();
            case "crm"        -> crmPanel.onPanelSelected();
            case "clientes"   -> clientesPanel.onPanelSelected();
            case "fiscal"     -> fiscalPanel.onPanelSelected();
            case "contabilidade" -> accountingPanel.onPanelSelected();
            case "approvals"  -> approvalsPanel.onPanelSelected();
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
            case "stock" -> "Stock & Armazéns";
            case "config" -> "Configurações";
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
}
