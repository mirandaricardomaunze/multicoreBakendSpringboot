package mz.multicore.erp.modules.financeira.service;

import mz.multicore.erp.architecture.format.DisplayLocale;
import mz.multicore.erp.modules.financeira.dto.CashFlowAlertDTO;
import mz.multicore.erp.modules.financeira.dto.CashFlowBucketDTO;
import mz.multicore.erp.modules.financeira.dto.CashFlowForecastDTO;
import mz.multicore.erp.modules.financeira.dto.CashFlowItemDTO;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Calcula os baldes temporais, trajectória acumulada de liquidez e alertas de fluxo de caixa. */
@Component
public class CashFlowProjectionCalculator {

    private record BucketDefinition(String code, String label) {}

    private static final List<BucketDefinition> BUCKETS = List.of(
            new BucketDefinition("OVERDUE", "Vencido"),
            new BucketDefinition("TODAY", "Hoje"),
            new BucketDefinition("DAYS_1_7", "1-7 Dias"),
            new BucketDefinition("DAYS_8_15", "8-15 Dias"),
            new BucketDefinition("DAYS_16_30", "16-30 Dias"),
            new BucketDefinition("DAYS_31_60", "31-60 Dias"),
            new BucketDefinition("DAYS_PLUS_60", "+60 Dias")
    );

    public CashFlowForecastDTO calculate(LocalDate today, CashFlowDataCollector.CollectedCashFlowData data) {
        Map<String, BigDecimal> inflowsByBucket = new HashMap<>();
        Map<String, BigDecimal> outflowsByBucket = new HashMap<>();
        for (BucketDefinition def : BUCKETS) {
            inflowsByBucket.put(def.code(), BigDecimal.ZERO);
            outflowsByBucket.put(def.code(), BigDecimal.ZERO);
        }

        for (CashFlowItemDTO r : data.receivables()) {
            inflowsByBucket.merge(r.bucketCode(), r.amount(), BigDecimal::add);
        }
        for (CashFlowItemDTO p : data.payables()) {
            outflowsByBucket.merge(p.bucketCode(), p.amount(), BigDecimal::add);
        }

        List<CashFlowBucketDTO> bucketDTOs = new ArrayList<>();
        BigDecimal cumulative = data.availableLiquidity();
        String firstDeficitBucket = null;
        BigDecimal maxDeficit = BigDecimal.ZERO;

        for (BucketDefinition def : BUCKETS) {
            BigDecimal in = inflowsByBucket.get(def.code());
            BigDecimal out = outflowsByBucket.get(def.code());
            BigDecimal net = in.subtract(out);
            cumulative = cumulative.add(net);

            if (cumulative.compareTo(BigDecimal.ZERO) < 0) {
                if (firstDeficitBucket == null) {
                    firstDeficitBucket = def.label();
                }
                BigDecimal deficit = cumulative.abs();
                if (deficit.compareTo(maxDeficit) > 0) {
                    maxDeficit = deficit;
                }
            }
            bucketDTOs.add(new CashFlowBucketDTO(def.code(), def.label(), in, out, net, cumulative));
        }

        CashFlowAlertDTO alert = buildAlert(firstDeficitBucket, maxDeficit);

        List<CashFlowItemDTO> topReceivables = data.receivables().stream()
                .sorted((a, b) -> b.amount().compareTo(a.amount()))
                .limit(10)
                .collect(Collectors.toList());

        List<CashFlowItemDTO> topPayables = data.payables().stream()
                .sorted((a, b) -> b.amount().compareTo(a.amount()))
                .limit(10)
                .collect(Collectors.toList());

        return new CashFlowForecastDTO(
                today,
                data.cashBalance(),
                data.bankBalance(),
                data.availableLiquidity(),
                data.totalReceivables(),
                data.totalPayables(),
                cumulative,
                bucketDTOs,
                topReceivables,
                topPayables,
                alert
        );
    }

    private CashFlowAlertDTO buildAlert(String firstDeficitBucket, BigDecimal maxDeficit) {
        if (firstDeficitBucket == null) {
            return new CashFlowAlertDTO(
                    "HEALTHY",
                    "Posição de liquidez saudável sem projeção de défice no horizonte de 60 dias.",
                    null,
                    BigDecimal.ZERO,
                    "Manter ritmo corrente de cobranças e conciliações bancárias periódicas."
            );
        }
        boolean isImmediate = "Vencido".equals(firstDeficitBucket) || "Hoje".equals(firstDeficitBucket)
                || "1-7 Dias".equals(firstDeficitBucket) || "8-15 Dias".equals(firstDeficitBucket);
        String status = isImmediate ? "CRITICAL" : "WARNING";
        String msg = String.format(DisplayLocale.NUMBERS, "Atenção: Projeção de défice de caixa de %,.2f MT com início no período '%s'.",
                maxDeficit, firstDeficitBucket);
        String rec = isImmediate
                ? "Acelerar cobrança urgente de faturas vencidas e renegociar pagamentos imediatos a fornecedores."
                : "Planear reforço de fundo de maneio ou antecipação de recebimentos a médio prazo.";
        return new CashFlowAlertDTO(status, msg, firstDeficitBucket, maxDeficit, rec);
    }
}
