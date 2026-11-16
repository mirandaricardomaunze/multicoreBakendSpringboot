package mz.multicore.erp.desktop.client;

import mz.multicore.erp.architecture.paging.PageResponse;
import mz.multicore.erp.modules.comercial.dto.CancelReasonRequest;
import mz.multicore.erp.modules.comercial.dto.CompleteOrderPickingRequest;
import mz.multicore.erp.modules.comercial.dto.CreateFulfillmentOrderRequest;
import mz.multicore.erp.modules.comercial.dto.CreateOrderRequest;
import mz.multicore.erp.modules.comercial.dto.InvoiceDTO;
import mz.multicore.erp.modules.comercial.dto.OrderActionRequest;
import mz.multicore.erp.modules.comercial.dto.OrderDTO;
import mz.multicore.erp.modules.comercial.dto.OrderEventDTO;
import mz.multicore.erp.modules.comercial.dto.OrderPickingDetailsDTO;
import mz.multicore.erp.modules.comercial.dto.ReprintAuthorizationRequest;
import mz.multicore.erp.modules.comercial.dto.UpdateOrderRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Cliente HTTP especializado para encomendas comerciais, picking, fulfillment e aprovações.
 */
@Component
@Profile("desktop")
public class CommercialOrderApiClient {

    private final DesktopClientFactory clientFactory;

    public CommercialOrderApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<OrderDTO> getOrdersByCompany(Long companyId) {
        return clientFactory.authenticatedClient()
                .getList("/api/comercial/orders?companyId=" + companyId, OrderDTO.class);
    }

    public PageResponse<OrderDTO> getOrdersPage(Long companyId, int page, int size) {
        return clientFactory.authenticatedClient()
                .getGeneric("/api/comercial/orders/page?companyId=" + companyId + "&page=" + page + "&size=" + size,
                        PageResponse.class, OrderDTO.class);
    }

    public List<OrderDTO> getPendingOrdersByCompany(Long companyId) {
        return clientFactory.authenticatedClient()
                .getList("/api/comercial/orders/pending?companyId=" + companyId, OrderDTO.class);
    }

    public List<OrderDTO> searchPendingOrders(String query) {
        return clientFactory.authenticatedClient()
                .getList("/api/comercial/orders/pending/search?query=" + enc(query), OrderDTO.class);
    }

    public List<OrderDTO> searchCancellableOrders(String query) {
        return clientFactory.authenticatedClient()
                .getList("/api/comercial/orders/cancellable/search?query=" + enc(query), OrderDTO.class);
    }

    public OrderDTO getOrderById(Long orderId) {
        return clientFactory.authenticatedClient().get("/api/comercial/orders/" + orderId, OrderDTO.class);
    }

    public OrderDTO createOrder(CreateOrderRequest request) {
        return clientFactory.authenticatedClient().post("/api/comercial/orders", request, OrderDTO.class);
    }

    public OrderDTO updateOrder(Long id, UpdateOrderRequest request) {
        return clientFactory.authenticatedClient().put("/api/comercial/orders/" + id, request, OrderDTO.class);
    }

    public OrderDTO submitFulfillmentOrder(CreateOrderRequest request, String key, String terminal) {
        return clientFactory.authenticatedClient().post("/api/comercial/orders/fulfillment",
                new CreateFulfillmentOrderRequest(key, request, terminal), OrderDTO.class);
    }

    public byte[] printPicking(Long id, String terminal) {
        return clientFactory.authenticatedClient().postForBytes("/api/comercial/orders/" + id + "/picking-print",
                new OrderActionRequest(null, terminal));
    }

    public byte[] reprintPicking(Long id, String username, String password, String reason, String terminal) {
        return clientFactory.authenticatedClient().postForBytes("/api/comercial/orders/" + id + "/picking-reprint",
                new ReprintAuthorizationRequest(username, password, reason, terminal));
    }

    public OrderPickingDetailsDTO getOrderPickingDetails(Long id) {
        return clientFactory.authenticatedClient()
                .get("/api/comercial/orders/" + id + "/picking-details", OrderPickingDetailsDTO.class);
    }

    public byte[] printPickingA4(Long id, String terminal) {
        return clientFactory.authenticatedClient().postForBytes("/api/comercial/orders/" + id + "/picking-sheet-a4",
                new OrderActionRequest(null, terminal));
    }

    public OrderDTO confirmOrderPicking(Long id, CompleteOrderPickingRequest request) {
        return clientFactory.authenticatedClient()
                .post("/api/comercial/orders/" + id + "/picking-confirm", request, OrderDTO.class);
    }

    public OrderDTO completeSeparation(Long id, String terminal) {
        return clientFactory.authenticatedClient().post("/api/comercial/orders/" + id + "/separate",
                new OrderActionRequest(null, terminal), OrderDTO.class);
    }

    public InvoiceDTO billFulfillmentOrder(Long id, String terminal) {
        return clientFactory.authenticatedClient().post("/api/comercial/orders/" + id + "/fulfillment-bill",
                new OrderActionRequest(null, terminal), InvoiceDTO.class);
    }

    public List<OrderEventDTO> getOrderEvents(Long id) {
        return clientFactory.authenticatedClient().getList("/api/comercial/orders/" + id + "/events", OrderEventDTO.class);
    }

    public InvoiceDTO billOrder(Long orderId) {
        return clientFactory.authenticatedClient()
                .post("/api/comercial/orders/" + orderId + "/bill", null, InvoiceDTO.class);
    }

    public void cancelOrder(Long orderId, String reason) {
        clientFactory.authenticatedClient()
                .post("/api/comercial/orders/" + orderId + "/cancel", new CancelReasonRequest(reason));
    }

    public OrderDTO approveOrder(Long orderId, String comments) {
        return clientFactory.authenticatedClient().post(
                "/api/comercial/orders/" + orderId + "/approve",
                new mz.multicore.erp.modules.approvals.dto.ApprovalActionDTO(comments),
                OrderDTO.class);
    }

    public OrderDTO rejectOrder(Long orderId, String reason) {
        return clientFactory.authenticatedClient().post(
                "/api/comercial/orders/" + orderId + "/reject",
                new CancelReasonRequest(reason),
                OrderDTO.class);
    }

    public OrderDTO markOrderPrinted(Long orderId, String operator) {
        return clientFactory.authenticatedClient().post(
                "/api/comercial/orders/" + orderId + "/print?operator=" + enc(operator), null, OrderDTO.class);
    }

    private static String enc(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
