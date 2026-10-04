package mz.multicore.erp.gui;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.ToastManager;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.comercial.dto.QuotationDTO;
import mz.multicore.erp.modules.comercial.dto.QuotationLineDTO;
import mz.multicore.erp.modules.inventory.dto.WarehouseDTO;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;

/**
 * Gestor de acções de importação e conversão de cotações para o Ponto de Venda (POS).
 * Permite ao operador carregar uma cotação aberta no carrinho mantendo os preços e descontos acordados.
 */
public class PosQuotationActions {

    private final POSPanel owner;
    private final ComercialApiClient comercialApiClient;

    public PosQuotationActions(POSPanel owner, ComercialApiClient comercialApiClient) {
        this.owner = owner;
        this.comercialApiClient = comercialApiClient;
    }

    /**
     * Abre o diálogo modal para pesquisa e selecção de cotação aberta.
     */
    public void openImportDialog() {
        Window window = SwingUtilities.getWindowAncestor(owner);
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        new PosImportQuotationDialog(window, comercialApiClient, companyId, this::applyQuotation).setVisible(true);
    }

    /**
     * Aplica os dados da cotação no cabeçalho e carrinho do POS.
     */
    public void applyQuotation(QuotationDTO quotation) {
        if (quotation == null) return;

        if (quotation.lines() == null || quotation.lines().isEmpty()) {
            owner.showPosNotice(FeedbackType.WARNING, "Cotação vazia",
                    "A cotação " + quotation.quotationNumber() + " não tem artigos.");
            return;
        }

        // Se o carrinho já tem artigos, pede confirmação para substituir
        if (!owner.cartItems.isEmpty()) {
            int resp = JOptionPane.showConfirmDialog(owner,
                    "O carrinho actual contém " + owner.cartItems.size() + " artigo(s).\n"
                            + "Deseja substituir o conteúdo actual pelos artigos da cotação " + quotation.quotationNumber() + "?",
                    "Importar Cotação", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (resp != JOptionPane.YES_OPTION) return;
        }

        // 1. Cliente
        applyClient(quotation);

        // 2. Armazém
        applyWarehouse(quotation);

        // 3. Limpa e adiciona artigos com preço cotado
        owner.cartItems.clear();
        owner.cartModel.setRowCount(0);

        for (QuotationLineDTO qLine : quotation.lines()) {
            ProductDTO prod = findProduct(qLine);
            if (prod == null) {
                // Constrói ProductDTO de fallback para garantir faturabilidade do artigo cotado
                prod = createFallbackProduct(qLine);
            }
            owner.registerSellableProduct(prod);

            POSPanel.CartItem item = new POSPanel.CartItem(
                    prod,
                    qLine.quantity(),
                    qLine.discountPercentage() != null ? qLine.discountPercentage() : BigDecimal.ZERO,
                    null,
                    null,
                    qLine.unitPrice()
            );
            item.note = "Cotação " + quotation.quotationNumber();
            owner.cartItems.add(item);
        }

        owner.currentQuotationId = quotation.id();
        owner.currentQuotationNumber = quotation.quotationNumber();
        owner.updateCartTotal(-1);
        owner.updateQuotationBanner();

        ToastManager.success(owner, "Cotação " + quotation.quotationNumber() + " importada com sucesso ("
                + quotation.lines().size() + " artigos, " + quotation.totalAmount() + " MT).");
    }

    public void clearQuotationLink() {
        owner.currentQuotationId = null;
        owner.currentQuotationNumber = null;
        owner.updateQuotationBanner();
    }

    private void applyClient(QuotationDTO quotation) {
        if (quotation.clientId() != null && owner.filteredClients != null) {
            boolean found = false;
            for (int i = 0; i < owner.filteredClients.size(); i++) {
                ClientDTO c = owner.filteredClients.get(i);
                if (c.id().equals(quotation.clientId())) {
                    owner.clientCombo.setSelectedIndex(i);
                    found = true;
                    break;
                }
            }
            if (!found && quotation.clientName() != null && owner.clientSearchField != null) {
                owner.clientSearchField.setText(quotation.clientName());
            }
        } else if (quotation.walkInName() != null && owner.clientSearchField != null) {
            owner.clientSearchField.setText(quotation.walkInName());
        } else if (quotation.clientName() != null && owner.clientSearchField != null) {
            owner.clientSearchField.setText(quotation.clientName());
        }
    }

    private void applyWarehouse(QuotationDTO quotation) {
        if (quotation.warehouseId() != null && owner.warehousesList != null) {
            for (int i = 0; i < owner.warehousesList.size(); i++) {
                WarehouseDTO w = owner.warehousesList.get(i);
                if (w.id().equals(quotation.warehouseId())) {
                    owner.warehouseCombo.setSelectedIndex(i);
                    break;
                }
            }
        }
    }

    private ProductDTO findProduct(QuotationLineDTO qLine) {
        if (owner.productsList != null) {
            for (ProductDTO p : owner.productsList) {
                if (p.id().equals(qLine.productId())) {
                    return p;
                }
            }
        }
        return null;
    }

    private ProductDTO createFallbackProduct(QuotationLineDTO qLine) {
        return new ProductDTO(
                qLine.productId(),
                qLine.productSku(),
                qLine.productSku(),
                qLine.productSku(),
                qLine.productName(),
                qLine.unitPrice(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                null,
                qLine.unitsPerBox(),
                qLine.unitsPerBox(),
                1,
                "UNIT",
                true,
                null,
                null,
                null,
                qLine.taxRate(),
                "IVA Normal",
                null,
                null,
                null,
                null
        );
    }
}
