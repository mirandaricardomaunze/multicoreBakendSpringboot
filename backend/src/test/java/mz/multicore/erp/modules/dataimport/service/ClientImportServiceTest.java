package mz.multicore.erp.modules.dataimport.service;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.service.ClientLookup;
import mz.multicore.erp.modules.dataimport.dto.ImportPreviewDTO;
import mz.multicore.erp.modules.dataimport.dto.ImportRowDTO;
import mz.multicore.erp.modules.dataimport.dto.ImportRowStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * IMPORTACAO_DADOS_HARNESS — IMP-CL-01: cliente com NUIT obrigatório e válido, email opcional;
 * linha inválida é {@code ERRO} com coluna e mensagem PT-MZ (SPEC §4.1, §3.3).
 * IMP-CL-02: NUIT da empresa → {@code JÁ EXISTE}; NUIT de outra empresa → {@code ASSOCIADO};
 * NUIT repetido no ficheiro → {@code ERRO} em ambas as linhas.
 */
class ClientImportServiceTest {

    private static final String HEADER = "nome;nuit;email;morada;prazo_pagamento_dias;limite_credito";

    private static final long COMPANY_ID = 7L;

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final ClientLookup clientLookup = mock(ClientLookup.class);
    private final ClientImportService service = new ClientImportService(validator, clientLookup);

    @BeforeEach
    void activeCompany() {
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        when(clientLookup.existsForCompanyByTaxId(anyString(), anyLong())).thenReturn(false);
        when(clientLookup.existsByTaxId(anyString())).thenReturn(false);
    }

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

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

    // IMP-CL-02
    @Test
    void taxIdAlreadyInActiveCompanyIsAlreadyExisting() {
        when(clientLookup.existsForCompanyByTaxId("400123456", COMPANY_ID)).thenReturn(true);
        when(clientLookup.existsByTaxId("400123456")).thenReturn(true);

        ImportPreviewDTO preview = preview(HEADER, "Loja Maputo;400123456;;;;", "Loja Beira;400123457;;;;");

        assertThat(preview.rows()).extracting(ImportRowDTO::status)
                .containsExactly(ImportRowStatus.JA_EXISTE, ImportRowStatus.NOVO);
        assertThat(preview.rows().get(0).column()).isNull();
        assertThat(preview.count(ImportRowStatus.JA_EXISTE)).isEqualTo(1);
        assertThat(preview.hasErrors()).isFalse();
    }

    // IMP-CL-02
    @Test
    void taxIdOfAnotherCompanyIsAssociated() {
        when(clientLookup.existsByTaxId("400123456")).thenReturn(true);

        ImportRowDTO row = singleRow("Outro Nome;400123456;outro@mail.co.mz;;;");

        assertThat(row.status()).isEqualTo(ImportRowStatus.ASSOCIADO);
        assertThat(row.message()).isNull();
    }

    // IMP-CL-02
    @Test
    void repeatedTaxIdInTheFileIsErrorOnBothLines() {
        ImportPreviewDTO preview = preview(
                HEADER,
                "Loja A;400123456;;;;",
                "Loja B;400123457;;;;",
                "Loja A bis; 400123456 ;;;;");

        assertThat(preview.rows()).extracting(ImportRowDTO::status)
                .containsExactly(ImportRowStatus.ERRO, ImportRowStatus.NOVO, ImportRowStatus.ERRO);
        ImportRowDTO first = preview.rows().get(0);
        assertThat(first.column()).isEqualTo("nuit");
        assertThat(first.message()).contains("repetido").contains("2").contains("4");
        assertThat(preview.rows().get(2).column()).isEqualTo("nuit");
    }

    // IMP-CL-02
    @Test
    void repeatedTaxIdIsErrorEvenWhenItAlreadyExists() {
        when(clientLookup.existsForCompanyByTaxId("400123456", COMPANY_ID)).thenReturn(true);
        when(clientLookup.existsByTaxId("400123456")).thenReturn(true);

        ImportPreviewDTO preview = preview(HEADER, "Loja A;400123456;;;;", "Loja A;400123456;;;;");

        assertThat(preview.rows()).extracting(ImportRowDTO::status)
                .containsExactly(ImportRowStatus.ERRO, ImportRowStatus.ERRO);
    }

    // IMP-CL-02
    @Test
    void lineWithItsOwnErrorKeepsThatErrorWhenTheTaxIdIsRepeated() {
        ImportPreviewDTO preview = preview(HEADER, "Loja A;400123456;sem-arroba;;;", "Loja B;400123456;;;;");

        assertThat(preview.rows()).extracting(ImportRowDTO::column).containsExactly("email", "nuit");
        assertThat(preview.rows()).extracting(ImportRowDTO::status)
                .containsExactly(ImportRowStatus.ERRO, ImportRowStatus.ERRO);
    }

    // IMP-CL-02
    @Test
    void previewRequiresAnActiveCompany() {
        CurrentUserContext.clear();

        assertThatThrownBy(() -> preview(HEADER, "Loja A;400123456;;;;"))
                .isInstanceOf(BusinessRuleException.class);
    }
}
