package mz.multicore.erp.modules.intelligence.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Pergunta do utilizador; nunca contém instruções de execução automática. */
public record AiAssistantRequest(
        @NotBlank(message = "Escreva uma pergunta para o assistente.")
        @Size(min = 3, max = 600, message = "A pergunta deve ter entre 3 e 600 caracteres.")
        String question
) {
    public AiAssistantRequest {
        question = question == null ? null : question.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", " ").trim();
    }
}
