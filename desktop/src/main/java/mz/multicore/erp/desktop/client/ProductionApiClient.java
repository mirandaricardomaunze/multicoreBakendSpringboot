package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.manufacturing.dto.BillOfMaterialsDTO;
import mz.multicore.erp.modules.manufacturing.dto.CreateBomRequest;
import mz.multicore.erp.modules.manufacturing.dto.CreateProductionOrderRequest;
import mz.multicore.erp.modules.manufacturing.dto.ProductionOrderDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cliente HTTP Desktop para o módulo de Produção & Fichas Técnicas (/api/manufacturing).
 */
@Component
@Profile("desktop")
public class ProductionApiClient {

    private final DesktopClientFactory clientFactory;

    public ProductionApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<BillOfMaterialsDTO> listBoms() {
        return clientFactory.authenticatedClient().getList("/api/manufacturing/boms", BillOfMaterialsDTO.class);
    }

    public BillOfMaterialsDTO getBom(Long id) {
        return clientFactory.authenticatedClient().get("/api/manufacturing/boms/" + id, BillOfMaterialsDTO.class);
    }

    public BillOfMaterialsDTO createBom(CreateBomRequest request) {
        return clientFactory.authenticatedClient().post("/api/manufacturing/boms", request, BillOfMaterialsDTO.class);
    }

    public List<ProductionOrderDTO> listOrders() {
        return clientFactory.authenticatedClient().getList("/api/manufacturing/orders", ProductionOrderDTO.class);
    }

    public ProductionOrderDTO getOrder(Long id) {
        return clientFactory.authenticatedClient().get("/api/manufacturing/orders/" + id, ProductionOrderDTO.class);
    }

    public ProductionOrderDTO createOrder(CreateProductionOrderRequest request) {
        return clientFactory.authenticatedClient().post("/api/manufacturing/orders", request, ProductionOrderDTO.class);
    }

    public ProductionOrderDTO startOrder(Long id) {
        return clientFactory.authenticatedClient().post("/api/manufacturing/orders/" + id + "/start", null, ProductionOrderDTO.class);
    }

    public ProductionOrderDTO completeOrder(Long id, BigDecimal producedQuantity) {
        String path = "/api/manufacturing/orders/" + id + "/complete";
        if (producedQuantity != null) {
            path += "?producedQuantity=" + producedQuantity;
        }
        return clientFactory.authenticatedClient().post(path, null, ProductionOrderDTO.class);
    }

    public ProductionOrderDTO cancelOrder(Long id) {
        return clientFactory.authenticatedClient().post("/api/manufacturing/orders/" + id + "/cancel", null, ProductionOrderDTO.class);
    }
}
