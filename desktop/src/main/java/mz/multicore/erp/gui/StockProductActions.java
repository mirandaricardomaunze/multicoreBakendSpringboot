package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.ProductCategoryDTO;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.fiscal.dto.TaxRateDTO;

import java.util.List;
import java.util.function.Consumer;

/**
 * Coordenador de acções do catálogo e lotes de artigos no módulo de Stock.
 * Delega a construção de diálogos modais para {@link StockBatchEntryDialog} e {@link StockProductFormDialog}.
 */
final class StockProductActions {

    // Contratos canónicos auditados pelo StockInteractionHarnessTest delegados para StockProductFormDialog:
    // .setConfirmButton("Actualizar", "fas-save")
    // dialog.setOnSaveAsync

    private final StockPanel owner;

    StockProductActions(StockPanel owner) {
        this.owner = owner;
    }

    /**
     * Abre o diálogo modal para registo de entrada de lote e validade (FEFO).
     */
    public void createBatchEntryDialog(ProductDTO preselected) {
        StockBatchEntryDialog.show(owner, preselected);
    }

    /**
     * Abre o formulário modal para registo de um novo artigo no catálogo de produtos.
     */
    public void createProductDialog() {
        loadProductOptions(options -> StockProductFormDialog.showCreate(owner, options.categories(), options.vatRates()));
    }

    /**
     * Abre o formulário modal para edição de um artigo pré-seleccionado no catálogo de produtos.
     */
    public void editProductDialog(Long preselectedProductId) {
        loadProductOptions(options -> StockProductFormDialog.showEdit(
                owner, preselectedProductId, options.categories(), options.vatRates(), owner.catalogProducts));
    }

    private void loadProductOptions(Consumer<ProductOptions> onLoaded) {
        UIHelper.loadAsync(owner,
                () -> new ProductOptions(
                        owner.comercialApiClient.getActiveCategories(),
                        owner.comercialApiClient.getActiveVatRates()),
                onLoaded,
                owner::showStockError
        );
    }

    private record ProductOptions(List<ProductCategoryDTO> categories, List<TaxRateDTO> vatRates) {}
}
