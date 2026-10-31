package mz.multicore.erp.gui.components;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class TableFilterMatcher {

    private TableFilterMatcher() {}

    static boolean rowMatches(List<String> cells, String search, Map<Integer, String> exactByColumn) {
        if (search != null && !search.isBlank()) {
            String q = normalizeText(search);
            boolean any = false;
            for (String c : cells) {
                if (c != null) {
                    String cNorm = normalizeText(c);
                    String hNorm = normalizeText(UIHelper.humanStatus(c));
                    if (cNorm.contains(q) || hNorm.contains(q)) {
                        any = true;
                        break;
                    }
                }
            }
            if (!any) return false;
        }
        if (exactByColumn != null) {
            for (Map.Entry<Integer, String> en : exactByColumn.entrySet()) {
                String want = en.getValue();
                if (want == null || want.isBlank()) continue;
                int col = en.getKey();
                String cell = (col >= 0 && col < cells.size()) ? cells.get(col) : "";
                if (!cellMatches(cell, want)) return false;
            }
        }
        return true;
    }

    static boolean cellMatches(String cell, String want) {
        if (want == null || want.isBlank() || want.startsWith("Todos") || want.startsWith("Todas") || want.equalsIgnoreCase("Todos")) {
            return true;
        }
        if (cell == null) cell = "";
        cell = cell.trim();
        want = want.trim();
        if (want.equalsIgnoreCase(cell)) return true;

        String cellNorm = normalizeText(cell);
        String wantNorm = normalizeText(want);
        if (cellNorm.equalsIgnoreCase(wantNorm)) return true;

        String cellHuman = UIHelper.humanStatus(cell);
        if (want.equalsIgnoreCase(cellHuman) || wantNorm.equalsIgnoreCase(normalizeText(cellHuman))) return true;

        String wantHuman = UIHelper.humanStatus(want);
        if (cell.equalsIgnoreCase(wantHuman) || cellNorm.equalsIgnoreCase(normalizeText(wantHuman))) return true;

        if (cellHuman.equalsIgnoreCase(wantHuman) || normalizeText(cellHuman).equalsIgnoreCase(normalizeText(wantHuman))) return true;

        return ("PENDING".equalsIgnoreCase(cell) || "POR FACTURAR".equalsIgnoreCase(cellNorm) || "POR FATURAR".equalsIgnoreCase(cellNorm) || "PENDENTE".equalsIgnoreCase(cellNorm))
                && ("PENDENTE".equalsIgnoreCase(wantNorm) || "POR FACTURAR".equalsIgnoreCase(wantNorm) || "POR FATURAR".equalsIgnoreCase(wantNorm) || "PENDING".equalsIgnoreCase(want));
    }

    static String normalizeText(String s) {
        if (s == null) return "";
        return Normalizer.normalize(s.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
    }

    static boolean matchesDateRange(LocalDate date, LocalDate from, LocalDate to) {
        if (from == null && to == null) return true;
        if (date == null) return false;
        return (from == null || !date.isBefore(from)) && (to == null || !date.isAfter(to));
    }

    static LocalDate parseCellDate(String cell) {
        if (cell == null || cell.length() < 10) return null;
        try {
            return LocalDate.parse(cell.substring(0, 10), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (RuntimeException ex) {
            return null;
        }
    }

    static boolean matchesPeriod(LocalDate date, String option, LocalDate today) {
        if (option == null || option.isBlank() || option.startsWith("Todo")) return true;
        if (date == null) return false;
        return switch (option) {
            case "Hoje" -> date.isEqual(today);
            case "Ontem" -> date.isEqual(today.minusDays(1));
            case "Esta semana" -> {
                LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                yield !date.isBefore(monday) && !date.isAfter(today);
            }
            case "Últimos 7 dias" -> !date.isBefore(today.minusDays(6)) && !date.isAfter(today);
            case "Este mês" -> date.getMonthValue() == today.getMonthValue() && date.getYear() == today.getYear() && !date.isAfter(today);
            case "Últimos 30 dias" -> !date.isBefore(today.minusDays(29)) && !date.isAfter(today);
            case "Este ano" -> date.getYear() == today.getYear() && !date.isAfter(today);
            default -> true;
        };
    }
}
