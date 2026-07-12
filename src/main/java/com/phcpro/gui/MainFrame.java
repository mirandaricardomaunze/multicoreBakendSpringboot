package com.phcpro.gui;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.desktop.session.DesktopSession;
import com.phcpro.desktop.session.DesktopSessionStore;
import com.phcpro.desktop.client.ComercialApiClient;
import com.phcpro.gui.components.Theme;
import com.phcpro.gui.components.TopNavBar;
import com.phcpro.gui.components.UIHelper;
import com.phcpro.modules.approvals.service.ApprovalService;
import com.phcpro.modules.audit.service.AuditLogService;
import com.phcpro.modules.backup.service.BackupService;
import com.phcpro.modules.comercial.service.ComercialService;
import com.phcpro.modules.company.service.CompanyService;
import com.phcpro.modules.crm.service.CRMService;
import com.phcpro.modules.financeira.service.FinanceService;
import com.phcpro.modules.hr.service.HRService;
import com.phcpro.modules.hr.service.PayrollTaxService;
import com.phcpro.modules.inventory.service.InventoryService;
import com.phcpro.modules.inventory.service.StockTransferService;
import com.phcpro.modules.pos.service.POSService;
import com.phcpro.modules.printing.InvoicePrintService;
import com.phcpro.modules.comercial.service.CreditNoteService;
import com.phcpro.modules.comercial.service.DebitNoteService;
import com.phcpro.modules.fiscal.service.FiscalSummaryService;
import com.phcpro.modules.fiscal.service.TaxRateService;
import com.phcpro.modules.fiscal.service.WithholdingService;
import com.phcpro.modules.printing.CreditNotePrintService;
import com.phcpro.modules.printing.DebitNotePrintService;
import com.phcpro.modules.printing.InventoryReportPrintService;
import com.phcpro.modules.printing.IvaDeclarationPrintService;
import com.phcpro.modules.printing.PayrollFiscalMapPrintService;
import com.phcpro.modules.printing.OrderPrintService;
import com.phcpro.modules.printing.PayslipPrintService;
import com.phcpro.modules.printing.ReceiptPrintService;
import com.phcpro.modules.printing.StockTransferPrintService;
import com.phcpro.modules.purchases.service.PurchaseService;
import com.phcpro.modules.users.service.AppUserService;

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
    private final ApprovalsPanel approvalsPanel;
    private final POSPanel posPanel;
    private final StockPanel stockPanel;
    private final ComprasPanel comprasPanel;
    private final ConfigPanel configPanel;
    private final PlataformaPanel plataformaPanel;

    private final CompanyService companyService;
    private final DesktopSessionStore desktopSessionStore;
    private final com.phcpro.modules.subscription.service.SubscriptionService subscriptionService;
    private final boolean superAdmin;
    private TopNavBar topBar;
    private com.phcpro.gui.components.StatusBar statusBar;
    private String sessionDisplayName;

    /** Antecedência (dias) a partir da qual se avisa o assinante que a assinatura vai expirar. */
    private static final long SUB_ALERT_DAYS = 7;
    /** Intervalo da vigia de assinatura enquanto a app está aberta (6 horas). */
    private static final int SUB_WATCH_INTERVAL_MS = 6 * 60 * 60 * 1000;
    private javax.swing.Timer subscriptionWatch;
    private boolean subscriptionEnforced;

    public MainFrame(
            ComercialService comercialService,
            ComercialApiClient comercialApiClient,
            FinanceService financeService,
            ApprovalService approvalService,
            CRMService crmService,
            HRService hrService,
            InventoryService inventoryService,
            POSService posService,
            PurchaseService purchaseService,
            com.phcpro.modules.purchases.service.PurchaseOrderService purchaseOrderService,
            com.phcpro.modules.purchases.service.ReorderService reorderService,
            com.phcpro.modules.comercial.service.ProductCategoryService productCategoryService,
            AppUserService userService,
            AuditLogService auditLogService,
            BackupService backupService,
            com.phcpro.modules.backup.service.DatabaseBackupService databaseBackupService,
            com.phcpro.modules.backup.service.ScheduledBackupService scheduledBackupService,
            CompanyService companyService,
            com.phcpro.modules.promotions.service.PromotionService promotionService,
            com.phcpro.modules.movimentos.service.MovimentosService movimentosService,
            DesktopSessionStore desktopSessionStore,
            ReceiptPrintService receiptPrintService,
            InvoicePrintService invoicePrintService,
            OrderPrintService orderPrintService,
            StockTransferService stockTransferService,
            StockTransferPrintService stockTransferPrintService,
            InventoryReportPrintService inventoryReportPrintService,
            com.phcpro.modules.printing.InventoryCountSheetPrintService inventoryCountSheetPrintService,
            PayslipPrintService payslipPrintService,
            CreditNoteService creditNoteService,
            DebitNoteService debitNoteService,
            CreditNotePrintService creditNotePrintService,
            DebitNotePrintService debitNotePrintService,
            TaxRateService taxRateService,
            WithholdingService withholdingService,
            FiscalSummaryService fiscalSummaryService,
            com.phcpro.modules.fiscal.service.FiscalSalesExportService fiscalSalesExportService,
            PayrollTaxService payrollTaxService,
            IvaDeclarationPrintService ivaDeclarationPrintService,
            PayrollFiscalMapPrintService payrollFiscalMapPrintService,
            com.phcpro.modules.printing.GuideRemittancePrintService guideRemittancePrintService,
            com.phcpro.modules.documents.service.DocumentConfigService documentConfigService,
            com.phcpro.modules.platform.service.PlatformCompanyService platformCompanyService,
            com.phcpro.modules.subscription.service.SubscriptionService subscriptionService,
            com.phcpro.modules.platform.service.PlatformUserService platformUserService,
            com.phcpro.modules.support.service.SupportService supportService,
            com.phcpro.modules.pos.scale.ScaleBarcodeParser scaleBarcodeParser,
            com.phcpro.modules.inventory.service.InventoryCountService inventoryCountService,
            com.phcpro.modules.printing.POSZReportPrintService posZReportPrintService
    ) {
        this.companyService = companyService;
        this.desktopSessionStore = desktopSessionStore;
        this.subscriptionService = subscriptionService;
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
            posPanel = null; stockPanel = null; comprasPanel = null; configPanel = null;
            plataformaPanel = new PlataformaPanel(platformCompanyService, subscriptionService, platformUserService, supportService);
            contentPanel.add(plataformaPanel, "plataforma");
        } else {
            dashboardPanel  = new DashboardPanel(comercialService, financeService, approvalService, crmService, purchaseService, inventoryService);
            comercialPanel  = new ComercialPanel(comercialService, inventoryService, financeService, invoicePrintService, orderPrintService, guideRemittancePrintService, companyService, creditNoteService, debitNoteService, creditNotePrintService, debitNotePrintService, posService, promotionService, movimentosService);
            financeiroPanel = new FinanceiroPanel(financeService, comercialService);
            hrPanel         = new HRPanel(hrService, payslipPrintService);
            crmPanel        = new CRMPanel(crmService);
            clientesPanel   = new ClientesPanel(comercialApiClient);
            fiscalPanel     = new FiscalPanel(taxRateService, withholdingService, fiscalSummaryService, fiscalSalesExportService, payrollTaxService, ivaDeclarationPrintService, payrollFiscalMapPrintService);
            approvalsPanel  = new ApprovalsPanel(approvalService);
            posPanel        = new POSPanel(posService, comercialService, inventoryService, financeService, receiptPrintService, companyService, promotionService, scaleBarcodeParser, posZReportPrintService);
            stockPanel      = new StockPanel(inventoryService, comercialService, stockTransferService, stockTransferPrintService, inventoryReportPrintService, inventoryCountSheetPrintService, inventoryCountService, productCategoryService);
            comprasPanel    = new ComprasPanel(purchaseService, purchaseOrderService, reorderService, inventoryService, comercialService, financeService);
            configPanel     = new ConfigPanel(userService, auditLogService, backupService, databaseBackupService, scheduledBackupService, documentConfigService, supportService, subscriptionService);
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
            contentPanel.add(approvalsPanel,  "approvals");
            contentPanel.add(configPanel,     "config");
        }

        setLayout(new BorderLayout());

        topBar = buildTopBar();
        statusBar = new com.phcpro.gui.components.StatusBar();
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
        }
    }

    private javax.swing.Icon navIcon(String code) {
        return UIHelper.icon(code, 20, topBarIconTint());
    }

    /** Tinta dos ícones da barra de topo — escura sobre barra clara, clara sobre barra escura. */
    private static Color topBarIconTint() {
        return UIHelper.isLight() ? new Color(71, 85, 105) : new Color(229, 231, 235);
    }

    private TopNavBar buildTopBar() {
        if (superAdmin) {
            return buildSuperAdminTopBar();
        }

        TopNavBar bar = new TopNavBar("MULTICORE", "ERP Profissional");

        // Navegação: ícones-only com tooltip = nome do módulo (CONVENTIONS: UIHelper.icon, sem emojis).
        bar.addItem(navIcon("fas-th-large"),            "Painel Inicial",     UIHelper.MODULE_DASHBOARD,  () -> navigate("dashboard"));
        bar.addItem(navIcon("fas-cash-register"),       "POS — Caixa",        UIHelper.MODULE_POS,        () -> navigate("pos"));
        bar.addItem(navIcon("fas-file-invoice"),        "Vendas & Faturação", UIHelper.MODULE_COMERCIAL,  () -> navigate("comercial"));
        bar.addItem(navIcon("fas-shopping-cart"),       "Compras",            UIHelper.MODULE_COMPRAS,    () -> navigate("compras"));
        bar.addItem(navIcon("fas-boxes"),               "Stock & Armazéns",   UIHelper.MODULE_STOCK,      () -> navigate("stock"));
        bar.addItem(navIcon("fas-coins"),               "Tesouraria",         UIHelper.MODULE_FINANCEIRO, () -> navigate("financeiro"));
        bar.addItem(navIcon("fas-users"),               "Recursos Humanos",   UIHelper.MODULE_HR,         () -> navigate("hr"));
        bar.addItem(navIcon("fas-headset"),             "CRM & Assistência",  UIHelper.MODULE_CRM,        () -> navigate("crm"));
        bar.addItem(navIcon("fas-address-book"),        "Clientes",           UIHelper.MODULE_CLIENTES,   () -> navigate("clientes"));
        bar.addItem(navIcon("fas-percent"),             "Área Fiscal",        UIHelper.MODULE_FISCAL,     () -> navigate("fiscal"));
        bar.addItem(navIcon("fas-check-double"),        "Aprovações",         UIHelper.MODULE_APPROVALS,  () -> navigate("approvals"));
        bar.addItem(navIcon("fas-cog"),                 "Configurações",      UIHelper.MODULE_CONFIG,     () -> navigate("config"));

        // Área direita: seletor de empresa + chip de utilizador.
        JComboBox<DesktopSession.CompanyAccess> companyCombo = buildCompanyCombo();
        UIHelper.styleComboBox(companyCombo);
        companyCombo.setToolTipText("Empresa ativa");
        companyCombo.setPreferredSize(new Dimension(210, 32));
        // Reaplicar o renderer DEPOIS de styleComboBox (senão mostraria CompanyAccess[...]).
        applyCompanyRenderer(companyCombo);
        bar.addTrailing(buildThemeToggle());
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
                setBackground(isSelected ? new Color(55, 65, 81) : UIHelper.BG_CARD);
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
        sessionRoleLabel.setForeground(new Color(156, 163, 175));
        textStack.add(sessionUserLabel);
        textStack.add(sessionRoleLabel);
        chip.add(textStack, BorderLayout.CENTER);

        return chip;
    }

    /** Assinatura da empresa activa (null para superadmin ou se falhar). Nunca deixa a UI rebentar. */
    private com.phcpro.modules.subscription.dto.MySubscriptionDTO mySubscriptionSafe() {
        if (superAdmin) return null;
        try {
            return subscriptionService.getMySubscription();
        } catch (RuntimeException ex) {
            return null;
        }
    }

    /** -1 = expirada/suspensa (vermelho); 0 = a expirar em ≤7 dias (amarelo); 1 = ok (sem chip). */
    private static int subscriptionSeverity(com.phcpro.modules.subscription.dto.MySubscriptionDTO s) {
        if (s == null || !s.hasSubscription()) return 1;
        if ("EXPIRED".equals(s.status()) || "SUSPENDED".equals(s.status())) return -1;
        Long d = s.daysRemaining();
        if (d != null && d >= 0 && d <= SUB_ALERT_DAYS) return 0;
        return 1;
    }

    /** Chip na barra de topo — só aparece quando a assinatura está a expirar ou já expirou. */
    private javax.swing.JComponent buildSubscriptionChip() {
        com.phcpro.modules.subscription.dto.MySubscriptionDTO s = mySubscriptionSafe();
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
        com.phcpro.modules.subscription.dto.MySubscriptionDTO s = mySubscriptionSafe();
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
            com.phcpro.modules.subscription.dto.MySubscriptionDTO s = mySubscriptionSafe();
            if (subscriptionSeverity(s) == -1) enforceExpiredSubscription(s);
        });
        // O arranque já é coberto por checkSubscriptionOnStartup; a vigia trata da app já aberta.
        subscriptionWatch.setInitialDelay(SUB_WATCH_INTERVAL_MS);
        subscriptionWatch.start();
    }

    /** Aviso de expiração + logout forçado. Idempotente — dispara uma única vez. */
    private void enforceExpiredSubscription(com.phcpro.modules.subscription.dto.MySubscriptionDTO s) {
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
                case "approvals"  -> "Aprova\u00e7\u00f5es";
                case "config"     -> "Configura\u00e7\u00f5es";
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
            case "approvals"  -> approvalsPanel.onPanelSelected();
            case "pos"        -> posPanel.onPanelSelected();
            case "stock"      -> stockPanel.onPanelSelected();
            case "compras"    -> comprasPanel.onPanelSelected();
            case "config"     -> configPanel.onPanelSelected();
            case "plataforma" -> plataformaPanel.onPanelSelected();
        }
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
            else if (comp instanceof ApprovalsPanel p)  p.onPanelSelected();
            else if (comp instanceof POSPanel p)        p.onPanelSelected();
            else if (comp instanceof StockPanel p)      p.onPanelSelected();
            else if (comp instanceof ComprasPanel p)    p.onPanelSelected();
            else if (comp instanceof ConfigPanel p)     p.onPanelSelected();
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
