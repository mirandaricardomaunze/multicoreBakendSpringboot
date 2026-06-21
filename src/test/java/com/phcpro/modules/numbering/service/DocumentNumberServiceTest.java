package com.phcpro.modules.numbering.service;

import com.phcpro.modules.numbering.model.DocumentSequence;
import com.phcpro.modules.numbering.repository.DocumentSequenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Testes do emissor de números sequenciais. Foco: numeração gapless por série e ano,
 * formato {@code SERIE-ANO/N}, criação da sequência na primeira emissão e resolução
 * de corrida na criação concorrente. Repositório mockado — não levanta o Spring.
 */
class DocumentNumberServiceTest {

    private DocumentSequenceRepository repository;
    private DocumentNumberService service;
    private int year;

    @BeforeEach
    void setUp() {
        repository = mock(DocumentSequenceRepository.class);
        service = new DocumentNumberService(repository);
        year = LocalDate.now().getYear();
    }

    @Test
    void next_primeiraEmissao_criaSequencia_eDevolveUm() {
        when(repository.lockBySeriesAndYear(DocumentSeries.INVOICE, year)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(DocumentSequence.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repository.save(any(DocumentSequence.class))).thenAnswer(inv -> inv.getArgument(0));

        String number = service.next(DocumentSeries.INVOICE);

        assertEquals("FT-" + year + "/1", number);
    }

    @Test
    void next_sequenciaExistente_incrementaSemSaltos() {
        DocumentSequence existing = sequence(DocumentSeries.INVOICE, 7);
        when(repository.lockBySeriesAndYear(DocumentSeries.INVOICE, year)).thenReturn(Optional.of(existing));
        when(repository.save(any(DocumentSequence.class))).thenAnswer(inv -> inv.getArgument(0));

        String number = service.next(DocumentSeries.INVOICE);

        assertEquals("FT-" + year + "/8", number);
        assertEquals(8, existing.getLastNumber());
    }

    @Test
    void next_serieNotaDebito_usaPrefixoND() {
        when(repository.lockBySeriesAndYear(DocumentSeries.DEBIT_NOTE, year)).thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(DocumentSequence.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repository.save(any(DocumentSequence.class))).thenAnswer(inv -> inv.getArgument(0));

        String number = service.next(DocumentSeries.DEBIT_NOTE);

        assertEquals("ND-" + year + "/1", number);
    }

    @Test
    void next_seriesIndependentes_naoPartilhamContador() {
        when(repository.lockBySeriesAndYear(DocumentSeries.INVOICE, year))
                .thenReturn(Optional.of(sequence(DocumentSeries.INVOICE, 4)));
        when(repository.lockBySeriesAndYear(DocumentSeries.CREDIT_NOTE, year))
                .thenReturn(Optional.of(sequence(DocumentSeries.CREDIT_NOTE, 1)));
        when(repository.save(any(DocumentSequence.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals("FT-" + year + "/5", service.next(DocumentSeries.INVOICE));
        assertEquals("NC-" + year + "/2", service.next(DocumentSeries.CREDIT_NOTE));
    }

    @Test
    void next_criacaoConcorrente_releComBloqueio_eContinua() {
        DocumentSequence concurrent = sequence(DocumentSeries.RECEIPT, 3);
        // Primeira leitura: não existe → tenta criar e bate na unique constraint.
        // Segunda leitura (após a corrida): já existe a linha criada pela outra transação.
        when(repository.lockBySeriesAndYear(DocumentSeries.RECEIPT, year))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(concurrent));
        when(repository.saveAndFlush(any(DocumentSequence.class)))
                .thenThrow(new DataIntegrityViolationException("unique violation"));
        when(repository.save(any(DocumentSequence.class))).thenAnswer(inv -> inv.getArgument(0));

        String number = service.next(DocumentSeries.RECEIPT);

        assertEquals("RC-" + year + "/4", number);
        verify(repository, times(2)).lockBySeriesAndYear(DocumentSeries.RECEIPT, year);
    }

    @Test
    void next_criacaoConcorrente_semLinhaAposCorrida_propagaErro() {
        when(repository.lockBySeriesAndYear(eq(DocumentSeries.ORDER), eq(year)))
                .thenReturn(Optional.empty());
        when(repository.saveAndFlush(any(DocumentSequence.class)))
                .thenThrow(new DataIntegrityViolationException("unique violation"));

        assertThrows(DataIntegrityViolationException.class, () -> service.next(DocumentSeries.ORDER));
    }

    private DocumentSequence sequence(String series, long lastNumber) {
        DocumentSequence s = new DocumentSequence();
        s.setSeries(series);
        s.setYear(year);
        s.setLastNumber(lastNumber);
        return s;
    }
}
