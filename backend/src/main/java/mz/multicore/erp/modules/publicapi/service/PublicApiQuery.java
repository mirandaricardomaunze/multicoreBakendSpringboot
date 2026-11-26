package mz.multicore.erp.modules.publicapi.service;

import mz.multicore.erp.architecture.exception.BusinessRuleException;
import mz.multicore.erp.modules.publicapi.dto.PublicPage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.List;

/** Query parameters shared by the public API lists: pagination and ISO 8601 instants (§3.2, §3.3.1). */
final class PublicApiQuery {

    static final int DEFAULT_SIZE = 50;
    static final int MAX_SIZE = 100;

    private PublicApiQuery() {
    }

    /** Page {@code page} (from 0) of {@code all}; {@code size} defaults to 50 and is capped at 100. */
    static <T> PublicPage<T> page(List<T> all, Integer page, Integer size) {
        int effectiveSize = size == null || size < 1 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        int effectivePage = page == null || page < 0 ? 0 : page;
        long from = (long) effectivePage * effectiveSize;
        List<T> items = from >= all.size() ? List.of()
                : all.subList((int) from, (int) Math.min(all.size(), from + effectiveSize));
        int totalPages = (int) ((all.size() + effectiveSize - 1) / effectiveSize);
        return new PublicPage<>(items, effectivePage, effectiveSize, all.size(), totalPages);
    }

    /**
     * An ISO 8601 date, local date-time or offset date-time as a server-local date-time; {@code null} when absent.
     *
     * @throws BusinessRuleException when the value is not ISO 8601
     */
    static LocalDateTime instant(String name, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.trim();
        try {
            return OffsetDateTime.parse(text).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
        } catch (DateTimeParseException ignored) {
            // not an offset date-time; try the local forms below
        }
        try {
            return LocalDateTime.parse(text);
        } catch (DateTimeParseException ignored) {
            // not a local date-time; try a plain date
        }
        try {
            return LocalDate.parse(text).atStartOfDay();
        } catch (DateTimeParseException e) {
            throw new BusinessRuleException("O parâmetro " + name
                    + " tem de ser uma data ISO 8601 (ex.: 2026-11-01 ou 2026-11-01T08:00:00+02:00).");
        }
    }
}
