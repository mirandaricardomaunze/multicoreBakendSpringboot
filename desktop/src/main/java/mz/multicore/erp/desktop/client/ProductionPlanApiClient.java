package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.productionplan.dto.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/** HTTP client for the bakery day plan ({@code /api/production-plans}, F4). */
@Component
@Profile("desktop")
public class ProductionPlanApiClient {

    private final DesktopClientFactory clientFactory;

    public ProductionPlanApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public ProductionPlanDTO day(Long companyId, Long warehouseId, LocalDate date) {
        return clientFactory.authenticatedClient().get("/api/production-plans/day?companyId=" + companyId + "&warehouseId="
                + warehouseId + "&date=" + date, ProductionPlanDTO.class);
    }

    public List<ProductionSuggestionDTO> suggestions(Long companyId, LocalDate date) {
        return clientFactory.authenticatedClient().getList("/api/production-plans/suggestions?companyId=" + companyId + "&date=" + date,
                ProductionSuggestionDTO.class);
    }

    public ProductionPlanDTO save(SaveProductionPlanRequest request) {
        return clientFactory.authenticatedClient().put("/api/production-plans", request, ProductionPlanDTO.class);
    }

    public List<MaterialRequirementDTO> requirements(Long companyId, Long planId) {
        return clientFactory.authenticatedClient().getList("/api/production-plans/" + planId + "/requirements?companyId=" + companyId,
                MaterialRequirementDTO.class);
    }

    public ProductionRunDTO recordRun(RecordRunRequest request) {
        return clientFactory.authenticatedClient().post("/api/production-plans/runs", request, ProductionRunDTO.class);
    }

    public ProductionPlanDTO close(Long planId, ClosePlanRequest request) {
        return clientFactory.authenticatedClient().post("/api/production-plans/" + planId + "/close", request, ProductionPlanDTO.class);
    }
}
