package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.comercial.dto.CustomerStatementDTO;
import mz.multicore.erp.modules.comercial.dto.EmailDispatchResultDTO;
import mz.multicore.erp.modules.comercial.dto.SendStatementEmailRequest;
import mz.multicore.erp.modules.purchases.dto.SupplierStatementDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Cliente HTTP Desktop para consulta e emissão de extratos de conta corrente de clientes e fornecedores.
 */
@Component
@Profile("desktop")
public class AccountStatementApiClient {

    private final DesktopClientFactory clientFactory;

    public AccountStatementApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public CustomerStatementDTO getCustomerStatement(Long clientId, LocalDate startDate, LocalDate endDate) {
        StringBuilder sb = new StringBuilder("/api/comercial/statements/customer?clientId=").append(clientId);
        if (startDate != null) sb.append("&startDate=").append(startDate);
        if (endDate != null) sb.append("&endDate=").append(endDate);
        return clientFactory.authenticatedClient().get(sb.toString(), CustomerStatementDTO.class);
    }

    public byte[] getCustomerStatementPdf(Long clientId, LocalDate startDate, LocalDate endDate) {
        StringBuilder sb = new StringBuilder("/api/comercial/statements/customer/pdf?clientId=").append(clientId);
        if (startDate != null) sb.append("&startDate=").append(startDate);
        if (endDate != null) sb.append("&endDate=").append(endDate);
        return clientFactory.authenticatedClient().getBytes(sb.toString());
    }

    public EmailDispatchResultDTO sendCustomerStatementEmail(SendStatementEmailRequest request) {
        return clientFactory.authenticatedClient()
                .post("/api/comercial/statements/customer/email", request, EmailDispatchResultDTO.class);
    }

    public SupplierStatementDTO getSupplierStatement(Long supplierId, LocalDate startDate, LocalDate endDate) {
        StringBuilder sb = new StringBuilder("/api/purchases/statements/supplier?supplierId=").append(supplierId);
        if (startDate != null) sb.append("&startDate=").append(startDate);
        if (endDate != null) sb.append("&endDate=").append(endDate);
        return clientFactory.authenticatedClient().get(sb.toString(), SupplierStatementDTO.class);
    }

    public byte[] getSupplierStatementPdf(Long supplierId, LocalDate startDate, LocalDate endDate) {
        StringBuilder sb = new StringBuilder("/api/purchases/statements/supplier/pdf?supplierId=").append(supplierId);
        if (startDate != null) sb.append("&startDate=").append(startDate);
        if (endDate != null) sb.append("&endDate=").append(endDate);
        return clientFactory.authenticatedClient().getBytes(sb.toString());
    }
}
