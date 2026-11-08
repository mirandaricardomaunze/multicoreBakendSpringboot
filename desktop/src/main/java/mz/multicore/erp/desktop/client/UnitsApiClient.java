package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.units.dto.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** HTTP client for units of measure and product units ({@code /api/units}, SA-C02). */
@Component
@Profile("desktop")
public class UnitsApiClient {

    private final DesktopClientFactory clientFactory;

    public UnitsApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<UnitOfMeasureDTO> units(Long companyId) {
        return clientFactory.authenticatedClient().getList("/api/units?companyId=" + companyId, UnitOfMeasureDTO.class);
    }

    public UnitOfMeasureDTO createPackage(CreateUnitOfMeasureRequest request) {
        return clientFactory.authenticatedClient().post("/api/units", request, UnitOfMeasureDTO.class);
    }

    public ProductUnitsDTO productUnits(Long companyId, Long productId) {
        return clientFactory.authenticatedClient().get(
                "/api/units/products/" + productId + "?companyId=" + companyId, ProductUnitsDTO.class);
    }

    public ProductUnitsDTO setBaseUnit(SetBaseUnitRequest request) {
        return clientFactory.authenticatedClient().put("/api/units/products/base", request, ProductUnitsDTO.class);
    }

    public ProductUnitsDTO saveProductUnit(SaveProductUnitRequest request) {
        return clientFactory.authenticatedClient().put("/api/units/products", request, ProductUnitsDTO.class);
    }

    public void deleteProductUnit(Long companyId, Long productUnitId) {
        clientFactory.authenticatedClient().delete("/api/units/products/units/" + productUnitId + "?companyId=" + companyId);
    }

    public UnitConversionDTO convert(Long companyId, Long productId, String unitCode, BigDecimal quantity) {
        return clientFactory.authenticatedClient().get("/api/units/products/" + productId + "/convert?companyId="
                + companyId + "&unitCode=" + URLEncoder.encode(unitCode, StandardCharsets.UTF_8)
                + "&quantity=" + quantity.toPlainString(), UnitConversionDTO.class);
    }
}
