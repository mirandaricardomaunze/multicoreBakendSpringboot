package com.phcpro.architecture.paging;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Página de resultados na fronteira HTTP.
 *
 * <p>Existe para o desktop não depender do {@code org.springframework.data.domain.Page}
 * (formato JSON instável e cheio de campos internos) nem de entidades JPA: é um record simples,
 * com o que a UI precisa para desenhar um rodapé de paginação.
 *
 * @param items         os registos desta página, já convertidos em DTO
 * @param page          índice da página, a começar em 0
 * @param size          tamanho pedido
 * @param totalElements total de registos que a consulta encontrou
 * @param totalPages    total de páginas
 */
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    /** Há mais páginas a seguir a esta. */
    public boolean hasNext() {
        return page + 1 < totalPages;
    }

    /** Esta não é a primeira página. */
    public boolean hasPrevious() {
        return page > 0;
    }

    /** Converte uma página do Spring Data, aplicando o mapeamento para DTO. */
    public static <E, D> PageResponse<D> of(Page<E> page, Function<E, D> toDTO) {
        return new PageResponse<>(
                page.getContent().stream().map(toDTO).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    /** Página única a partir de uma lista já em memória (adopção incremental). */
    public static <T> PageResponse<T> single(List<T> items) {
        return new PageResponse<>(items, 0, items.size(), items.size(), 1);
    }
}
