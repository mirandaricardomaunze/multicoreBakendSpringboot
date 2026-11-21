package mz.multicore.erp.modules.dataimport.service;

import mz.multicore.erp.modules.dataimport.dto.ImportCommitResultDTO;

/**
 * Texto do registo de auditoria {@code DATA_IMPORT} (IMPORTACAO_DADOS_SPEC §3.1 passo 4): entidade, nome do
 * ficheiro e totais. Classe pura, sem Spring.
 */
public final class ImportAuditDetails {

    /** Acção gravada na auditoria por cada confirmação. */
    public static final String ACTION = "DATA_IMPORT";

    static final int MAX_FILE_NAME = 200;
    static final String NO_FILE_NAME = "(sem nome)";

    private ImportAuditDetails() {
    }

    /** Ex.: {@code Importação de clientes do ficheiro "clientes.csv": 2 novos, 0 associados, 1 já existente.} */
    public static String describe(ImportEntity entity, String fileName, ImportCommitResultDTO result) {
        return "Importação de " + entity.label() + " do ficheiro \"" + fileName(fileName) + "\": "
                + count(result.created(), "novo", "novos") + ", "
                + count(result.associated(), "associado", "associados") + ", "
                + count(result.alreadyExisting(), "já existente", "já existentes") + ".";
    }

    private static String fileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return NO_FILE_NAME;
        }
        String name = fileName.strip();
        return name.length() > MAX_FILE_NAME ? name.substring(0, MAX_FILE_NAME) : name;
    }

    private static String count(long value, String singular, String plural) {
        return value + " " + (value == 1 ? singular : plural);
    }
}
