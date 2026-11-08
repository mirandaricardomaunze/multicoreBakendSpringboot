package mz.multicore.erp.architecture.paging;

import java.util.List;

/**
 * Page of results exposed at the HTTP boundary.
 *
 * <p>Keeps the desktop independent of the server's internal pagination mechanism and persistent
 * entities. This simple record contains what the UI needs to render pagination controls.
 *
 * @param items         records on this page, already converted to DTOs
 * @param page          zero-based page index
 * @param size          requested page size
 * @param totalElements total records found by the query
 * @param totalPages    total number of pages
 */
public record PageResponse<T>(
        List<T> items,
        int page,
        int size,
        long totalElements,
        int totalPages
) {

    /** Returns whether another page follows this one. */
    public boolean hasNext() {
        return page + 1 < totalPages;
    }

    /** Returns whether this is not the first page. */
    public boolean hasPrevious() {
        return page > 0;
    }

    /** Creates a single page from an in-memory list for incremental adoption. */
    public static <T> PageResponse<T> single(List<T> items) {
        return new PageResponse<>(items, 0, items.size(), items.size(), 1);
    }
}
