package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.returnables.dto.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/** HTTP client for the returnables account ({@code /api/returnables}, SA-C12). */
@Component
@Profile("desktop")
public class ReturnablesApiClient {

    private final DesktopClientFactory clientFactory;

    public ReturnablesApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<ReturnableItemDTO> items(Long companyId) {
        return clientFactory.authenticatedClient().getList("/api/returnables/items?companyId=" + companyId, ReturnableItemDTO.class);
    }

    public ReturnableItemDTO saveItem(SaveReturnableItemRequest request) {
        return clientFactory.authenticatedClient().put("/api/returnables/items", request, ReturnableItemDTO.class);
    }

    public List<ReturnableBalanceDTO> balances(Long companyId) {
        return clientFactory.authenticatedClient().getList("/api/returnables/balances?companyId=" + companyId, ReturnableBalanceDTO.class);
    }

    public List<ReturnableMovementDTO> movements(Long companyId, int days) {
        return clientFactory.authenticatedClient().getList("/api/returnables/movements?companyId=" + companyId + "&days=" + days,
                ReturnableMovementDTO.class);
    }

    public ReturnableMovementDTO record(RecordReturnableRequest request) {
        return clientFactory.authenticatedClient().post("/api/returnables/movements", request, ReturnableMovementDTO.class);
    }

    public ReturnableMovementDTO charge(ChargeReturnablesRequest request) {
        return clientFactory.authenticatedClient().post("/api/returnables/charges", request, ReturnableMovementDTO.class);
    }
}
