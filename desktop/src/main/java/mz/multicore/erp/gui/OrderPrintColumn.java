package mz.multicore.erp.gui;

import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.OrderDTO;
import mz.multicore.erp.modules.printing.dto.DocumentPrintStatusDTO;
import mz.multicore.erp.modules.printing.dto.SensitiveDocumentType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * "Impressões" column of the orders list. An order is printed in two ways: the picking sheet, counted on the
 * order itself, and the official A4 copy, counted by the sensitive print control. The column shows both
 * together; before this it only counted picking sheets, so A4 orders always showed "—".
 */
final class OrderPrintColumn {

    static final int COLUMN = 5;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private OrderPrintColumn() {
    }

    /** Loads the official copies of the listed orders and rewrites the column; keeps the order count on failure. */
    static void refresh(ComercialPanel owner, List<OrderDTO> orders) {
        if (owner.printApiClient == null || orders == null || orders.isEmpty()) return;
        List<Long> ids = orders.stream().map(OrderDTO::id).toList();
        UIHelper.loadAsync(owner, () -> owner.printApiClient.getPrintStatuses(SensitiveDocumentType.ORDER, ids),
                statuses -> apply(owner, orders, statuses), error -> { });
    }

    static void apply(ComercialPanel owner, List<OrderDTO> orders, List<DocumentPrintStatusDTO> statuses) {
        Map<Long, DocumentPrintStatusDTO> official = statuses.stream()
                .collect(Collectors.toMap(DocumentPrintStatusDTO::documentId, s -> s, (a, b) -> a));
        Map<Long, OrderDTO> byId = orders.stream().collect(Collectors.toMap(OrderDTO::id, o -> o, (a, b) -> a));
        for (int row = 0; row < owner.ordersTableModel.getRowCount(); row++) {
            OrderDTO order = byId.get((Long) owner.ordersTableModel.getValueAt(row, 0));
            if (order == null) continue;
            owner.ordersTableModel.setValueAt(label(order.printCount(), order.printedAt(), official.get(order.id())), row, COLUMN);
        }
    }

    /** Total prints and the latest print date, e.g. "3 × 14/10/2026", or "Não impressa". */
    static String label(int pickingPrints, LocalDateTime pickingPrintedAt, DocumentPrintStatusDTO official) {
        int picking = Math.max(0, pickingPrints);
        int a4 = official == null ? 0 : Math.max(0, official.printCount());
        int total = picking + a4;
        if (total == 0) return "Não impressa";
        LocalDateTime last = latest(pickingPrintedAt, official == null ? null : official.printedAt());
        return last == null ? String.valueOf(total) : total + " × " + last.format(DATE);
    }

    private static LocalDateTime latest(LocalDateTime a, LocalDateTime b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.isAfter(b) ? a : b;
    }
}
