package com.phcpro.modules.numbering.service;

import com.phcpro.modules.numbering.model.DocumentSequence;
import com.phcpro.modules.numbering.repository.DocumentSequenceRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Emite números de documento sequenciais, sem saltos (gapless) e seguros à
 * concorrência, por série e por ano — conforme exigido pela AT/SAF-T.
 *
 * O incremento corre na transação do chamador (Propagation.MANDATORY indiretamente
 * via REQUIRED) sob bloqueio pessimista; assim o número só fica "consumido" se a
 * operação de negócio confirmar (commit). Se a transação reverter, o contador
 * reverte com ela — evitando saltos.
 */
@Service
public class DocumentNumberService {

    private final DocumentSequenceRepository repository;

    public DocumentNumberService(DocumentSequenceRepository repository) {
        this.repository = repository;
    }

    /**
     * Devolve o próximo número da série indicada para o ano corrente,
     * no formato {@code SERIE-ANO/N} (ex.: {@code FT-2026/1}).
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public String next(String series) {
        int year = LocalDate.now().getYear();

        DocumentSequence sequence = repository.lockBySeriesAndYear(series, year)
                .orElseGet(() -> createSequence(series, year));

        long number = sequence.getLastNumber() + 1;
        sequence.setLastNumber(number);
        repository.save(sequence);

        return series + "-" + year + "/" + number;
    }

    private DocumentSequence createSequence(String series, int year) {
        DocumentSequence sequence = new DocumentSequence();
        sequence.setSeries(series);
        sequence.setYear(year);
        sequence.setLastNumber(0);
        try {
            return repository.saveAndFlush(sequence);
        } catch (DataIntegrityViolationException concurrentInsert) {
            // Outra transação criou a série em paralelo — relê já com bloqueio.
            return repository.lockBySeriesAndYear(series, year)
                    .orElseThrow(() -> concurrentInsert);
        }
    }
}
