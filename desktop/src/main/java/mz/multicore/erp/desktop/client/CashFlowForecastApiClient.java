package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.financeira.dto.CashFlowForecastDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("desktop")
public class CashFlowForecastApiClient {

    private final DesktopClientFactory clientFactory;

    public CashFlowForecastApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public CashFlowForecastDTO getForecast() {
        return clientFactory.authenticatedClient().get("/api/finance/forecast", CashFlowForecastDTO.class);
    }

    public byte[] getForecastPdf() {
        return clientFactory.authenticatedClient().getBytes("/api/finance/forecast/pdf");
    }
}
