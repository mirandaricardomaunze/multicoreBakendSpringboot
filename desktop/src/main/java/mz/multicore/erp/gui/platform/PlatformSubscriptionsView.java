package mz.multicore.erp.gui.platform;

import mz.multicore.erp.desktop.client.PlatformApiClient;
import mz.multicore.erp.gui.components.FeedbackType;
import mz.multicore.erp.gui.components.KpiCard;
import mz.multicore.erp.gui.components.ModernButton;
import mz.multicore.erp.gui.components.ModernMessageDialog;
import mz.multicore.erp.gui.components.ModernPanel;
import mz.multicore.erp.gui.components.SimplePieChart;
import mz.multicore.erp.gui.components.TableFilter;
import mz.multicore.erp.gui.components.UIHelper;
import mz.multicore.erp.modules.subscription.dto.SubscriptionDTO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Vista de gestão de assinaturas, planos, métricas financeiras (MRR) e histórico de pagamentos.
 */
public class PlatformSubscriptionsView extends JPanel {

    private final PlatformApiClient platformApiClient;
    private final PlatformFeedbackHandler feedbackHandler;

    private DefaultTableModel subsModel;
    private JTable subsTable;
    private List<SubscriptionDTO> subscriptions = new ArrayList<>();

    private JLabel subMrrLabel;
    private JLabel subActiveLabel;
    private JLabel subRiskLabel;
    private JLabel subPaymentsLabel;
    private SimplePieChart subPlansChart;

    public PlatformSubscriptionsView(PlatformApiClient platformApiClient, PlatformFeedbackHandler feedbackHandler) {
        this.platformApiClient = platformApiClient;
        this.feedbackHandler = feedbackHandler;

        setLayout(new BorderLayout(0, 12));
        setBackground(UIHelper.BG_DARK);
        setBorder(new EmptyBorder(15, 15, 15, 15));

        ModernButton payBtn = UIHelper.createSuccessButton("Registar Pagamento");
        payBtn.setIcon(UIHelper.icon("fas-money-bill-wave", 14));
        payBtn.addActionListener(e -> recordPayment());

        ModernButton refreshBtn = UIHelper.createRefreshButton(this::loadSubscriptions);
        mz.multicore.erp.gui.components.ActionMenuButton moreBtn = UIHelper.createActionMenuButton("Mais acções")
                .addAction("Definir Plano/Validade", UIHelper.icon("fas-sliders-h", 14), this::defineSubscription)
                .addAction("Ver Pagamentos", UIHelper.icon("fas-receipt", 14), this::showPayments)
                .addAction("Suspender/Reactivar", UIHelper.icon("fas-power-off", 14), this::toggleSubscriptionStatus);

        // Barra Executiva de KPIs de Subscrições
        JPanel kpiGrid = KpiCard.createGrid(4);
        subMrrLabel = new JLabel("0,00 MT");
        subActiveLabel = new JLabel("0");
        subRiskLabel = new JLabel("0");
        subPaymentsLabel = new JLabel("0");

        kpiGrid.add(KpiCard.createMetricCard("Receita Recorrente (MRR)", subMrrLabel, "Faturação mensal das assinaturas", "fas-money-bill-wave", UIHelper.APPROVED_GREEN));
        kpiGrid.add(KpiCard.createMetricCard("Assinaturas Activas", subActiveLabel, "Empresas regularizadas", "fas-check-circle", UIHelper.ACCENT_BLUE));
        kpiGrid.add(KpiCard.createMetricCard("Em Risco / Expiradas", subRiskLabel, "≤ 7 dias ou vencidas", "fas-exclamation-triangle", UIHelper.REJECTED_RED));
        kpiGrid.add(KpiCard.createMetricCard("Pagamentos Registados", subPaymentsLabel, "Histórico liquidado", "fas-receipt", UIHelper.PENDING_YELLOW));

        add(kpiGrid, BorderLayout.NORTH);

        ModernPanel listCard = new ModernPanel(16);
        listCard.setLayout(new BorderLayout());
        listCard.setBorder(new EmptyBorder(15, 15, 15, 15));

        String[] cols = {"Empresa", "Plano", "Estado", "Válida até", "Preço/mês", "Nº Pagamentos"};
        subsModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        subsTable = new JTable(subsModel);
        UIHelper.styleTable(subsTable);
        UIHelper.ensureHeadersFit(subsTable);

        TableCellRenderer baseRenderer = subsTable.getDefaultRenderer(Object.class);
        subsTable.setDefaultRenderer(Object.class, (table, value, sel, focus, row, col) -> {
            Component c = baseRenderer.getTableCellRendererComponent(table, value, sel, focus, row, col);
            int modelRow = row >= 0 ? subsTable.convertRowIndexToModel(row) : -1;
            if (!sel && modelRow >= 0 && modelRow < subscriptions.size()) {
                int sev = subSeverity(subscriptions.get(modelRow));
                if (sev == -1) c.setForeground(UIHelper.REJECTED_RED);
                else if (sev == 0) c.setForeground(UIHelper.PENDING_YELLOW);
            }
            return c;
        });

        JScrollPane scroll = new JScrollPane(subsTable);
        UIHelper.styleScrollPane(scroll);
        JTextField sSearch = TableFilter.searchField("Empresa…");
        JComboBox<String> sEstado = TableFilter.combo("Todos os estados", "Activa", "Suspensa", "Expirada", "Avaliação", "Sem assinatura");
        JComboBox<String> sPlano = TableFilter.combo("Todos os planos", "Avaliação", "Básico", "Profissional", "Empresarial");
        TableFilter.install(subsTable, sSearch, new TableFilter.ColumnFilter(sEstado, 2), new TableFilter.ColumnFilter(sPlano, 1));
        JPanel sBar = TableFilter.bar(sSearch, TableFilter.label("Estado:"), sEstado, TableFilter.label("Plano:"), sPlano);
        sBar.setBorder(new EmptyBorder(0, 0, 10, 0));

        listCard.add(UIHelper.tableCardTop("Subscrições das Empresas", sBar, refreshBtn, moreBtn, payBtn), BorderLayout.NORTH);
        listCard.add(scroll, BorderLayout.CENTER);

        ModernPanel chartCard = new ModernPanel(16);
        chartCard.setLayout(new BorderLayout());
        chartCard.setBorder(new EmptyBorder(12, 14, 12, 14));
        chartCard.setPreferredSize(new Dimension(280, 0));
        subPlansChart = new SimplePieChart("Distribuição por Plano", true);
        chartCard.add(subPlansChart, BorderLayout.CENTER);

        JPanel centerSplit = new JPanel(new BorderLayout(12, 0));
        centerSplit.setOpaque(false);
        centerSplit.add(listCard, BorderLayout.CENTER);
        centerSplit.add(chartCard, BorderLayout.EAST);

        add(centerSplit, BorderLayout.CENTER);
    }

