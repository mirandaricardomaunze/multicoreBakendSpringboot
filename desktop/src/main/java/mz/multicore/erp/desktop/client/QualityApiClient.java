package mz.multicore.erp.desktop.client;

import mz.multicore.erp.architecture.paging.PageResponse;
import mz.multicore.erp.modules.quality.dto.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * Cliente HTTP Desktop para o módulo de Qualidade e Segurança de Equipamentos (/api/quality).
 */
@Component
@Profile("desktop")
public class QualityApiClient {

    private final DesktopClientFactory clientFactory;

    public QualityApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public PageResponse<EquipmentDTO> searchEquipments(Long companyId, String search, int page, int size) {
        String path = "/api/quality/equipments?companyId=" + companyId + "&page=" + page + "&size=" + size;
        if (search != null && !search.isBlank()) {
            path += "&search=" + encode(search.trim());
        }
        return clientFactory.authenticatedClient()
                .getGeneric(path, PageResponse.class, EquipmentDTO.class);
    }

    public List<EquipmentDTO> listActiveEquipments(Long companyId) {
        return clientFactory.authenticatedClient()
                .getList("/api/quality/equipments/active?companyId=" + companyId, EquipmentDTO.class);
    }

    public EquipmentDTO createEquipment(EquipmentCreateRequest req) {
        return clientFactory.authenticatedClient()
                .post("/api/quality/equipments", req, EquipmentDTO.class);
    }

    public EquipmentDTO updateEquipment(Long id, EquipmentCreateRequest req) {
        return clientFactory.authenticatedClient()
                .put("/api/quality/equipments/" + id, req, EquipmentDTO.class);
    }

    public PageResponse<WorkerEquipmentAssignmentDTO> searchAssignments(
            Long companyId,
            AssignmentStatus status,
            Long employeeId,
            String search,
            int page,
            int size
    ) {
        StringBuilder path = new StringBuilder("/api/quality/assignments?companyId=").append(companyId)
                .append("&page=").append(page)
                .append("&size=").append(size);
        if (status != null) {
            path.append("&status=").append(status.name());
        }
        if (employeeId != null) {
            path.append("&employeeId=").append(employeeId);
        }
        if (search != null && !search.isBlank()) {
            path.append("&search=").append(encode(search.trim()));
        }
        return clientFactory.authenticatedClient()
                .getGeneric(path.toString(), PageResponse.class, WorkerEquipmentAssignmentDTO.class);
    }

    public WorkerEquipmentAssignmentDTO assignEquipment(EquipmentAssignmentRequest req) {
        return clientFactory.authenticatedClient()
                .post("/api/quality/assignments", req, WorkerEquipmentAssignmentDTO.class);
    }

    public WorkerEquipmentAssignmentDTO renewAssignment(Long id, Long companyId, LocalDate newAssignedDate, String notes) {
        StringBuilder path = new StringBuilder("/api/quality/assignments/").append(id)
                .append("/renew?companyId=").append(companyId);
        if (newAssignedDate != null) {
            path.append("&newAssignedDate=").append(newAssignedDate);
        }
        if (notes != null && !notes.isBlank()) {
            path.append("&notes=").append(encode(notes.trim()));
        }
        return clientFactory.authenticatedClient()
                .post(path.toString(), null, WorkerEquipmentAssignmentDTO.class);
    }

    public void returnAssignment(Long id, Long companyId, String notes) {
        StringBuilder path = new StringBuilder("/api/quality/assignments/").append(id)
                .append("/return?companyId=").append(companyId);
        if (notes != null && !notes.isBlank()) {
            path.append("&notes=").append(encode(notes.trim()));
        }
        clientFactory.authenticatedClient().post(path.toString(), null, Void.class);
    }

    public List<ExpiredWorkerAlertDTO> getExpiredWorkerAlerts(Long companyId) {
        return clientFactory.authenticatedClient()
                .getList("/api/quality/expired-workers?companyId=" + companyId, ExpiredWorkerAlertDTO.class);
    }

    public QualityKpiDTO getQualityKpis(Long companyId) {
        return clientFactory.authenticatedClient()
                .get("/api/quality/kpis?companyId=" + companyId, QualityKpiDTO.class);
    }

    public byte[] getDeliverySheetPdf(Long assignmentId, Long companyId) {
        String path = "/api/quality/assignments/" + assignmentId + "/delivery-sheet"
                + (companyId != null ? "?companyId=" + companyId : "");
        return clientFactory.authenticatedClient().getBytes(path);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
