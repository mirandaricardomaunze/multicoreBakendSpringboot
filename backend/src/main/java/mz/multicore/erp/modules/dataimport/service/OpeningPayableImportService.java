package mz.multicore.erp.modules.dataimport.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.dataimport.dto.ImportPreviewDTO;
import mz.multicore.erp.modules.dataimport.dto.ImportRowDTO;
import mz.multicore.erp.modules.dataimport.dto.ImportRowStatus;
import mz.multicore.erp.modules.purchases.dto.CreateOpeningPayableRequest;
import mz.multicore.erp.modules.purchases.model.Supplier;
import mz.multicore.erp.modules.purchases.service.SupplierLookup;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Importação das dívidas a fornecedores por CSV (IMPORTACAO_DADOS_SPEC §5.3): cada linha é uma factura por pagar do
 * sistema antigo, que a confirmação grava como um documento "Saldo Inicial Fornecedor" (série SIF). O fornecedor é o
 * do NUIT na empresa activa. Mesmas regras do SI (§5.2): chave fornecedor + {@code numero_original} — já importada →
 * {@code JÁ EXISTE}, repetida no ficheiro → {@code ERRO} em ambas (§3.3) — e datas e valor pelas regras comuns de
 * {@link OpeningDebtColumns}.
 */
@Service
public class OpeningPayableImportService {

    static final String SUPPLIER_TAX_ID = "nuit_fornecedor";

    static final List<String> REQUIRED_COLUMNS = OpeningDebtColumns.requiredColumns(SUPPLIER_TAX_ID);

    private final CsvImportReader reader = new CsvImportReader();
    private final ImportValueParser parser = new ImportValueParser();
    private final OpeningDebtColumns debtColumns = new OpeningDebtColumns();
    private final SupplierLookup supplierLookup;

    public OpeningPayableImportService(SupplierLookup supplierLookup) {
        this.supplierLookup = supplierLookup;
    }

    /** Lê o ficheiro e classifica cada linha na empresa activa, sem gravar nada (§3.1 passo 2). */
    public ImportPreviewDTO preview(byte[] content) {
        return plan(content).preview();
    }

    /** Classificação com o pedido de cada linha válida; base da pré-visualização e da confirmação. */
    ImportPlan<CreateOpeningPayableRequest> plan(byte[] content) {
        Long companyId = CurrentUserContext.requireCurrentCompanyId();
        CsvTable table = reader.read(content, REQUIRED_COLUMNS, OpeningDebtColumns.OPTIONAL_COLUMNS);
        Map<String, Optional<Supplier>> suppliersByTaxId = new HashMap<>();
        List<ImportPlan.Line<CreateOpeningPayableRequest>> converted = table.rows().stream()
                .map(row -> convert(table, row, companyId, suppliersByTaxId))
                .toList();
        DuplicateKeys duplicates = DuplicateKeys.ofLines(converted, OpeningPayableImportService::key);
        List<ImportPlan.Line<CreateOpeningPayableRequest>> lines = converted.stream()
                .map(line -> line.item() == null ? line : classify(line, duplicates, companyId))
                .toList();
        return new ImportPlan<>(lines, table.header().extraColumns());
    }

    private ImportPlan.Line<CreateOpeningPayableRequest> classify(
            ImportPlan.Line<CreateOpeningPayableRequest> line, DuplicateKeys duplicates, Long companyId) {
        int number = line.row().lineNumber();
        CreateOpeningPayableRequest request = line.item();
        return duplicates.violation(number, key(request), OpeningDebtColumns.ORIGINAL_NUMBER,
                        "Fornecedor e número original repetidos")
                .map(ImportPlan.Line::<CreateOpeningPayableRequest>error)
                .orElseGet(() -> new ImportPlan.Line<>(ImportRowDTO.of(number, statusOf(request, companyId)), request));
    }

    private ImportRowStatus statusOf(CreateOpeningPayableRequest request, Long companyId) {
        return supplierLookup.existsOpeningPayable(companyId, request.supplierId(), request.originalNumber())
                ? ImportRowStatus.JA_EXISTE
                : ImportRowStatus.NOVO;
    }

    /** Chave fornecedor + número original (§5.3). */
    private static String key(CreateOpeningPayableRequest request) {
        return request.supplierId() + "|" + request.originalNumber();
    }

    private ImportPlan.Line<CreateOpeningPayableRequest> convert(CsvTable table, CsvTable.Row row, Long companyId,
                                                                 Map<String, Optional<Supplier>> suppliersByTaxId) {
        try {
            return new ImportPlan.Line<>(ImportRowDTO.of(row.number(), ImportRowStatus.NOVO),
                    toRequest(table, row, companyId, suppliersByTaxId));
        } catch (ImportValueException invalidValue) {
            return ImportPlan.Line.error(
                    ImportRowDTO.error(row.number(), invalidValue.getColumn(), invalidValue.getMessage()));
        }
    }

    /** O fornecedor primeiro (primeira coluna do modelo), depois as colunas comuns pela sua ordem. */
    private CreateOpeningPayableRequest toRequest(CsvTable table, CsvTable.Row row, Long companyId,
                                                  Map<String, Optional<Supplier>> suppliersByTaxId) {
        int line = row.number();
        Supplier supplier = supplier(parser.requireText(table.value(row, SUPPLIER_TAX_ID), line, SUPPLIER_TAX_ID),
                companyId, suppliersByTaxId, line);
        OpeningDebtColumns.Values debt = debtColumns.read(table, row);
        return new CreateOpeningPayableRequest(supplier.getId(), debt.originalNumber(), debt.date(), debt.dueDate(),
                debt.amount(), debt.description());
    }

    private Supplier supplier(String taxId, Long companyId, Map<String, Optional<Supplier>> suppliersByTaxId,
                              int line) {
        return suppliersByTaxId
                .computeIfAbsent(taxId, key -> supplierLookup.findForCompanyByTaxId(key, companyId))
                .orElseThrow(() -> new ImportValueException(line, SUPPLIER_TAX_ID, "o fornecedor com NUIT «" + taxId
                        + "» não existe na empresa; importe primeiro os fornecedores."));
    }
}
