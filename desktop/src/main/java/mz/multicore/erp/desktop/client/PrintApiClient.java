package mz.multicore.erp.desktop.client;

import mz.multicore.erp.modules.comercial.dto.ReprintAuthorizationRequest;
import mz.multicore.erp.modules.printing.dto.DocumentPrintStatusDTO;
import mz.multicore.erp.modules.printing.dto.SensitiveDocumentType;
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

    /** Emissão do Mapa Oficial de Amortizações e Reintegrações (Modelo 22 Anexo D - Moçambique). */
    public byte[] renderAssetDepreciationMap(Long companyId, Integer fiscalYear) {
        String url = "/api/print/asset-depreciation-map?companyId=" + companyId
                + (fiscalYear != null ? "&fiscalYear=" + fiscalYear : "");
        return clientFactory.authenticatedClient().getBytes(url);
    }

    /** Emissão do Livro de Inventário Valorizado de Fim de Exercício (Código Comercial e PGC-NIRF). */
    public byte[] renderValuedInventoryBook(Long companyId, Integer fiscalYear) {
        String url = "/api/print/valued-inventory-book?companyId=" + companyId
                + (fiscalYear != null ? "&fiscalYear=" + fiscalYear : "");
        return clientFactory.authenticatedClient().getBytes(url);
    }

    /** Consultar antes de imprimir; nunca chamar no EDT. */
    public DocumentPrintStatusDTO getPrintStatus(SensitiveDocumentType type, Long documentId) {
        return clientFactory.authenticatedClient().get(
                "/api/print/status/" + type.name() + "/" + documentId,
                DocumentPrintStatusDTO.class);
    }

    /** Estado de uma página de documentos, usado pelas colunas de rastreabilidade. */
    public java.util.List<DocumentPrintStatusDTO> getPrintStatuses(
            SensitiveDocumentType type, java.util.List<Long> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) return java.util.List.of();
        String ids = documentIds.stream().map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(","));
        return clientFactory.authenticatedClient().getList(
                "/api/print/status/" + type.name() + "?ids=" + ids,
                DocumentPrintStatusDTO.class);
    }

    /** Reimpressão autorizada; as credenciais são usadas pelo servidor e nunca persistidas. */
    public byte[] reprint(SensitiveDocumentType type, Long documentId,
                          ReprintAuthorizationRequest request) {
        return clientFactory.authenticatedClient().postForBytes(
                "/api/print/" + type.name() + "/" + documentId + "/reprint", request);
    }
}
