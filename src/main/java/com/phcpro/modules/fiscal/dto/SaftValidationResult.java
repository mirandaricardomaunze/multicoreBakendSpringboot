package com.phcpro.modules.fiscal.dto;

import java.util.List;

/**
 * Resultado da validação de um ficheiro SAF-T contra a XSD.
 *
 * @param xsdConfigured a XSD está configurada e acessível (senão nem se chega a validar)
 * @param valid         o XML é válido face à XSD (só significativo quando {@code xsdConfigured})
 * @param errors        erros de validação (vazio quando válido)
 * @param message       mensagem legível para a UI
 */
public record SaftValidationResult(
        boolean xsdConfigured,
        boolean valid,
        List<String> errors,
        String message
) {}
