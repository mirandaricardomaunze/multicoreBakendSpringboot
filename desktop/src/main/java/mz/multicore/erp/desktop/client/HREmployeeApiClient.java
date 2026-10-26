package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.hr.dto.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Cliente HTTP especializado para gestão de funcionários, medicina no trabalho e documentos pessoais.
 */
@Component
@Profile("desktop")
public class HREmployeeApiClient {

    private final DesktopClientFactory clientFactory;

    public HREmployeeApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<EmployeeDTO> getAllEmployees() {
        return clientFactory.authenticatedClient().getList("/api/hr/employees", EmployeeDTO.class);
    }

    public EmployeeDTO createEmployee(UpsertEmployeeRequest request) {
        return clientFactory.authenticatedClient().post("/api/hr/employees", request, EmployeeDTO.class);
    }

    public EmployeeDTO updateEmployee(Long id, UpsertEmployeeRequest request) {
        return clientFactory.authenticatedClient().put("/api/hr/employees/" + id, request, EmployeeDTO.class);
    }

    public EmployeeDTO changeEmployeeStatus(Long id, String status) {
        return clientFactory.authenticatedClient()
                .post("/api/hr/employees/" + id + "/status", Map.of("status", status), EmployeeDTO.class);
    }

    public OccupationalHealthSummaryDTO getOccupationalHealthSummary(Long employeeId) {
        return clientFactory.authenticatedClient().get(
                "/api/hr/occupational-health/employee/" + employeeId + "/summary", OccupationalHealthSummaryDTO.class);
    }

    public List<OccupationalHealthExamDTO> getOccupationalHealthHistory(Long employeeId) {
        return clientFactory.authenticatedClient().getList(
                "/api/hr/occupational-health/employee/" + employeeId, OccupationalHealthExamDTO.class);
    }

    public List<OccupationalHealthExamDTO> getExpiringOccupationalHealthExams() {
        return clientFactory.authenticatedClient().getList(
                "/api/hr/occupational-health/expiring", OccupationalHealthExamDTO.class);
    }

    public OccupationalHealthExamDTO registerOccupationalHealthExam(SaveOccupationalHealthExamRequest request) {
        return clientFactory.authenticatedClient().post(
                "/api/hr/occupational-health", request, OccupationalHealthExamDTO.class);
    }

    public List<HealthProviderDTO> getHealthProviders() {
        return clientFactory.authenticatedClient().getList(
                "/api/hr/occupational-health/providers", HealthProviderDTO.class);
    }

    public List<MissingHealthExamDTO> getEmployeesMissingHealthExam() {
        return clientFactory.authenticatedClient().getList(
                "/api/hr/occupational-health/missing", MissingHealthExamDTO.class);
    }

    public OccupationalHealthCostDTO getOccupationalHealthCosts(LocalDate from, LocalDate to) {
        return clientFactory.authenticatedClient().get(
                "/api/hr/occupational-health/costs?from=" + from + "&to=" + to, OccupationalHealthCostDTO.class);
    }

    public OccupationalHealthAttachmentDTO getOccupationalHealthAttachment(Long examId) {
        return clientFactory.authenticatedClient().get(
                "/api/hr/occupational-health/exam/" + examId + "/attachment", OccupationalHealthAttachmentDTO.class);
    }

    public OccupationalHealthExamDTO payOccupationalHealthExam(Long examId) {
        return clientFactory.authenticatedClient().post(
                "/api/hr/occupational-health/" + examId + "/pay", null, OccupationalHealthExamDTO.class);
    }

    public List<EmployeeDocumentDTO> getEmployeeDocuments(Long employeeId) {
        return clientFactory.authenticatedClient().getList(
                "/api/hr/employee-documents/employee/" + employeeId, EmployeeDocumentDTO.class);
    }

    public List<EmployeeDocumentDTO> getExpiringEmployeeDocuments() {
        return clientFactory.authenticatedClient().getList(
                "/api/hr/employee-documents/expiring", EmployeeDocumentDTO.class);
    }

    public EmployeeDocumentDTO saveEmployeeDocument(SaveEmployeeDocumentRequest request) {
        return clientFactory.authenticatedClient().post("/api/hr/employee-documents", request, EmployeeDocumentDTO.class);
    }

    public void deleteEmployeeDocument(Long id) {
        clientFactory.authenticatedClient().delete("/api/hr/employee-documents/" + id);
    }
}
