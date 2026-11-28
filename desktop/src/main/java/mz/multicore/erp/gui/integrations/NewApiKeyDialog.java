package mz.multicore.erp.gui.integrations;

import mz.multicore.erp.desktop.client.ApiKeyApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.publicapi.dto.CreateApiKeyRequest;
import mz.multicore.erp.modules.publicapi.dto.CreatedApiKeyDTO;
import mz.multicore.erp.modules.publicapi.model.ApiKeyPermission;

import javax.swing.*;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * "Nova chave" (docs/API_PUBLICA_WEBHOOKS_SPEC.md §4.4.1): name and one box per permission, checked locally with
 * the backend's messages; the backend decides and its refusal is shown in the dialog as it comes.
 */
final class NewApiKeyDialog {

    static final int MAX_NAME_LENGTH = 100;

    private final ApiKeyApiClient apiKeyApiClient;

    NewApiKeyDialog(ApiKeyApiClient apiKeyApiClient) {
        this.apiKeyApiClient = apiKeyApiClient;
    }

    /** Opens the dialog; the created key with its secret, or {@code null} when cancelled. */
    CreatedApiKeyDTO show() {
        JTextField name = new JTextField();
        UIHelper.styleTextField(name);
        Map<ApiKeyPermission, JCheckBox> boxes = new EnumMap<>(ApiKeyPermission.class);
        JPanel permissions = new JPanel();
        permissions.setOpaque(false);
        permissions.setLayout(new BoxLayout(permissions, BoxLayout.Y_AXIS));
        for (ApiKeyPermission permission : ApiKeyPermission.values()) {
            JCheckBox box = new JCheckBox(ApiKeyLabels.permission(permission));
            box.setOpaque(false);
            box.setForeground(UIHelper.TEXT_LIGHT);
            boxes.put(permission, box);
            permissions.add(box);
        }

        JPanel form = UIHelper.createDialogForm(
                "", new FormField("Nome", name, true, "Ex.: Loja online, Contabilista."),
                "Permissões:", permissions);
        ModernFormDialog dialog = new ModernFormDialog(UIHelper.mainWindow, "Nova chave de API", "fas-key",
                "Chave da empresa activa para um sistema externo", form);
        AtomicReference<CreatedApiKeyDTO> created = new AtomicReference<>();
        dialog.setOnSaveAsync(() -> {
            Set<ApiKeyPermission> chosen = EnumSet.noneOf(ApiKeyPermission.class);
            boxes.forEach((permission, box) -> {
                if (box.isSelected()) chosen.add(permission);
            });
            CreateApiKeyRequest request = validated(name.getText(), chosen);
            return () -> {
                created.set(apiKeyApiClient.create(request));
                return created.get();
            };
        });
        return dialog.showDialog() ? created.get() : null;
    }

    /** Same rules and messages as the backend (§3.1.1); throws when the request would be refused. */
    static CreateApiKeyRequest validated(String rawName, Set<ApiKeyPermission> permissions) {
        String name = rawName == null ? "" : rawName.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Indique o nome da chave de API (ex.: Loja online).");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException("O nome da chave de API tem no máximo " + MAX_NAME_LENGTH + " caracteres.");
        }
        if (permissions == null || permissions.isEmpty()) {
            throw new IllegalArgumentException("Escolha pelo menos uma permissão para a chave de API.");
        }
        return new CreateApiKeyRequest(name, EnumSet.copyOf(permissions));
    }
}
