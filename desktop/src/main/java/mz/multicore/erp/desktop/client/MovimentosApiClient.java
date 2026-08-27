package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.movimentos.dto.MovimentoDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/** Cliente HTTP para o mapa unificado de movimentos comerciais ({@code /api/movimentos}). */
@Component
@Profile("desktop")
public class MovimentosApiClient {

    private final DesktopClientFactory clientFactory;

    public MovimentosApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    public List<MovimentoDTO> listar(Long companyId, String query, LocalDate from, LocalDate to) {
        StringBuilder path = new StringBuilder("/api/movimentos?companyId=").append(companyId);
        if (query != null && !query.isBlank()) {
            path.append("&query=").append(URLEncoder.encode(query, StandardCharsets.UTF_8));
        }
        if (from != null) path.append("&from=").append(from);
        if (to != null) path.append("&to=").append(to);
        return clientFactory.authenticatedClient().getList(path.toString(), MovimentoDTO.class);
    }
}
