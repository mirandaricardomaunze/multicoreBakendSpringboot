package mz.multicore.erp.modules.dataimport.dto;

/**
 * Totais de uma importação confirmada (IMPORTACAO_DADOS_SPEC §3.1 passo 3): registos criados, clientes
 * partilhados associados à empresa activa e registos que já existiam (ignorados, nunca alterados).
 */
public record ImportCommitResultDTO(long created, long associated, long alreadyExisting) {

    /** Totais a partir da classificação revalidada que foi gravada. */
    public static ImportCommitResultDTO of(ImportPreviewDTO preview) {
        return new ImportCommitResultDTO(
                preview.count(ImportRowStatus.NOVO),
                preview.count(ImportRowStatus.ASSOCIADO),
                preview.count(ImportRowStatus.JA_EXISTE));
    }
}
