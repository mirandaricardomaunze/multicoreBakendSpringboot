package mz.multicore.erp.gui.sector;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.PricingApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;
import mz.multicore.erp.modules.pricing.dto.PriceListDTO;

import javax.swing.*;
import java.util.List;

/**
 * Assigns the list to a client. A client holds at most one list per company, so choosing another replaces
 * the previous one. The walk-in client is hidden because a list would change every counter sale.
 */
final class ClientPriceListAssignDialog {

    /** Same technical NUIT as the server ({@code WalkInClientProvider.WALK_IN_TAX_ID}). */
    private static final String WALK_IN_TAX_ID = "000000000";

    private ClientPriceListAssignDialog() {
    }

    static void show(JComponent parent, PricingApiClient pricingApi, ComercialApiClient comercialApi, PriceListDTO list,
                     Runnable onSaved, InlineFeedbackPanel feedback) {
        UIHelper.loadAsync(parent, comercialApi::getAllClients,
                clients -> open(pricingApi, list, clients, onSaved),
                error -> feedback.show(FeedbackType.ERROR, "Não foi possível carregar os clientes", error.getMessage(),
                        "Tentar novamente", () -> show(parent, pricingApi, comercialApi, list, onSaved, feedback)));
    }

    private static void open(PricingApiClient api, PriceListDTO list, List<ClientDTO> clients, Runnable onSaved) {
        Long companyId = CurrentUserContext.findCurrentCompanyId();
        if (companyId == null) return;
        List<ClientDTO> eligible = clients.stream().filter(c -> !WALK_IN_TAX_ID.equals(c.taxId())).toList();

        SearchableComboBox<ClientDTO> combo = new SearchableComboBox<>("Pesquisar cliente por nome ou NUIT",
                c -> c.name() + (c.taxId() == null ? "" : "  ·  NUIT " + c.taxId()),
                c -> c.name() + " " + (c.taxId() == null ? "" : c.taxId()) + " " + (c.code() == null ? "" : c.code()));
        combo.setItems(eligible);
        JLabel hint = new JLabel("<html>Se o cliente já tiver outra tabela, passa a usar esta. "
                + "As faturas já emitidas não mudam.</html>");
        hint.setForeground(UIHelper.TEXT_MUTED);
        FormField clientForm = new FormField("Cliente", combo, true, null);
        JPanel form = UIHelper.createDialogForm(1, new Object[]{"", clientForm, "", hint});

        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Atribuir Cliente", "fas-user-tag",
                "Tabela " + list.name(), form);
        boolean[] saved = {false};
        dialog.setOnSaveAsync(() -> {
            ClientDTO client = combo.selectedValue();
            if (client == null) {
                clientForm.setError("Escolha o cliente.");
                throw new IllegalArgumentException("Escolha o cliente.");
            }
            return () -> { api.assign(companyId, client.id(), list.id()); saved[0] = true; return null; };
        });
        if (dialog.showDialog() && saved[0]) onSaved.run();
    }
}
