package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.QuickPeekPanel;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.purchases.dto.SupplierDTO;

import java.util.List;
import java.util.function.Consumer;

/**
 * Coordenador de apresentação, detalhes de Quick Peek e alternância de estado de fornecedores.
 */
public final class PurchaseSuppliersViewCoordinator {

    private PurchaseSuppliersViewCoordinator() {}

    public static void populatePeek(QuickPeekPanel peek, int modelRow, ComprasPanel owner,
                                    Consumer<SupplierDTO> onEdit) {
        if (owner.suppliersList == null || modelRow < 0 || modelRow >= owner.suppliersList.size()) return;
        SupplierDTO sup = owner.suppliersList.get(modelRow);
        boolean active = sup.active();
        peek.setHeaderIcon("fas-truck-loading", UIHelper.ACCENT_BLUE);
        peek.setTitle(sup.name() != null ? sup.name() : "");
        peek.setSubtitle("NUIT: " + (sup.taxId() != null ? sup.taxId() : "-"));
        peek.setStatus(active ? "ACTIVO" : "INACTIVO", active ? UIHelper.APPROVED_GREEN : UIHelper.REJECTED_RED);

        peek.setItems(List.of(
                new QuickPeekPanel.PeekItem("NUIT / NIF", sup.taxId() != null ? sup.taxId() : "-", false),
                new QuickPeekPanel.PeekItem("Pessoa de Contacto", sup.contactPerson() != null ? sup.contactPerson() : "-", false),
                new QuickPeekPanel.PeekItem("Telefone", sup.phone() != null ? sup.phone() : "-", false),
                new QuickPeekPanel.PeekItem("Correio Eletrónico", sup.email() != null ? sup.email() : "-", false),
                new QuickPeekPanel.PeekItem("Endereço", sup.address() != null ? sup.address() : "-", false)
        ));

        peek.getOpenFullButton().setText("Editar Fornecedor");
        peek.setOnOpenFullAction(ignored -> onEdit.accept(sup));
    }

    public static SupplierDTO selectedSupplier(ComprasPanel owner) {
        int row = TableFilter.selectedModelRow(owner.suppliersTable);
        if (row < 0 || owner.suppliersList == null || row >= owner.suppliersList.size()) {
            owner.showPurchaseNotice(FeedbackType.WARNING, "Seleccione um fornecedor",
                    "Escolha um fornecedor na tabela para continuar.");
            return null;
        }
        return owner.suppliersList.get(row);
    }

    public static void toggleSelectedSupplier(ComprasPanel owner) {
        SupplierDTO sel = selectedSupplier(owner);
        if (sel == null) return;
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.runWithProgress(owner, "A actualizar fornecedor…",
                () -> owner.purchaseApiClient.setSupplierActive(sel.id(), companyId, !sel.active()),
                ignored -> owner.loadSuppliers(), owner::showPurchaseError);
    }
}
