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
import mz.multicore.erp.modules.comercial.service.ExternalOrderPlacementService;
import mz.multicore.erp.modules.inventory.model.Warehouse;
import mz.multicore.erp.modules.inventory.service.WarehouseLookup;
import mz.multicore.erp.modules.publicapi.dto.ApiKeyPrincipal;
import mz.multicore.erp.modules.publicapi.dto.CreatePublicOrderRequest;
import mz.multicore.erp.modules.publicapi.dto.PublicOrderDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * {@code POST /api/public/v1/orders} (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.4, §3.4.1, API-08): translates SKUs, the
 * warehouse and the customer of the external system into a commercial order, created in the key's company by the key
 * acting as a seller. Prices and VAT always come from the system; the order reserves stock through the commercial
 * module's {@link ExternalOrderPlacementService}.
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

    public PublicOrderService(PublicCatalogService catalogService, WarehouseLookup warehouseLookup,
                              ClientLookup clientLookup, ComercialService comercialService,
                              ExternalOrderPlacementService placementService) {
        this.catalogService = catalogService;
        this.warehouseLookup = warehouseLookup;
        this.clientLookup = clientLookup;
        this.comercialService = comercialService;
        this.placementService = placementService;
    }

    @Transactional
    public PublicOrderDTO create(ApiKeyPrincipal key, CreatePublicOrderRequest request) {
        return CurrentUserContext.runAs(actorName(key), ACTOR_ROLE, key.companyId(), () -> place(key, request));
    }

    /** Audit name of the key acting on the order, e.g. {@code API · Loja online}. */
    static String actorName(ApiKeyPrincipal key) {
        return "API · " + key.keyName();
    }

    private PublicOrderDTO place(ApiKeyPrincipal key, CreatePublicOrderRequest request) {
        Long companyId = key.companyId();
        Warehouse warehouse = PublicSalesWarehouses.require(warehouseLookup.listForCompany(companyId), request.warehouse());
        List<CreateInvoiceLineRequest> lines = request.lines().stream().map(line -> {
            Product product = catalogService.requireProduct(companyId, line.sku().trim());
            return new CreateInvoiceLineRequest(product.getId(), line.quantity(), product.effectiveTaxRate(),
                    BigDecimal.ZERO, null, null);
        }).toList();
        String reference = request.externalReference().trim();
        boolean walkIn = isBlank(request.nuit());
        Long clientId = walkIn ? requireContact(request) : customerFor(companyId, request);
        CreateOrderRequest order = new CreateOrderRequest(clientId, walkIn ? request.contactName().trim() : null,
                companyId, warehouse.getId(), lines, OrderKind.FORMAL_ORDER, null, null, null, null,
                trimToNull(request.notes()));
        ExternalOrderSource source = new ExternalOrderSource(key.keyName(), reference,
                "API:" + key.keyId() + ":" + reference, trimToNull(request.contactPhone()));
        return PublicOrderMapper.toPublic(placementService.placeReservedFormalOrder(order, source), reference);
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
