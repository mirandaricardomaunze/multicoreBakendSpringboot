package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.comercial.dto.AgingSummaryDTO;
import mz.multicore.erp.modules.comercial.dto.ClientCreditEligibilityDTO;
import mz.multicore.erp.modules.comercial.dto.ClientDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cliente HTTP especializado para gestão de clientes e análise de crédito.
 */
@Component
@Profile("desktop")
public class CommercialClientApiClient {

    private final DesktopClientFactory clientFactory;

    public CommercialClientApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<ClientDTO> getClients() {
        return clientFactory.authenticatedClient().getList("/api/comercial/clients", ClientDTO.class);
    }

    public ClientCreditEligibilityDTO getClientCreditEligibility(Long clientId) {
        return clientFactory.authenticatedClient()
                .get("/api/comercial/clients/" + clientId + "/credit-eligibility", ClientCreditEligibilityDTO.class);
    }

    public ClientDTO createClient(String name, String taxId, String email, String address,
                                  int paymentTermsDays, BigDecimal creditLimit) {
        return clientFactory.authenticatedClient().post("/api/comercial/clients",
                new SaveClientRequest(name, taxId, email, address, paymentTermsDays, creditLimit), ClientDTO.class);
    }

    public ClientDTO updateClient(Long id, String name, String taxId, String email, String address,
                                  int paymentTermsDays, BigDecimal creditLimit) {
        return clientFactory.authenticatedClient().put("/api/comercial/clients/" + id,
                new SaveClientRequest(name, taxId, email, address, paymentTermsDays, creditLimit), ClientDTO.class);
    }

    public AgingSummaryDTO getReceivablesAging() {
        return clientFactory.authenticatedClient()
                .get("/api/comercial/receivables/aging", AgingSummaryDTO.class);
    }

    public void deleteClient(Long id) {
        clientFactory.authenticatedClient().delete("/api/comercial/clients/" + id);
    }

    public record SaveClientRequest(String name, String taxId, String email, String address,
                                    int paymentTermsDays, BigDecimal creditLimit) {}
}
