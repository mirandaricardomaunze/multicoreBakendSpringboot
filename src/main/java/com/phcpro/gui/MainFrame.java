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

    private static final Color C_DASHBOARD  = UIHelper.ACCENT_BLUE;
    private static final Color C_POS        = new Color(236, 72, 153);
    private static final Color C_COMERCIAL  = UIHelper.ACCENT;
    private static final Color C_COMPRAS    = new Color(245, 158, 11);
    private static final Color C_STOCK      = new Color(16, 185, 129);
    private static final Color C_FINANCEIRO = UIHelper.APPROVED_GREEN;
    private static final Color C_HR         = UIHelper.ACCENT;
    private static final Color C_CRM        = UIHelper.ACCENT_BLUE;
    private static final Color C_CLIENTES   = new Color(14, 165, 233);
    private static final Color C_FISCAL     = new Color(202, 138, 4);
    private static final Color C_APPROVALS  = UIHelper.PENDING_YELLOW;
    private static final Color C_CONFIG     = new Color(107, 114, 128);

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
    private final boolean superAdmin;
    private TopNavBar topBar;
    private String sessionDisplayName;

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
            com.phcpro.modules.subscription.service.SubscriptionService subscriptionService
    ) {
        this.companyService = companyService;
        this.desktopSessionStore = desktopSessionStore;
        this.superAdmin = desktopSessionStore.requireSession().superAdmin();

        setTitle("MULTICORE — Gestão Profissional");
        setIconImage(UIHelper.iconImage("fas-cube", 64, UIHelper.ACCENT));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 820);
        setMinimumSize(new Dimension(1024, 700));
        setLocationRelativeTo(null);
        UIHelper.registerMainWindow(this); // contém modais dentro da janela principal (mesmo ao arrastar)
        getContentPane().setBackground(UIHelper.BG_DARK);

        dashboardPanel  = new DashboardPanel(comercialService, financeService, approvalService, crmService, purchaseService, inventoryService);
        comercialPanel  = new ComercialPanel(comercialService, inventoryService, financeService, invoicePrintService, orderPrintService, guideRemittancePrintService, companyService, creditNoteService, debitNoteService, creditNotePrintService, debitNotePrintService, posService, promotionService, movimentosService);
        financeiroPanel = new FinanceiroPanel(financeService, comercialService);
        hrPanel         = new HRPanel(hrService, payslipPrintService);
        crmPanel        = new CRMPanel(crmService);
        clientesPanel   = new ClientesPanel(comercialApiClient);
        fiscalPanel     = new FiscalPanel(taxRateService, withholdingService, fiscalSummaryService, fiscalSalesExportService, payrollTaxService, ivaDeclarationPrintService, payrollFiscalMapPrintService);
        approvalsPanel  = new ApprovalsPanel(approvalService);
        posPanel        = new POSPanel(posService, comercialService, inventoryService, financeService, receiptPrintService, companyService, promotionService);
        stockPanel      = new StockPanel(inventoryService, comercialService, stockTransferService, stockTransferPrintService, inventoryReportPrintService, productCategoryService);
        comprasPanel    = new ComprasPanel(purchaseService, purchaseOrderService, reorderService, inventoryService, comercialService, financeService);
        configPanel     = new ConfigPanel(userService, auditLogService, backupService, databaseBackupService, documentConfigService);
        plataformaPanel = new PlataformaPanel(platformCompanyService, subscriptionService);

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
        contentPanel.add(plataformaPanel, "plataforma");

        setLayout(new BorderLayout());

        topBar = buildTopBar();
        add(topBar, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);

        if (superAdmin) {
            navigate("plataforma");
            topBar.setActive("Plataforma");
        } else {
            topBar.setActive("Painel Inicial");
        }
    }

    /** Called by the application bootstrap once the user is authenticated. */
    public void applyAuthenticatedUser(String displayName, String role) {
        sessionDisplayName = displayName;
        String activeRole = CurrentUserContext.getRole();
        dashboardPanel.updateWelcomeMessage(displayName, activeRole);
        if (sessionUserLabel != null) sessionUserLabel.setText(displayName);
        if (sessionRoleLabel != null) sessionRoleLabel.setText(UIHelper.humanRole(activeRole));
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
        bar.addItem(navIcon("fas-th-large"),            "Painel Inicial",     C_DASHBOARD,  () -> navigate("dashboard"));
        bar.addItem(navIcon("fas-cash-register"),       "POS — Caixa",        C_POS,        () -> navigate("pos"));
        bar.addItem(navIcon("fas-file-invoice"),        "Vendas & Faturação", C_COMERCIAL,  () -> navigate("comercial"));
        bar.addItem(navIcon("fas-shopping-cart"),       "Compras",            C_COMPRAS,    () -> navigate("compras"));
        bar.addItem(navIcon("fas-boxes"),               "Stock & Armazéns",   C_STOCK,      () -> navigate("stock"));
        bar.addItem(navIcon("fas-coins"),               "Tesouraria",         C_FINANCEIRO, () -> navigate("financeiro"));
        bar.addItem(navIcon("fas-users"),               "Recursos Humanos",   C_HR,         () -> navigate("hr"));
        bar.addItem(navIcon("fas-headset"),             "CRM & Assistência",  C_CRM,        () -> navigate("crm"));
        bar.addItem(navIcon("fas-address-book"),        "Clientes",           C_CLIENTES,   () -> navigate("clientes"));
        bar.addItem(navIcon("fas-percent"),             "Área Fiscal",        C_FISCAL,     () -> navigate("fiscal"));
        bar.addItem(navIcon("fas-check-double"),        "Aprovações",         C_APPROVALS,  () -> navigate("approvals"));
        bar.addItem(navIcon("fas-cog"),                 "Configurações",      C_CONFIG,     () -> navigate("config"));

        // Área direita: seletor de empresa + chip de utilizador.
        JComboBox<DesktopSession.CompanyAccess> companyCombo = buildCompanyCombo();
        UIHelper.styleComboBox(companyCombo);
        companyCombo.setToolTipText("Empresa ativa");
        companyCombo.setPreferredSize(new Dimension(210, 32));
        // Reaplicar o renderer DEPOIS de styleComboBox (senão mostraria CompanyAccess[...]).
        applyCompanyRenderer(companyCombo);
        bar.addTrailing(buildThemeToggle());
        bar.addTrailing(companyCombo);
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
        bar.addItem(navIcon("fas-server"), "Plataforma", C_CONFIG, () -> navigate("plataforma"));
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
        sessionUserLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        sessionUserLabel.setForeground(UIHelper.TEXT_LIGHT);
        sessionRoleLabel = new JLabel("—");
        sessionRoleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        sessionRoleLabel.setForeground(new Color(156, 163, 175));
        textStack.add(sessionUserLabel);
        textStack.add(sessionRoleLabel);
        chip.add(textStack, BorderLayout.CENTER);

        return chip;
    }

    private void navigate(String cardName) {
        cardLayout.show(contentPanel, cardName);
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
        if (sessionDisplayName != null) {
            dashboardPanel.updateWelcomeMessage(sessionDisplayName, activeRole);
        }
    }
}
