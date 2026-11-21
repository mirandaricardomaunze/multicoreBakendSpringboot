package mz.multicore.erp.modules.dataimport.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.validation.TaxIdValidator;
import mz.multicore.erp.modules.dataimport.dto.ImportRowDTO;

import java.util.Optional;

/**
 * A mesma verificação de NUIT que o registo manual faz antes de gravar (IMPORTACAO_DADOS_SPEC §3.3),
 * traduzida num {@code ERRO} da linha. Classe pura, partilhada por clientes e fornecedores.
 */
final class ImportTaxIdRule {

    private ImportTaxIdRule() {
    }

    static Optional<ImportRowDTO> violation(int lineNumber, String taxId, String column) {
        try {
            TaxIdValidator.validate(taxId);
            return Optional.empty();
        } catch (BusinessRuleException invalidTaxId) {
            return Optional.of(ImportRowDTO.error(lineNumber, column, invalidTaxId.getMessage()));
        }
    }
}
