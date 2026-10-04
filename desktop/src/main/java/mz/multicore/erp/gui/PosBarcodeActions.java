package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Leitura de códigos normais e etiquetas de balança. */
final class PosBarcodeActions {
    private final POSPanel owner;
    PosBarcodeActions(POSPanel owner) { this.owner = owner; }

    public void handleBarcodeScan() {
        String code = owner.barcodeField.getText() == null ? "" : owner.barcodeField.getText().trim();
        if (code.isEmpty()) return;

        // 1) Etiqueta de balança (código de barras de medida variável): resolve o artigo pelo PLU e
        //    adiciona ao carrinho já com o peso lido. Se não for etiqueta de balança, segue o caminho normal.
        var scale = owner.scaleBarcodeParser.parse(code);
        if (scale.isPresent()) {
            handleScaleScan(scale.get());
            return;
        }

        Long warehouseId = owner.getSelectedWarehouseId();
        UIHelper.loadAsync(owner, () -> owner.comercialApiClient.findPOSCatalogItemByBarcode(code, warehouseId), item -> {
            if (item == null) {
                showProductNotFound(code);
                return;
            }
            if (!item.sellable()) {
                showOutOfStock(item.product());
                return;
            }
            owner.registerSellableProduct(item.product());
            owner.addProductToCart(item.product());
            clearAndRefocus();
        }, error -> owner.showPosLoadError("produto pelo código de barras", error));
    }

    /**
     * Trata uma etiqueta de balança já interpretada: resolve o artigo pelo PLU (guardado no campo
     * "Código de barras" do produto pesado), calcula a quantidade em quilos — directamente do peso
     * embutido, ou derivada do preço total quando a balança embute o preço — e adiciona ao carrinho.
     */
    private void handleScaleScan(mz.multicore.erp.modules.pos.scale.ScaleBarcode scale) {
        UIHelper.loadAsync(owner, () -> resolveWeighedProduct(scale.itemCode()), item -> {
            if (item == null) {
                showWeighedProductNotFound(scale.itemCode());
                return;
            }
            if (!item.sellable()) {
                showOutOfStock(item.product());
                return;
            }
            owner.registerSellableProduct(item.product());
            processScaleScan(scale, item.product());
        }, error -> owner.showPosLoadError("artigo pesado", error));
    }

    private void processScaleScan(mz.multicore.erp.modules.pos.scale.ScaleBarcode scale, ProductDTO product) {
        if (product == null) {
            return;
        }
        if (!"WEIGHT".equalsIgnoreCase(product.saleType())) {
            owner.showPosNotice(FeedbackType.WARNING, "Artigo sem venda ao peso",
                    "Defina o Tipo de Venda = Peso no cadastro de '" + product.name() + "'.");
            owner.barcodeField.setText("");
            owner.barcodeField.requestFocusInWindow();
            return;
        }

        BigDecimal qtyKg;
        if (owner.scaleBarcodeParser.embedsPrice()) {
            BigDecimal unit = product.unitPrice();
            if (unit == null || unit.signum() <= 0) {
                owner.showPosNotice(FeedbackType.WARNING, "Preço por quilo em falta",
                        "O artigo '" + product.name() + "' não tem preço/kg definido.");
                owner.barcodeField.setText("");
                owner.barcodeField.requestFocusInWindow();
                return;
            }
            // Balança embute o preço já calculado → deriva o peso = preço ÷ preço/kg.
            qtyKg = owner.scaleBarcodeParser.priceMt(scale).divide(unit, 3, RoundingMode.HALF_UP);
        } else {
            qtyKg = owner.scaleBarcodeParser.weightKg(scale);
        }

        if (qtyKg.signum() <= 0) {
            owner.showPosNotice(FeedbackType.WARNING, "Peso inválido",
                    "A etiqueta da balança indica peso zero.");
            owner.barcodeField.setText("");
            owner.barcodeField.requestFocusInWindow();
            return;
        }

        addWeighedProductToCart(product, qtyKg);
        owner.barcodeField.setText("");
        owner.barcodeField.requestFocusInWindow();
    }

    /** Resolve o artigo pesado pelo PLU: tenta o código tal-e-qual e depois sem zeros à esquerda. */
    private mz.multicore.erp.modules.comercial.dto.POSCatalogItemDTO resolveWeighedProduct(String itemCode) {
        Long warehouseId = owner.getSelectedWarehouseId();
        var product = owner.comercialApiClient.findPOSCatalogItemByBarcode(itemCode, warehouseId);
        if (product == null) {
            String stripped = itemCode.replaceFirst("^0+", "");
            if (!stripped.isEmpty() && !stripped.equals(itemCode)) {
                product = owner.comercialApiClient.findPOSCatalogItemByBarcode(stripped, warehouseId);
            }
        }
        return product;
    }

