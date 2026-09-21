package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.printing.dto.TableExportRequest;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Cliente HTTP das impressões genéricas ({@code /api/print}).
 *
 * <p>Hoje serve a exportação de listagens: o desktop manda o que está no ecrã e recebe o PDF já
 * com o cabeçalho da empresa — que só o servidor conhece.</p>
 */
@Component
@Profile("desktop")
public class PrintApiClient {

    private final DesktopClientFactory clientFactory;

    public PrintApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    /** Chamar <b>fora do EDT</b>: é rede. */
    public byte[] renderTable(Long companyId, TableExportRequest request) {
        return clientFactory.authenticatedClient()
                .postForBytes("/api/print/table?companyId=" + companyId, request);
    }
}
