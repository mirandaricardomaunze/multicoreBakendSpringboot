package mz.multicore.erp.modules.publicapi.dto;

/**
 * Result of {@code POST /api/public/v1/orders} (docs/API_PUBLICA_WEBHOOKS_SPEC.md §3.4.2): the order and whether this
 * call created it ({@code 201}) or replayed one already created with the same reference and content ({@code 200}).
 */
public record PublicOrderSubmission(PublicOrderDTO order, boolean created) {
}
