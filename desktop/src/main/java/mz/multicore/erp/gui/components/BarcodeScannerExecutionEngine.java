package mz.multicore.erp.gui.components;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.desktop.client.ComercialApiClient;
import mz.multicore.erp.desktop.client.InventoryApiClient;
import mz.multicore.erp.desktop.client.ProductionApiClient;
import mz.multicore.erp.modules.comercial.dto.ProductDTO;
import mz.multicore.erp.modules.manufacturing.dto.BillOfMaterialsDTO;
import mz.multicore.erp.modules.manufacturing.dto.CreateProductionOrderRequest;

import javax.swing.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

/**
 * Motor de execução de leitura por código de barras, busca de artigos,
 * verificação de fichas técnicas (BOM) e lançamento assíncrono de ordens de fabrico.
 */
public final class BarcodeScannerExecutionEngine {

    private BarcodeScannerExecutionEngine() {}

    public static void searchProduct(
            String code, ComercialApiClient client,
            Consumer<ProductDTO> onSuccess, Consumer<String> onError, Runnable onFinished
    ) {
        new SwingWorker<ProductDTO, Void>() {
            @Override protected ProductDTO doInBackground() {
                try {
                    ProductDTO p = client.findProductByBarcode(code);
                    if (p != null) return p;
                } catch (Exception ignored) {}
                try {
                    for (ProductDTO p : client.getAllProducts()) {
                        if (code.equalsIgnoreCase(p.sku()) || code.equalsIgnoreCase(p.barcode())) return p;
                    }
                } catch (Exception ignored) {}
                return null;
            }
            @Override protected void done() {
                try {
                    ProductDTO product = get();
                    if (product != null) onSuccess.accept(product);
                    else onError.accept("Artigo não encontrado para o código: " + code);
                } catch (Exception ex) {
                    onError.accept("Erro ao pesquisar: " + ex.getMessage());
                } finally {
                    if (onFinished != null) onFinished.run();
                }
            }
        }.execute();
    }

    public static void checkBomAndEnable(ProductDTO product, ProductionApiClient client, BarcodeScannerProductDetailsCard card) {
        card.setBomStatus("A verificar Ficha Técnica (BOM)...", UIHelper.PENDING_YELLOW, false);
        new SwingWorker<BillOfMaterialsDTO, Void>() {
            @Override protected BillOfMaterialsDTO doInBackground() {
                try {
                    for (BillOfMaterialsDTO b : client.listBoms()) {
                        if (b.finishedProductId().equals(product.id())) return b;
                    }
                } catch (Exception ignored) {}
                return null;
            }
            @Override protected void done() {
                try {
                    BillOfMaterialsDTO matchedBom = get();
                    card.setMatchedBom(matchedBom);
                    if (matchedBom != null) {
                        int count = matchedBom.items() != null ? matchedBom.items().size() : 0;
                        card.setBomStatus("Ficha Técnica Activa: " + matchedBom.bomCode() + " (" + count + " itens)", UIHelper.APPROVED_GREEN, true);
                    } else {
                        card.setBomStatus("Sem Ficha Técnica (BOM) associada a este produto.", UIHelper.REJECTED_RED, false);
                    }
                } catch (Exception ex) {
                    card.setBomStatus("Falha ao verificar BOM.", UIHelper.REJECTED_RED, false);
                }
            }
        }.execute();
    }

    public static void launchProductionOrder(
            BillOfMaterialsDTO matchedBom, int qty,
            InventoryApiClient invClient, ProductionApiClient prodClient,
            Runnable onSuccess, Consumer<String> onError, Runnable onFinally
    ) {
        new SwingWorker<Void, Void>() {
            @Override protected Void doInBackground() throws Exception {
                Long whId = 1L;
                if (invClient != null) {
                    try {
                        Long compId = CurrentUserContext.findCurrentCompanyId();
                        if (compId != null) {
                            var whs = invClient.getWarehousesByCompany(compId);
                            if (!whs.isEmpty()) whId = whs.get(0).id();
                        }
                    } catch (Exception ignored) {}
                }
                var req = new CreateProductionOrderRequest(matchedBom.id(), new BigDecimal(qty), whId, whId, LocalDate.now().plusDays(1));
                var order = prodClient.createOrder(req);
                prodClient.startOrder(order.id());
                return null;
            }
            @Override protected void done() {
                try {
                    get();
                    if (onSuccess != null) onSuccess.run();
                } catch (Exception ex) {
                    if (onError != null) onError.accept(ex.getMessage());
                } finally {
                    if (onFinally != null) onFinally.run();
                }
            }
        }.execute();
    }
}
