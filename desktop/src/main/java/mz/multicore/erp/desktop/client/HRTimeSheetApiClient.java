package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.hr.dto.*;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente HTTP especializado para assiduidade, folha de ponto, faltas e férias.
 */
@Component
@Profile("desktop")
public class HRTimeSheetApiClient {

    private final DesktopClientFactory clientFactory;

    public HRTimeSheetApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public TimeSheetDTO getMonthlySheet(int year, int month) {
        return clientFactory.authenticatedClient()
                .get("/api/hr/timesheet/" + year + "/" + month, TimeSheetDTO.class);
    }

    public List<TimeEntryDTO> getTimeEntries(int year, int month) {
        return clientFactory.authenticatedClient()
                .getList("/api/hr/timesheet/" + year + "/" + month + "/entries", TimeEntryDTO.class);
    }

    public TimeEntryDTO recordTimeEntry(CreateTimeEntryRequest request) {
        return clientFactory.authenticatedClient()
                .post("/api/hr/timesheet/entries", request, TimeEntryDTO.class);
    }

    public void deleteTimeEntry(Long id) {
        clientFactory.authenticatedClient().delete("/api/hr/timesheet/entries/" + id);
    }

    public TimeSheetDTO closeTimeSheet(int year, int month) {
        return clientFactory.authenticatedClient()
                .post("/api/hr/timesheet/" + year + "/" + month + "/close", null, TimeSheetDTO.class);
    }

    public TimeSheetDTO reopenTimeSheet(int year, int month, String reason) {
        String query = "?reason=" + URLEncoder.encode(reason, StandardCharsets.UTF_8);
        return clientFactory.authenticatedClient()
                .post("/api/hr/timesheet/" + year + "/" + month + "/reopen" + query, null, TimeSheetDTO.class);
    }

    public List<AbsenceDTO> getAllAbsences() {
        return clientFactory.authenticatedClient().getList("/api/hr/absences", AbsenceDTO.class);
    }

    public AbsenceDTO recordAbsence(CreateAbsenceRequest request) {
        return clientFactory.authenticatedClient().post("/api/hr/absences", request, AbsenceDTO.class);
    }

    public void deleteAbsence(Long id) {
        clientFactory.authenticatedClient().delete("/api/hr/absences/" + id);
    }

    public AbsenceDTO justifyAbsence(Long id, String absenceType, String reason, boolean hasDocument) {
        String query = "?absenceType=" + URLEncoder.encode(absenceType, StandardCharsets.UTF_8)
                + "&reason=" + URLEncoder.encode(reason, StandardCharsets.UTF_8)
                + "&hasDocument=" + hasDocument;
        return clientFactory.authenticatedClient()
                .post("/api/hr/absences/" + id + "/justify" + query, null, AbsenceDTO.class);
    }

    public List<VacationDTO> getAllVacations() {
        return clientFactory.authenticatedClient().getList("/api/hr/vacations", VacationDTO.class);
    }

    public VacationDTO submitVacation(CreateVacationRequest request) {
        return clientFactory.authenticatedClient().post("/api/hr/vacations", request, VacationDTO.class);
    }

    public VacationDTO decideVacation(Long id, boolean approve, String rejectionReason) {
        Map<String, Object> body = new HashMap<>();
        body.put("approve", approve);
        body.put("rejectionReason", rejectionReason);
        return clientFactory.authenticatedClient()
                .post("/api/hr/vacations/" + id + "/decide", body, VacationDTO.class);
    }
}
