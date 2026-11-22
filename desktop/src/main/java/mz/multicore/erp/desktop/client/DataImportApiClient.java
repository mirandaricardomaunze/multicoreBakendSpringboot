package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.dataimport.dto.ImportCommitResultDTO;
import mz.multicore.erp.modules.dataimport.dto.ImportPreviewDTO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Cliente HTTP da importação de cadastros por CSV ({@code /api/data-import}, IMPORTACAO_DADOS_SPEC §3.1).
 * {@code entity} é {@code clients}, {@code suppliers} ou {@code products}.
 */
@Component
@Profile("desktop")
public class DataImportApiClient {

    private static final String BASE = "/api/data-import/";

    private final DesktopClientFactory clientFactory;

    public DataImportApiClient(DesktopClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    /** Modelo CSV da entidade (UTF-8 com BOM, {@code ;}, só cabeçalho). */
    public byte[] template(String entity) {
        return clientFactory.authenticatedClient().getBytes(BASE + entity + "/template", "text/csv");
    }

    /** Classifica cada linha do ficheiro sem gravar nada. */
    public ImportPreviewDTO preview(String entity, byte[] content) {
        return clientFactory.authenticatedClient().postBytes(BASE + entity + "/preview", content, ImportPreviewDTO.class);
    }

    /** Revalida e grava tudo numa transacção; com qualquer erro o servidor recusa e nada é gravado. */
    public ImportCommitResultDTO commit(String entity, String fileName, byte[] content) {
        String query = fileName == null ? "" : "?fileName=" + URLEncoder.encode(fileName, StandardCharsets.UTF_8);
        return clientFactory.authenticatedClient()
                .postBytes(BASE + entity + "/commit" + query, content, ImportCommitResultDTO.class);
    }
}
