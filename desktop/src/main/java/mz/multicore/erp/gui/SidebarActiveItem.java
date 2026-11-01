package mz.multicore.erp.gui;

/**
 * Qual o item da sidebar que representa o ecrã aberto. O título do ecrã nem sempre é o nome do item:
 * Vendas abre-se por Pedidos e Movimentos, e o menu do vendedor tem nomes próprios.
 */
final class SidebarActiveItem {

    private SidebarActiveItem() {}

    static String labelFor(String card, String moduleTitle, String role, ComercialPanel commercial) {
        return labelFor(card, moduleTitle, role, commercial == null ? "" : commercial.activeTabTitle());
    }

    static String labelFor(String card, String moduleTitle, String role, String commercialTab) {
        boolean seller = "SELLER".equalsIgnoreCase(role);
        return switch (card) {
            case "comercial" -> {
                if (seller) yield "Pedidos e Cotações";
                yield commercialTab != null && commercialTab.startsWith("Movimentos") ? "Movimentos" : "Pedidos";
            }
            case "dashboard" -> seller ? "Painel do Vendedor" : moduleTitle;
            case "crm" -> seller ? "CRM e Assistência" : moduleTitle;
            case "desempenho" -> "Desempenho";
            default -> moduleTitle;
        };
    }
}
