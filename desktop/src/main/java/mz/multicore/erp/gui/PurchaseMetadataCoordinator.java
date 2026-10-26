package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.financeira.dto.TreasuryAccountDTO;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;
import mz.multicore.erp.modules.purchases.dto.SupplierDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Coordenador assíncrono para carregamento e sincronização de metadados
 * do domínio de compras: fornecedores, armazéns, contas e catálogo de produtos.
 */
final class PurchaseMetadataCoordinator {

    private PurchaseMetadataCoordinator() {}

    static void loadSuppliers(ComprasPanel owner) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(owner, () -> owner.purchaseApiClient.getSuppliersByCompany(companyId),
                all -> applySuppliers(owner, all),
                error -> owner.showPurchaseLoadError("fornecedores", error));
    }

    static void applySuppliers(ComprasPanel owner, List<SupplierDTO> all) {
        owner.supplierComboList = all.stream().filter(SupplierDTO::active).toList();
        if (owner.supplierCombo != null) {
            owner.supplierCombo.removeAllItems();
            for (SupplierDTO s : owner.supplierComboList) owner.supplierCombo.addItem(s.name() + " (" + s.taxId() + ")");
        }
        if (owner.poSupplierCombo != null) {
            owner.poSupplierCombo.removeAllItems();
            for (SupplierDTO s : owner.supplierComboList) owner.poSupplierCombo.addItem(s.name() + " (" + s.taxId() + ")");
        }

        owner.suppliersList = all;
        if (owner.suppliersModel != null) {
            owner.suppliersModel.setRowCount(0);
            for (SupplierDTO s : owner.suppliersList) {
                owner.suppliersModel.addRow(new Object[]{
                        s.name(), s.taxId(), dash(s.phone()), dash(s.contactPerson()),
                        dash(s.email()), dash(s.address()), s.active() ? "Activo" : "Inactivo"
                });
            }
        }
    }

    static void loadWarehouses(ComprasPanel owner) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(owner, () -> owner.inventoryApiClient.getWarehousesByCompany(companyId),
                loaded -> applyWarehouses(owner, loaded),
                error -> owner.showPurchaseLoadError("armazéns", error));
    }

    static void applyWarehouses(ComprasPanel owner, List<WarehouseDTO> loaded) {
        owner.warehousesList = loaded;
        if (owner.warehouseCombo != null) {
            owner.warehouseCombo.removeAllItems();
            for (WarehouseDTO w : owner.warehousesList) owner.warehouseCombo.addItem(w.name());
        }
        owner.refreshPoCombos();
    }

    static void loadAccounts(ComprasPanel owner) {
        UIHelper.loadAsync(owner, owner.financeApiClient::getAllAccounts,
                loaded -> applyAccounts(owner, loaded),
                error -> owner.showPurchaseLoadError("contas de tesouraria", error));
    }

    static void applyAccounts(ComprasPanel owner, List<TreasuryAccountDTO> loaded) {
        owner.accountsList = loaded;
        if (owner.accountCombo != null) {
            owner.accountCombo.removeAllItems();
            owner.accountCombo.addItem("A crédito (pagar depois)");
            for (TreasuryAccountDTO acc : owner.accountsList) {
                owner.accountCombo.addItem(acc.name() + " (" + String.format("%.2f", acc.balance()) + " MT)");
            }
        }
    }

    static void loadProducts(ComprasPanel owner) {
        UIHelper.loadAsync(owner, owner.comercialApiClient::getAllProducts,
                loaded -> applyProducts(owner, loaded),
                error -> owner.showPurchaseLoadError("produtos", error));
    }

    static void applyProducts(ComprasPanel owner, List<ProductDTO> loaded) {
        owner.productsList = loaded;
        if (owner.productCombo != null) owner.productCombo.setProducts(owner.productsList);
        if (owner.poProductCombo != null) owner.poProductCombo.setProducts(owner.productsList);
        updateDefaultPrice(owner);
        refreshPurchasePackaging(owner);
    }

    static void updateDefaultPrice(ComprasPanel owner) {
        if (owner.productCombo == null || owner.priceField == null) return;
        ProductDTO product = owner.productCombo.selectedProduct();
        if (product != null) {
            BigDecimal sellPrice = product.unitPrice();
            BigDecimal costPrice = sellPrice.multiply(new BigDecimal("0.60")).setScale(2, RoundingMode.HALF_UP);
            owner.priceField.setText(costPrice.toString());
        }
    }

    static void refreshPurchasePackaging(ComprasPanel owner) {
        if (owner.purchasePackageEditor == null || owner.productCombo == null) return;
        ProductDTO product = owner.productCombo.selectedProduct();
        if (product != null) {
            owner.purchasePackageEditor.setPackaging(product.packagesPerBox(), product.unitsPerPackage());
        }
    }

    static String warehouseName(ComprasPanel owner, Long warehouseId) {
        if (warehouseId == null) return "—";
        return owner.warehousesList.stream()
                .filter(w -> warehouseId.equals(w.id()))
                .map(WarehouseDTO::name)
                .findFirst()
                .orElse("#" + warehouseId);
    }

    private static String dash(String v) {
        return v != null && !v.isBlank() ? v : "-";
    }
}
