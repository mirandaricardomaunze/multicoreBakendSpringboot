package mz.multicore.erp.modules.numbering.service;

import mz.multicore.erp.modules.company.model.Company;
import mz.multicore.erp.modules.company.repository.CompanyRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;

/** As séries do ano e do seguinte existem antes do primeiro documento (DN-01; carga de 2026-11-07). */
class DocumentSequenceBootstrapTest {

    @Test
    void arranque_criaTodasAsSeriesDoAnoEDoSeguinteEmCadaEmpresa() {
        CompanyRepository companies = mock(CompanyRepository.class);
        DocumentNumberService numbering = mock(DocumentNumberService.class);
        Company a = new Company();
        a.setId(1L);
        Company b = new Company();
        b.setId(2L);
        when(companies.findAll()).thenReturn(List.of(a, b));
        int year = LocalDate.now().getYear();

        new DocumentSequenceBootstrap(companies, numbering).run(null);

        for (Long company : List.of(1L, 2L)) {
            for (String series : DocumentSeries.ALL) {
                verify(numbering).ensureSequence(company, series, year);
                verify(numbering).ensureSequence(company, series, year + 1);
            }
        }
    }
}
