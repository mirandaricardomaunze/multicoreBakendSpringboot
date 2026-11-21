package mz.multicore.erp.modules.dataimport.service;

import jakarta.validation.Validator;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.validation.TaxIdValidator;
import mz.multicore.erp.modules.comercial.dto.SaveClientRequest;
import mz.multicore.erp.modules.dataimport.dto.ImportPreviewDTO;
import mz.multicore.erp.modules.dataimport.dto.ImportRowDTO;
import mz.multicore.erp.modules.dataimport.dto.ImportRowStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    public ClientImportService(Validator validator) {
        this.requestValidator = new ImportRequestValidator(validator);
    }

    /** Lê o ficheiro e classifica cada linha, sem gravar nada (§3.1 passo 2). */
    public ImportPreviewDTO preview(byte[] content) {
        CsvTable table = reader.read(content, REQUIRED_COLUMNS, OPTIONAL_COLUMNS);
        List<ImportRowDTO> rows = table.rows().stream().map(row -> classify(table, row)).toList();
        return new ImportPreviewDTO(rows, table.header().extraColumns());
    }

    private ImportRowDTO classify(CsvTable table, CsvTable.Row row) {
        SaveClientRequest request;
        try {
            request = toRequest(table, row);
        } catch (ImportValueException invalidValue) {
            return ImportRowDTO.error(row.number(), invalidValue.getColumn(), invalidValue.getMessage());
        }
        return requestValidator.firstViolation(row.number(), request, COLUMN_BY_PROPERTY)
                .or(() -> taxIdViolation(row.number(), request.taxId()))
                .orElseGet(() -> ImportRowDTO.of(row.number(), ImportRowStatus.NOVO));
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

    /** A mesma verificação de NUIT que o registo manual faz antes de gravar. */
    private static Optional<ImportRowDTO> taxIdViolation(int lineNumber, String taxId) {
        try {
            TaxIdValidator.validate(taxId);
            return Optional.empty();
        } catch (BusinessRuleException invalidTaxId) {
            return Optional.of(ImportRowDTO.error(lineNumber, TAX_ID, invalidTaxId.getMessage()));
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
