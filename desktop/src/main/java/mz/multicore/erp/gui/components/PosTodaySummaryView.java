package mz.multicore.erp.gui.components;

import mz.multicore.erp.architecture.security.CurrentUserContext;
import mz.multicore.erp.modules.comercial.dto.POSSalesSummaryDTO;
import mz.multicore.erp.desktop.client.ComercialApiClient;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.Consumer;

/**
 * Cartões de resumo KPI de vendas diárias do ponto de venda (POS).
 * Apresenta quantidade, total monetário e variação percentual vs o dia anterior.
 */
public class PosTodaySummaryView {

    private final ComercialApiClient comercialApiClient;
    private final Consumer<Throwable> errorHandler;
    private final JPanel container;

    private JLabel todaySalesCountValue;
    private JLabel todaySalesCountSub;
    private JLabel todaySalesTotalValue;
    private JLabel todaySalesTotalSub;
    private JLabel todaySalesVariationValue;
    private JLabel todaySalesVariationSub;

    public PosTodaySummaryView(ComercialApiClient comercialApiClient, Consumer<Throwable> errorHandler) {
        this.comercialApiClient = comercialApiClient;
        this.errorHandler = errorHandler;
        this.container = buildBar();
    }

    public JPanel getComponent() {
        return container;
    }

    private JPanel buildBar() {
        todaySalesCountValue = KpiCard.valueLabel("0", 22);
        todaySalesCountSub = kpiSubLabel("Ontem: 0 vendas");
        todaySalesTotalValue = KpiCard.valueLabel("0,00 MT", 22);
        todaySalesTotalSub = kpiSubLabel("Ontem: 0,00 MT");
        todaySalesVariationValue = KpiCard.valueLabel("—", 22);
        todaySalesVariationSub = kpiSubLabel("Receita vs ontem");

        JPanel bar = new JPanel(new GridLayout(1, 3, 10, 0));
        bar.setOpaque(false);
        bar.add(KpiCard.create("Vendas hoje", "fas-receipt", Color.WHITE, todaySalesCountValue,
                todaySalesCountSub, UIHelper.ACCENT_BLUE, UIHelper.ACCENT_BLUE_HOVER));
        bar.add(KpiCard.create("Total hoje", "fas-cash-register", Color.WHITE, todaySalesTotalValue,
                todaySalesTotalSub, UIHelper.APPROVED_GREEN, UIHelper.APPROVED_GREEN_HOVER));
        bar.add(KpiCard.create("Variação", "fas-chart-line", Color.WHITE, todaySalesVariationValue,
                todaySalesVariationSub, UIHelper.ACCENT, UIHelper.ACCENT_HOVER));
        return bar;
    }

    private JLabel kpiSubLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font(UIHelper.FONT, Font.PLAIN, 11));
        label.setForeground(UIHelper.TEXT_MUTED);
        return label;
    }

    public void loadTodaySalesSummary(JPanel hostPanel) {
        if (todaySalesCountValue == null) return;
        Long companyId = CurrentUserContext.getCurrentCompanyId();
        LocalDate today = LocalDate.now();
        UIHelper.loadAsync(hostPanel, () -> comercialApiClient.getPOSSalesSummary(companyId, today, today),
                this::applyTodaySalesSummary,
                errorHandler);
    }

    public void applyTodaySalesSummary(POSSalesSummaryDTO summary) {
        if (summary == null || todaySalesCountValue == null) return;
        todaySalesCountValue.setText(String.valueOf(summary.count()));
        todaySalesTotalValue.setText(String.format("%,.2f MT", safe(summary.totalAmount())));
        todaySalesCountSub.setText(summary.previousCount() == null
                ? "Sem comparativo"
                : variationText(summary.countVariationPercent(), summary.previousCount() + " vendas ontem"));
        todaySalesTotalSub.setText(summary.previousTotalAmount() == null
                ? "Sem comparativo"
                : variationText(summary.totalVariationPercent(),
                        String.format("%,.2f MT ontem", safe(summary.previousTotalAmount()))));
        todaySalesVariationValue.setText(summary.totalVariationPercent() == null
                ? "Novo"
                : signedPercent(summary.totalVariationPercent()));
        todaySalesVariationSub.setText("Receita vs ontem");
    }

    private String variationText(BigDecimal percent, String fallback) {
        return percent == null ? fallback : signedPercent(percent) + " · " + fallback;
    }

    private String signedPercent(BigDecimal value) {
        String sign = value.signum() > 0 ? "+" : "";
        return sign + value.stripTrailingZeros().toPlainString() + "%";
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
