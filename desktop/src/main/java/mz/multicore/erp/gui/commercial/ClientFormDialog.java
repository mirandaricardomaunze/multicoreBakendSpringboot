package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;

import java.awt.*;
import java.math.BigDecimal;

/**
 * Diálogo modal para criação e edição de clientes comerciais.
 */
public final class ClientFormDialog {

    private ClientFormDialog() {
    }

    public static void show(Component parent, ClientDTO existing, ComercialApiClient client, Runnable onSuccess) {
        ClientFormFields fields = new ClientFormFields(existing);

        String title = existing == null ? "Novo Cliente" : "Editar Cliente — " + existing.name();
        ModernFormDialog dialog = new ModernFormDialog(
                UIHelper.mainWindow, title, null, "Dados de cadastro do cliente", fields.form());
        dialog.setOnSaveAsync(() -> {
            if (!fields.validateAll()) throw new IllegalArgumentException("Corrija os campos assinalados com formato inválido.");

            String name = fields.name();
            String taxId = fields.taxId();
            String email = fields.email();
            String address = fields.address();
            int terms = fields.paymentTermsDays();
            BigDecimal creditLimit = fields.creditLimit();

            return () -> {
                if (existing == null) {
                    client.createClient(name, taxId, email, address, terms, creditLimit);
                } else {
                    client.updateClient(existing.id(), name, taxId, email, address, terms, creditLimit);
                }
                return null;
            };
        });

        if (dialog.showDialog()) {
            ToastManager.success(parent, existing == null ? "Cliente criado." : "Cliente actualizado.");
            if (onSuccess != null) onSuccess.run();
        }
    }
}
