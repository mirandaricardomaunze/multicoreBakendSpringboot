package mz.multicore.erp.modules.publicapi.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.CreateInvoiceLineRequest;
import mz.multicore.erp.modules.comercial.dto.CreateOrderRequest;
import mz.multicore.erp.modules.comercial.dto.SaveClientRequest;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.model.ExternalOrderSource;
import mz.multicore.erp.modules.comercial.model.OrderKind;
import mz.multicore.erp.modules.comercial.model.Product;
import mz.multicore.erp.modules.comercial.service.ClientLookup;
import mz.multicore.erp.modules.comercial.service.ComercialService;
import mz.multicore.erp.modules.comercial.service.CustomerOrderTrackingService;
import mz.multicore.erp.modules.comercial.service.ExternalOrderPlacementService;
import mz.multicore.erp.modules.comercial.service.PlacedExternalOrder;
import mz.multicore.erp.modules.comercial.service.TrackedCustomerOrder;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.service.WarehouseLookup;
import mz.multicore.erp.modules.publicapi.dto.ApiKeyPrincipal;
import mz.multicore.erp.modules.publicapi.dto.CreatePublicOrderRequest;
import mz.multicore.erp.modules.publicapi.dto.PublicOrderDTO;
import mz.multicore.erp.modules.publicapi.dto.PublicOrderSubmission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * {@code POST /api/public/v1/orders} (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.4, §3.4.1, API-08): translates SKUs, the
 * warehouse and the customer of the external system into a commercial order, created in the key's company by the key
 * acting as a seller. Prices and VAT always come from the system; the order reserves stock through the commercial
 * module's {@link ExternalOrderPlacementService}. {@code GET /api/public/v1/orders/{number}} (§3.4.3, API-10) reads any
 * customer order of the key's company through {@link CustomerOrderTrackingService}.
 */
@Service
public class PublicOrderService {

    /** The key sells like a seller: same approval thresholds as an order typed at the desktop by a SELLER. */
    static final String ACTOR_ROLE = "SELLER";

    private final PublicCatalogService catalogService;
    private final WarehouseLookup warehouseLookup;
    private final ClientLookup clientLookup;
    private final ComercialService comercialService;
    private final ExternalOrderPlacementService placementService;
    private final CustomerOrderTrackingService trackingService;

    public PublicOrderService(PublicCatalogService catalogService, WarehouseLookup warehouseLookup,
                              ClientLookup clientLookup, ComercialService comercialService,
                              ExternalOrderPlacementService placementService,
                              CustomerOrderTrackingService trackingService) {
        this.catalogService = catalogService;
        this.warehouseLookup = warehouseLookup;
        this.clientLookup = clientLookup;
        this.comercialService = comercialService;
        this.placementService = placementService;
        this.trackingService = trackingService;
    }

    /** The customer order with this number in the key's company, in its current state (SPEC §3.4.3). */
    @Transactional(readOnly = true)
    public PublicOrderDTO getOrder(ApiKeyPrincipal key, String number) {
        return trackingService.findByNumber(key.companyId(), number).map(PublicOrderService::toPublic)
                .orElseThrow(() -> new PublicApiNotFoundException("ORDER_NOT_FOUND",
                        "A encomenda " + (number == null ? "" : number.trim()) + " não existe nesta empresa."));
    }

    /**
     * Creates the order, or replays the one this key already created with the same {@code externalReference} and
     * content (SPEC §3.4.2); the same reference with different content is a {@code 409}.
     */
    @Transactional
    public PublicOrderSubmission submit(ApiKeyPrincipal key, CreatePublicOrderRequest request) {
        return CurrentUserContext.runAs(actorName(key), ACTOR_ROLE, key.companyId(), () -> replayOrPlace(key, request));
    }

    /** {@link #submit} when only the order matters, not whether this call created it. */
    @Transactional
    public PublicOrderDTO create(ApiKeyPrincipal key, CreatePublicOrderRequest request) {
        return submit(key, request).order();
    }

