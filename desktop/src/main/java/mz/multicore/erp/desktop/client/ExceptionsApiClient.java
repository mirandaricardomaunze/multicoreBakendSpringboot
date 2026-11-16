package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.exceptions.dto.ExceptionReportDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/** HTTP client for the anti-fraud exception report ({@code /api/exceptions}, SA-C21). */
@Component
@Profile("desktop")
public class ExceptionsApiClient {

    private final DesktopClientFactory clientFactory;

    public ExceptionsApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public ExceptionReportDTO report(Long companyId, LocalDate from, LocalDate to) {
        return clientFactory.authenticatedClient().get("/api/exceptions?companyId=" + companyId + "&from=" + from + "&to=" + to,
                ExceptionReportDTO.class);
    }

    public byte[] pdf(Long companyId, LocalDate from, LocalDate to) {
        return clientFactory.authenticatedClient().getBytes("/api/exceptions/pdf?companyId=" + companyId + "&from=" + from + "&to=" + to);
    }
}
