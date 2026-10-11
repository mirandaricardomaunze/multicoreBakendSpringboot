package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.recurring.dto.BatchBillingResultDTO;
import mz.multicore.erp.modules.recurring.dto.CreateRecurringContractRequest;
import mz.multicore.erp.modules.recurring.dto.RecurringContractDTO;
import mz.multicore.erp.modules.recurring.model.ContractStatus;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * Cliente HTTP Desktop para o módulo de Contratos & Facturação Recorrente (/api/recurring/contracts).
 */
@Component
@Profile("desktop")
public class RecurringContractApiClient {

    private final DesktopClientFactory clientFactory;

    public RecurringContractApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<RecurringContractDTO> listContracts() {
        return clientFactory.authenticatedClient().getList("/api/recurring/contracts", RecurringContractDTO.class);
    }

    public RecurringContractDTO getContract(Long id) {
        return clientFactory.authenticatedClient().get("/api/recurring/contracts/" + id, RecurringContractDTO.class);
    }

    public RecurringContractDTO createContract(CreateRecurringContractRequest request) {
        return clientFactory.authenticatedClient().post("/api/recurring/contracts", request, RecurringContractDTO.class);
    }

    public RecurringContractDTO updateStatus(Long id, ContractStatus status) {
        return clientFactory.authenticatedClient().patch("/api/recurring/contracts/" + id + "/status?status=" + status.name(), null, RecurringContractDTO.class);
    }

    public BatchBillingResultDTO runBatchBilling(LocalDate referenceDate) {
        String url = "/api/recurring/contracts/batch-billing";
        if (referenceDate != null) {
            url += "?referenceDate=" + referenceDate;
        }
        return clientFactory.authenticatedClient().post(url, null, BatchBillingResultDTO.class);
    }
}
