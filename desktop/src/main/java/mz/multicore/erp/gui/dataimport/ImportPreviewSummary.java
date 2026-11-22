package mz.multicore.erp.gui.dataimport;

import mz.multicore.erp.modules.dataimport.dto.ImportCommitResultDTO;
import mz.multicore.erp.modules.dataimport.dto.ImportPreviewDTO;
import mz.multicore.erp.modules.dataimport.dto.ImportRowStatus;

/**
 * Frases PT-MZ do ecrã "Importar dados" (IMPORTACAO_DADOS_SPEC §3.1): totais por estado, colunas
 * ignoradas e categorias a criar na pré-visualização, e o resultado de uma confirmação. Sem Swing.
 */
final class ImportPreviewSummary {

    private ImportPreviewSummary() {
    }

    static String describe(String fileName, ImportPreviewDTO preview) {
        StringBuilder text = new StringBuilder("<html>Ficheiro <b>").append(escape(fileName)).append("</b>: ")
                .append(totals(preview.count(ImportRowStatus.NOVO), preview.count(ImportRowStatus.ASSOCIADO),
                        preview.count(ImportRowStatus.JA_EXISTE)))
                .append(", ").append(count(preview.count(ImportRowStatus.ERRO), "erro", "erros")).append('.');
        if (!preview.extraColumns().isEmpty()) {
            text.append("<br>Colunas ignoradas: ").append(escape(String.join(", ", preview.extraColumns()))).append('.');
        }
        if (!preview.categoriesToCreate().isEmpty()) {
            text.append("<br>Categorias a criar: ").append(escape(String.join(", ", preview.categoriesToCreate()))).append('.');
        }
        text.append("<br>").append(preview.hasErrors()
                ? "Corrija os erros no ficheiro e volte a escolhê-lo: com erros nada é gravado."
                : "Sem erros: pode confirmar a importação.");
        return text.append("</html>").toString();
    }

    static String committed(ImportEntityOption entity, ImportCommitResultDTO result) {
        return "Importação de " + entity.lowerLabel() + " concluída: "
                + totals(result.created(), result.associated(), result.alreadyExisting()) + ".";
    }

    private static String totals(long created, long associated, long existing) {
        return count(created, "novo", "novos") + ", " + count(associated, "associado", "associados")
                + ", " + count(existing, "já existente", "já existentes");
    }

    private static String count(long n, String singular, String plural) {
        return n + " " + (n == 1 ? singular : plural);
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
