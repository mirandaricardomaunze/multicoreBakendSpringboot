package com.phcpro.gui;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.desktop.session.DesktopSession;
import com.phcpro.desktop.session.DesktopSessionStore;
import com.phcpro.desktop.client.ComercialApiClient;
import com.phcpro.gui.components.CollapsibleSidebar;
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

    private final CompanyService companyService;
    private final DesktopSessionStore desktopSessionStore;
    private CollapsibleSidebar sidebar;
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
            AppUserService userService,
            AuditLogService auditLogService,
            BackupService backupService,
            CompanyService companyService,
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
            PayrollTaxService payrollTaxService,
            IvaDeclarationPrintService ivaDeclarationPrintService
    ) {
        this.companyService = companyService;
        this.desktopSessionStore = desktopSessionStore;

        setTitle("MULTICORE — Gestão Profissional");
        setIconImage(UIHelper.iconImage("fas-cube", 64, UIHelper.ACCENT));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 820);
        setMinimumSize(new Dimension(1024, 700));
        setLocationRelativeTo(null);
        getContentPane().setBackground(UIHelper.BG_DARK);

        dashboardPanel  = new DashboardPanel(comercialService, financeService, approvalService, crmService, purchaseService, inventoryService);
        comercialPanel  = new ComercialPanel(comercialService, inventoryService, financeService, invoicePrintService, orderPrintService, companyService, creditNoteService, debitNoteService, creditNotePrintService, debitNotePrintService, posService);
        financeiroPanel = new FinanceiroPanel(financeService, comercialService);
        hrPanel         = new HRPanel(hrService, payslipPrintService);
        crmPanel        = new CRMPanel(crmService);
        clientesPanel   = new ClientesPanel(comercialApiClient);
        fiscalPanel     = new FiscalPanel(taxRateService, withholdingService, fiscalSummaryService, payrollTaxService, ivaDeclarationPrintService);
        approvalsPanel  = new ApprovalsPanel(approvalService);
        posPanel        = new POSPanel(posService, comercialService, inventoryService, financeService, receiptPrintService, companyService);
        stockPanel      = new StockPanel(inventoryService, comercialService, stockTransferService, stockTransferPrintService, inventoryReportPrintService);
        comprasPanel    = new ComprasPanel(purchaseService, inventoryService, comercialService, financeService);
        configPanel     = new ConfigPanel(userService, auditLogService, backupService);

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

        setLayout(new BorderLayout());

        sidebar = buildSidebar();
        add(sidebar, BorderLayout.WEST);
        add(contentPanel, BorderLayout.CENTER);

        sidebar.setActive("Painel Inicial");
    }

    /** Called by the application bootstrap once the user is authenticated. */
    public void applyAuthenticatedUser(String displayName, String role) {
        sessionDisplayName = displayName;
        String activeRole = CurrentUserContext.getRole();
        dashboardPanel.updateWelcomeMessage(displayName, activeRole);
        if (sessionUserLabel != null) sessionUserLabel.setText(displayName);
        if (sessionRoleLabel != null) sessionRoleLabel.setText(activeRole);
    }

    private javax.swing.Icon navIcon(String code) {
        return UIHelper.icon(code, 18, new Color(229, 231, 235));
    }

    private CollapsibleSidebar buildSidebar() {
        CollapsibleSidebar bar = new CollapsibleSidebar("MULTICORE", "ERP Profissional");

        bar.addExpandedOnly(buildContextSelector("Empresa Ativa", buildCompanyCombo()));
        bar.addExpandedOnly(buildSessionCard());

        bar.addSection("Operações");
        bar.addItem(navIcon("fas-th-large"),            "Painel Inicial",     C_DASHBOARD,  () -> navigate("dashboard"));
        bar.addItem(navIcon("fas-cash-register"),       "POS — Caixa",        C_POS,        () -> navigate("pos"));
        bar.addItem(navIcon("fas-file-invoice"),        "Vendas & Faturação", C_COMERCIAL,  () -> navigate("comercial"));
        bar.addItem(navIcon("fas-shopping-cart"),       "Compras",            C_COMPRAS,    () -> navigate("compras"));

        bar.addSection("Armazém");
        bar.addItem(navIcon("fas-boxes"),               "Stock & Armazéns",   C_STOCK,      () -> navigate("stock"));

        bar.addSection("Financeiro");
        bar.addItem(navIcon("fas-coins"),               "Tesouraria",         C_FINANCEIRO, () -> navigate("financeiro"));

        bar.addSection("Gestão");
        bar.addItem(navIcon("fas-users"),               "Recursos Humanos",   C_HR,         () -> navigate("hr"));
        bar.addItem(navIcon("fas-headset"),             "CRM & Assistência",  C_CRM,        () -> navigate("crm"));
        bar.addItem(navIcon("fas-address-book"),        "Clientes",           C_CLIENTES,   () -> navigate("clientes"));
        bar.addItem(navIcon("fas-percent"),             "Área Fiscal",        C_FISCAL,     () -> navigate("fiscal"));
        bar.addItem(navIcon("fas-check-double"),        "Aprovações",         C_APPROVALS,  () -> navigate("approvals"));

        bar.addSection("Sistema");
        bar.addItem(navIcon("fas-cog"),                 "Configurações",      C_CONFIG,     () -> navigate("config"));

        return bar;
    }

    private JPanel buildContextSelector(String title, JComboBox<?> combo) {
        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
        wrapper.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        wrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel label = new JLabel(title);
        label.setFont(new Font("Segoe UI", Font.BOLD, 10));
        label.setForeground(new Color(156, 163, 175));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(label);
        wrapper.add(Box.createRigidArea(new Dimension(0, 4)));

        UIHelper.styleComboBox(combo);
        combo.setMaximumSize(new Dimension(210, 32));
        combo.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrapper.add(combo);
        return wrapper;
    }

    private JComboBox<DesktopSession.CompanyAccess> buildCompanyCombo() {
        DesktopSession session = desktopSessionStore.requireSession();
        List<DesktopSession.CompanyAccess> companies = session.companies();
        JComboBox<DesktopSession.CompanyAccess> combo =
                new JComboBox<>(companies.toArray(new DesktopSession.CompanyAccess[0]));
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof DesktopSession.CompanyAccess company) {
                    setText(company.name() + "  [" + company.role() + "]");
                }
                return this;
            }
        });

        if (!companies.isEmpty()) {
            selectDesktopCompany(companies.get(0));
        }

        combo.addActionListener(e -> {
            DesktopSession.CompanyAccess selected = (DesktopSession.CompanyAccess) combo.getSelectedItem();
            if (selected != null) {
                selectDesktopCompany(selected);
                updateSessionRole();
                refreshActivePanel();
            }
        });
        return combo;
    }

    private void selectDesktopCompany(DesktopSession.CompanyAccess company) {
        DesktopSession session = desktopSessionStore.requireSession();
        session.selectCompany(company.id());
        CurrentUserContext.setCurrentUser(session.username(), company.role());
        CurrentUserContext.setCurrentCompanyId(company.id());
    }

    private JLabel sessionUserLabel;
    private JLabel sessionRoleLabel;

    private JPanel buildSessionCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("UTILIZADOR ATIVO");
        title.setFont(new Font("Segoe UI", Font.BOLD, 10));
        title.setForeground(new Color(156, 163, 175));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createRigidArea(new Dimension(0, 6)));

        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        JLabel avatar = new JLabel(UIHelper.icon("fas-user-circle", 26, UIHelper.ACCENT));
        row.add(avatar, BorderLayout.WEST);

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
        row.add(textStack, BorderLayout.CENTER);

        card.add(row);
        return card;
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
        }
    }

    private void updateSessionRole() {
        String activeRole = CurrentUserContext.getRole();
        if (sessionRoleLabel != null) {
            sessionRoleLabel.setText(activeRole);
        }
        if (sessionDisplayName != null) {
            dashboardPanel.updateWelcomeMessage(sessionDisplayName, activeRole);
        }
    }
}
