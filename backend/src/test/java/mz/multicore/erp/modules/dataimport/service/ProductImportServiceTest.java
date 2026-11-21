package mz.multicore.erp.modules.dataimport.service;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.dataimport.dto.ImportPreviewDTO;
import mz.multicore.erp.modules.dataimport.dto.ImportRowDTO;
import mz.multicore.erp.modules.dataimport.dto.ImportRowStatus;
import mz.multicore.erp.modules.fiscal.model.TaxRate;
import mz.multicore.erp.modules.fiscal.model.TaxType;
import mz.multicore.erp.modules.fiscal.service.TaxRateLookup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * IMPORTACAO_DADOS_HARNESS — IMP-AR-01: o {@code iva} do artigo é a percentagem ou o código de uma taxa
 * de IVA já existente na empresa activa; inexistente ou ambígua (duas taxas com a mesma percentagem)
 * → {@code ERRO} na coluna {@code iva} (SPEC §4.3).
 */
class ProductImportServiceTest {

    private static final String HEADER =
            "sku;nome;preco_venda;iva;categoria;codigo_barras;referencia;preco_grosso;unidade;descricao";

    private static final long COMPANY_ID = 7L;

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final TaxRateLookup taxRateLookup = mock(TaxRateLookup.class);
    private final ProductImportService service = new ProductImportService(validator, taxRateLookup);
    private final List<TaxRate> companyRates = new ArrayList<>();

    @BeforeEach
    void activeCompany() {
        CurrentUserContext.setCurrentCompanyId(COMPANY_ID);
        companyRates.add(rate("IVA16", TaxType.IVA_STANDARD, "0.1600"));
        companyRates.add(rate("IVA5", TaxType.IVA_REDUCED, "0.0500"));
        when(taxRateLookup.listActiveForCompany(COMPANY_ID)).thenReturn(companyRates);
    }

    @AfterEach
    void clearContext() {
        CurrentUserContext.clear();
    }

    private static TaxRate rate(String code, TaxType type, String fraction) {
        TaxRate rate = new TaxRate();
        rate.setCode(code);
        rate.setName(code);
        rate.setType(type);
        rate.setRate(new BigDecimal(fraction));
        return rate;
    }

    private ImportPreviewDTO preview(String... lines) {
        return service.preview(String.join("\n", lines).getBytes(StandardCharsets.UTF_8));
    }

    private ImportRowDTO singleRow(String line) {
        ImportPreviewDTO preview = preview(HEADER, line);
        assertThat(preview.rows()).hasSize(1);
        return preview.rows().get(0);
    }

    private static String product(String iva) {
        return "ART-001;Arroz 5kg;350,00;" + iva + ";Mercearia;;;;;";
    }

    @ParameterizedTest
    @ValueSource(strings = {"16", "16,00", "16.0", "16%", "5", "IVA16", "iva5", " IVA16 "})
    void existingRateByPercentageOrCodeIsNew(String iva) {
        ImportRowDTO row = singleRow(product(iva));

        assertThat(row.status()).isEqualTo(ImportRowStatus.NOVO);
        assertThat(row.column()).isNull();
    }

    @Test
    void onlyRequiredColumnsAreEnough() {
        ImportPreviewDTO preview = preview("SKU;Nome;Preço_Venda;IVA;Categoria", "ART-001;Arroz;350;16;Mercearia");

        assertThat(preview.rows()).extracting(ImportRowDTO::status).containsExactly(ImportRowStatus.NOVO);
        assertThat(preview.hasErrors()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"17", "IVA99", "abc"})
    void unknownRateIsErrorInIvaColumn(String iva) {
        ImportRowDTO row = singleRow(product(iva));

        assertThat(row.status()).isEqualTo(ImportRowStatus.ERRO);
        assertThat(row.lineNumber()).isEqualTo(2);
        assertThat(row.column()).isEqualTo("iva");
        assertThat(row.message()).contains("IVA").contains(iva);
    }

    @Test
    void emptyRateIsErrorInIvaColumn() {
        ImportRowDTO row = singleRow(product(""));

        assertThat(row.status()).isEqualTo(ImportRowStatus.ERRO);
        assertThat(row.column()).isEqualTo("iva");
        assertThat(row.message()).contains("IVA");
    }

    @Test
    void ambiguousPercentageIsErrorButTheCodeResolvesIt() {
        companyRates.add(rate("IVA0", TaxType.IVA_ZERO, "0"));
        companyRates.add(rate("ISENTO", TaxType.IVA_EXEMPT, "0.0000"));

        ImportPreviewDTO preview = preview(HEADER, product("0"), product("ISENTO").replace("ART-001", "ART-002"));

        ImportRowDTO ambiguous = preview.rows().get(0);
        assertThat(ambiguous.status()).isEqualTo(ImportRowStatus.ERRO);
        assertThat(ambiguous.column()).isEqualTo("iva");
        assertThat(ambiguous.message()).contains("ambígu").contains("IVA0").contains("ISENTO");
        assertThat(preview.rows().get(1).status()).isEqualTo(ImportRowStatus.NOVO);
    }

    @Test
    void nonVatRatesOfTheCompanyAreNotAcceptedAsIva() {
        companyRates.add(rate("RF_SERV", TaxType.WITHHOLDING, "0.1000"));

        assertThat(singleRow(product("10")).status()).isEqualTo(ImportRowStatus.ERRO);
        assertThat(singleRow(product("RF_SERV")).status()).isEqualTo(ImportRowStatus.ERRO);
    }

    @Test
    void ratesOfOtherCompaniesAreNotConsulted() {
        when(taxRateLookup.listActiveForCompany(COMPANY_ID)).thenReturn(List.of());

        ImportRowDTO row = singleRow(product("16"));

        assertThat(row.status()).isEqualTo(ImportRowStatus.ERRO);
        assertThat(row.column()).isEqualTo("iva");
    }

    @Test
    void requiredValuesAndNumbersAreCheckedLikeTheManualForm() {
        ImportPreviewDTO preview = preview(HEADER,
                ";Arroz;350;16;Mercearia;;;;;",
                "ART-002;;350;16;Mercearia;;;;;",
                "ART-003;Arroz;caro;16;Mercearia;;;;;",
                "ART-004;Arroz;;16;Mercearia;;;;;");

        assertThat(preview.rows()).extracting(ImportRowDTO::column)
                .containsExactly("sku", "nome", "preco_venda", "preco_venda");
        assertThat(preview.rows()).extracting(ImportRowDTO::status).containsOnly(ImportRowStatus.ERRO);
    }

    @Test
    void missingIvaColumnRefusesTheWholeFile() {
        assertThatThrownBy(() -> preview("sku;nome;preco_venda;categoria", "ART-001;Arroz;350;Mercearia"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("iva");
    }

    @Test
    void previewRequiresAnActiveCompany() {
        CurrentUserContext.clear();

        assertThatThrownBy(() -> preview(HEADER, product("16")))
                .isInstanceOf(BusinessRuleException.class);
    }
}
