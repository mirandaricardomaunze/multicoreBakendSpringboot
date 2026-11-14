package mz.multicore.erp.gui;

import mz.multicore.erp.desktop.session.DesktopSession;
import mz.multicore.erp.gui.components.CollapsibleSidebar;
import mz.multicore.erp.gui.components.SidebarCollapsePreference;
import mz.multicore.erp.gui.components.UIHelper;

import java.util.function.Consumer;

/**
 * Construtor modular da barra lateral de navegação do {@link MainFrame}.
 * Isola a hierarquia de módulos por perfil e mantém o MainFrame focado no ciclo de vida da janela.
 */
final class MainFrameSidebarBuilder {

    private MainFrameSidebarBuilder() {
    }

    static String companyLabel(DesktopSession session) {
        return session.activeCompany() == null ? "ERP Profissional" : session.activeCompany().name();
    }

    static CollapsibleSidebar build(
            DesktopSession initialSession,
            boolean superAdmin,
            Consumer<String> navigate,
            ComercialPanel comercialPanel,
            SectorModules sectors
    ) {
        CollapsibleSidebar sb = buildItems(initialSession, superAdmin, navigate, comercialPanel, sectors);
        if (SidebarCollapsePreference.startCollapsed()) sb.setCollapsedState(true);
        return sb;
    }

    static CollapsibleSidebar build(
            DesktopSession initialSession,
            boolean superAdmin,
            Consumer<String> navigate,
            ComercialPanel comercialPanel
    ) {
        return build(initialSession, superAdmin, navigate, comercialPanel, null);
    }

