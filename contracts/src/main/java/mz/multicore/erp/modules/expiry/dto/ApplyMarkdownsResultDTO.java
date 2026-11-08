package mz.multicore.erp.modules.expiry.dto;

import java.util.List;

/** Markdowns created by the rules, and the lots that already had at least that discount. */
public record ApplyMarkdownsResultDTO(int created, int alreadyMarkedDown, List<String> products) {}
