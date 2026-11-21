package mz.multicore.erp.modules.dataimport.service;

import jakarta.validation.Validator;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.SaveClientRequest;
import mz.multicore.erp.modules.comercial.service.ClientLookup;
import mz.multicore.erp.modules.dataimport.dto.ImportPreviewDTO;
import mz.multicore.erp.modules.dataimport.dto.ImportRowDTO;
import mz.multicore.erp.modules.dataimport.dto.ImportRowStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Importação de clientes por CSV (IMPORTACAO_DADOS_SPEC §4.1). Cada linha é convertida no mesmo
 * {@link SaveClientRequest} do registo manual e validada pelas mesmas regras: NUIT obrigatório e válido,
 * email opcional (se vier, válido), prazo e limite de crédito com os limites do ecrã.
 */
@Service
public class ClientImportService {

    static final String NAME = "nome";
    static final String TAX_ID = "nuit";
    static final String EMAIL = "email";
    static final String ADDRESS = "morada";
    static final String PAYMENT_TERMS_DAYS = "prazo_pagamento_dias";
    static final String CREDIT_LIMIT = "limite_credito";

    private static final List<String> REQUIRED_COLUMNS = List.of(NAME, TAX_ID);
    private static final List<String> OPTIONAL_COLUMNS = List.of(EMAIL, ADDRESS, PAYMENT_TERMS_DAYS, CREDIT_LIMIT);
    private static final Map<String, String> COLUMN_BY_PROPERTY = columnByProperty();

    private final CsvImportReader reader = new CsvImportReader();
    private final ImportValueParser parser = new ImportValueParser();
    private final ImportRequestValidator requestValidator;
    private final ClientLookup clientLookup;

    public ClientImportService(Validator validator, ClientLookup clientLookup) {
        this.requestValidator = new ImportRequestValidator(validator);
        this.clientLookup = clientLookup;
    }

    /** Lê o ficheiro e classifica cada linha na empresa activa, sem gravar nada (§3.1 passo 2). */
    public ImportPreviewDTO preview(byte[] content) {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        CsvTable table = reader.read(content, REQUIRED_COLUMNS, OPTIONAL_COLUMNS);
        DuplicateKeys duplicates = DuplicateKeys.of(table.rows(),
                row -> parser.parseText(table.value(row, TAX_ID)));
        List<ImportRowDTO> rows = table.rows().stream()
                .map(row -> classify(table, row, duplicates, companyId))
                .toList();
        return new ImportPreviewDTO(rows, table.header().extraColumns());
    }

    private ImportRowDTO classify(CsvTable table, CsvTable.Row row, DuplicateKeys duplicates, Long companyId) {
        SaveClientRequest request;
        try {
            request = toRequest(table, row);
        } catch (ImportValueException invalidValue) {
            return ImportRowDTO.error(row.number(), invalidValue.getColumn(), invalidValue.getMessage());
        }
        return requestValidator.firstViolation(row.number(), request, COLUMN_BY_PROPERTY)
                .or(() -> ImportTaxIdRule.violation(row.number(), request.taxId(), TAX_ID))
                .or(() -> duplicates.violation(row.number(), request.taxId(), TAX_ID, "NUIT"))
                .orElseGet(() -> ImportRowDTO.of(row.number(), statusOf(request.taxId(), companyId)));
    }

    /** §4.1: já na empresa → ignorado; só noutra empresa → o cliente partilhado é associado. */
    private ImportRowStatus statusOf(String taxId, Long companyId) {
        if (clientLookup.existsForCompanyByTaxId(taxId, companyId)) {
            return ImportRowStatus.JA_EXISTE;
        }
        return clientLookup.existsByTaxId(taxId) ? ImportRowStatus.ASSOCIADO : ImportRowStatus.NOVO;
    }

    private SaveClientRequest toRequest(CsvTable table, CsvTable.Row row) {
        int line = row.number();
        return new SaveClientRequest(
                parser.parseText(table.value(row, NAME)),
                parser.parseText(table.value(row, TAX_ID)),
                parser.parseText(table.value(row, EMAIL)),
                parser.parseText(table.value(row, ADDRESS)),
                parseDays(table.value(row, PAYMENT_TERMS_DAYS), line),
                parser.parseDecimal(table.value(row, CREDIT_LIMIT), line, CREDIT_LIMIT));
    }

    private Integer parseDays(String raw, int lineNumber) {
        BigDecimal days = parser.parseDecimal(raw, lineNumber, PAYMENT_TERMS_DAYS);
        if (days == null) {
            return null;
        }
        try {
            return days.intValueExact();
        } catch (ArithmeticException notWholeDays) {
            throw new ImportValueException(lineNumber, PAYMENT_TERMS_DAYS,
                    "o prazo de pagamento tem de ser um número inteiro de dias.");
        }
    }

    private static Map<String, String> columnByProperty() {
        Map<String, String> columns = new LinkedHashMap<>();
        columns.put("name", NAME);
        columns.put("taxId", TAX_ID);
        columns.put("email", EMAIL);
        columns.put("address", ADDRESS);
        columns.put("paymentTermsDays", PAYMENT_TERMS_DAYS);
        columns.put("creditLimit", CREDIT_LIMIT);
        return Collections.unmodifiableMap(columns); // mantém a ordem das colunas do modelo
    }
}
