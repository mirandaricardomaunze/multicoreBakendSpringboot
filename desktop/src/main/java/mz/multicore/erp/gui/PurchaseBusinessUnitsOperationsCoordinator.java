package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.QuickPeekPanel;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.purchases.dto.BusinessUnitDTO;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Coordenador de operações visuais e transições de estado para linhas de negócio. */
final class PurchaseBusinessUnitsOperationsCoordinator {

    private PurchaseBusinessUnitsOperationsCoordinator() {}

    static void populateQuickPeek(QuickPeekPanel peek, List<BusinessUnitDTO> businessUnitsList,
                                  int modelRow, Consumer<BusinessUnitDTO> onEdit) {
        if (businessUnitsList == null || modelRow < 0 || modelRow >= businessUnitsList.size()) return;
        BusinessUnitDTO bu = businessUnitsList.get(modelRow);
        boolean active = bu.active();

        peek.setHeaderIcon("fas-briefcase", UIHelper.ACCENT);
        peek.setTitle(bu.name() != null ? bu.name() : "");
        peek.setSubtitle("Código: " + bu.code() + " • " + (bu.supplierName() != null ? bu.supplierName() : "Fornecedor livre"));
        peek.setStatus(active ? "ACTIVO" : "INACTIVO", active ? UIHelper.APPROVED_GREEN : UIHelper.REJECTED_RED);

        List<QuickPeekPanel.PeekItem> items = new ArrayList<>();
        items.add(new QuickPeekPanel.PeekItem("Código Comercial", bu.code(), true));
        items.add(new QuickPeekPanel.PeekItem("Nome da Linha", bu.name(), false));
        items.add(new QuickPeekPanel.PeekItem("Fornecedor Principal", bu.supplierName() != null ? bu.supplierName() : "—", false));
        items.add(new QuickPeekPanel.PeekItem("Artigos Vinculados", String.valueOf(bu.productCount()), true));
        items.add(new QuickPeekPanel.PeekItem("Descrição", bu.description() != null && !bu.description().isBlank() ? bu.description() : "—", false));
        peek.setItems(items);

        peek.getOpenFullButton().setText("Editar Linha de Negócio");
        peek.setOnOpenFullAction(ignored -> onEdit.accept(bu));
    }

    static void toggleActive(JComponent parent, ComprasPanel owner, BusinessUnitDTO sel, Runnable onReload) {
        if (sel == null) return;
        UIHelper.runWithProgress(parent, "A alterar estado…", () -> {
            owner.purchaseApiClient.toggleActiveBusinessUnit(sel.id());
            return null;
        }, ignored -> {
            ToastManager.success(parent, "Estado da linha de negócio " + sel.code() + " actualizado.");
            onReload.run();
        }, error -> owner.showPurchaseNotice(FeedbackType.ERROR, "Erro ao alterar estado", error.getMessage()));
    }
}
