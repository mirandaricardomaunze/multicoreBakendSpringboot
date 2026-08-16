package com.phcpro.gui;

import com.phcpro.architecture.security.CurrentUserContext;
import com.phcpro.gui.components.UIHelper;
import com.phcpro.modules.comercial.dto.ProductDTO;

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

        UIHelper.loadAsync(owner, () -> owner.comercialApiClient.findPOSCatalogItemByBarcode(code), item -> {
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
    private void handleScaleScan(com.phcpro.modules.pos.scale.ScaleBarcode scale) {
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

    private void processScaleScan(com.phcpro.modules.pos.scale.ScaleBarcode scale, ProductDTO product) {
        if (product == null) {
            return;
        }
        if (!"WEIGHT".equalsIgnoreCase(product.saleType())) {
            JOptionPane.showMessageDialog(owner,
                    "O artigo '" + product.name() + "' não é vendido ao peso.\n"
                            + "Defina o Tipo de Venda = Peso no cadastro do produto.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            owner.barcodeField.setText("");
            owner.barcodeField.requestFocusInWindow();
            return;
        }

        BigDecimal qtyKg;
        if (owner.scaleBarcodeParser.embedsPrice()) {
            BigDecimal unit = product.unitPrice();
            if (unit == null || unit.signum() <= 0) {
                JOptionPane.showMessageDialog(owner,
                        "O artigo '" + product.name() + "' não tem preço/kg definido.",
                        "Aviso", JOptionPane.WARNING_MESSAGE);
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
            JOptionPane.showMessageDialog(owner,
                    "Peso inválido (zero) na etiqueta da balança.",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            owner.barcodeField.setText("");
            owner.barcodeField.requestFocusInWindow();
            return;
        }

        addWeighedProductToCart(product, qtyKg);
        owner.barcodeField.setText("");
        owner.barcodeField.requestFocusInWindow();
    }

    /** Resolve o artigo pesado pelo PLU: tenta o código tal-e-qual e depois sem zeros à esquerda. */
    private com.phcpro.modules.comercial.dto.POSCatalogItemDTO resolveWeighedProduct(String itemCode) {
        var product = owner.comercialApiClient.findPOSCatalogItemByBarcode(itemCode);
        if (product == null) {
            String stripped = itemCode.replaceFirst("^0+", "");
            if (!stripped.isEmpty() && !stripped.equals(itemCode)) {
                product = owner.comercialApiClient.findPOSCatalogItemByBarcode(stripped);
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
            JOptionPane.showMessageDialog(owner,
                    "O artigo '" + product.name() + "' está esgotado e não pode ser adicionado.",
                    "Sem Stock", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (owner.activeSession == null) {
            JOptionPane.showMessageDialog(owner,
                    "Não é possível adicionar artigos sem caixa aberta.\nClique em \"Abrir Caixa\" primeiro.",
                    "Caixa Fechada", JOptionPane.WARNING_MESSAGE);
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
                }, error -> JOptionPane.showMessageDialog(owner,
                        "Não foi possível consultar promoções: " + error.getMessage(),
                        "Erro de ligação", JOptionPane.ERROR_MESSAGE));
    }

    private void showProductNotFound(String code) {
        JOptionPane.showMessageDialog(owner, "Produto com código de barras '" + code + "' não encontrado.",
                "Aviso", JOptionPane.WARNING_MESSAGE);
        owner.barcodeField.selectAll();
        owner.barcodeField.requestFocusInWindow();
    }

    private void showWeighedProductNotFound(String code) {
        JOptionPane.showMessageDialog(owner,
                "Artigo pesado com código (PLU) '" + code + "' não encontrado.\n"
                        + "Registe o PLU da balança no campo \"Código de barras\" do produto.",
                "Aviso", JOptionPane.WARNING_MESSAGE);
        owner.barcodeField.selectAll();
        owner.barcodeField.requestFocusInWindow();
    }

    private void showOutOfStock(ProductDTO product) {
        JOptionPane.showMessageDialog(owner, "O artigo '" + product.name() + "' está esgotado.",
                "Sem Stock", JOptionPane.WARNING_MESSAGE);
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
