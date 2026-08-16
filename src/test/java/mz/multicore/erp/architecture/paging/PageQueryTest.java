package mz.multicore.erp.architecture.paging;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Contrato de paginação (PG-01..PG-08). Ver docs/PAGINACAO_SPEC.md.
 */
class PageQueryTest {

    @Test // PG-01
    void semParametros_usaOTamanhoPorOmissao() {
        Pageable pageable = PageQuery.of(null, null);

        assertEquals(0, pageable.getPageNumber());
        assertEquals(PageQuery.DEFAULT_SIZE, pageable.getPageSize());
    }

    @Test // PG-02
    void paginaNegativaValeZero() {
        assertEquals(0, PageQuery.of(-5, 10).getPageNumber());
    }

    @Test // PG-03
    void tamanhoAbusivoEEncaixadoNoTecto() {
        assertEquals(PageQuery.MAX_SIZE, PageQuery.of(0, 1_000_000).getPageSize(),
                "um cliente não pode obrigar o servidor a materializar a tabela inteira");
    }

    @Test // PG-04
    void tamanhoZeroOuNegativoCaiNoDefault() {
        assertEquals(PageQuery.DEFAULT_SIZE, PageQuery.of(0, 0).getPageSize());
        assertEquals(PageQuery.DEFAULT_SIZE, PageQuery.of(0, -10).getPageSize());
    }

    @Test // PG-05
    void pedidoValidoPassaIntacto() {
        Pageable pageable = PageQuery.of(3, 25);

        assertEquals(3, pageable.getPageNumber());
        assertEquals(25, pageable.getPageSize());
    }

    @Test // PG-06
    void converteAPaginaDoSpringData_aplicandoOMapeamento() {
        Page<String> source = new PageImpl<>(List.of("a", "b"), PageRequest.of(1, 2), 10);

        PageResponse<String> response = PageResponse.of(source, String::toUpperCase);

        assertEquals(List.of("A", "B"), response.items());
        assertEquals(1, response.page());
        assertEquals(2, response.size());
        assertEquals(10, response.totalElements());
        assertEquals(5, response.totalPages());
    }

    @Test // PG-07
    void sabeSeHaPaginaSeguinteEAnterior() {
        PageResponse<String> meio = new PageResponse<>(List.of("x"), 1, 1, 3, 3);
        assertTrue(meio.hasNext());
        assertTrue(meio.hasPrevious());

        PageResponse<String> primeira = new PageResponse<>(List.of("x"), 0, 1, 3, 3);
        assertTrue(primeira.hasNext());
        assertFalse(primeira.hasPrevious());

        PageResponse<String> ultima = new PageResponse<>(List.of("x"), 2, 1, 3, 3);
        assertFalse(ultima.hasNext());
        assertTrue(ultima.hasPrevious());
    }

    @Test // PG-08
    void paginaVaziaNaoTemSeguinteNemAnterior() {
        PageResponse<String> vazia = PageResponse.of(
                new PageImpl<String>(List.of(), PageRequest.of(0, 50), 0), s -> s);

        assertTrue(vazia.items().isEmpty());
        assertEquals(0, vazia.totalElements());
        assertFalse(vazia.hasNext());
        assertFalse(vazia.hasPrevious());
    }
}
