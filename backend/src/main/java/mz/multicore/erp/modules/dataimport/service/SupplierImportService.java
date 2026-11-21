package mz.multicore.erp.modules.dataimport.service;

import jakarta.validation.Validator;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.dataimport.dto.ImportPreviewDTO;
import mz.multicore.erp.modules.dataimport.dto.ImportRowDTO;
import mz.multicore.erp.modules.dataimport.dto.ImportRowStatus;
import mz.multicore.erp.modules.purchases.dto.CreateSupplierRequest;
import mz.multicore.erp.modules.purchases.service.SupplierLookup;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Importação de fornecedores por CSV (IMPORTACAO_DADOS_SPEC §4.2). Cada linha é convertida no mesmo
 * {@link CreateSupplierRequest} do registo manual e validada pelas mesmas regras: NUIT obrigatório e
 * válido, email opcional (se vier, válido), telefone moçambicano. A chave é o NUIT na empresa activa:
 * já existe → {@code JÁ EXISTE}; fornecedores não são partilhados entre empresas.
 */
@Service
public class SupplierImportService {

    static final String NAME = "nome";
    static final String TAX_ID = "nuit";
    static final String EMAIL = "email";
    static final String PHONE = "telefone";
    static final String ADDRESS = "morada";
    static final String CONTACT = "contacto";

    static final List<String> REQUIRED_COLUMNS = List.of(NAME, TAX_ID);
    static final List<String> OPTIONAL_COLUMNS = List.of(EMAIL, PHONE, ADDRESS, CONTACT);
    private static final Map<String, String> COLUMN_BY_PROPERTY = columnByProperty();

    private final CsvImportReader reader = new CsvImportReader();
    private final ImportValueParser parser = new ImportValueParser();
    private final ImportRequestValidator requestValidator;
    private final SupplierLookup supplierLookup;

    public SupplierImportService(Validator validator, SupplierLookup supplierLookup) {
        this.requestValidator = new ImportRequestValidator(validator);
        this.supplierLookup = supplierLookup;
    }

    /** Lê o ficheiro e classifica cada linha na empresa activa, sem gravar nada (§3.1 passo 2). */
    public ImportPreviewDTO preview(byte[] content) {
        return plan(content).preview();
    }

    /** Classificação com o pedido de cada linha válida; base da pré-visualização e da confirmação. */
    ImportPlan<CreateSupplierRequest> plan(byte[] content) {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        CsvTable table = reader.read(content, REQUIRED_COLUMNS, OPTIONAL_COLUMNS);
        DuplicateKeys duplicates = DuplicateKeys.of(table.rows(),
                row -> parser.parseText(table.value(row, TAX_ID)));
        List<ImportPlan.Line<CreateSupplierRequest>> lines = table.rows().stream()
                .map(row -> classify(table, row, duplicates, companyId))
                .toList();
        return new ImportPlan<>(lines, table.header().extraColumns());
    }

    private ImportPlan.Line<CreateSupplierRequest> classify(CsvTable table, CsvTable.Row row,
                                                           DuplicateKeys duplicates, Long companyId) {
        CreateSupplierRequest request = toRequest(table, row, companyId);
        return requestValidator.firstViolation(row.number(), request, COLUMN_BY_PROPERTY)
                .or(() -> ImportTaxIdRule.violation(row.number(), request.taxId(), TAX_ID))
                .or(() -> duplicates.violation(row.number(), request.taxId(), TAX_ID, "NUIT repetido"))
                .map(ImportPlan.Line::<CreateSupplierRequest>error)
                .orElseGet(() -> new ImportPlan.Line<>(
                        ImportRowDTO.of(row.number(), statusOf(request.taxId(), companyId)), request));
    }

    private ImportRowStatus statusOf(String taxId, Long companyId) {
        return supplierLookup.existsForCompanyByTaxId(taxId, companyId)
                ? ImportRowStatus.JA_EXISTE
                : ImportRowStatus.NOVO;
    }

    private CreateSupplierRequest toRequest(CsvTable table, CsvTable.Row row, Long companyId) {
        return new CreateSupplierRequest(
                parser.parseText(table.value(row, NAME)),
                parser.parseText(table.value(row, TAX_ID)),
                parser.parseText(table.value(row, EMAIL)),
                parser.parseText(table.value(row, ADDRESS)),
                parser.parseText(table.value(row, PHONE)),
                parser.parseText(table.value(row, CONTACT)),
                companyId);
    }

    private static Map<String, String> columnByProperty() {
        Map<String, String> columns = new LinkedHashMap<>();
        columns.put("name", NAME);
        columns.put("taxId", TAX_ID);
        columns.put("email", EMAIL);
        columns.put("phone", PHONE);
        columns.put("address", ADDRESS);
        columns.put("contactPerson", CONTACT);
        return Collections.unmodifiableMap(columns); // mantém a ordem das colunas do modelo
    }
}
