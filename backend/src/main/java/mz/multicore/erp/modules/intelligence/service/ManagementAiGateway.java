package mz.multicore.erp.modules.intelligence.service;

import java.util.Optional;

/** Porta para um modelo externo. Nunca recebe entidades, identificadores de tenant ou PII. */
public interface ManagementAiGateway {
    Optional<String> answer(String question, ManagementAiContext context);
}
