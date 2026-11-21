package mz.multicore.erp.modules.dataimport.service;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.dataimport.dto.ImportPreviewDTO;
import mz.multicore.erp.modules.dataimport.dto.ImportRowDTO;
import mz.multicore.erp.modules.dataimport.dto.ImportRowStatus;
import mz.multicore.erp.modules.purchases.service.SupplierLookup;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * IMPORTACAO_DADOS_HARNESS — IMP-FO-01: fornecedor com NUIT obrigatório e válido, email opcional;
 * NUIT já na empresa activa → {@code JÁ EXISTE}; NUIT repetido no ficheiro → {@code ERRO} em ambas as
 * linhas (SPEC §4.2, §3.3).
 */
class SupplierImportServiceTest {

    private static final String HEADER = "nome;nuit;email;telefone;morada;contacto";

    private static final long COMPANY_ID = 7L;

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final SupplierLookup supplierLookup = mock(SupplierLookup.class);
    private final SupplierImportService service = new SupplierImportService(validator, supplierLookup);

    @BeforeEach
    void activeCompany() {
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        when(supplierLookup.existsForCompanyByTaxId(anyString(), anyLong())).thenReturn(false);
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

    @Test
    void validSupplierWithoutEmailIsNew() {
        ImportRowDTO row = singleRow("Distribuidora Sul;400123456;;841234567;Av. Angola;Sr. Mussa");

        assertThat(row.status()).isEqualTo(ImportRowStatus.NOVO);
        assertThat(row.lineNumber()).isEqualTo(2);
        assertThat(row.column()).isNull();
    }

    @Test
    void onlyRequiredColumnsAreEnough() {
        ImportPreviewDTO preview = preview("Nome;NUIT", "Distribuidora Sul;400123456");

        assertThat(preview.rows()).extracting(ImportRowDTO::status).containsExactly(ImportRowStatus.NOVO);
        assertThat(preview.hasErrors()).isFalse();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Forn;;;;;                       | nuit     | NUIT",
            "Forn;12345;;;;                  | nuit     | NUIT",
            "Forn;40012345A;;;;              | nuit     | NUIT",
            ";400123456;;;;                  | nome     | nome",
            "Forn;400123456;sem-arroba;;;    | email    | email",
            "Forn;400123456;;123;;           | telefone | telefone"
    })
    void invalidRowIsErrorWithColumnAndPortugueseMessage(String line, String column, String messagePart) {
        ImportRowDTO row = singleRow(line.strip());

        assertThat(row.status()).isEqualTo(ImportRowStatus.ERRO);
        assertThat(row.lineNumber()).isEqualTo(2);
        assertThat(row.column()).isEqualTo(column);
        assertThat(row.message()).containsIgnoringCase(messagePart);
    }

    @Test
    void missingNuitColumnRefusesTheWholeFile() {
        assertThatThrownBy(() -> preview("nome;email", "Forn;forn@x.co.mz"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("nuit");
    }

    @Test
    void taxIdAlreadyInActiveCompanyIsAlreadyExisting() {
        when(supplierLookup.existsForCompanyByTaxId("400123456", COMPANY_ID)).thenReturn(true);

        ImportPreviewDTO preview = preview(HEADER, "Forn A;400123456;;;;", "Forn B;400123457;;;;");

        assertThat(preview.rows()).extracting(ImportRowDTO::status)
                .containsExactly(ImportRowStatus.JA_EXISTE, ImportRowStatus.NOVO);
        assertThat(preview.rows().get(0).column()).isNull();
        assertThat(preview.count(ImportRowStatus.JA_EXISTE)).isEqualTo(1);
        assertThat(preview.hasErrors()).isFalse();
        verify(supplierLookup).existsForCompanyByTaxId("400123456", COMPANY_ID);
    }

    @Test
    void suppliersAreNeverAssociatedAcrossCompanies() {
        // §4.2: a chave é o NUIT na empresa — um fornecedor de outra empresa não conta.
        ImportRowDTO row = singleRow("Forn A;400123456;;;;");

        assertThat(row.status()).isEqualTo(ImportRowStatus.NOVO);
        verify(supplierLookup).existsForCompanyByTaxId("400123456", COMPANY_ID);
        verify(supplierLookup, never()).existsForCompanyByTaxId(anyString(), eq(99L));
    }

    @Test
    void repeatedTaxIdInTheFileIsErrorOnBothLines() {
        ImportPreviewDTO preview = preview(
                HEADER,
                "Forn A;400123456;;;;",
                "Forn B;400123457;;;;",
                "Forn A bis; 400123456 ;;;;");

        assertThat(preview.rows()).extracting(ImportRowDTO::status)
                .containsExactly(ImportRowStatus.ERRO, ImportRowStatus.NOVO, ImportRowStatus.ERRO);
        ImportRowDTO first = preview.rows().get(0);
        assertThat(first.column()).isEqualTo("nuit");
        assertThat(first.message()).contains("repetido").contains("2").contains("4");
    }

    @Test
    void repeatedTaxIdIsErrorEvenWhenItAlreadyExists() {
        when(supplierLookup.existsForCompanyByTaxId("400123456", COMPANY_ID)).thenReturn(true);

        ImportPreviewDTO preview = preview(HEADER, "Forn A;400123456;;;;", "Forn A;400123456;;;;");

        assertThat(preview.rows()).extracting(ImportRowDTO::status)
                .containsExactly(ImportRowStatus.ERRO, ImportRowStatus.ERRO);
    }

    @Test
    void previewRequiresAnActiveCompany() {
        CurrentUserContext.clear();

        assertThatThrownBy(() -> preview(HEADER, "Forn A;400123456;;;;"))
                .isInstanceOf(BusinessRuleException.class);
    }
}
