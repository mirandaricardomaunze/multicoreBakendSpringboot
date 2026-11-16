package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.bulkstock.dto.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/** HTTP client for bulk stock measured on the pile ({@code /api/bulk-stock}, SA-C17). */
@Component
@Profile("desktop")
public class BulkStockApiClient {

    private final DesktopClientFactory clientFactory;

    public BulkStockApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<BulkStockSettingDTO> settings(Long companyId) {
        return clientFactory.authenticatedClient().getList("/api/bulk-stock/settings?companyId=" + companyId, BulkStockSettingDTO.class);
    }

    public List<BulkStockSettingDTO> saveSetting(Long companyId, Long productId, BigDecimal tolerancePercent) {
        BulkStockSettingDTO[] saved = clientFactory.authenticatedClient().put("/api/bulk-stock/settings",
                new SaveBulkStockSettingRequest(companyId, productId, tolerancePercent), BulkStockSettingDTO[].class);
        return saved == null ? List.of() : List.of(saved);
    }

    public List<BulkMeasurementDTO> measurements(Long companyId) {
        return clientFactory.authenticatedClient().getList("/api/bulk-stock/measurements?companyId=" + companyId, BulkMeasurementDTO.class);
    }

    public BulkMeasurementDTO preview(BulkMeasurementRequest request) {
        return clientFactory.authenticatedClient().post("/api/bulk-stock/measurements/preview", request, BulkMeasurementDTO.class);
    }

    public BulkMeasurementDTO record(BulkMeasurementRequest request) {
        return clientFactory.authenticatedClient().post("/api/bulk-stock/measurements", request, BulkMeasurementDTO.class);
    }

    public BulkMeasurementDTO approve(Long companyId, Long id) {
        return clientFactory.authenticatedClient().post("/api/bulk-stock/measurements/" + id + "/approve",
                new ReviewBulkMeasurementRequest(companyId, null), BulkMeasurementDTO.class);
    }

    public BulkMeasurementDTO reject(Long companyId, Long id, String reason) {
        return clientFactory.authenticatedClient().post("/api/bulk-stock/measurements/" + id + "/reject",
                new ReviewBulkMeasurementRequest(companyId, reason), BulkMeasurementDTO.class);
    }
}
