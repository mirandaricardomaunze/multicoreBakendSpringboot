package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.projects.dto.CustomerProjectDTO;
import mz.multicore.erp.modules.projects.dto.ProjectStatementDTO;
import mz.multicore.erp.modules.projects.dto.SaveCustomerProjectRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

/** HTTP client for client projects or construction sites ({@code /api/projects}, SA-C09). */
@Component
@Profile("desktop")
public class ProjectsApiClient {

    private final DesktopClientFactory clientFactory;

    public ProjectsApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<CustomerProjectDTO> list(Long companyId, Long clientId) {
        return clientFactory.authenticatedClient().getList("/api/projects?companyId=" + companyId
                + (clientId == null ? "" : "&clientId=" + clientId), CustomerProjectDTO.class);
    }

    public CustomerProjectDTO save(Long companyId, Long clientId, String name) {
        return clientFactory.authenticatedClient().put("/api/projects",
                new SaveCustomerProjectRequest(null, companyId, clientId, name, null, null, true), CustomerProjectDTO.class);
    }

    public CustomerProjectDTO save(SaveCustomerProjectRequest request) {
        return clientFactory.authenticatedClient().put("/api/projects", request, CustomerProjectDTO.class);
    }

    public CustomerProjectDTO attach(Long companyId, Long projectId, Long orderId) {
        return clientFactory.authenticatedClient().put("/api/projects/" + projectId + "/orders/" + orderId
                + "?companyId=" + companyId, null, CustomerProjectDTO.class);
    }

    /** Project of an order, or {@code null} when the order has none (the server answers 204). */
    public CustomerProjectDTO projectOfOrder(Long companyId, Long orderId) {
        return clientFactory.authenticatedClient().get("/api/projects/orders/" + orderId + "?companyId=" + companyId,
                CustomerProjectDTO.class);
    }

    public ProjectStatementDTO statement(Long companyId, Long projectId) {
        return clientFactory.authenticatedClient().get("/api/projects/" + projectId + "/statement?companyId=" + companyId,
                ProjectStatementDTO.class);
    }
}
