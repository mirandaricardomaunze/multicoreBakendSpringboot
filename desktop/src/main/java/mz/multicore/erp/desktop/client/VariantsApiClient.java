package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.variants.dto.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** HTTP client for product variant families ({@code /api/variants}, SA-C04). */
@Component
@Profile("desktop")
public class VariantsApiClient {

    private final DesktopClientFactory clientFactory;

    public VariantsApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<VariantFamilyDTO> families(Long companyId) {
        return clientFactory.authenticatedClient().getList("/api/variants/families?companyId=" + companyId, VariantFamilyDTO.class);
    }

    public VariantFamilyDTO create(CreateVariantFamilyRequest request) {
        return clientFactory.authenticatedClient().post("/api/variants/families", request, VariantFamilyDTO.class);
    }

    public VariantFamilyDTO addValues(Long familyId, AddVariantValuesRequest request) {
        return clientFactory.authenticatedClient().post("/api/variants/families/" + familyId + "/values", request, VariantFamilyDTO.class);
    }

    public VariantFamilyDTO setPrice(Long companyId, Long familyId, BigDecimal price) {
        return clientFactory.authenticatedClient().put("/api/variants/families/" + familyId + "/price",
                new SetFamilyPriceRequest(companyId, price), VariantFamilyDTO.class);
    }
}