    public void loadSubscriptions() {
        UIHelper.loadAsync(this, platformApiClient::listOverview, loaded -> {
            subscriptions = loaded;
            subsModel.setRowCount(0);
            BigDecimal totalMrr = BigDecimal.ZERO;
            int activeCount = 0;
            int riskCount = 0;
            long totalPayments = 0;

            Map<String, BigDecimal> planCountMap = new LinkedHashMap<>();
            planCountMap.put("TRIAL", BigDecimal.ZERO);
            planCountMap.put("BASIC", BigDecimal.ZERO);
            planCountMap.put("PRO", BigDecimal.ZERO);
            planCountMap.put("ENTERPRISE", BigDecimal.ZERO);
            planCountMap.put("Sem Plano", BigDecimal.ZERO);

            for (SubscriptionDTO s : subscriptions) {
                subsModel.addRow(new Object[]{
                        s.companyName(), s.planLabel(), s.statusLabel(),
                        s.validUntil() == null ? "—" : s.validUntil().toString(),
                        s.monthlyPrice() == null ? "—" : s.monthlyPrice().toPlainString(),
                        s.paymentCount()
                });

                if (s.hasSubscription()) {
                    if ("ACTIVE".equals(s.status())) {
                        activeCount++;
                        if (s.monthlyPrice() != null) totalMrr = totalMrr.add(s.monthlyPrice());
                    }
                    if (subSeverity(s) <= 0) riskCount++;
                    totalPayments += s.paymentCount();

                    String pKey = s.plan() != null ? s.plan() : "Sem Plano";
                    planCountMap.put(pKey, planCountMap.getOrDefault(pKey, BigDecimal.ZERO).add(BigDecimal.ONE));
                } else {
                    planCountMap.put("Sem Plano", planCountMap.getOrDefault("Sem Plano", BigDecimal.ZERO).add(BigDecimal.ONE));
                }
            }

            if (subMrrLabel != null) subMrrLabel.setText(String.format("%,.2f MT", totalMrr));
            if (subActiveLabel != null) subActiveLabel.setText(activeCount + " / " + subscriptions.size());
            if (subRiskLabel != null) {
                subRiskLabel.setText(String.valueOf(riskCount));
                subRiskLabel.setForeground(riskCount > 0 ? UIHelper.REJECTED_RED : UIHelper.APPROVED_GREEN);
            }
            if (subPaymentsLabel != null) subPaymentsLabel.setText(String.valueOf(totalPayments));

            if (subPlansChart != null) {
                List<String> pLabels = new ArrayList<>();
                List<BigDecimal> pValues = new ArrayList<>();
                for (Map.Entry<String, BigDecimal> entry : planCountMap.entrySet()) {
                    if (entry.getValue().compareTo(BigDecimal.ZERO) > 0) {
                        pLabels.add(entry.getKey());
                        pValues.add(entry.getValue());
                    }
                }
                subPlansChart.setData(pLabels.toArray(new String[0]), pValues.toArray(new BigDecimal[0]), null);
            }
        }, error -> feedbackHandler.showError("assinaturas", error, this::loadSubscriptions));
    }

