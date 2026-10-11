package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.assets.dto.AssetDTO;
import mz.multicore.erp.modules.assets.dto.CreateAssetRequest;
import mz.multicore.erp.modules.assets.dto.DepreciationScheduleDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Cliente HTTP Desktop para o módulo de Activos Fixos e Património (/api/assets).
 */
@Component
@Profile("desktop")
public class AssetApiClient {

    private final DesktopClientFactory clientFactory;

    public AssetApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<AssetDTO> listAssets() {
        return clientFactory.authenticatedClient().getList("/api/assets", AssetDTO.class);
    }

    public AssetDTO getAsset(Long id) {
        return clientFactory.authenticatedClient().get("/api/assets/" + id, AssetDTO.class);
    }

    public AssetDTO createAsset(CreateAssetRequest request) {
        return clientFactory.authenticatedClient().post("/api/assets", request, AssetDTO.class);
    }

    public List<DepreciationScheduleDTO> runMonthlyDepreciation(int year, int month) {
        return clientFactory.authenticatedClient().postForList(
                "/api/assets/depreciate?year=" + year + "&month=" + month,
                null,
                DepreciationScheduleDTO.class
        );
    }

    public List<DepreciationScheduleDTO> getDepreciationHistory(Long id) {
        return clientFactory.authenticatedClient().getList("/api/assets/" + id + "/depreciations", DepreciationScheduleDTO.class);
    }
}
