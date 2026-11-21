package mz.multicore.erp.modules.dataimport.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import mz.multicore.erp.modules.dataimport.dto.ImportRowDTO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Passa o pedido de cada linha pelas mesmas anotações de validação do registo manual
 * (IMPORTACAO_DADOS_SPEC §3.3: a importação não tem regras mais fracas do que o ecrã) e traduz a
 * primeira violação, pela ordem das colunas do modelo, num {@code ERRO} da linha.
 */
public class ImportRequestValidator {

    private final Validator validator;

    public ImportRequestValidator(Validator validator) {
        this.validator = validator;
    }

    /**
     * @param columnByProperty propriedade do pedido → coluna do modelo, na ordem das colunas do modelo
     */
    public Optional<ImportRowDTO> firstViolation(int lineNumber, Object request, Map<String, String> columnByProperty) {
        List<String> properties = new ArrayList<>(columnByProperty.keySet());
        return validator.validate(request).stream()
                .min(Comparator.comparingInt((ConstraintViolation<Object> violation) -> order(properties, violation))
                        .thenComparing(ConstraintViolation::getMessage))
                .map(violation -> ImportRowDTO.error(lineNumber,
                        columnByProperty.getOrDefault(property(violation), property(violation)),
                        violation.getMessage()));
    }

    private static int order(List<String> properties, ConstraintViolation<Object> violation) {
        int index = properties.indexOf(property(violation));
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    private static String property(ConstraintViolation<Object> violation) {
        return violation.getPropertyPath().toString();
    }
}
