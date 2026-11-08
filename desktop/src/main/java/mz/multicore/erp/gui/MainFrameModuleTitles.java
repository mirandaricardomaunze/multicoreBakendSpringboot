package mz.multicore.erp.gui;

/** Título mostrado na barra de topo e nos recentes para cada ecrã do {@link MainFrame}. */
final class MainFrameModuleTitles {

    private MainFrameModuleTitles() {}

    static String titleFor(String card) {
        return switch (card) {
            case "dashboard"  -> "Painel Inicial";
            case "pos"        -> "POS — Caixa";
            case "comercial"  -> "Vendas & Faturação";
            case "compras"    -> "Compras";
            case "stock"      -> "Stock";
            case "armazens"   -> "Armazéns";
            case "categorias" -> "Categorias";
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
            default           -> SectorModules.title(card);
        };
    }
}
