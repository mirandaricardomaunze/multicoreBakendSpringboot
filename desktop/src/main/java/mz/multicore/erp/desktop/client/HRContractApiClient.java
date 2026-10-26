package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.hr.dto.ContractAlertsDTO;
import mz.multicore.erp.modules.hr.dto.CreateContractRequest;
import mz.multicore.erp.modules.hr.dto.EmploymentContractDTO;
import mz.multicore.erp.modules.hr.dto.RenewContractRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * Cliente HTTP especializado para contratos de trabalho, renovações, alertas e PDF.
 */
@Component
@Profile("desktop")
public class HRContractApiClient {

    private final DesktopClientFactory clientFactory;

    public HRContractApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<EmploymentContractDTO> getAllContracts() {
        return clientFactory.authenticatedClient().getList("/api/hr/contracts", EmploymentContractDTO.class);
    }

    public EmploymentContractDTO createContract(CreateContractRequest request) {
        return clientFactory.authenticatedClient().post("/api/hr/contracts", request, EmploymentContractDTO.class);
    }

    public EmploymentContractDTO activateContract(Long id) {
        return clientFactory.authenticatedClient().post("/api/hr/contracts/" + id + "/activate", null, EmploymentContractDTO.class);
    }

    public EmploymentContractDTO renewContract(Long id, RenewContractRequest request) {
        return clientFactory.authenticatedClient().post("/api/hr/contracts/" + id + "/renew", request, EmploymentContractDTO.class);
    }

    public EmploymentContractDTO terminateContract(Long id, LocalDate terminationDate, String reason) {
        String query = "?reason=" + URLEncoder.encode(reason, StandardCharsets.UTF_8)
                + (terminationDate == null ? "" : "&terminationDate=" + terminationDate);
        return clientFactory.authenticatedClient().post("/api/hr/contracts/" + id + "/terminate" + query, null, EmploymentContractDTO.class);
    }

    public byte[] renderContract(Long id) {
        return clientFactory.authenticatedClient().getBytes("/api/print/employment-contract/" + id);
    }

    public ContractAlertsDTO getContractAlerts() {
        return clientFactory.authenticatedClient().get("/api/hr/contracts/alerts", ContractAlertsDTO.class);
    }
}
