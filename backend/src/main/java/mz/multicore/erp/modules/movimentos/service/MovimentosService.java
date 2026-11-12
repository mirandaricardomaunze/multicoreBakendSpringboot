package mz.multicore.erp.modules.movimentos.service;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.repository.CreditNoteRepository;
import mz.multicore.erp.modules.comercial.repository.DebitNoteRepository;
import mz.multicore.erp.modules.comercial.repository.ReceiptRepository;
import mz.multicore.erp.modules.comercial.service.InvoiceLookup;
import mz.multicore.erp.modules.comercial.service.OrderLookup;
import mz.multicore.erp.modules.inventory.repository.StockTransferRepository;
import mz.multicore.erp.modules.movimentos.dto.MovimentoDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Leitura agregada dos documentos comerciais numa só lista filtrável por cliente/nº e período.
 * Read-only: não cria nem altera documentos. Delega a coleta ao {@link MovimentoCollector}.
 */
@Service
public class MovimentosService {

    private final MovimentoCollector collector;

    @Autowired
    public MovimentosService(MovimentoCollector collector) {
        this.collector = collector;
    }

    public MovimentosService(
            InvoiceLookup invoiceLookup,
            OrderLookup orderLookup,
            CreditNoteRepository creditNoteRepository,
            DebitNoteRepository debitNoteRepository
    ) {
        this(new MovimentoCollector(invoiceLookup, orderLookup, creditNoteRepository, debitNoteRepository,
                null, null));
    }

    public MovimentosService(
            InvoiceLookup invoiceLookup,
            OrderLookup orderLookup,
            CreditNoteRepository creditNoteRepository,
            DebitNoteRepository debitNoteRepository,
            ReceiptRepository receiptRepository,
            StockTransferRepository stockTransferRepository
    ) {
        this(new MovimentoCollector(invoiceLookup, orderLookup, creditNoteRepository, debitNoteRepository,
                receiptRepository, stockTransferRepository));
    }

    /**
     * Lista os movimentos comerciais da empresa, ordenados por data descendente.
     *
     * @param companyId empresa activa (validada contra o contexto do utilizador)
     * @param query     substring case-insensitive sobre nº OU cliente; nulo/vazio = sem filtro
     * @param from      data mínima inclusiva; nulo = sem limite inferior
     * @param to        data máxima inclusiva; nulo = sem limite superior
     */
    @Transactional(readOnly = true)
    public List<MovimentoDTO> listar(Long companyId, String query, LocalDate from, LocalDate to) {
        CurrentUserContext.requireCompany(companyId);
        return collector.collectMovimentos(companyId, query, from, to);
    }
}
