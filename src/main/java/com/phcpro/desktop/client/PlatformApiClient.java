package com.phcpro.desktop.client;

import com.phcpro.modules.platform.dto.CreateCompanyRequest;
import com.phcpro.modules.platform.dto.CreatePlatformUserRequest;
import com.phcpro.modules.platform.dto.GrantAccessRequest;
import com.phcpro.modules.platform.dto.PlatformCompanyDTO;
import com.phcpro.modules.platform.dto.PlatformUserDTO;
import com.phcpro.modules.platform.dto.UpdateCompanyRequest;
import com.phcpro.modules.subscription.dto.RecordPaymentRequest;
import com.phcpro.modules.subscription.dto.SaveSubscriptionRequest;
import com.phcpro.modules.subscription.dto.SubscriptionDTO;
import com.phcpro.modules.subscription.dto.SubscriptionPaymentDTO;
import com.phcpro.modules.support.dto.SupportMessageDTO;
import com.phcpro.modules.support.dto.SupportTicketDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Cliente HTTP para a consola do superadmin ({@code /api/platform/**}). Colapsa num só cliente os 4
 * serviços que o {@code PlataformaPanel} usava em processo: empresas, utilizadores globais,
 * assinaturas/pagamentos e assistência. Espelha o padrão do {@link ComercialApiClient}.
 */
@Component
@Profile("desktop")
public class PlatformApiClient {

    private final DesktopClientFactory clientFactory;

    public PlatformApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    // ─── Empresas ─────────────────────────────────────────────────────────────
    public List<PlatformCompanyDTO> listCompanies() {
        return clientFactory.authenticatedClient().getList("/api/platform/companies", PlatformCompanyDTO.class);
    }

    /** Que versão do programa cada empresa está a usar — a lista a olhar antes de subir a mínima. */
    public List<com.phcpro.architecture.version.ClientVersionUsageDTO> listClientVersions() {
        return clientFactory.authenticatedClient().getList("/api/platform/client-versions",
                com.phcpro.architecture.version.ClientVersionUsageDTO.class);
    }

    public PlatformCompanyDTO createCompany(CreateCompanyRequest request) {
        return clientFactory.authenticatedClient().post("/api/platform/companies", request, PlatformCompanyDTO.class);
    }

    public PlatformCompanyDTO updateCompany(Long id, UpdateCompanyRequest request) {
        return clientFactory.authenticatedClient().put("/api/platform/companies/" + id, request, PlatformCompanyDTO.class);
    }

    public PlatformCompanyDTO setCompanyActive(Long id, boolean active) {
        return clientFactory.authenticatedClient()
                .patch("/api/platform/companies/" + id + "/active", Map.of("active", active), PlatformCompanyDTO.class);
    }

    /** Envia o logótipo (imagem já reduzida) da empresa como octet-stream. */
    public void updateCompanyLogo(Long id, byte[] logo) {
        clientFactory.authenticatedClient().postBytes("/api/platform/companies/" + id + "/logo", logo);
    }

    // ─── Utilizadores globais ─────────────────────────────────────────────────
    public List<PlatformUserDTO> listUsers() {
        return clientFactory.authenticatedClient().getList("/api/platform/users", PlatformUserDTO.class);
    }

    public PlatformUserDTO createUser(CreatePlatformUserRequest request) {
        return clientFactory.authenticatedClient().post("/api/platform/users", request, PlatformUserDTO.class);
    }

    public PlatformUserDTO updateUser(String username, String name) {
        return clientFactory.authenticatedClient()
                .put("/api/platform/users/" + username, Map.of("name", name), PlatformUserDTO.class);
    }

    public PlatformUserDTO setUserActive(String username, boolean active) {
        return clientFactory.authenticatedClient()
                .patch("/api/platform/users/" + username + "/active", Map.of("active", active), PlatformUserDTO.class);
    }

    public void resetPassword(String username, String password) {
        clientFactory.authenticatedClient()
                .patch("/api/platform/users/" + username + "/password", Map.of("password", password), Void.class);
    }

    public PlatformUserDTO grantAccess(String username, GrantAccessRequest request) {
        return clientFactory.authenticatedClient()
                .post("/api/platform/users/" + username + "/access", request, PlatformUserDTO.class);
    }

    public void revokeAccess(String username, Long companyId) {
        clientFactory.authenticatedClient().delete("/api/platform/users/" + username + "/access/" + companyId);
    }

    // ─── Assinaturas & pagamentos ─────────────────────────────────────────────
    public List<SubscriptionDTO> listOverview() {
        return clientFactory.authenticatedClient().getList("/api/platform/subscriptions", SubscriptionDTO.class);
    }

    public SubscriptionDTO saveSubscription(Long companyId, SaveSubscriptionRequest request) {
        return clientFactory.authenticatedClient()
                .put("/api/platform/subscriptions/" + companyId, request, SubscriptionDTO.class);
    }

    public SubscriptionDTO changeSubscriptionStatus(Long companyId, String status) {
        return clientFactory.authenticatedClient().patch(
                "/api/platform/subscriptions/" + companyId + "/status", Map.of("status", status), SubscriptionDTO.class);
    }

    public List<SubscriptionPaymentDTO> listPayments(Long companyId) {
        return clientFactory.authenticatedClient()
                .getList("/api/platform/subscriptions/" + companyId + "/payments", SubscriptionPaymentDTO.class);
    }

    public SubscriptionPaymentDTO recordPayment(Long companyId, RecordPaymentRequest request) {
        return clientFactory.authenticatedClient()
                .post("/api/platform/subscriptions/" + companyId + "/payments", request, SubscriptionPaymentDTO.class);
    }

    public List<String> planOptions() {
        return clientFactory.authenticatedClient().getList("/api/platform/subscriptions/plan-options", String.class);
    }

    public List<String> methodOptions() {
        return clientFactory.authenticatedClient().getList("/api/platform/subscriptions/method-options", String.class);
    }

    // ─── Assistência (lado superadmin) ────────────────────────────────────────
    public List<SupportTicketDTO> listAllTickets() {
        return clientFactory.authenticatedClient().getList("/api/platform/support/tickets", SupportTicketDTO.class);
    }

    public List<SupportMessageDTO> listPlatformMessages(Long id) {
        return clientFactory.authenticatedClient()
                .getList("/api/platform/support/tickets/" + id + "/messages", SupportMessageDTO.class);
    }

    public SupportTicketDTO addSuperAdminReply(Long id, String body) {
        return clientFactory.authenticatedClient().post(
                "/api/platform/support/tickets/" + id + "/messages", Map.of("body", body), SupportTicketDTO.class);
    }

    public SupportTicketDTO changeTicketStatus(Long id, String status) {
        return clientFactory.authenticatedClient().patch(
                "/api/platform/support/tickets/" + id + "/status", Map.of("status", status), SupportTicketDTO.class);
    }

    public List<String> statusOptions() {
        return clientFactory.authenticatedClient().getList("/api/platform/support/tickets/status-options", String.class);
    }
}