    /**
     * Adiciona um artigo <b>vendido ao peso</b> com a quantidade (kg) lida da balança. Faz merge com
     * uma linha existente do mesmo artigo (soma o peso), aplica a melhor promoção para a quantidade e
     * deixa o cálculo de dinheiro à engine (preço/kg × kg, IVA por unidade), como qualquer outra linha.
     */
    private void addWeighedProductToCart(ProductDTO product, BigDecimal qtyKg) {
        if (!owner.isProductSellable(product)) {
            owner.showPosNotice(FeedbackType.WARNING, "Sem stock",
                    "O artigo '" + product.name() + "' está esgotado e não pode ser adicionado.");
            return;
        }
        if (owner.activeSession == null) {
            owner.showPosNotice(FeedbackType.WARNING, "Caixa fechado",
                    "Abra o caixa antes de adicionar artigos.");
            return;
        }
        for (POSPanel.CartItem it : owner.cartItems) {
            if (it.serial == null && it.product.id().equals(product.id())) {
                it.qty = it.qty.add(qtyKg);
                owner.updateCartTotal();
                return;
            }
        }
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        UIHelper.loadAsync(owner,
                () -> owner.promotionApiClient.bestPromotion(
                        companyId, product.id(), product.categoryId(), qtyKg),
                promo -> {
                    BigDecimal discount = promo.map(p -> p.discountPercent()).orElse(BigDecimal.ZERO);
                    POSPanel.CartItem item = new POSPanel.CartItem(product, qtyKg, discount, null, null);
                    item.note = promo.map(p -> "Promo: " + p.name()).orElse("-");
                    owner.cartItems.add(item);
                    owner.updateCartTotal();
                }, error -> owner.showPosNotice(FeedbackType.ERROR,
                        "Não foi possível consultar promoções", error.getMessage()));
    }

    private void showProductNotFound(String code) {
        mz.multicore.erp.gui.pos.audio.PosAudioFeedbackEngine.getInstance().playAsync(mz.multicore.erp.gui.pos.audio.PosAudioFeedbackEngine.SoundEvent.ERROR);
        owner.showPosNotice(FeedbackType.WARNING, "Produto não encontrado",
                "Não existe produto com o código de barras '" + code + "'.");
        owner.barcodeField.selectAll();
        owner.barcodeField.requestFocusInWindow();
    }

    private void showWeighedProductNotFound(String code) {
        mz.multicore.erp.gui.pos.audio.PosAudioFeedbackEngine.getInstance().playAsync(mz.multicore.erp.gui.pos.audio.PosAudioFeedbackEngine.SoundEvent.ERROR);
        owner.showPosNotice(FeedbackType.WARNING, "Artigo pesado não encontrado",
                "Registe o PLU '" + code + "' no campo Código de barras do produto.");
        owner.barcodeField.selectAll();
        owner.barcodeField.requestFocusInWindow();
    }

    private void showOutOfStock(ProductDTO product) {
        mz.multicore.erp.gui.pos.audio.PosAudioFeedbackEngine.getInstance().playAsync(mz.multicore.erp.gui.pos.audio.PosAudioFeedbackEngine.SoundEvent.ERROR);
        owner.showPosNotice(FeedbackType.WARNING, "Sem stock",
                "O artigo '" + product.name() + "' está esgotado.");
        clearAndRefocus();
    }

    private void clearAndRefocus() {
        owner.barcodeField.setText("");
        owner.barcodeField.requestFocusInWindow();
    }

    // ─── Form-layout helpers ────────────────────────────────────────────────────

    /**
     * Painel para colocar dentro de um {@link JScrollPane} vertical: acompanha a largura do viewport
     * (sem scroll horizontal) e só permite scroll vertical quando o conteúdo é mais alto que o viewport
     * (caso contrário, estica para preencher a altura — a tabela usa o espaço disponível).
     */
    private static final class VScrollPanel extends JPanel implements Scrollable {
        VScrollPanel(LayoutManager lm) { super(lm); setOpaque(false); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 100; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport vp && vp.getHeight() >= getPreferredSize().height;
        }
    }

}
