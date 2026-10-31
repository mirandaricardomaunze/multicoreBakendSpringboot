package mz.multicore.erp.gui.components;

import mz.multicore.erp.modules.audit.dto.ForensicAuditSummaryDTO;
import mz.multicore.erp.modules.comercial.dto.CreditRiskSummaryDTO;
import mz.multicore.erp.modules.financeira.dto.CashFlowAlertDTO;
import mz.multicore.erp.modules.financeira.dto.CashFlowForecastDTO;
import mz.multicore.erp.modules.performance.dto.SalesGoalProgressDTO;

import javax.swing.JLabel;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

final class StrategicPulseDataPresenter {

    private final DecimalFormat currencyFormatter;
    private final DecimalFormat percentFormatter;

    StrategicPulseDataPresenter() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("pt", "MZ"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        this.currencyFormatter = new DecimalFormat("#,##0.00 MT", symbols);
        this.percentFormatter = new DecimalFormat("0.0'%'", symbols);
    }

    void applyForensic(ForensicAuditSummaryDTO forensic, JLabel scoreLabel, JLabel subLabel) {
        if (forensic != null) {
            String scoreStr = forensic.complianceScore() != null ? forensic.complianceScore() : "100.0%";
            scoreLabel.setText(scoreStr);
            if (scoreStr.contains("100") || scoreStr.contains("CONFORME")) {
                scoreLabel.setForeground(UIHelper.APPROVED_GREEN);
            } else if (scoreStr.contains("9") || scoreStr.contains("8")) {
                scoreLabel.setForeground(UIHelper.PENDING_YELLOW);
            } else {
                scoreLabel.setForeground(UIHelper.REJECTED_RED);
            }
            int crit = forensic.criticalCount();
            int susp = forensic.suspiciousCount();
            if (crit == 0 && susp == 0) {
                subLabel.setText("Nenhuma anomalia detetada");
                subLabel.setForeground(UIHelper.APPROVED_GREEN);
            } else {
                subLabel.setText(crit + " críticas · " + susp + " suspeitas");
                subLabel.setForeground(crit > 0 ? UIHelper.REJECTED_RED : UIHelper.PENDING_YELLOW);
            }
        } else {
            scoreLabel.setText("100.0%");
            scoreLabel.setForeground(UIHelper.APPROVED_GREEN);
            subLabel.setText("Sem anomalias ativas");
            subLabel.setForeground(UIHelper.TEXT_MUTED);
        }
    }

    void applyLiquidity(CashFlowForecastDTO forecast, JLabel valLabel, JLabel subLabel) {
        if (forecast != null) {
            BigDecimal proj = forecast.netProjectedPosition() != null ? forecast.netProjectedPosition() : BigDecimal.ZERO;
            valLabel.setText(currencyFormatter.format(proj));
            valLabel.setForeground(proj.compareTo(BigDecimal.ZERO) >= 0 ? UIHelper.APPROVED_GREEN : UIHelper.REJECTED_RED);
            CashFlowAlertDTO alert = forecast.alert();
            if (alert != null && "CRITICAL".equalsIgnoreCase(alert.status())) {
                subLabel.setText("Alerta: Défice de tesouraria previsto");
                subLabel.setForeground(UIHelper.REJECTED_RED);
            } else {
                subLabel.setText("Posição líquida a 30 dias estável");
                subLabel.setForeground(UIHelper.TEXT_MUTED);
            }
        } else {
            valLabel.setText("0,00 MT");
            valLabel.setForeground(UIHelper.TEXT_LIGHT);
            subLabel.setText("Sem projeção disponível");
            subLabel.setForeground(UIHelper.TEXT_MUTED);
        }
    }

    void applyCreditRisk(CreditRiskSummaryDTO credit, JLabel valLabel, JLabel subLabel) {
        if (credit != null) {
            BigDecimal overdue = credit.totalOverdue() != null ? credit.totalOverdue() : BigDecimal.ZERO;
            valLabel.setText(currencyFormatter.format(overdue));
            valLabel.setForeground(overdue.compareTo(BigDecimal.ZERO) > 0 ? UIHelper.PENDING_YELLOW : UIHelper.APPROVED_GREEN);
            int atRisk = credit.criticalRiskCount() + credit.highRiskCount();
            subLabel.setText(atRisk + " clientes em mora de crédito");
            subLabel.setForeground(atRisk > 0 ? UIHelper.PENDING_YELLOW : UIHelper.TEXT_MUTED);
        } else {
            valLabel.setText("0,00 MT");
            valLabel.setForeground(UIHelper.APPROVED_GREEN);
            subLabel.setText("0 clientes em mora crítica");
            subLabel.setForeground(UIHelper.TEXT_MUTED);
        }
    }

    void applyGoals(List<SalesGoalProgressDTO> goals, JLabel valLabel, JLabel subLabel) {
        if (goals != null && !goals.isEmpty()) {
            BigDecimal totalTarget = BigDecimal.ZERO;
            BigDecimal totalAchieved = BigDecimal.ZERO;
            for (SalesGoalProgressDTO g : goals) {
                if (g.targetRevenue() != null) totalTarget = totalTarget.add(g.targetRevenue());
                if (g.currentRevenue() != null) totalAchieved = totalAchieved.add(g.currentRevenue());
            }
            BigDecimal pct = totalTarget.compareTo(BigDecimal.ZERO) > 0
                    ? totalAchieved.multiply(BigDecimal.valueOf(100)).divide(totalTarget, 1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            valLabel.setText(percentFormatter.format(pct));
            valLabel.setForeground(pct.compareTo(BigDecimal.valueOf(100)) >= 0 ? UIHelper.APPROVED_GREEN : UIHelper.TEXT_LIGHT);
            subLabel.setText(currencyFormatter.format(totalAchieved) + " de " + currencyFormatter.format(totalTarget));
            subLabel.setForeground(UIHelper.TEXT_MUTED);
        } else {
            valLabel.setText("100.0%");
            valLabel.setForeground(UIHelper.APPROVED_GREEN);
            subLabel.setText("Sem metas ativas no ciclo");
            subLabel.setForeground(UIHelper.TEXT_MUTED);
        }
    }
}
