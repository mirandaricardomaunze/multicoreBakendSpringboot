package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.approvals.dto.ApprovalRequestDTO;
import mz.multicore.erp.modules.comercial.dto.ClientCreditRiskDTO;
import mz.multicore.erp.modules.comercial.dto.CreditExceptionApprovalRequest;
import mz.multicore.erp.modules.comercial.dto.CreditRiskSummaryDTO;
import mz.multicore.erp.modules.comercial.dto.DebtCollectionNoticeDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@Profile("desktop")
public class CreditRiskApiClient {

    private final DesktopClientFactory clientFactory;

    public CreditRiskApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public CreditRiskSummaryDTO getSummary(LocalDate referenceDate) {
        String path = "/api/comercial/credit-risk/summary";
        if (referenceDate != null) {
            path += "?reference=" + referenceDate;
        }
        return clientFactory.authenticatedClient().get(path, CreditRiskSummaryDTO.class);
    }

    public List<ClientCreditRiskDTO> getClientsRisk(LocalDate referenceDate) {
        String path = "/api/comercial/credit-risk/clients";
        if (referenceDate != null) {
            path += "?reference=" + referenceDate;
        }
        return clientFactory.authenticatedClient().getList(path, ClientCreditRiskDTO.class);
    }

    public DebtCollectionNoticeDTO getNotice(Long clientId, LocalDate referenceDate) {
        String path = "/api/comercial/credit-risk/notice?clientId=" + clientId;
        if (referenceDate != null) {
            path += "&reference=" + referenceDate;
        }
        return clientFactory.authenticatedClient().get(path, DebtCollectionNoticeDTO.class);
    }

    public byte[] getNoticePdf(Long clientId, LocalDate referenceDate) {
        String path = "/api/comercial/credit-risk/notice/pdf?clientId=" + clientId;
        if (referenceDate != null) {
            path += "&reference=" + referenceDate;
        }
        return clientFactory.authenticatedClient().getBytes(path);
    }

    public ApprovalRequestDTO requestCreditException(CreditExceptionApprovalRequest request) {
        return clientFactory.authenticatedClient()
                .post("/api/comercial/credit-risk/exception-request", request, ApprovalRequestDTO.class);
    }
}
