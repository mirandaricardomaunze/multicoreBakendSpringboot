package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.purchases.dto.CreatePurchaseOrderRequest;
import mz.multicore.erp.modules.purchases.dto.CreatePurchaseRequest;
import mz.multicore.erp.modules.purchases.dto.CreateSupplierRequest;
import mz.multicore.erp.modules.purchases.dto.PayableDTO;
import mz.multicore.erp.modules.purchases.dto.PurchaseDTO;
import mz.multicore.erp.modules.purchases.dto.PurchaseOrderDTO;
import mz.multicore.erp.modules.purchases.dto.ReceivePurchaseOrderRequest;
import mz.multicore.erp.modules.purchases.dto.ReorderSuggestionDTO;
import mz.multicore.erp.modules.purchases.dto.SupplierDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Cliente HTTP para as compras ({@code /api/purchases}). Colapsa num só cliente os serviços que o
 * {@code ComprasPanel} usava em processo: compras, fornecedores, contas a pagar, encomendas a
 * fornecedor e sugestões de reposição. Espelha o padrão do {@link ComercialApiClient}.
 */
@Component
@Profile("desktop")
public class PurchaseApiClient {

    private final DesktopClientFactory clientFactory;

    public PurchaseApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    // ─── Compras ─────────────────────────────────────────────────────────────
    public List<PurchaseDTO> getPurchasesByCompany(Long companyId) {
        return clientFactory.authenticatedClient()
                .getList("/api/purchases?companyId=" + companyId, PurchaseDTO.class);
    }

    public PurchaseDTO createPurchase(CreatePurchaseRequest request) {
        return clientFactory.authenticatedClient().post("/api/purchases", request, PurchaseDTO.class);
    }

    // ─── Fornecedores ────────────────────────────────────────────────────────
    public List<SupplierDTO> getSuppliersByCompany(Long companyId) {
        return clientFactory.authenticatedClient()
                .getList("/api/purchases/suppliers?companyId=" + companyId, SupplierDTO.class);
    }

    public SupplierDTO createSupplier(CreateSupplierRequest request) {
        return clientFactory.authenticatedClient().post("/api/purchases/suppliers", request, SupplierDTO.class);
    }

    public SupplierDTO updateSupplier(Long id, CreateSupplierRequest request) {
        return clientFactory.authenticatedClient().put("/api/purchases/suppliers/" + id, request, SupplierDTO.class);
    }

    public SupplierDTO setSupplierActive(Long id, Long companyId, boolean value) {
        return clientFactory.authenticatedClient().patch(
                "/api/purchases/suppliers/" + id + "/active?companyId=" + companyId + "&value=" + value,
                null, SupplierDTO.class);
    }

    // ─── Contas a pagar ──────────────────────────────────────────────────────
    public List<PayableDTO> findPayablesByCompany(Long companyId) {
        return clientFactory.authenticatedClient()
                .getList("/api/purchases/payables?companyId=" + companyId, PayableDTO.class);
    }

    public PurchaseDTO registerSupplierPayment(Long id, BigDecimal amount, Long financeAccountId, String reference) {
        String path = "/api/purchases/" + id + "/pay?amount=" + amount + "&financeAccountId=" + financeAccountId;
        if (reference != null && !reference.isBlank()) {
            path += "&reference=" + URLEncoder.encode(reference, StandardCharsets.UTF_8);
        }
        return clientFactory.authenticatedClient().post(path, null, PurchaseDTO.class);
    }

    // ─── Encomendas a fornecedor ─────────────────────────────────────────────
    public List<PurchaseOrderDTO> findOrdersByCompany(Long companyId) {
        return clientFactory.authenticatedClient()
                .getList("/api/purchases/orders?companyId=" + companyId, PurchaseOrderDTO.class);
    }

    public PurchaseOrderDTO createOrder(CreatePurchaseOrderRequest request) {
        return clientFactory.authenticatedClient().post("/api/purchases/orders", request, PurchaseOrderDTO.class);
    }

    public PurchaseOrderDTO receiveOrder(Long id) {
        return clientFactory.authenticatedClient()
                .post("/api/purchases/orders/" + id + "/receive", null, PurchaseOrderDTO.class);
    }

    public PurchaseOrderDTO receivePartial(Long id, ReceivePurchaseOrderRequest request) {
        return clientFactory.authenticatedClient()
                .post("/api/purchases/orders/" + id + "/receive-partial", request, PurchaseOrderDTO.class);
    }

    public PurchaseOrderDTO cancelOrder(Long id, String reason) {
        return clientFactory.authenticatedClient().post(
                "/api/purchases/orders/" + id + "/cancel?reason=" + URLEncoder.encode(reason, StandardCharsets.UTF_8),
                null, PurchaseOrderDTO.class);
    }

    // ─── Reposição automática ────────────────────────────────────────────────
    public List<ReorderSuggestionDTO> suggestions(Long companyId) {
        return clientFactory.authenticatedClient()
                .getList("/api/purchases/reorder-suggestions?companyId=" + companyId, ReorderSuggestionDTO.class);
    }
}
