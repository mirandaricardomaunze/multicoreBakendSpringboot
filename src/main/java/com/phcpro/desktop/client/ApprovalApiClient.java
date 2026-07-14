package com.phcpro.desktop.client;

import com.phcpro.modules.approvals.dto.ApprovalActionDTO;
import com.phcpro.modules.approvals.dto.ApprovalRequestDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Cliente HTTP para a fila de aprovações ({@code /api/approvals}). Espelha o padrão do
 * {@link ComercialApiClient}: métodos tipados sobre o {@link DesktopClientFactory}, que
 * anexa o token e o {@code X-Company-Id} da sessão activa a cada pedido.
 */
@Component
@Profile("desktop")
public class ApprovalApiClient {

    private final DesktopClientFactory clientFactory;

    public ApprovalApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<ApprovalRequestDTO> getPendingRequests() {
        return clientFactory.authenticatedClient().getList("/api/approvals/pending", ApprovalRequestDTO.class);
    }

    public List<ApprovalRequestDTO> getAllRequests() {
        return clientFactory.authenticatedClient().getList("/api/approvals", ApprovalRequestDTO.class);
    }

    public ApprovalRequestDTO approveRequest(Long id, String comments) {
        return clientFactory.authenticatedClient().post(
                "/api/approvals/" + id + "/approve", new ApprovalActionDTO(comments), ApprovalRequestDTO.class);
    }

    public ApprovalRequestDTO rejectRequest(Long id, String reason) {
        return clientFactory.authenticatedClient().post(
                "/api/approvals/" + id + "/reject", new ApprovalActionDTO(reason), ApprovalRequestDTO.class);
    }
}
