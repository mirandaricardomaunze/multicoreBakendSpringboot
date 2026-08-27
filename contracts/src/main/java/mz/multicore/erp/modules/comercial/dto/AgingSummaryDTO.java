package mz.multicore.erp.modules.comercial.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Mapa de antiguidade de saldos (<i>aging</i>) à data de referência.
 *
 * @param referenceDate data a que os atrasos foram calculados (o "hoje" do servidor)
 * @param buckets       totais por escalão, sempre com os cinco escalões pela ordem da escala
 * @param clients       repartição por cliente, do maior devedor para o menor
 * @param total         tudo o que está por receber (corrente + em atraso)
 * @param overdueTotal  só o que já passou do vencimento
 */
public record AgingSummaryDTO(
        LocalDate referenceDate,
        List<AgingBucketTotalDTO> buckets,
        List<ClientAgingDTO> clients,
        BigDecimal total,
        BigDecimal overdueTotal
) {}