    private static CollapsibleSidebar buildItems(
            DesktopSession initialSession,
            boolean superAdmin,
            Consumer<String> navigate,
            ComercialPanel comercialPanel,
            SectorModules sectors
    ) {
        CollapsibleSidebar sb = new CollapsibleSidebar("MULTICORE", companyLabel(initialSession));

        if (superAdmin) {
            sb.addSection("Plataforma");
            sb.addItem(UIHelper.icon("fas-server", 16, UIHelper.ACCENT_BLUE), "Plataforma", UIHelper.ACCENT_BLUE, () -> navigate.accept("plataforma"));
            return sb;
        }

        String activeRole = initialSession.activeRole();
        if ("SELLER".equalsIgnoreCase(activeRole)) {
            sb.addSection("Vendas");
            sb.addItem(UIHelper.icon("fas-th-large", 16, UIHelper.MODULE_DASHBOARD), "Painel do Vendedor", UIHelper.MODULE_DASHBOARD, () -> navigate.accept("dashboard"));
            sb.addItem(UIHelper.icon("fas-cash-register", 16, UIHelper.MODULE_POS), "POS — Caixa", UIHelper.MODULE_POS, () -> navigate.accept("pos"));
            sb.addItem(UIHelper.icon("fas-clipboard-list", 16, UIHelper.MODULE_COMERCIAL), "Pedidos e Cotações", UIHelper.MODULE_COMERCIAL, () -> navigate.accept("comercial"));
            if (sectors != null) sectors.addSidebarItem(sb, SectorModules.PICKUPS, true, navigate);
            sb.addItem(UIHelper.icon("fas-address-book", 16, UIHelper.MODULE_CLIENTES), "Clientes", UIHelper.MODULE_CLIENTES, () -> navigate.accept("clientes"));
            if (sectors != null) sectors.addSidebarItem(sb, SectorModules.PROJECTS, true, navigate);
            if (sectors != null) sectors.addSidebarItem(sb, SectorModules.BUTCHERY, true, navigate);
            if (sectors != null) sectors.addSidebarItem(sb, SectorModules.EXPIRY, true, navigate);
            if (sectors != null) sectors.addSidebarItem(sb, SectorModules.PRODUCTION_PLAN, true, navigate);
            if (sectors != null) sectors.addSidebarItem(sb, SectorModules.CUSTOM_ORDERS, true, navigate);
            if (sectors != null) sectors.addSidebarItem(sb, SectorModules.CONSIGNMENT, true, navigate);
            if (sectors != null) sectors.addSidebarItem(sb, SectorModules.RETURNABLES, true, navigate);
            if (sectors != null) sectors.addSidebarItem(sb, SectorModules.WEIGHBRIDGE, true, navigate);
            sb.addItem(UIHelper.icon("fas-headset", 16, UIHelper.MODULE_CRM), "CRM e Assistência", UIHelper.MODULE_CRM, () -> navigate.accept("crm"));
            sb.addSection("Avisos");
            sb.addItem(UIHelper.icon("fas-bell", 16, UIHelper.ACCENT_ORANGE), "Notificações", UIHelper.ACCENT_ORANGE, () -> navigate.accept("notifications"));
            return sb;
        }
        if (!"MANAGER".equalsIgnoreCase(activeRole) && !"ADMIN".equalsIgnoreCase(activeRole)) {
            sb.addSection("Área pessoal");
            sb.addItem(UIHelper.icon("fas-th-large", 16, UIHelper.MODULE_DASHBOARD), "Painel Inicial", UIHelper.MODULE_DASHBOARD, () -> navigate.accept("dashboard"));
            sb.addItem(UIHelper.icon("fas-bell", 16, UIHelper.ACCENT_ORANGE), "Notificações", UIHelper.ACCENT_ORANGE, () -> navigate.accept("notifications"));
            return sb;
        }

        // 1. OPERAÇÕES
        sb.addSection("Operações");
        sb.addItem(UIHelper.icon("fas-th-large", 16, UIHelper.MODULE_DASHBOARD), "Painel Inicial", UIHelper.MODULE_DASHBOARD, () -> navigate.accept("dashboard"));
        sb.addItem(UIHelper.icon("fas-cash-register", 16, UIHelper.MODULE_POS), "POS — Caixa", UIHelper.MODULE_POS, () -> navigate.accept("pos"));
        sb.addItem(UIHelper.icon("fas-clipboard-list", 16, UIHelper.MODULE_COMERCIAL), "Pedidos", UIHelper.MODULE_COMERCIAL, () -> {
            navigate.accept("comercial");
            if (comercialPanel != null) comercialPanel.showCustomerOrders();
        });
        if (sectors != null) sectors.addSidebarItem(sb, SectorModules.PICKUPS, false, navigate);
        sb.addItem(UIHelper.icon("fas-shopping-cart", 16, UIHelper.MODULE_COMPRAS), "Compras", UIHelper.MODULE_COMPRAS, () -> navigate.accept("compras"));
        sb.addItem(UIHelper.icon("fas-boxes", 16, UIHelper.MODULE_STOCK), "Stock", UIHelper.MODULE_STOCK, () -> navigate.accept("stock"));
        sb.addItem(UIHelper.icon("fas-warehouse", 16, UIHelper.ACCENT_CYAN), "Armazéns", UIHelper.ACCENT_CYAN, () -> navigate.accept("armazens"));
        sb.addItem(UIHelper.icon("fas-tags", 16, UIHelper.ACCENT_PINK), "Categorias", UIHelper.ACCENT_PINK, () -> navigate.accept("categorias"));
        sb.addItem(UIHelper.icon("fas-exchange-alt", 16, UIHelper.ACCENT_BLUE), "Movimentos", UIHelper.ACCENT_BLUE, () -> {
            navigate.accept("comercial");
            if (comercialPanel != null) comercialPanel.showMovements();
        });
        sb.addItem(UIHelper.icon("fas-industry", 16, UIHelper.ACCENT_BLUE), "Produção & Fabrico", UIHelper.ACCENT_BLUE, () -> navigate.accept("manufacturing"));
        sb.addItem(UIHelper.icon("fas-car", 16, UIHelper.ACCENT_CYAN), "Gestão de Frotas", UIHelper.ACCENT_CYAN, () -> navigate.accept("fleet"));

        // 2. GESTÃO & CRM
        sb.addSection("Gestão & CRM");
        if ("MANAGER".equalsIgnoreCase(activeRole) || "ADMIN".equalsIgnoreCase(activeRole)) {
            sb.addItem(UIHelper.icon("fas-lightbulb", 16, UIHelper.ACCENT_ORANGE), "Assistente de Gestão", UIHelper.ACCENT_ORANGE, () -> navigate.accept("assistente_ia"));
        }
        sb.addItem(UIHelper.icon("fas-users", 16, UIHelper.MODULE_HR), "Recursos Humanos", UIHelper.MODULE_HR, () -> navigate.accept("hr"));
        sb.addItem(UIHelper.icon("fas-shield-alt", 16, UIHelper.ACCENT_CYAN), "Qualidade & HST", UIHelper.ACCENT_CYAN, () -> navigate.accept("quality"));
        sb.addItem(UIHelper.icon("fas-trophy", 16, UIHelper.MODULE_COMERCIAL), "Desempenho", UIHelper.MODULE_COMERCIAL, () -> navigate.accept("desempenho"));
        sb.addItem(UIHelper.icon("fas-headset", 16, UIHelper.MODULE_CRM), "CRM & Assistência", UIHelper.MODULE_CRM, () -> navigate.accept("crm"));
        sb.addItem(UIHelper.icon("fas-address-book", 16, UIHelper.MODULE_CLIENTES), "Clientes", UIHelper.MODULE_CLIENTES, () -> navigate.accept("clientes"));
        if (sectors != null) {
            sectors.addSidebarItem(sb, SectorModules.PROJECTS, false, navigate);
            sectors.addSidebarItem(sb, SectorModules.ADVANCES, false, navigate);
            sectors.addSidebarItem(sb, SectorModules.BUTCHERY, false, navigate);
            sectors.addSidebarItem(sb, SectorModules.EXPIRY, false, navigate);
            sectors.addSidebarItem(sb, SectorModules.PRODUCTION_PLAN, false, navigate);
            sectors.addSidebarItem(sb, SectorModules.CUSTOM_ORDERS, false, navigate);
            sectors.addSidebarItem(sb, SectorModules.CONSIGNMENT, false, navigate);
            sectors.addSidebarItem(sb, SectorModules.RETURNABLES, false, navigate);
            sectors.addSidebarItem(sb, SectorModules.VARIANTS, false, navigate);
            sectors.addSidebarItem(sb, SectorModules.WEIGHBRIDGE, false, navigate);
            sectors.addSidebarItem(sb, SectorModules.EXCEPTIONS, false, navigate);
            sectors.addSidebarItem(sb, SectorModules.PRICES, false, navigate);
        }
        sb.addItem(UIHelper.icon("fas-file-contract", 16, UIHelper.ACCENT_BLUE), "Avenças & Contratos", UIHelper.ACCENT_BLUE, () -> navigate.accept("recurring"));

        // 3. FINANÇAS & FISCAL
        sb.addSection("Finanças & Fiscal");
        sb.addItem(UIHelper.icon("fas-coins", 16, UIHelper.MODULE_FINANCEIRO), "Tesouraria", UIHelper.MODULE_FINANCEIRO, () -> navigate.accept("financeiro"));
        sb.addItem(UIHelper.icon("fas-percent", 16, UIHelper.MODULE_FISCAL), "Área Fiscal", UIHelper.MODULE_FISCAL, () -> navigate.accept("fiscal"));
        sb.addItem(UIHelper.icon("fas-calculator", 16, UIHelper.MODULE_ACCOUNTING), "Contabilidade", UIHelper.MODULE_ACCOUNTING, () -> navigate.accept("contabilidade"));
        sb.addItem(UIHelper.icon("fas-building", 16, UIHelper.ACCENT_BLUE), "Activos & Património", UIHelper.ACCENT_BLUE, () -> navigate.accept("assets"));
        sb.addItem(UIHelper.icon("fas-check-double", 16, UIHelper.MODULE_APPROVALS), "Aprovações", UIHelper.MODULE_APPROVALS, () -> navigate.accept("approvals"));
        sb.addItem(UIHelper.icon("fas-shield-alt", 16, UIHelper.REJECTED_RED), "Auditoria Forense", UIHelper.REJECTED_RED, () -> navigate.accept("auditoria_forense"));

        // 4. SISTEMA
        sb.addSection("Sistema");
        sb.addItem(UIHelper.icon("fas-bell", 16, UIHelper.ACCENT_CYAN), "Notificações", UIHelper.ACCENT_CYAN, () -> navigate.accept("notifications"));
        sb.addItem(UIHelper.icon("fas-cog", 16, UIHelper.ACCENT_BLUE), "Configurações", UIHelper.ACCENT_BLUE, () -> navigate.accept("config"));

        return sb;
    }
}
