package mz.multicore.erp.modules.dataimport.service;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.dataimport.dto.ImportPreviewDTO;
import mz.multicore.erp.modules.dataimport.dto.ImportRowDTO;
import mz.multicore.erp.modules.dataimport.dto.ImportRowStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * IMPORTACAO_DADOS_HARNESS — IMP-CL-01: cliente com NUIT obrigatório e válido, email opcional;
 * linha inválida é {@code ERRO} com coluna e mensagem PT-MZ (SPEC §4.1, §3.3).
 */
class ClientImportServiceTest {

    private static final String HEADER = "nome;nuit;email;morada;prazo_pagamento_dias;limite_credito";

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final ClientImportService service = new ClientImportService(validator);

    private ImportPreviewDTO preview(String... lines) {
        return service.preview(String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
    }

    private ImportRowDTO singleRow(String line) {
        ImportPreviewDTO preview = preview(HEADER, line);
        assertThat(preview.rows()).hasSize(1);
        return preview.rows().get(0);
    }

    // IMP-CL-01
    @Test
    void validClientWithoutEmailIsNew() {
        ImportRowDTO row = singleRow("Loja Maputo;400123456;;Av. 24 de Julho;30;5000,00");

        assertThat(row.status()).isEqualTo(ImportRowStatus.NOVO);
        assertThat(row.lineNumber()).isEqualTo(2);
        assertThat(row.column()).isNull();
    }

    // IMP-CL-01
    @Test
    void validClientWithEmailAndOnlyRequiredColumnsIsNew() {
        ImportPreviewDTO preview = preview("nome;nuit;email", "Loja Beira;400123457;loja@beira.co.mz");

        assertThat(preview.rows()).extracting(ImportRowDTO::status).containsExactly(ImportRowStatus.NOVO);
        assertThat(preview.hasErrors()).isFalse();
    }

    // IMP-CL-01
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Loja;;;;;                    | nuit                 | NUIT",
            "Loja;12345;;;;               | nuit                 | NUIT",
            "Loja;40012345A;;;;           | nuit                 | NUIT",
            ";400123456;;;;               | nome                 | nome",
            "Loja;400123456;sem-arroba;;; | email                | email",
            "Loja;400123456;;;abc;        | prazo_pagamento_dias | prazo_pagamento_dias",
            "Loja;400123456;;;30,5;       | prazo_pagamento_dias | prazo",
            "Loja;400123456;;;400;        | prazo_pagamento_dias | 365",
            "Loja;400123456;;;;-10        | limite_credito       | negativo",
            "Loja;400123456;;;;muito      | limite_credito       | limite_credito"
    })
    void invalidRowIsErrorWithColumnAndPortugueseMessage(String line, String column, String messagePart) {
        ImportRowDTO row = singleRow(line.strip());

        assertThat(row.status()).isEqualTo(ImportRowStatus.ERRO);
        assertThat(row.lineNumber()).isEqualTo(2);
        assertThat(row.column()).isEqualTo(column);
        assertThat(row.message()).containsIgnoringCase(messagePart);
    }

    // IMP-CL-01
    @Test
    void reportsFileLineNumbersAndKeepsValidRowsApartFromErrors() {
        ImportPreviewDTO preview = preview(
                HEADER,
                "Loja A;400123456;;;;",
                "",
                "Loja B;123;;;;",
                "Loja C;400123458;c@loja.co.mz;;;");

        assertThat(preview.rows()).extracting(ImportRowDTO::lineNumber).containsExactly(2, 4, 5);
        assertThat(preview.rows()).extracting(ImportRowDTO::status)
                .containsExactly(ImportRowStatus.NOVO, ImportRowStatus.ERRO, ImportRowStatus.NOVO);
        assertThat(preview.count(ImportRowStatus.NOVO)).isEqualTo(2);
        assertThat(preview.hasErrors()).isTrue();
    }

    // IMP-CL-01
    @Test
    void missingNuitColumnRefusesTheWholeFile() {
        assertThatThrownBy(() -> preview("nome;email", "Loja;loja@x.co.mz"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("nuit");
    }

    // IMP-CL-01
    @Test
    void emptyFileIsRefused() {
        assertThatThrownBy(() -> service.preview(new byte[0]))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("vazio");
    }

    // IMP-CL-01
    @Test
    void extraColumnsAreAnnounced() {
        ImportPreviewDTO preview = preview("Nome ;NUIT;Telefone", "Loja;400123456;84 000 0000");

        assertThat(preview.extraColumns()).containsExactly("Telefone");
        assertThat(preview.rows()).extracting(ImportRowDTO::status).containsExactly(ImportRowStatus.NOVO);
    }
}
