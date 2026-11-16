package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.GlobalSearchDialog;
import mz.multicore.erp.gui.components.UIHelper;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Global search entries for the SELLER role, kept out of {@link MainFrame} so the frame only wires them. */
final class MainFrameSellerSearchIndex {

    private MainFrameSellerSearchIndex() {
    }

    /**
     * @param navigate     opens a card by name
     * @param sellerAction runs a seller dashboard action ("new-order", "new-quotation", ...)
     * @param owner        frame used to request logout
     */
    static List<GlobalSearchDialog.SearchItem> build(Consumer<String> navigate, Consumer<String> sellerAction, Component owner) {
        List<GlobalSearchDialog.SearchItem> items = new ArrayList<>();
        items.add(new GlobalSearchDialog.SearchItem("seller_dashboard", "Painel do Vendedor", "Vendas", null,
                UIHelper.icon("fas-th-large", 16, UIHelper.MODULE_DASHBOARD), UIHelper.MODULE_DASHBOARD,
                () -> navigate.accept("dashboard"), List.of("inicio", "resumo", "vendedor")));
        items.add(new GlobalSearchDialog.SearchItem("seller_pos", "Nova venda no POS", "Vendas", "F9",
                UIHelper.icon("fas-cash-register", 16, UIHelper.MODULE_POS), UIHelper.MODULE_POS,
                () -> navigate.accept("pos"), List.of("venda", "caixa", "pagamento")));
        items.add(new GlobalSearchDialog.SearchItem("seller_order", "Criar nova encomenda", "Vendas", null,
                UIHelper.icon("fas-clipboard-list", 16, UIHelper.ACCENT_BLUE), UIHelper.ACCENT_BLUE,
                () -> sellerAction.accept("new-order"), List.of("encomenda", "pedido")));
        items.add(new GlobalSearchDialog.SearchItem("seller_quotation", "Criar nova cotação", "Vendas", null,
                UIHelper.icon("fas-file-signature", 16, UIHelper.PENDING_YELLOW), UIHelper.PENDING_YELLOW,
                () -> sellerAction.accept("new-quotation"), List.of("cotacao", "proposta", "orcamento")));
        items.add(new GlobalSearchDialog.SearchItem("seller_clients", "Pesquisar clientes", "Vendas", null,
                UIHelper.icon("fas-address-book", 16, UIHelper.MODULE_CLIENTES), UIHelper.MODULE_CLIENTES,
                () -> navigate.accept("clientes"), List.of("cliente", "contacto", "nuit")));
        items.add(new GlobalSearchDialog.SearchItem("seller_products", "Consultar produtos", "Vendas", null,
                UIHelper.icon("fas-box-open", 16, UIHelper.ACCENT_CYAN), UIHelper.ACCENT_CYAN,
                () -> navigate.accept("pos"), List.of("produto", "artigo", "preco", "stock")));
        items.add(new GlobalSearchDialog.SearchItem("seller_crm", "CRM & Assistência", "Vendas", null,
                UIHelper.icon("fas-headset", 16, UIHelper.MODULE_CRM), UIHelper.MODULE_CRM,
                () -> navigate.accept("crm"), List.of("crm", "assistencia", "ticket")));
        items.add(new GlobalSearchDialog.SearchItem("seller_notifications", "Notificações", "Sistema", null,
                UIHelper.icon("fas-bell", 16, UIHelper.ACCENT_ORANGE), UIHelper.ACCENT_ORANGE,
                () -> navigate.accept("notifications"), List.of("alertas", "avisos")));
        items.add(new GlobalSearchDialog.SearchItem("seller_logout", "Terminar sessão", "Sistema", null,
                UIHelper.icon("fas-sign-out-alt", 16, UIHelper.REJECTED_RED), UIHelper.REJECTED_RED,
                () -> UIHelper.requestLogout(owner), List.of("logout", "sair")));
        return items;
    }
}
