package mz.multicore.erp.architecture.events;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Uma retenção da folha foi entregue ao Estado.
 *
 * <p>Fecha o par do {@link PayslipPaidEvent}: aquele <b>cria</b> a dívida ao Estado no razão, este
 * <b>liquida-a</b>. Sem o segundo, a conta de retenções a entregar crescia para sempre e o
 * balancete deixava de bater com a realidade — o dinheiro tinha saído da caixa e a dívida continuava
 * lá.
 *
 * @param liabilityType IRPS · INSS_TRABALHADOR · INSS_PATRONAL
 */
public record PayrollLiabilityDeliveredEvent(
        Long companyId,
        Long liabilityId,
        String liabilityType,
        int year,
        int month,
        LocalDate date,
        BigDecimal amount,
        String paymentReference
) {}
