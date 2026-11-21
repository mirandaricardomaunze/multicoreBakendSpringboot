package mz.multicore.erp.modules.dataimport.service;

import mz.multicore.erp.modules.dataimport.dto.ImportPreviewDTO;
import org.springframework.stereotype.Service;

/**
 * Pré-visualizar e gravar os saldos iniciais (IMPORTACAO_DADOS_SPEC §5): stock inicial, dívidas de clientes (SI) e
 * dívidas a fornecedores (SIF). Cada entidade é classificada pelo seu serviço de importação e gravada pelo seu escritor,
 * que recusa o ficheiro com qualquer {@code ERRO}. A transacção e a auditoria são as do {@link DataImportService}.
 */
@Service
public class OpeningBalanceImportService {

    private final StockOpeningImportService stockImport;
    private final StockOpeningWriter stockWriter;
    private final OpeningReceivableImportService receivableImport;
    private final OpeningReceivableWriter receivableWriter;
    private final OpeningPayableImportService payableImport;
    private final OpeningPayableWriter payableWriter;

    public OpeningBalanceImportService(StockOpeningImportService stockImport, StockOpeningWriter stockWriter,
                                       OpeningReceivableImportService receivableImport,
                                       OpeningReceivableWriter receivableWriter,
                                       OpeningPayableImportService payableImport, OpeningPayableWriter payableWriter) {
        this.stockImport = stockImport;
        this.stockWriter = stockWriter;
        this.receivableImport = receivableImport;
        this.receivableWriter = receivableWriter;
        this.payableImport = payableImport;
        this.payableWriter = payableWriter;
    }

    /** §3.1 passo 2: classifica cada linha, sem gravar nada. */
    ImportPreviewDTO preview(ImportEntity entity, byte[] content) {
        return switch (entity) {
            case OPENING_STOCK -> stockImport.preview(content);
            case OPENING_RECEIVABLES -> receivableImport.preview(content);
            case OPENING_PAYABLES -> payableImport.preview(content);
            default -> throw notOpeningBalance(entity);
        };
    }

    /** §3.1 passo 3: volta a classificar o ficheiro do zero e grava tudo ou nada; devolve a classificação gravada. */
    ImportPreviewDTO commit(ImportEntity entity, byte[] content) {
        return switch (entity) {
            case OPENING_STOCK -> stockWriter.write(stockImport.plan(content));
            case OPENING_RECEIVABLES -> receivableWriter.write(receivableImport.plan(content));
            case OPENING_PAYABLES -> payableWriter.write(payableImport.plan(content));
            default -> throw notOpeningBalance(entity);
        };
    }

    private static IllegalArgumentException notOpeningBalance(ImportEntity entity) {
        return new IllegalArgumentException("Não é um saldo inicial: " + entity);
    }
}
