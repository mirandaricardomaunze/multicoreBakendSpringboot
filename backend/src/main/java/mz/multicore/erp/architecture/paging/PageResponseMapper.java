package mz.multicore.erp.architecture.paging;

import org.springframework.data.domain.Page;

import java.util.function.Function;

/** Adapta a paginação interna do Spring Data ao contrato HTTP independente de framework. */
public final class PageResponseMapper {

    private PageResponseMapper() {
    }

    public static <E, D> PageResponse<D> from(Page<E> page, Function<E, D> toDTO) {
        return new PageResponse<>(
                page.getContent().stream().map(toDTO).toList(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