    /** Audit name of the key acting on the order, e.g. {@code API · Loja online}. */
    static String actorName(ApiKeyPrincipal key) {
        return "API · " + key.keyName();
    }

    private PublicOrderSubmission replayOrPlace(ApiKeyPrincipal key, CreatePublicOrderRequest request) {
        String reference = request.externalReference().trim();
        String idempotencyKey = "API:" + key.keyId() + ":" + reference;
        String fingerprint = PublicOrderFingerprint.of(request);
        return placementService.findPlaced(key.companyId(), idempotencyKey)
                .map(placed -> replay(key.companyId(), placed, fingerprint, reference))
                .orElseGet(() -> new PublicOrderSubmission(
                        place(key, request, new ExternalOrderSource(key.keyName(), reference, idempotencyKey,
                                fingerprint, trimToNull(request.contactPhone()))), true));
    }

    private PublicOrderSubmission replay(Long companyId, PlacedExternalOrder placed, String fingerprint,
                                         String reference) {
        if (!fingerprint.equals(placed.requestFingerprint())) {
            throw PublicApiException.conflict("EXTERNAL_REFERENCE_CONFLICT",
                    "A referência externa " + reference + " já foi usada na encomenda " + placed.order().orderNumber()
                            + " com outro conteúdo. Use uma referência nova ou repita o pedido original.");
        }
        // The replay shows the order as it is now, invoice number included (SPEC §3.4.2, §3.4.3).
        return new PublicOrderSubmission(trackingService.findByNumber(companyId, placed.order().orderNumber())
                .map(PublicOrderService::toPublic).orElseThrow(), false);
    }

    private PublicOrderDTO place(ApiKeyPrincipal key, CreatePublicOrderRequest request, ExternalOrderSource source) {
        Long companyId = key.companyId();
        Warehouse warehouse = PublicSalesWarehouses.require(warehouseLookup.listForCompany(companyId), request.warehouse());
        List<CreateInvoiceLineRequest> lines = request.lines().stream().map(line -> {
            Product product = catalogService.requireProduct(companyId, line.sku().trim());
            return new CreateInvoiceLineRequest(product.getId(), line.quantity(), product.effectiveTaxRate(),
                    BigDecimal.ZERO, null, null);
        }).toList();
        boolean walkIn = isBlank(request.nuit());
        Long clientId = walkIn ? requireContact(request) : customerFor(companyId, request);
        CreateOrderRequest order = new CreateOrderRequest(clientId, walkIn ? request.contactName().trim() : null,
                companyId, warehouse.getId(), lines, OrderKind.FORMAL_ORDER, null, null, null, null,
                trimToNull(request.notes()));
        return PublicOrderMapper.toPublic(placementService.placeReservedFormalOrder(order, source),
                source.externalReference(), null);
    }

    private static PublicOrderDTO toPublic(TrackedCustomerOrder tracked) {
        return PublicOrderMapper.toPublic(tracked.order(), tracked.externalReference(), tracked.invoiceNumber());
    }

    /** Consumidor Final: no client record, but the order must say who to call. */
    private static Long requireContact(CreatePublicOrderRequest request) {
        if (isBlank(request.contactName()) || isBlank(request.contactPhone())) {
            throw PublicApiException.badRequest("CONTACT_REQUIRED",
                    "Sem NUIT, indique o nome (contactName) e o telefone (contactPhone) de contacto do cliente.");
        }
        return null;
    }

    /** The company's client with this NUIT, registered by the manual-registration rules when new. */
    private Long customerFor(Long companyId, CreatePublicOrderRequest request) {
        String nuit = request.nuit().trim();
        return clientLookup.findForCompanyByTaxId(nuit, companyId).map(Client::getId).orElseGet(() -> {
            if (isBlank(request.customerName())) {
                throw PublicApiException.badRequest("CUSTOMER_NAME_REQUIRED",
                        "O NUIT " + nuit + " ainda não é cliente da empresa: indique o nome do cliente (customerName).");
            }
            return comercialService.createClient(
                    new SaveClientRequest(request.customerName().trim(), nuit, null, null)).id();
        });
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String trimToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
