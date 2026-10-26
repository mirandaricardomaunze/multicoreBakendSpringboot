package mz.multicore.erp.gui.inventory;

import mz.multicore.erp.desktop.client.InventoryPhysicalCountingApiClient;
import mz.multicore.erp.gui.components.*;
import mz.multicore.erp.modules.inventory.dto.CreateInventorySessionRequest;
import mz.multicore.erp.modules.inventory.dto.InventorySessionDTO;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.function.Consumer;

/**
 * Coordenador das operações de ciclo de vida e relatórios de sessões de inventário físico.
 */
public final class PhysicalInventoryOperationsCoordinator {

    private final InventoryPhysicalCountingApiClient apiClient;

    public PhysicalInventoryOperationsCoordinator(InventoryPhysicalCountingApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void openNewSessionDialog(Component parent, ModernButton triggerBtn, Runnable onCreated) {
        JTextField descField = new JTextField("Inventário Físico Armazém " + LocalDate.now());
        UIHelper.styleTextField(descField);
        JCheckBox blindCheckBox = new JCheckBox("Activar Contagem Cega (Oculta stock esperado até o fecho)");
        blindCheckBox.setForeground(UIHelper.TEXT_LIGHT);
        blindCheckBox.setOpaque(false);

        JPanel panel = new JPanel(new GridLayout(2, 1, 0, 8));
        panel.add(UIHelper.createFilterGroup("Descrição:", descField));
        panel.add(blindCheckBox);

        ModernFormDialog dialog = new ModernFormDialog(SwingUtilities.getWindowAncestor(parent),
                "Abertura de Nova Sessão de Inventário", "fas-clipboard-list",
                "Defina o âmbito da contagem física", panel)
                .setConfirmButton("Abrir Sessão", "fas-play");
        if (dialog.showDialog()) {
            String desc = descField.getText().trim();
            CreateInventorySessionRequest req = new CreateInventorySessionRequest(desc, blindCheckBox.isSelected(), null);
            UIHelper.submitAsync(triggerBtn, () -> apiClient.createSession(req), created -> {
                if (onCreated != null) onCreated.run();
            }, null);
        }
    }

    public void handleStartCounting(Component parent, InventorySessionDTO currentSession, ActionMenuButton menu, Consumer<InventorySessionDTO> onSuccess) {
        if (currentSession == null) return;
        UIHelper.submitAsync(menu, () -> apiClient.startCounting(currentSession.id()), updated -> {
            ToastManager.success(parent, "Contagem de inventário iniciada.");
            if (onSuccess != null) onSuccess.accept(updated);
        }, null);
    }

    public void handleCloseSession(Component parent, InventorySessionDTO currentSession, ActionMenuButton menu, Consumer<InventorySessionDTO> onSuccess) {
        if (currentSession == null) return;
        boolean confirmed = ModernMessageDialog.confirm(SwingUtilities.getWindowAncestor(parent),
                FeedbackType.WARNING,
                "Fecho e Acerto de Stock",
                "Tem certeza que deseja encerrar o inventário " + currentSession.inventoryNumber() + "?\nO stock lógico dos produtos será ajustado automaticamente para igualar a contagem física.",
                "Encerrar Inventário");
        if (confirmed) {
            UIHelper.submitAsync(menu, () -> apiClient.closeAndAdjustStock(currentSession.id()), updated -> {
                ToastManager.success(parent, "Inventário concluído e stock ajustado com sucesso.");
                if (onSuccess != null) onSuccess.accept(updated);
            }, null);
        }
    }

    public void handleCancelSession(Component parent, InventorySessionDTO currentSession, ActionMenuButton menu, Consumer<InventorySessionDTO> onSuccess) {
        if (currentSession == null) return;
        boolean confirmed = ModernMessageDialog.confirm(SwingUtilities.getWindowAncestor(parent),
                FeedbackType.WARNING,
                "Cancelar Inventário",
                "Deseja cancelar a sessão " + currentSession.inventoryNumber() + "?",
                "Cancelar Sessão");
        if (confirmed) {
            UIHelper.submitAsync(menu, () -> apiClient.cancelSession(currentSession.id()), updated -> {
                if (onSuccess != null) onSuccess.accept(updated);
            }, null);
        }
    }

    public void handlePrintPdf(Component parent, InventorySessionDTO currentSession, ModernButton btn) {
        if (currentSession == null) return;
        UIHelper.submitAsync(btn, () -> apiClient.renderPdf(currentSession.id()), pdfBytes -> {
            UIHelper.previewOrPrintPdf(parent, pdfBytes, "Dossiê de Inventário Físico - " + currentSession.inventoryNumber());
        }, null);
    }
}
