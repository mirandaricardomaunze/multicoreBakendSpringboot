package mz.multicore.erp.modules.printing;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.printing.dto.TableExportRequest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A exportação de uma listagem sai com os dados da empresa.
 *
 * <p>O desktop desenhava estas folhas sozinho e elas saíam sem nome, sem NUIT e sem morada — papel
 * que não diz quem o emitiu, contra a {@code DADOS_EMPRESA_DOCUMENTOS_SPEC}. Estes casos lêem o
 * <b>texto do PDF</b>, e não o código que o gera: se o cabeçalho voltar a desaparecer, falham.</p>
 */
class TableExportPrintServiceTest {

    @Test // TE-01
    void aListagemSaiComOCabecalhoDaEmpresa() throws Exception {
        String text = render(request("Clientes",
                List.of("Nome", "NUIT"),
                List.of(List.of("Maria Cossa", "400111222"))));

        assertThat(text)
                .as("quem emitiu o papel tem de estar no papel")
                .contains("Mercearia Bom Preco", "400123456", "Av. 25 de Setembro, Maputo",
                        "+258 84 000 0000", "geral@bompreco.co.mz");
    }

    @Test // TE-02
    void asLinhasDoEcraSaoAsLinhasDoPapel() throws Exception {
        String text = render(request("Clientes",
                List.of("Nome", "Cidade"),
                List.of(List.of("Maria Cossa", "Maputo"), List.of("Joao Nhaca", "Beira"))));

        assertThat(text).contains("Nome", "Cidade", "Maria Cossa", "Maputo", "Joao Nhaca", "Beira");
        assertThat(text).as("o total conta as linhas exportadas").contains("Total de registos: 2");
    }

    @Test // TE-03
    void umaListagemVaziaEUmRelatorioSemLinhas() throws Exception {
        String text = render(new TableExportRequest("Clientes", List.of("Nome"), null));

        assertThat(text).contains("Clientes", "Total de registos: 0");
    }

    @Test // TE-04
    void umaLinhaCurtaOuCompridaNaoDesalinhaATabela() throws Exception {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("Maria Cossa"));                                  // faltam colunas
        rows.add(List.of("Joao Nhaca", "Beira", "coluna a mais"));         // sobram colunas
        rows.add(Arrays.asList("Ana Timba", null));                        // célula nula

        String text = render(request("Clientes", List.of("Nome", "Cidade"), rows));

        assertThat(text).contains("Maria Cossa", "Joao Nhaca", "Beira", "Ana Timba");
        assertThat(text).as("o que sobra não entra na grelha").doesNotContain("coluna a mais");
        assertThat(text).contains("Total de registos: 3");
    }

    @Test // TE-05
    void umaListagemGrandeDemaisERecusadaComRazao() {
        List<List<String>> rows = new ArrayList<>();
        for (int i = 0; i <= TableExportPrintService.MAX_ROWS; i++) {
            rows.add(List.of("linha " + i));
        }

        assertThatThrownBy(() -> service().render(1L, request("Clientes", List.of("Nome"), rows)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("linhas a mais")
                .hasMessageContaining("Filtre a listagem");
    }

    @Test // TE-06
    void colunasAMaisSaoRecusadasAntesDeDesenharOPdf() {
        List<String> headers = new ArrayList<>();
        for (int i = 0; i <= TableExportPrintService.MAX_COLUMNS; i++) {
            headers.add("Coluna " + i);
        }

        assertThatThrownBy(() -> service().render(1L, request("Clientes", headers, List.of())))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("colunas a mais");
    }

    private static TableExportRequest request(String title, List<String> headers, List<List<String>> rows) {
        return new TableExportRequest(title, headers, rows);
    }

    private static TableExportPrintService service() {
        CompanyService companyService = mock(CompanyService.class);
        when(companyService.getCompanyById(anyLong())).thenReturn(company());
        return new TableExportPrintService(companyService);
    }

    private static String render(TableExportRequest request) throws Exception {
        byte[] pdf = service().render(1L, request);
        PdfReader reader = new PdfReader(pdf);
        try {
            return new PdfTextExtractor(reader).getTextFromPage(1);
        } finally {
            reader.close();
        }
    }

    private static Company company() {
        Company company = new Company();
        company.setId(1L);
        company.setName("Mercearia Bom Preco");
        company.setTaxId("400123456");
        company.setAddress("Av. 25 de Setembro, Maputo");
        company.setPhone("+258 84 000 0000");
        company.setEmail("geral@bompreco.co.mz");
        return company;
    }
}
