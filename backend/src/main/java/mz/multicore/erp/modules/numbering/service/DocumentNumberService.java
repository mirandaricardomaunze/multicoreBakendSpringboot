package mz.multicore.erp.modules.numbering.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.numbering.model.DocumentSequence;
import mz.multicore.erp.modules.numbering.repository.DocumentSequenceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;

/**
 * Emite números de documento sequenciais, sem saltos (gapless) e seguros à
 * concorrência, por série e por ano — conforme exigido pela AT/SAF-T.
 *
 * O incremento corre na transação do chamador (Propagation.MANDATORY indiretamente
 * via REQUIRED) sob bloqueio pessimista; assim o número só fica "consumido" se a
 * operação de negócio confirmar (commit). Se a transação reverter, o contador
 * reverte com ela — evitando saltos.
 *
 * <p>A <b>criação</b> da linha da série é diferente: corre numa transacção à parte. A carga de
 * 2026-11-07 mostrou porquê: cinco postos a emitir a primeira fatura do ano ao mesmo tempo tentavam
 * criá-la os cinco; em PostgreSQL o INSERT recusado aborta a transacção do chamador e deixa a sessão
 * do Hibernate inutilizável, e quatro vendas saíam com erro 500. Criar à parte isola essa recusa, e
 * {@link DocumentSequenceBootstrap} cria as séries do ano e do seguinte no arranque, para que a
 * corrida quase nunca aconteça (incluindo na passagem de ano).
 */
@Service
public class DocumentNumberService {

    private final DocumentSequenceRepository repository;
    /** Nulo só no construtor dos testes antigos: aí a série é criada na própria transacção. */
    private final TransactionTemplate ownTransaction;

    @Autowired
    public DocumentNumberService(DocumentSequenceRepository repository, PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.ownTransaction = new TransactionTemplate(transactionManager);
        this.ownTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public DocumentNumberService(DocumentSequenceRepository repository) {
        this.repository = repository;
        this.ownTransaction = null;
    }

    /**
     * Devolve o próximo número da série indicada, <b>para a empresa activa</b> e o ano corrente,
     * no formato {@code SERIE-ANO/N} (ex.: {@code FT-2026/1}). Cada empresa (NUIT) tem a sua própria
     * sequência gapless — documentos de outras empresas não criam saltos.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public String next(String series) {
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        if (companyId == null) {
            throw new BusinessRuleException("Empresa activa em falta para numerar o documento.");
        }
        int year = LocalDate.now().getYear();

        DocumentSequence sequence = repository.lockByCompanyAndSeriesAndYear(companyId, series, year)
                .orElseGet(() -> {
                    ensureSequence(companyId, series, year);
                    return repository.lockByCompanyAndSeriesAndYear(companyId, series, year)
                            .orElseThrow(() -> new IllegalStateException("A série " + series + " não ficou disponível."));
                });

        long number = sequence.getLastNumber() + 1;
        sequence.setLastNumber(number);
        repository.save(sequence);

        return series + "-" + year + "/" + number;
    }

    /**
     * Garante que a série existe, a zero se for nova. Uma recusa da chave única quer dizer que
     * outro posto a criou no mesmo instante, e isso serve: não é erro.
     */
    public void ensureSequence(Long companyId, String series, int year) {
        if (ownTransaction == null) {
            insertIfMissing(companyId, series, year);
            return;
        }
        try {
            ownTransaction.executeWithoutResult(status -> insertIfMissing(companyId, series, year));
        } catch (DataIntegrityViolationException createdByAnotherTerminal) {
            // Outro posto ganhou a corrida; a série dele serve.
        }
    }

    private void insertIfMissing(Long companyId, String series, int year) {
        if (repository.lockByCompanyAndSeriesAndYear(companyId, series, year).isPresent()) return;
        DocumentSequence sequence = new DocumentSequence();
        sequence.setCompanyId(companyId);
        sequence.setSeries(series);
        sequence.setYear(year);
        sequence.setLastNumber(0);
        repository.saveAndFlush(sequence);
    }
}
