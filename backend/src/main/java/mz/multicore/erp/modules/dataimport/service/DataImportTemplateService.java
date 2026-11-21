package mz.multicore.erp.modules.dataimport.service;

import mz.multicore.erp.modules.dataimport.dto.ImportTemplateDTO;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Gera o modelo CSV de cada entidade (IMPORTACAO_DADOS_SPEC §3.1 passo 1): UTF-8 com BOM e separador
 * {@code ;}, para abrir bem no Excel em português, e apenas a linha de cabeçalho com as colunas que a
 * importação reconhece.
 */
@Service
public class DataImportTemplateService {

    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final String SEPARATOR = ";";
    private static final String LINE_END = "\r\n";

    public ImportTemplateDTO template(String entityPath) {
        ImportEntity entity = ImportEntity.fromPath(entityPath);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(UTF8_BOM);
        out.writeBytes((String.join(SEPARATOR, entity.columns()) + LINE_END).getBytes(StandardCharsets.UTF_8));
        return new ImportTemplateDTO(entity.templateFileName(), out.toByteArray());
    }
}
