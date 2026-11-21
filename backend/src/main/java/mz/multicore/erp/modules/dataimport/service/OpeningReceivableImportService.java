package mz.multicore.erp.modules.dataimport.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.CreateOpeningReceivableRequest;
import mz.multicore.erp.modules.comercial.model.Client;
import mz.multicore.erp.modules.comercial.service.ClientLookup;
import mz.multicore.erp.modules.comercial.service.InvoiceLookup;
import mz.multicore.erp.modules.dataimport.dto.ImportPreviewDTO;
import mz.multicore.erp.modules.dataimport.dto.ImportRowDTO;
import mz.multicore.erp.modules.dataimport.dto.ImportRowStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Importação das dívidas de clientes por CSV (IMPORTACAO_DADOS_SPEC §5.2): cada linha é uma factura em aberto do
 * sistema antigo, que a confirmação grava como um documento "Saldo Inicial" (série SI). O cliente é o do NUIT na
 * empresa activa. A chave é cliente + {@code numero_original}: já importada → {@code JÁ EXISTE}; repetida no ficheiro
 * → {@code ERRO} em ambas (§3.3).
 */
@Service
public class OpeningReceivableImportService {

    static final String CLIENT_TAX_ID = "nuit_cliente";
    static final String ORIGINAL_NUMBER = "numero_original";
    static final String DATE = "data";
    static final String DUE_DATE = "vencimento";
    static final String AMOUNT = "valor_em_divida";
    static final String DESCRIPTION = "descricao";

    static final List<String> REQUIRED_COLUMNS = List.of(CLIENT_TAX_ID, ORIGINAL_NUMBER, DATE, DUE_DATE, AMOUNT);
    static final List<String> OPTIONAL_COLUMNS = List.of(DESCRIPTION);

    private final CsvImportReader reader = new CsvImportReader();
    private final ImportValueParser parser = new ImportValueParser();
    private final ClientLookup clientLookup;
    private final InvoiceLookup invoiceLookup;

    public OpeningReceivableImportService(ClientLookup clientLookup, InvoiceLookup invoiceLookup) {
        this.clientLookup = clientLookup;
        this.invoiceLookup = invoiceLookup;
    }

    /** Lê o ficheiro e classifica cada linha na empresa activa, sem gravar nada (§3.1 passo 2). */
    public ImportPreviewDTO preview(byte[] content) {
        return plan(content).preview();
    }

    /** Classificação com o pedido de cada linha válida; base da pré-visualização e da confirmação. */
    ImportPlan<CreateOpeningReceivableRequest> plan(byte[] content) {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        CsvTable table = reader.read(content, REQUIRED_COLUMNS, OPTIONAL_COLUMNS);
        Map<String, Optional<Client>> clientsByTaxId = new HashMap<>();
        List<ImportPlan.Line<CreateOpeningReceivableRequest>> converted = table.rows().stream()
                .map(row -> convert(table, row, companyId, clientsByTaxId))
                .toList();
        DuplicateKeys duplicates = DuplicateKeys.ofLines(converted, OpeningReceivableImportService::key);
        List<ImportPlan.Line<CreateOpeningReceivableRequest>> lines = converted.stream()
                .map(line -> line.item() == null ? line : classify(line, duplicates, companyId))
                .toList();
        return new ImportPlan<>(lines, table.header().extraColumns());
    }

    private ImportPlan.Line<CreateOpeningReceivableRequest> classify(
            ImportPlan.Line<CreateOpeningReceivableRequest> line, DuplicateKeys duplicates, Long companyId) {
        int number = line.row().lineNumber();
        CreateOpeningReceivableRequest request = line.item();
        return duplicates.violation(number, key(request), ORIGINAL_NUMBER, "Cliente e número original repetidos")
                .map(ImportPlan.Line::<CreateOpeningReceivableRequest>error)
                .orElseGet(() -> new ImportPlan.Line<>(ImportRowDTO.of(number, statusOf(request, companyId)), request));
    }

    private ImportRowStatus statusOf(CreateOpeningReceivableRequest request, Long companyId) {
        return invoiceLookup.existsOpeningReceivable(companyId, request.clientId(), request.originalNumber())
                ? ImportRowStatus.JA_EXISTE
                : ImportRowStatus.NOVO;
    }

    /** Chave cliente + número original (§5.2). */
    private static String key(CreateOpeningReceivableRequest request) {
        return request.clientId() + "|" + request.originalNumber();
    }

    /** Valores pela ordem das colunas do modelo: o primeiro problema da linha é o que se reporta. */
    private ImportPlan.Line<CreateOpeningReceivableRequest> convert(CsvTable table, CsvTable.Row row, Long companyId,
                                                                    Map<String, Optional<Client>> clientsByTaxId) {
        try {
            return new ImportPlan.Line<>(ImportRowDTO.of(row.number(), ImportRowStatus.NOVO),
                    toRequest(table, row, companyId, clientsByTaxId));
        } catch (ImportValueException invalidValue) {
            return ImportPlan.Line.error(
                    ImportRowDTO.error(row.number(), invalidValue.getColumn(), invalidValue.getMessage()));
        }
    }

    private CreateOpeningReceivableRequest toRequest(CsvTable table, CsvTable.Row row, Long companyId,
                                                     Map<String, Optional<Client>> clientsByTaxId) {
        int line = row.number();
        Client client = client(parser.requireText(table.value(row, CLIENT_TAX_ID), line, CLIENT_TAX_ID),
                companyId, clientsByTaxId, line);
        String originalNumber = parser.requireText(table.value(row, ORIGINAL_NUMBER), line, ORIGINAL_NUMBER);
        LocalDate date = requireDate(table.value(row, DATE), line, DATE);
        LocalDate dueDate = requireDate(table.value(row, DUE_DATE), line, DUE_DATE);
        BigDecimal amount = parser.requireDecimal(table.value(row, AMOUNT), line, AMOUNT);
        return new CreateOpeningReceivableRequest(client.getId(), originalNumber, date, dueDate, amount,
                parser.parseText(table.value(row, DESCRIPTION)));
    }

    private Client client(String taxId, Long companyId, Map<String, Optional<Client>> clientsByTaxId, int line) {
        return clientsByTaxId
                .computeIfAbsent(taxId, key -> clientLookup.findForCompanyByTaxId(key, companyId))
                .orElseThrow(() -> new ImportValueException(line, CLIENT_TAX_ID, "o cliente com NUIT «" + taxId
                        + "» não existe na empresa; importe primeiro os clientes."));
    }

    private LocalDate requireDate(String raw, int line, String column) {
        return parser.parseDate(parser.requireText(raw, line, column), line, column);
    }
}
