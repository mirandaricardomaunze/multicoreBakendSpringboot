package mz.multicore.erp.modules.publicapi.dto;

import java.util.List;

/** One page of a public API list (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.3.1): {@code page} starts at 0. */
public record PublicPage<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

    public PublicPage {
        items = List.copyOf(items);
    }
}
