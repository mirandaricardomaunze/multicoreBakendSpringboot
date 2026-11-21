package mz.multicore.erp.modules.dataimport.dto;

/**
 * Modelo CSV descarregável de uma entidade (IMPORTACAO_DADOS_SPEC §3.1 passo 1): o nome do ficheiro e
 * os bytes (UTF-8 com BOM, separador {@code ;}, só a linha de cabeçalho).
 */
public record ImportTemplateDTO(String fileName, byte[] content) {

    public ImportTemplateDTO {
        content = content.clone();
    }

    @Override
    public byte[] content() {
        return content.clone();
    }
}