    private static int subSeverity(SubscriptionDTO s) {
        if (!s.hasSubscription()) return 1;
        if ("EXPIRED".equals(s.status()) || "SUSPENDED".equals(s.status())) return -1;
        if (s.validUntil() != null) {
            long d = ChronoUnit.DAYS.between(LocalDate.now(), s.validUntil());
            if (d >= 0 && d <= 7) return 0;
        }
        return 1;
    }

    private SubscriptionDTO selectedSubscription() {
        int row = TableFilter.selectedModelRow(subsTable);
        if (row < 0 || row >= subscriptions.size()) {
            feedbackHandler.showNotice(FeedbackType.WARNING, "Seleccione uma empresa", "Escolha uma empresa na lista para continuar.");
            return null;
        }
        return subscriptions.get(row);
    }

    private void defineSubscription() {
        SubscriptionDTO sub = selectedSubscription();
        if (sub == null) return;

        if (DefineSubscriptionDialog.show(UIHelper.mainWindow, platformApiClient, sub)) {
            feedbackHandler.showSuccess("Assinatura actualizada.");
            loadSubscriptions();
        }
    }

    private void recordPayment() {
        SubscriptionDTO sub = selectedSubscription();
        if (sub == null) return;

        RecordSubscriptionPaymentDialog dlg = new RecordSubscriptionPaymentDialog(UIHelper.mainWindow, platformApiClient, sub);
        if (dlg.showDialog()) {
            feedbackHandler.showSuccess("Pagamento registado.");
            loadSubscriptions();
        }
    }

    private void showPayments() {
        SubscriptionDTO sub = selectedSubscription();
        if (sub == null) return;

        UIHelper.loadAsync(this, () -> platformApiClient.listPayments(sub.companyId()),
                payments -> SubscriptionPaymentsHistoryDialog.show(UIHelper.mainWindow, sub, payments),
                error -> feedbackHandler.showError("pagamentos", error, this::showPayments));
    }

    private void toggleSubscriptionStatus() {
        SubscriptionDTO sub = selectedSubscription();
        if (sub == null) return;
        if (!sub.hasSubscription()) {
            feedbackHandler.showNotice(FeedbackType.WARNING, "Assinatura necessária", "Defina primeiro a assinatura desta empresa.");
            return;
        }

        boolean suspended = "SUSPENDED".equals(sub.status());
        String target = suspended ? "ACTIVE" : "SUSPENDED";
        String verb = suspended ? "reactivar" : "suspender";
        if (!ModernMessageDialog.confirm(SwingUtilities.getWindowAncestor(this),
                suspended ? FeedbackType.INFO : FeedbackType.WARNING,
                suspended ? "Reactivar assinatura" : "Suspender assinatura",
                "Deseja " + verb + " a assinatura de '" + sub.companyName() + "'?"
                        + (suspended ? "" : "\nOs utilizadores desta empresa deixam de poder iniciar sessão."),
                suspended ? "Reactivar" : "Suspender")) return;

        UIHelper.runWithProgress(this, "A actualizar assinatura…",
                () -> platformApiClient.changeSubscriptionStatus(sub.companyId(), target),
                ignored -> loadSubscriptions(),
                error -> feedbackHandler.showError("assinatura", error, this::loadSubscriptions));
    }
}
