package mz.multicore.erp.modules.printing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Uma listagem do desktop, tal como está no ecrã, a caminho do PDF.
 *
 * <p>O cliente manda o que mostrou — cabeçalhos e linhas já formatados — porque é isso que o
 * operador viu e é isso que ele espera no papel. Quem acrescenta o cabeçalho da empresa (nome,
 * NUIT, morada, telefone, email, logótipo) é o servidor: os dados da empresa vivem lá, e o
 * desktop é um cliente fino que não os conhece.</p>
 */
public record TableExportRequest(
        @NotBlank(message = "O título do relatório é obrigatório.")
        String title,

        @NotEmpty(message = "O relatório tem de ter pelo menos uma coluna.")
        List<String> headers,

        List<List<String>> rows
) {
    /** Nunca devolve {@code null} — uma listagem vazia é um relatório sem linhas, não um erro. */
    public List<List<String>> rows() {
        return rows == null ? List.of() : rows;
    }
}
