package mz.multicore.erp.modules.printing;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.service.CompanyService;
import mz.multicore.erp.modules.printing.dto.TableExportRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Exportação de uma listagem do desktop para PDF, <b>com os dados da empresa</b>.
 *
 * <p>Até aqui o desktop desenhava estas exportações sozinho: título, tabela, e mais nada. Saíam
 * folhas sem nome, sem NUIT e sem morada — papel que não identifica quem o emitiu e que a
 * {@code DADOS_EMPRESA_DOCUMENTOS_SPEC} manda identificar em <i>todos</i> os documentos
 * imprimíveis. O desktop é um cliente fino e não tem a empresa; tem-na o servidor. Por isso a
 * listagem sobe, e desce PDF.</p>
 *
 * <p>Uma responsabilidade: pegar nas linhas que o operador está a ver e devolvê-las em PDF com o
 * mesmo cabeçalho que a factura, a guia e o recibo usam ({@link CompanyHeaderRenderer}).</p>
 */
@Service
public class TableExportPrintService {

    /** Tecto de segurança: uma listagem do ecrã não tem centenas de milhares de linhas. */
    static final int MAX_ROWS = 20_000;
    static final int MAX_COLUMNS = 40;

    private final CompanyService companyService;

    public TableExportPrintService(CompanyService companyService) {
        this.companyService = companyService;
    }

    @Transactional(readOnly = true)
    public byte[] render(Long companyId, TableExportRequest request) {
        validate(request);
        Company company = companyService.getCompanyById(companyId);
        return TablePdfExporter.render(company, request.title(),
                request.headers().toArray(new String[0]), toGrid(request));
    }

    private void validate(TableExportRequest request) {
        int columns = request.headers().size();
        if (columns > MAX_COLUMNS) {
            throw new BusinessRuleException(
                    "O relatório tem colunas a mais (" + columns + "); o máximo é " + MAX_COLUMNS + ".");
        }
        if (request.rows().size() > MAX_ROWS) {
            throw new BusinessRuleException("O relatório tem linhas a mais ("
                    + request.rows().size() + "); o máximo é " + MAX_ROWS + ". Filtre a listagem.");
        }
    }

    /**
     * Normaliza a grelha: uma linha curta é preenchida com vazios e uma linha comprida é cortada,
     * para que o número de células nunca desalinhe a tabela do PDF.
     */
    private String[][] toGrid(TableExportRequest request) {
        int columns = request.headers().size();
        List<List<String>> rows = request.rows();
        String[][] grid = new String[rows.size()][columns];
        for (int row = 0; row < rows.size(); row++) {
            List<String> cells = rows.get(row);
            for (int column = 0; column < columns; column++) {
                String value = cells != null && column < cells.size() ? cells.get(column) : null;
                grid[row][column] = value == null ? "" : value;
            }
        }
        return grid;
    }
}
