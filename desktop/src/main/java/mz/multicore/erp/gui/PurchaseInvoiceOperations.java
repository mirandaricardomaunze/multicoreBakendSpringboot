package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ModernFormDialog;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;
import mz.multicore.erp.modules.purchases.dto.CreatePurchaseRequest;
import mz.multicore.erp.modules.purchases.dto.PurchaseDTO;
import mz.multicore.erp.modules.purchases.dto.SupplierDTO;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Operações do ciclo de vida das faturas de compra:
 * abertura do formulário de entrada de stock, montagem de requisição de compra,
 * histórico consolidado e mapeamento de linhas de negócio.
 */
final class PurchaseInvoiceOperations {

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private PurchaseInvoiceOperations() {}

    static void openPurchaseFormDialog(ComprasPanel owner) {
        if (owner.supplierComboList.isEmpty()) {
            owner.showPurchaseNotice(FeedbackType.WARNING, "Fornecedor necessário",
                    "Registe um fornecedor activo antes de lançar a compra.");
            return;
        }

        clearPurchaseDraft(owner);
        Window parent = SwingUtilities.getWindowAncestor(owner);
        ModernFormDialog dlg = new ModernFormDialog(parent, "Registar Compra (Entrada de Stock)", owner.purchaseFormContent);
        dlg.setSize(900, 680);
        PurchaseDTO[] created = new PurchaseDTO[1];
        boolean[] onCredit = new boolean[1];

        dlg.setOnSaveAsync(() -> {
            CreatePurchaseRequest request = buildPurchaseRequest(owner, onCredit);
            return () -> created[0] = owner.purchaseApiClient.createPurchase(request);
        });

        if (dlg.showDialog()) {
            PurchaseDTO purchase = created[0];
            ToastManager.success(owner, "Compra " + purchase.purchaseNumber() + " registada · "
                    + (onCredit[0] ? "a crédito" : purchase.totalAmount() + " MT") + ".");
            clearPurchaseDraft(owner);
            owner.loadPurchasesHistory();
            owner.loadAccounts();
            owner.loadPayables();
        }
    }

    static CreatePurchaseRequest buildPurchaseRequest(ComprasPanel owner, boolean[] onCreditResult) {
        if (owner.warehousesList.isEmpty()) {
            throw new RuntimeException("Falta registar armazéns.");
        }
        if (owner.draftLines.isEmpty()) {
            throw new RuntimeException("Nenhum produto adicionado à compra.");
        }
        int supIdx = owner.supplierCombo.getSelectedIndex();
        int whIdx = owner.warehouseCombo.getSelectedIndex();
        int accIdx = owner.accountCombo.getSelectedIndex();
        if (supIdx < 0 || supIdx >= owner.supplierComboList.size() || whIdx < 0) {
            throw new RuntimeException("Selecione fornecedor e armazém.");
        }

        SupplierDTO supplier = owner.supplierComboList.get(supIdx);
        WarehouseDTO warehouse = owner.warehousesList.get(whIdx);
        boolean onCredit = accIdx <= 0;
        onCreditResult[0] = onCredit;
        Long financeAccountId = onCredit ? null : owner.accountsList.get(accIdx - 1).id();
        Long companyId = CurrentUserContext.getCurrentCompanyId();

        return new CreatePurchaseRequest(supplier.id(), warehouse.id(), companyId, financeAccountId,
                new ArrayList<>(owner.draftLines));
    }

    static void clearPurchaseDraft(ComprasPanel owner) {
        owner.draftLines.clear();
        if (owner.draftLinesModel != null) owner.draftLinesModel.setRowCount(0);
        owner.draftTotal = BigDecimal.ZERO;
        if (owner.totalLabel != null) owner.totalLabel.setText("Total Compra: 0,00 MT (excl. IVA)");
    }

    static void loadPurchasesHistory(ComprasPanel owner) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(owner, () -> owner.purchaseApiClient.getPurchasesByCompany(companyId),
                purchases -> applyPurchasesHistory(owner, purchases),
                error -> owner.showPurchaseLoadError("histórico de compras", error));
    }

    static void applyPurchasesHistory(ComprasPanel owner, List<PurchaseDTO> purchases) {
        if (owner.purchasesModel == null) return;
        owner.purchasesModel.setRowCount(0);
        Set<String> bus = new TreeSet<>();

        for (PurchaseDTO p : purchases) {
            String buName = p.businessUnitName() != null && !p.businessUnitName().isBlank() ? p.businessUnitName() : "—";
            if (!"—".equals(buName)) bus.add(buName);
            owner.purchasesModel.addRow(new Object[]{
                    p.purchaseNumber(),
                    p.supplierName(),
                    owner.warehouseName(p.warehouseId()),
                    p.totalAmount(),
                    p.taxAmount(),
                    p.purchaseDate().format(DATE_TIME_FMT),
                    buName
            });
        }

        if (owner.histBusinessUnitCombo != null) {
            Object prev = owner.histBusinessUnitCombo.getSelectedItem();
            owner.histBusinessUnitCombo.removeAllItems();
            owner.histBusinessUnitCombo.addItem("Todos os negócios");
            owner.histBusinessUnitCombo.addItem("—");
            for (String b : bus) owner.histBusinessUnitCombo.addItem(b);
            if (prev != null) owner.histBusinessUnitCombo.setSelectedItem(prev);
        }
    }
}
