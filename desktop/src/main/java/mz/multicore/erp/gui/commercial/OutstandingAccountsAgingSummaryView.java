package mz.multicore.erp.gui.commercial;

import mz.multicore.erp.gui.components.KpiCard;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.comercial.dto.AgingBucketTotalDTO;
import mz.multicore.erp.modules.comercial.dto.AgingSummaryDTO;
import mz.multicore.erp.modules.comercial.model.AgingBucket;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/** Resumo visual dos cinco escalões de dívida e do total vencido. */
final class OutstandingAccountsAgingSummaryView extends JPanel {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final JLabel referenceDate = new JLabel("Antiguidade da dívida");
    private final JLabel[] amounts = new JLabel[AgingBucket.values().length + 1];
    private final JLabel[] counts = new JLabel[AgingBucket.values().length + 1];

    OutstandingAccountsAgingSummaryView() {
        super(new BorderLayout(0, 5));
        setOpaque(false);
        setBorder(new EmptyBorder(2, 0, 3, 0));
        referenceDate.setFont(new Font(UIHelper.FONT, Font.BOLD, 11));
        referenceDate.setForeground(UIHelper.TEXT_MUTED);
        add(referenceDate, BorderLayout.NORTH);

        JPanel cards = KpiCard.createGrid(amounts.length, 8, 0);
        AgingBucket[] buckets = AgingBucket.values();
        for (int i = 0; i < amounts.length; i++) {
            amounts[i] = new JLabel("0,00 MT");
            counts[i] = new JLabel("0 faturas");
            String title = i == buckets.length ? "Em atraso" : shortLabel(buckets[i]);
            Color color = i == buckets.length ? UIHelper.REJECTED_RED :
                    i == 0 ? UIHelper.ACCENT_BLUE : UIHelper.PENDING_YELLOW;
            cards.add(KpiCard.createCard(title, amounts[i], null, counts[i], null, color));
        }
        add(cards, BorderLayout.CENTER);
    }

    void showSummary(AgingSummaryDTO summary) {
        if (summary == null) { clear(); return; }
        referenceDate.setText("Antiguidade da dívida a " + summary.referenceDate().format(DATE));
        for (int i = 0; i < AgingBucket.values().length; i++) {
            AgingBucket bucket = AgingBucket.values()[i];
            AgingBucketTotalDTO total = summary.buckets().stream()
                    .filter(item -> item.bucket() == bucket).findFirst().orElse(null);
            amounts[i].setText(UIHelper.formatMzn(total == null ? BigDecimal.ZERO : total.amount()));
            counts[i].setText(invoiceCount(total == null ? 0 : total.invoiceCount()));
        }
        int overdueIndex = amounts.length - 1;
        amounts[overdueIndex].setText(UIHelper.formatMzn(summary.overdueTotal()));
        counts[overdueIndex].setText("Total: " + UIHelper.formatMzn(summary.total()));
    }

    void clear() {
        referenceDate.setText("Antiguidade da dívida indisponível");
        for (JLabel amount : amounts) amount.setText("-");
        for (JLabel count : counts) count.setText(" ");
    }

    private static String shortLabel(AgingBucket bucket) {
        return switch (bucket) {
            case CORRENTE -> "Por vencer";
            case ATE_30 -> "1-30 dias";
            case DE_31_A_60 -> "31-60 dias";
            case DE_61_A_90 -> "61-90 dias";
            case MAIS_DE_90 -> "Mais de 90 dias";
        };
    }

    private static String invoiceCount(int count) {
        return count + (count == 1 ? " fatura" : " faturas");
    }
}
